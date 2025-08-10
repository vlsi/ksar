"""
Base parser class for SAR file parsing.

This module provides the abstract base class that all SAR parsers must implement.
"""

import re
from abc import ABC, abstractmethod
from datetime import datetime, date, time
from typing import Dict, List, Optional, Set, Any
from dataclasses import dataclass, field
import logging

logger = logging.getLogger(__name__)


@dataclass
class SystemInfo:
    """System information extracted from SAR file header."""
    os_type: Optional[str] = None
    hostname: Optional[str] = None
    kernel: Optional[str] = None
    cpu_type: Optional[str] = None
    os_version: Optional[str] = None
    mac_address: Optional[str] = None
    memory: Optional[str] = None
    nb_disk: Optional[str] = None
    nb_cpu: Optional[str] = None
    ent: Optional[str] = None


@dataclass
class TimeSeriesData:
    """Time series data for a specific metric."""
    metric_name: str
    timestamps: List[datetime] = field(default_factory=list)
    values: List[float] = field(default_factory=list)
    columns: List[str] = field(default_factory=list)
    metadata: Dict[str, Any] = field(default_factory=dict)


@dataclass
class ParsedData:
    """Container for parsed SAR data."""
    file_id: str
    system_info: SystemInfo
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    metrics: Dict[str, TimeSeriesData] = field(default_factory=dict)
    date_samples: Set[datetime] = field(default_factory=set)


