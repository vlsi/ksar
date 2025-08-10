"""
Parser service for handling SAR file parsing.

This module provides the service layer for parsing SAR files.
"""

import os
import uuid
from typing import Dict, Optional, List
from datetime import datetime
import logging

from ..parsers.base import BaseParser, ParsedData
from ..parsers.linux import LinuxParser
from ..models.time_series import TimeSeriesData, TimeSeriesCollection

logger = logging.getLogger(__name__)


class ParserService:
    """
    Service for parsing SAR files.
    
    This service handles the orchestration of parsing SAR files using the appropriate parser.
    """

    def __init__(self):
        """Initialize the parser service."""
        self.parsers: Dict[str, BaseParser] = {
            "Linux": LinuxParser,
        }
        self.parsed_files: Dict[str, ParsedData] = {}

    def detect_parser(self, header_line: str) -> Optional[str]:
        """
        Detect the appropriate parser based on the header line.
        
        Args:
            header_line: The first line of the SAR file
            
        Returns:
            Optional[str]: The name of the parser to use or None if not supported
        """
        if not header_line:
            return None
        
        # Check for Linux
        if header_line.startswith("Linux"):
            return "Linux"
        
        # Add more OS detection logic here as needed
        # if header_line.startswith("AIX"):
        #     return "AIX"
        # if header_line.startswith("HP-UX"):
        #     return "HPUX"
        # if header_line.startswith("SunOS"):
        #     return "Solaris"
        
        return None

    def parse_file(self, file_path: str, file_id: Optional[str] = None) -> Optional[ParsedData]:
        """
        Parse a SAR file and return the parsed data.
        
        Args:
            file_path: Path to the SAR file
            file_id: Optional file ID, will generate one if not provided
            
        Returns:
            Optional[ParsedData]: The parsed data or None if parsing failed
        """
        if not os.path.exists(file_path):
            logger.error(f"File not found: {file_path}")
            return None
        
        if file_id is None:
            file_id = str(uuid.uuid4())
        
        try:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                lines = f.readlines()
            
            if not lines:
                logger.error(f"Empty file: {file_path}")
                return None
            
            # Detect parser from header
            header_line = lines[0].strip()
            parser_name = self.detect_parser(header_line)
            
            if not parser_name:
                logger.error(f"Unsupported SAR format: {header_line}")
                return None
            
            if parser_name not in self.parsers:
                logger.error(f"Parser not implemented: {parser_name}")
                return None
            
            # Create parser instance
            parser_class = self.parsers[parser_name]
            parser = parser_class()
            
            # Parse header
            parser.parse_header(header_line)
            
            # Parse data lines
            for line_num, line in enumerate(lines[1:], start=2):
                line = line.strip()
                if not line:
                    continue
                
                columns = line.split()
                if not columns:
                    continue
                
                result = parser.parse_line(line, columns)
                if result == -1:
                    logger.warning(f"Error parsing line {line_num}: {line[:100]}...")
                elif result == 1:
                    logger.debug(f"Ignored line {line_num}: {line[:100]}...")
            
            # Get parsed data
            parsed_data = parser.get_parsed_data(file_id)
            
            # Store the parsed data
            self.parsed_files[file_id] = parsed_data
            
            logger.info(f"Successfully parsed file {file_path} as {file_id}")
            return parsed_data
            
        except Exception as e:
            logger.error(f"Error parsing file {file_path}: {e}")
            return None

    def get_parsed_file(self, file_id: str) -> Optional[ParsedData]:
        """
        Get parsed data for a specific file ID.
        
        Args:
            file_id: The file ID
            
        Returns:
            Optional[ParsedData]: The parsed data or None if not found
        """
        return self.parsed_files.get(file_id)

    def get_time_series_collection(self, file_id: str) -> Optional[TimeSeriesCollection]:
        """
        Get a TimeSeriesCollection for a specific file ID.
        
        Args:
            file_id: The file ID
            
        Returns:
            Optional[TimeSeriesCollection]: The time series collection or None if not found
        """
        parsed_data = self.get_parsed_file(file_id)
        if not parsed_data:
            return None
        
        # Create collection
        collection = TimeSeriesCollection(
            name=f"SAR Data - {file_id}",
            metadata={
                "file_id": file_id,
                "system_info": {
                    "os_type": parsed_data.system_info.os_type,
                    "hostname": parsed_data.system_info.hostname,
                    "kernel": parsed_data.system_info.kernel,
                    "nb_cpu": parsed_data.system_info.nb_cpu
                }
            }
        )
        
        # Add all series to collection
        for metric_name, metric_data in parsed_data.metrics.items():
            # Convert to TimeSeriesData
            time_series_data = TimeSeriesData(
                metric_name=metric_data.metric_name,
                timestamps=metric_data.timestamps,
                values=metric_data.values,
                columns=metric_data.columns,
                metadata=metric_data.metadata
            )
            collection.add_series(metric_name, time_series_data)
        
        return collection

    def list_parsed_files(self) -> List[Dict]:
        """
        List all parsed files with basic information.
        
        Returns:
            List[Dict]: List of parsed file information
        """
        files = []
        for file_id, parsed_data in self.parsed_files.items():
            files.append({
                "file_id": file_id,
                "os_type": parsed_data.system_info.os_type,
                "hostname": parsed_data.system_info.hostname,
                "start_time": parsed_data.start_time.isoformat() if parsed_data.start_time else None,
                "end_time": parsed_data.end_time.isoformat() if parsed_data.end_time else None,
                "metrics_count": len(parsed_data.metrics),
                "samples_count": len(parsed_data.date_samples)
            })
        return files

    def delete_parsed_file(self, file_id: str) -> bool:
        """
        Delete parsed data for a specific file ID.
        
        Args:
            file_id: The file ID
            
        Returns:
            bool: True if deleted, False if not found
        """
        if file_id in self.parsed_files:
            del self.parsed_files[file_id]
            logger.info(f"Deleted parsed file: {file_id}")
            return True
        return False

    def get_file_info(self, file_id: str) -> Optional[Dict]:
        """
        Get detailed information about a parsed file.
        
        Args:
            file_id: The file ID
            
        Returns:
            Optional[Dict]: File information or None if not found
        """
        parsed_data = self.get_parsed_file(file_id)
        if not parsed_data:
            return None
        
        return {
            "file_id": file_id,
            "system_info": {
                "os_type": parsed_data.system_info.os_type,
                "hostname": parsed_data.system_info.hostname,
                "kernel": parsed_data.system_info.kernel,
                "cpu_type": parsed_data.system_info.cpu_type,
                "os_version": parsed_data.system_info.os_version,
                "mac_address": parsed_data.system_info.mac_address,
                "memory": parsed_data.system_info.memory,
                "nb_disk": parsed_data.system_info.nb_disk,
                "nb_cpu": parsed_data.system_info.nb_cpu,
                "ent": parsed_data.system_info.ent
            },
            "time_range": {
                "start_time": parsed_data.start_time.isoformat() if parsed_data.start_time else None,
                "end_time": parsed_data.end_time.isoformat() if parsed_data.end_time else None
            },
            "metrics": {
                name: {
                    "metric_name": data.metric_name,
                    "columns": data.columns,
                    "data_points": len(data.timestamps),
                    "metadata": data.metadata
                }
                for name, data in parsed_data.metrics.items()
            },
            "samples_count": len(parsed_data.date_samples)
        } 