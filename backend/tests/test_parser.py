#!/usr/bin/env python3
"""
Test script for the SAR parser.

This script tests the parser with a real SAR file from the kSar samples.
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from app.services.parser_service import ParserService
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def test_parser_with_sample_file():
    """Test the parser with a sample SAR file."""
    # Path to the sample SAR file
    sample_file = "../kSar/samples/sar-11.6.1-full-output"
    
    if not os.path.exists(sample_file):
        logger.error(f"Sample file not found: {sample_file}")
        return False
    
    try:
        # Create parser service
        parser_service = ParserService()
        
        # Parse the file
        logger.info(f"Parsing file: {sample_file}")
        parsed_data = parser_service.parse_file(sample_file)
        
        if not parsed_data:
            logger.error("Failed to parse file")
            return False
        
        # Print file information
        logger.info("=== File Information ===")
        logger.info(f"File ID: {parsed_data.file_id}")
        logger.info(f"OS Type: {parsed_data.system_info.os_type}")
        logger.info(f"Hostname: {parsed_data.system_info.hostname}")
        logger.info(f"Kernel: {parsed_data.system_info.kernel}")
        logger.info(f"CPU Count: {parsed_data.system_info.nb_cpu}")
        logger.info(f"Start Time: {parsed_data.start_time}")
        logger.info(f"End Time: {parsed_data.end_time}")
        logger.info(f"Metrics Count: {len(parsed_data.metrics)}")
        logger.info(f"Samples Count: {len(parsed_data.date_samples)}")
        
        # Print metrics information
        logger.info("\n=== Metrics ===")
        for name, metric_data in parsed_data.metrics.items():
            logger.info(f"Metric: {name}")
            logger.info(f"  Columns: {metric_data.columns}")
            logger.info(f"  Data Points: {len(metric_data.timestamps)}")
            logger.info(f"  Time Range: {metric_data.timestamps[0] if metric_data.timestamps else 'N/A'} to {metric_data.timestamps[-1] if metric_data.timestamps else 'N/A'}")
        
        return True
        
    except Exception as e:
        logger.error(f"Error testing parser: {e}")
        return False


if __name__ == "__main__":
    success = test_parser_with_sample_file()
    if success:
        logger.info("Parser test completed successfully!")
    else:
        logger.error("Parser test failed!")
        sys.exit(1) 