#!/usr/bin/env python3
"""
Integration test for dashboard functionality.

This script tests the complete dashboard generation pipeline.
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from app.services.parser_service import ParserService
from app.services.chart_service import ChartService
from app.models.chart_models import ChartConfig, DashboardConfig, ChartType, ChartLayout, YAxisConfig, YAxisType
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def test_dashboard_integration():
    """Test the complete dashboard integration."""
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
        
        # Test enhanced time series functionality
        logger.info("Testing enhanced time series functionality...")
        
        # Test statistics
        stats_summary = collection.get_statistics_summary()
        logger.info(f"Statistics summary: {stats_summary['total_series']} series")
        
        # Test time range filtering
        time_range = collection.get_time_range()
        if time_range:
            logger.info(f"Time range: {time_range['start']} to {time_range['end']}")
        
        # Create enhanced dashboard configuration
        dashboard_config = DashboardConfig(
            name=f"Enhanced SAR Dashboard - {file_id}",
            title=f"Enhanced SAR Analysis Dashboard - {collection.metadata.get('system_info', {}).get('hostname', file_id)}",
            layout=ChartLayout.GRID
        )
        
        # Add CPU chart with enhanced configuration
        cpu_metrics = [name for name in collection.list_series() if name.startswith('cpu_')]
        if cpu_metrics:
            logger.info(f"Adding enhanced CPU chart with metrics: {cpu_metrics[:3]}")
            cpu_chart = ChartConfig(
                name="cpu_utilization",
                title="CPU Utilization",
                chart_type=ChartType.LINE,
                metrics=cpu_metrics[:3],
                y_axis=YAxisConfig(title="CPU %", type=YAxisType.LINEAR, min=0, max=100)
            )
            dashboard_config.add_chart(cpu_chart)
        
        # Add memory chart with enhanced configuration
        memory_metrics = [name for name in collection.list_series() if name.startswith('memory')]
        if memory_metrics:
            logger.info(f"Adding enhanced memory chart with metrics: {memory_metrics}")
            memory_chart = ChartConfig(
                name="memory_usage",
                title="Memory Usage",
                chart_type=ChartType.AREA,
                metrics=memory_metrics,
                y_axis=YAxisConfig(title="Memory (KB)")
            )
            dashboard_config.add_chart(memory_chart)
        
        # Add disk chart with enhanced configuration
        disk_metrics = [name for name in collection.list_series() if name.startswith('disk_')]
        if disk_metrics:
            logger.info(f"Adding enhanced disk chart with metrics: {disk_metrics[:2]}")
            disk_chart = ChartConfig(
                name="disk_io",
                title="Disk I/O",
                chart_type=ChartType.LINE,
                metrics=disk_metrics[:2],
                y_axis=YAxisConfig(title="Operations/sec")
            )
            dashboard_config.add_chart(disk_chart)
        
        # Generate enhanced dashboard
        logger.info("Generating enhanced dashboard...")
        dashboard_data = chart_service.create_dashboard(collection, dashboard_config)
        
        logger.info(f"Dashboard generated successfully with {len(dashboard_data.charts)} charts")
        
        # Test chart templates
        logger.info("Testing chart templates...")
        templates = chart_service.get_available_templates()
        logger.info(f"Available templates: {list(templates.keys())}")
        
        # Test creating chart from template
        if 'cpu_utilization' in templates:
            logger.info("Testing CPU utilization template...")
            cpu_chart_data = chart_service.create_chart_from_template('cpu_utilization', collection)
            logger.info(f"CPU template chart created: {cpu_chart_data.config.name}")
        
        # Test dashboard HTML generation
        logger.info("Testing dashboard HTML generation...")
        dashboard_html = chart_service.create_dashboard_html(dashboard_data)
        logger.info(f"Dashboard HTML generated: {len(dashboard_html)} characters")
        
        logger.info("✅ All dashboard integration tests passed!")
        return True
        
    except Exception as e:
        logger.error(f"❌ Dashboard integration test failed: {e}")
        import traceback
        traceback.print_exc()
        return False


if __name__ == "__main__":
    success = test_dashboard_integration()
    sys.exit(0 if success else 1) 