class BaseParser(ABC):
    """
    Abstract base class for SAR file parsers.
    
    This class provides the common functionality that all SAR parsers must implement.
    """

    # Date format regex patterns for automatic detection
    DATE_FORMAT_REGEXPS = {
        r"^\d{8}$": "%Y%m%d",
        r"^\d{1,2}-\d{1,2}-\d{4}$": "%d-%m-%Y",
        r"^\d{4}-\d{1,2}-\d{1,2}$": "%Y-%m-%d",
        r"^\d{1,2}/\d{1,2}/\d{4}$": "%m/%d/%Y",
        r"^\d{4}/\d{1,2}/\d{1,2}$": "%Y/%m/%d",
        r"^\d{1,2}\s[a-z]{3}\s\d{4}$": "%d %b %Y",
        r"^\d{1,2}\s[a-z]{4,}\s\d{4}$": "%d %B %Y",
        r"^\d{1,2}-\d{1,2}-\d{2}$": "%d-%m-%y",
        r"^\d{1,2}/\d{1,2}/\d{2}$": "%m/%d/%y",
    }

    def __init__(self):
        """Initialize the base parser."""
        self.parser_name: Optional[str] = None
        self.system_info = SystemInfo()
        self.sar_start_date: Optional[str] = None
        self.sar_end_date: Optional[str] = None
        self.start_of_graph: Optional[datetime] = None
        self.end_of_graph: Optional[datetime] = None
        self.date_samples: Set[datetime] = set()
        self.first_data_column: int = 0
        self.current_stat: str = "NONE"
        self.date_format: str = "%m/%d/%y"
        self.time_format: str = "%H:%M:%S"
        self.time_column: int = 1
        self.parse_date: Optional[date] = None
        self.parse_time: Optional[time] = None
        self.ignore_lines_beginning_with: Set[str] = set()

    @abstractmethod
    def parse_header(self, header_line: str) -> None:
        """
        Parse the header line of the SAR file.
        
        Args:
            header_line: The header line to parse
        """
        pass

    @abstractmethod
    def parse_line(self, line: str, columns: List[str]) -> int:
        """
        Parse a single line of SAR data.
        
        Args:
            line: The raw line to parse
            columns: The line split into columns
            
        Returns:
            int: Status code (0=success, -1=error, 1=ignore, 2=graph data, 3=no graph)
        """
        pass

    def set_start_and_end_of_graph(self, now_stat: datetime) -> None:
        """
        Update the start and end times of the graph data.
        
        Args:
            now_stat: Current datetime from the parsed line
        """
        if self.start_of_graph is None:
            self.start_of_graph = now_stat
        if self.end_of_graph is None:
            self.end_of_graph = now_stat

        if now_stat < self.start_of_graph:
            self.start_of_graph = now_stat
        if now_stat > self.end_of_graph:
            self.end_of_graph = now_stat

    def set_date(self, date_str: str) -> bool:
        """
        Parse and set the date from a string.
        
        Args:
            date_str: Date string to parse
            
        Returns:
            bool: True if successful, False otherwise
        """
        if self.sar_start_date is None:
            self.sar_start_date = date_str
        if self.sar_end_date is None:
            self.sar_end_date = date_str

        try:
            if self.date_format == "Automatic Detection":
                detected_format = self.determine_date_format(date_str)
                if detected_format:
                    formatter = detected_format
                else:
                    logger.error(f"Unable to determine date format for: {date_str}")
                    return False
            else:
                formatter = self.date_format

            logger.debug(f"Date formatter: {formatter}")
            current_date = datetime.strptime(date_str, formatter).date()

            start_date = datetime.strptime(self.sar_start_date, formatter).date()
            end_date = datetime.strptime(self.sar_end_date, formatter).date()

        except ValueError as ex:
            logger.error(f"Unable to parse date {date_str}: {ex}")
            return False

        self.parse_date = current_date

        if current_date < start_date:
            self.sar_start_date = date_str
        if current_date > end_date:
            self.sar_end_date = date_str

        logger.debug(f"parsedDate: {current_date}, startDate: {self.sar_start_date}, EndDate: {self.sar_end_date}")
        return True

    @classmethod
    def determine_date_format(cls, date_string: str) -> Optional[str]:
        """
        Automatically determine the date format from a date string.
        
        Args:
            date_string: The date string to analyze
            
        Returns:
            Optional[str]: The detected date format or None if not found
        """
        for regexp, format_str in cls.DATE_FORMAT_REGEXPS.items():
            if re.match(regexp, date_string.lower()):
                return format_str
        return None

    def get_info(self) -> str:
        """
        Get system information as a formatted string.
        
        Returns:
            str: Formatted system information
        """
        info_parts = []
        
        if self.system_info.os_type:
            info_parts.append(f"OS Type: {self.system_info.os_type}")
        if self.system_info.os_version:
            info_parts.append(f"OS Version: {self.system_info.os_version}")
        if self.system_info.kernel:
            info_parts.append(f"Kernel Release: {self.system_info.kernel}")
        if self.system_info.cpu_type:
            info_parts.append(f"CPU Type: {self.system_info.cpu_type}")
        if self.system_info.hostname:
            info_parts.append(f"Hostname: {self.system_info.hostname}")
        if self.system_info.mac_address:
            info_parts.append(f"Mac Address: {self.system_info.mac_address}")
        if self.system_info.memory:
            info_parts.append(f"Memory: {self.system_info.memory}")
        if self.system_info.nb_disk:
            info_parts.append(f"Number of disks: {self.system_info.nb_disk}")
        if self.system_info.nb_cpu:
            info_parts.append(f"Number of CPU: {self.system_info.nb_cpu}")
        if self.system_info.ent:
            info_parts.append(f"Ent: {self.system_info.ent}")
        if self.sar_start_date:
            info_parts.append(f"Start of SAR: {self.sar_start_date}")
        if self.sar_end_date:
            info_parts.append(f"End of SAR: {self.sar_end_date}")

        return "\n".join(info_parts)

    def should_ignore_line(self, columns: List[str]) -> bool:
        """
        Check if a line should be ignored based on its first column.
        
        Args:
            columns: The line split into columns
            
        Returns:
            bool: True if the line should be ignored
        """
        if not columns:
            return True
        return columns[0] in self.ignore_lines_beginning_with 