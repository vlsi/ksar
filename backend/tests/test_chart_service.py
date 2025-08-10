"""
Tests for the ChartService class.
"""

import pytest
import tempfile
import os
from unittest.mock import Mock, patch
from datetime import datetime, timedelta

from app.services.chart_service import ChartService
from app.services.parser_service import ParserService
from app.models.chart_models import (
    ChartConfig, DashboardConfig, ChartType, ChartLayout, 
    YAxisConfig, YAxisType
)


class TestChartService:
    """Test cases for ChartService"""
    
    def setup_method(self):
        """Set up test fixtures before each test method."""
        self.chart_service = ChartService()
        self.parser_service = ParserService()
        
        # Sample SAR content for testing
        self.sample_sar_content = """Linux 3.10.0-1160.53.1.el7.x86_64 (test-host.example.com) 04/08/18 _x86_64_ (4 CPU)

16:08:45        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle
16:08:55        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99
16:09:05        all      4.89      0.00      4.67      0.12      0.00      0.00      0.00      0.00      0.00     90.32
16:09:15        all      6.12      0.00      5.89      0.18      0.00      0.00      0.00      0.00      0.00     87.81

16:08:45    kbmemfree   kbmemused  %memused kbbuffers  kbcached  kbcommit   %commit
16:08:55     1234567    8765432      87.65    123456    654321   9876543     98.76
16:09:05     1234568    8765431      87.64    123457    654322   9876542     98.75
16:09:15     1234569    8765430      87.63    123458    654323   9876541     98.74"""
    
    def test_create_chart_success(self):
        """Test successful chart creation."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                # Parse file first
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                # Create chart configuration
                chart_config = ChartConfig(
                    name="cpu_utilization",
                    title="CPU Utilization",
                    chart_type=ChartType.LINE,
                    metrics=["cpu_all"]
                )
                
                # Create chart
                chart_data = self.chart_service.create_chart(file_id, chart_config)
                
                assert chart_data is not None
                assert "data" in chart_data
                assert "layout" in chart_data
                assert chart_data["layout"]["title"]["text"] == "CPU Utilization"
                
            finally:
                os.unlink(temp_file.name)
    
    def test_create_chart_with_multiple_metrics(self):
        """Test chart creation with multiple metrics."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                # Create chart with multiple metrics
                chart_config = ChartConfig(
                    name="system_metrics",
                    title="System Metrics Overview",
                    chart_type=ChartType.LINE,
                    metrics=["cpu_all", "memory_%memused"]
                )
                
                chart_data = self.chart_service.create_chart(file_id, chart_config)
                
                assert chart_data is not None
                assert len(chart_data["data"]) >= 2  # At least 2 traces
                
            finally:
                os.unlink(temp_file.name)
    
    def test_create_chart_with_custom_layout(self):
        """Test chart creation with custom layout configuration."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                # Create custom Y-axis configuration
                y_axis_config = YAxisConfig(
                    type=YAxisType.LINEAR,
                    title="CPU Usage (%)",
                    range=[0, 100]
                )
                
                chart_config = ChartConfig(
                    name="cpu_custom",
                    title="CPU Usage with Custom Layout",
                    chart_type=ChartType.LINE,
                    metrics=["cpu_all"],
                    y_axis_config=y_axis_config
                )
                
                chart_data = self.chart_service.create_chart(file_id, chart_config)
                
                assert chart_data is not None
                assert chart_data["layout"]["yaxis"]["title"]["text"] == "CPU Usage (%)"
                
            finally:
                os.unlink(temp_file.name)
    
    def test_create_chart_file_not_found(self):
        """Test chart creation with non-existent file."""
        chart_config = ChartConfig(
            name="test_chart",
            title="Test Chart",
            chart_type=ChartType.LINE,
            metrics=["cpu_all"]
        )
        
        chart_data = self.chart_service.create_chart("nonexistent-id", chart_config)
        assert chart_data is None
    
    def test_create_chart_invalid_metrics(self):
        """Test chart creation with invalid metrics."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                # Create chart with non-existent metrics
                chart_config = ChartConfig(
                    name="invalid_chart",
                    title="Invalid Chart",
                    chart_type=ChartType.LINE,
                    metrics=["nonexistent_metric"]
                )
                
                chart_data = self.chart_service.create_chart(file_id, chart_config)
                
                # Should handle gracefully - may return empty chart or None
                assert chart_data is not None
                
            finally:
                os.unlink(temp_file.name)
    
    def test_create_dashboard_success(self):
        """Test successful dashboard creation."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                # Create dashboard configuration
                dashboard_config = DashboardConfig(
                    name="test_dashboard",
                    title="Test Dashboard",
                    layout=ChartLayout.GRID
                )
                
                # Add CPU chart
                cpu_chart = ChartConfig(
                    name="cpu_utilization",
                    title="CPU Utilization",
                    chart_type=ChartType.LINE,
                    metrics=["cpu_all"]
                )
                dashboard_config.add_chart(cpu_chart)
                
                # Add memory chart
                memory_chart = ChartConfig(
                    name="memory_usage",
                    title="Memory Usage",
                    chart_type=ChartType.AREA,
                    metrics=["memory_%memused"]
                )
                dashboard_config.add_chart(memory_chart)
                
                # Create dashboard
                dashboard_data = self.chart_service.create_dashboard(file_id, dashboard_config)
                
                assert dashboard_data is not None
                assert "html" in dashboard_data
                assert "CPU Utilization" in dashboard_data["html"]
                assert "Memory Usage" in dashboard_data["html"]
                
            finally:
                os.unlink(temp_file.name)
    
    def test_create_dashboard_with_json_format(self):
        """Test dashboard creation with JSON format."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                dashboard_config = DashboardConfig(
                    name="json_dashboard",
                    title="JSON Dashboard",
                    layout=ChartLayout.GRID
                )
                
                chart_config = ChartConfig(
                    name="cpu_chart",
                    title="CPU Chart",
                    chart_type=ChartType.LINE,
                    metrics=["cpu_all"]
                )
                dashboard_config.add_chart(chart_config)
                
                # Create dashboard in JSON format
                dashboard_data = self.chart_service.create_dashboard(
                    file_id, dashboard_config, format="json"
                )
                
                assert dashboard_data is not None
                assert "charts" in dashboard_data
                assert "layout" in dashboard_data
                
            finally:
                os.unlink(temp_file.name)
    
    def test_create_dashboard_file_not_found(self):
        """Test dashboard creation with non-existent file."""
        dashboard_config = DashboardConfig(
            name="test_dashboard",
            title="Test Dashboard",
            layout=ChartLayout.GRID
        )
        
        dashboard_data = self.chart_service.create_dashboard("nonexistent-id", dashboard_config)
        assert dashboard_data is None
    
    def test_create_chart_from_template(self):
        """Test chart creation from template."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                # Create chart from CPU template
                chart_data = self.chart_service.create_chart_from_template(
                    file_id, "cpu_overview"
                )
                
                assert chart_data is not None
                assert "data" in chart_data
                assert "layout" in chart_data
                
            finally:
                os.unlink(temp_file.name)
    
    def test_create_chart_from_template_with_custom_metrics(self):
        """Test chart creation from template with custom metrics."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                # Create chart from template with custom metrics
                chart_data = self.chart_service.create_chart_from_template(
                    file_id, "cpu_overview", custom_metrics=["cpu_all", "memory_%memused"]
                )
                
                assert chart_data is not None
                
            finally:
                os.unlink(temp_file.name)
    
    def test_create_chart_from_template_file_not_found(self):
        """Test chart creation from template with non-existent file."""
        chart_data = self.chart_service.create_chart_from_template("nonexistent-id", "cpu_overview")
        assert chart_data is None
    
    def test_create_chart_from_template_invalid_template(self):
        """Test chart creation from invalid template."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                # Try to create chart from non-existent template
                chart_data = self.chart_service.create_chart_from_template(
                    file_id, "nonexistent_template"
                )
                
                # Should handle gracefully
                assert chart_data is None
                
            finally:
                os.unlink(temp_file.name)
    
    def test_get_chart_templates(self):
        """Test getting available chart templates."""
        templates = self.chart_service.get_chart_templates()
        
        assert isinstance(templates, list)
        assert len(templates) > 0
        
        # Check that expected templates are present
        template_names = [t["name"] for t in templates]
        assert "cpu_overview" in template_names
        assert "memory_overview" in template_names
    
    def test_create_chart_with_different_chart_types(self):
        """Test chart creation with different chart types."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                # Test LINE chart type
                line_chart_config = ChartConfig(
                    name="line_chart",
                    title="Line Chart",
                    chart_type=ChartType.LINE,
                    metrics=["cpu_all"]
                )
                line_chart = self.chart_service.create_chart(file_id, line_chart_config)
                assert line_chart is not None
                
                # Test AREA chart type
                area_chart_config = ChartConfig(
                    name="area_chart",
                    title="Area Chart",
                    chart_type=ChartType.AREA,
                    metrics=["memory_%memused"]
                )
                area_chart = self.chart_service.create_chart(file_id, area_chart_config)
                assert area_chart is not None
                
                # Test BAR chart type
                bar_chart_config = ChartConfig(
                    name="bar_chart",
                    title="Bar Chart",
                    chart_type=ChartType.BAR,
                    metrics=["cpu_all"]
                )
                bar_chart = self.chart_service.create_chart(file_id, bar_chart_config)
                assert bar_chart is not None
                
            finally:
                os.unlink(temp_file.name)
    
    def test_create_chart_with_empty_metrics(self):
        """Test chart creation with empty metrics list."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                chart_config = ChartConfig(
                    name="empty_chart",
                    title="Empty Chart",
                    chart_type=ChartType.LINE,
                    metrics=[]
                )
                
                chart_data = self.chart_service.create_chart(file_id, chart_config)
                
                # Should handle gracefully
                assert chart_data is not None
                
            finally:
                os.unlink(temp_file.name)
    
    def test_create_dashboard_with_empty_charts(self):
        """Test dashboard creation with no charts."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                dashboard_config = DashboardConfig(
                    name="empty_dashboard",
                    title="Empty Dashboard",
                    layout=ChartLayout.GRID
                )
                
                # No charts added
                dashboard_data = self.chart_service.create_dashboard(file_id, dashboard_config)
                
                # Should handle gracefully
                assert dashboard_data is not None
                
            finally:
                os.unlink(temp_file.name) 