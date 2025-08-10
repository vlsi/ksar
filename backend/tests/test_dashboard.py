#!/usr/bin/env python3
"""
Test script for dashboard functionality.

This script tests the dashboard generation with a real SAR file.
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from app.services.parser_service import ParserService
from app.services.chart_service import ChartService
from app.models.chart_models import ChartConfig, DashboardConfig, ChartType, ChartLayout
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def test_dashboard():
    """Test dashboard generation with a sample SAR file."""
    # Path to the sample SAR file
    sample_file = "../kSar/samples/sar-11.6.1-full-output"
    
    if not os.path.exists(sample_file):
        logger.error(f"Sample file not found: {sample_file}")
        return False
    
    try:
        # Create services
        parser_service = ParserService()
        chart_service = ChartService()
        
        # Parse the file
        logger.info(f"Parsing file: {sample_file}")
        parsed_data = parser_service.parse_file(sample_file)
        
        if not parsed_data:
            logger.error("Failed to parse file")
            return False
        
        file_id = parsed_data.file_id
        logger.info(f"File parsed successfully with ID: {file_id}")
        
        # Get time series collection
        collection = parser_service.get_time_series_collection(file_id)
        if not collection:
            logger.error("Failed to get time series collection")
            return False
        
        logger.info(f"Time series collection created with {len(collection.list_series())} series")
        
        # List available metrics
        logger.info("Available metrics:")
        for series_name in collection.list_series():
            series = collection.get_series(series_name)
            if series:
                logger.info(f"  - {series_name}: {len(series.timestamps)} data points")
        
        # Create dashboard configuration
        dashboard_config = DashboardConfig(
            name=f"SAR Dashboard - {file_id}",
            title=f"SAR Analysis Dashboard - {collection.metadata.get('system_info', {}).get('hostname', file_id)}",
            layout=ChartLayout.GRID
        )
        
        # Add CPU chart
        cpu_metrics = [name for name in collection.list_series() if name.startswith('cpu_')]
        if cpu_metrics:
            logger.info(f"Adding CPU chart with metrics: {cpu_metrics[:3]}")
            cpu_chart = ChartConfig(
                name="cpu_utilization",
                title="CPU Utilization",
                chart_type=ChartType.LINE,
                metrics=cpu_metrics[:3]  # Limit to first 3 CPU metrics
            )
            dashboard_config.add_chart(cpu_chart)
        
        # Add memory chart
        memory_metrics = [name for name in collection.list_series() if name.startswith('memory')]
        if memory_metrics:
            logger.info(f"Adding memory chart with metrics: {memory_metrics}")
            memory_chart = ChartConfig(
                name="memory_usage",
                title="Memory Usage",
                chart_type=ChartType.AREA,
                metrics=memory_metrics
            )
            dashboard_config.add_chart(memory_chart)
        
        # Add disk chart
        disk_metrics = [name for name in collection.list_series() if name.startswith('disk_')]
        if disk_metrics:
            logger.info(f"Adding disk chart with metrics: {disk_metrics[:2]}")
            disk_chart = ChartConfig(
                name="disk_io",
                title="Disk I/O",
                chart_type=ChartType.LINE,
                metrics=disk_metrics[:2]  # Limit to first 2 disk metrics
            )
            dashboard_config.add_chart(disk_chart)
        
        # Generate dashboard
        logger.info("Generating dashboard...")
        dashboard = chart_service.create_dashboard(collection, dashboard_config)
        
        logger.info("=== Dashboard Generated Successfully ===")
        logger.info(f"Dashboard name: {dashboard['config']['name']}")
        logger.info(f"Number of charts: {dashboard['metadata']['chart_count']}")
        logger.info(f"Time range: {dashboard['metadata']['time_range']}")
        
        # Save dashboard HTML to file
        dashboard_file = f"dashboard_{file_id}.html"
        with open(dashboard_file, 'w') as f:
            f.write(dashboard['html'])
        
        logger.info(f"Dashboard HTML saved to: {dashboard_file}")
        
        return True
        
    except Exception as e:
        logger.error(f"Error testing dashboard: {e}")
        return False


if __name__ == "__main__":
    success = test_dashboard()
    if success:
        logger.info("Dashboard test completed successfully!")
    else:
        logger.error("Dashboard test failed!")
        sys.exit(1) 