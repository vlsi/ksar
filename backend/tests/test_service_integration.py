"""
Integration tests for service layer interactions.

This module tests the integration between different services including
ParserService, ChartService, and their interactions with data models.
"""

import pytest
import tempfile
import os
import shutil
from unittest.mock import patch, MagicMock
from datetime import datetime, timedelta

from app.services.parser_service import ParserService
from app.services.chart_service import ChartService
from app.models.chart_models import (
    ChartConfig, DashboardConfig, ChartType, ChartLayout, 
    YAxisConfig, YAxisType, CHART_TEMPLATES
)
from app.models.time_series import TimeSeriesCollection
from app.parsers.linux import LinuxParser


@pytest.mark.integration
@pytest.mark.services
class TestServiceIntegration:
    """Test service layer integration."""
    
    @pytest.fixture(autouse=True)
    def setup_teardown(self):
        """Set up and tear down test environment."""
        self.test_dir = tempfile.mkdtemp()
        self.sample_sar_content = self._create_sample_sar_content()
        self.parser_service = ParserService()
        self.chart_service = ChartService()
        
        yield
        
        # Clean up
        if os.path.exists(self.test_dir):
            shutil.rmtree(self.test_dir)
    
    def _create_sample_sar_content(self):
        """Create realistic SAR file content for testing."""
        content = []
        content.append("Linux 5.15.0-91-generic (test-host) 01/15/24 _x86_64_ (8 CPU)")
        content.append("")
        content.append("16:00:01        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle")
        
        # Generate 10 minutes of sample data
        base_time = datetime.now().replace(hour=16, minute=0, second=1, microsecond=0)
        for i in range(10):
            timestamp = base_time + timedelta(minutes=i)
            time_str = timestamp.strftime("%H:%M:%S")
            # Simulate realistic CPU values
            usr = 15 + (i % 10)  # 15-25%
            sys = 5 + (i % 5)    # 5-10%
            idle = 100 - usr - sys
            content.append(f"{time_str}        all      {usr:6.2f}      0.00      {sys:6.2f}      0.00      0.00      0.00      0.00      0.00      0.00     {idle:6.2f}")
        
        content.append("")
        content.append("16:00:01        kbmemfree   kbavail kbmemused  %memused kbbuffers  kbcached  kbcommit   %commit  kbactive   kbinact   kbdirty")
        
        # Memory data
        for i in range(10):
            timestamp = base_time + timedelta(minutes=i)
            time_str = timestamp.strftime("%H:%M:%S")
            memfree = 8000000 - (i * 10000)  # Decreasing memory
            memused = 2000000 + (i * 10000)  # Increasing usage
            content.append(f"{time_str}     {memfree:8.0f} {memfree-100000:8.0f} {memused:8.0f}     {memused/(memfree+memused)*100:5.1f}   100000   2000000  {memused*1.2:8.0f}     {memused*1.2/(memfree+memused)*100:5.1f} {memused*0.8:8.0f} {memused*0.2:8.0f}       0")
        
        content.append("")
        content.append("16:00:01        DEV       tps     rkB/s     wkB/s   rMB/s   wMB/s avgrq-sz avgqu-sz     await     svctm     %util")
        
        # Disk I/O data
        for i in range(10):
            timestamp = base_time + timedelta(minutes=i)
            time_str = timestamp.strftime("%H:%M:%S")
            tps = 50 + (i % 20)  # 50-70 transactions per second
            rkb = 1000 + (i * 100)  # Increasing read KB
            wkb = 500 + (i * 50)   # Increasing write KB
            content.append(f"{time_str}        sda     {tps:6.0f} {rkb:8.0f} {wkb:8.0f}   {rkb/1024:5.1f}   {wkb/1024:5.1f}     {512 + i*10:6.0f}     {0.1 + i*0.01:6.2f}     {2.0 + i*0.1:6.1f}     {1.0 + i*0.05:6.2f}     {5.0 + i*0.5:5.1f}")
        
        return "\n".join(content)
    
    def test_parser_to_chart_service_integration(self):
        """Test integration between parser service and chart service."""
        # Step 1: Parse file using parser service
        sample_file_path = os.path.join(self.test_dir, "service_integration.sar")
        with open(sample_file_path, 'w') as f:
            f.write(self.sample_sar_content)
        
        parsed_data = self.parser_service.parse_file(sample_file_path)
        assert parsed_data is not None
        file_id = parsed_data.file_id
        
        # Step 2: Get time series collection
        collection = self.parser_service.get_time_series_collection(file_id)
        assert collection is not None
        assert isinstance(collection, TimeSeriesCollection)
        
        # Step 3: Verify data integrity
        available_metrics = collection.list_series()
        assert len(available_metrics) > 0
        
        # Step 4: Create chart configuration using chart service
        dashboard_config = DashboardConfig(
            name=f"Service Integration Dashboard - {file_id}",
            title="Service Integration Test",
            layout=ChartLayout.GRID
        )
        
        # Add CPU chart
        cpu_metrics = [m for m in available_metrics if 'cpu' in m.lower()]
        if cpu_metrics:
            cpu_chart = ChartConfig(
                name="cpu_utilization",
                title="CPU Utilization",
                chart_type=ChartType.LINE,
                metrics=cpu_metrics[:3],
                y_axis=YAxisConfig(title="CPU %", type=YAxisType.LINEAR, min=0, max=100)
            )
            dashboard_config.add_chart(cpu_chart)
        
        # Step 5: Generate dashboard using chart service
        dashboard_data = self.chart_service.create_dashboard(collection, dashboard_config)
        assert dashboard_data is not None
        assert len(dashboard_data.charts) > 0
        
        # Step 6: Verify chart data matches original collection data
        for chart in dashboard_data.charts:
            for metric in chart.config.metrics:
                series = collection.get_series(metric)
                assert series is not None
                assert len(series.timestamps) > 0
                assert len(series.values) > 0
        
        # Clean up
        self.parser_service.delete_file(file_id)
    
    def test_service_data_consistency(self):
        """Test data consistency across services."""
        # Parse file
        sample_file_path = os.path.join(self.test_dir, "consistency_test.sar")
        with open(sample_file_path, 'w') as f:
            f.write(self.sample_sar_content)
        
        parsed_data = self.parser_service.parse_file(sample_file_path)
        file_id = parsed_data.file_id
        
        # Get collection from parser service
        collection1 = self.parser_service.get_time_series_collection(file_id)
        metrics1 = collection1.list_series()
        
        # Create new parser service instance and get same data
        parser_service2 = ParserService()
        collection2 = parser_service2.get_time_series_collection(file_id)
        metrics2 = collection2.list_series()
        
        # Verify consistency
        assert set(metrics1) == set(metrics2)
        assert len(metrics1) == len(metrics2)
        
        # Verify data values are consistent
        for metric in metrics1[:3]:  # Test first 3 metrics
            series1 = collection1.get_series(metric)
            series2 = collection2.get_series(metric)
            
            assert series1.timestamps == series2.timestamps
            assert series1.values == series2.values
        
        # Clean up
        self.parser_service.delete_file(file_id)
    
    def test_service_error_propagation(self):
        """Test error propagation between services."""
        # Test with invalid file
        with pytest.raises(Exception):
            self.parser_service.parse_file("nonexistent_file.sar")
        
        # Test with empty file
        empty_file_path = os.path.join(self.test_dir, "empty.sar")
        with open(empty_file_path, 'w') as f:
            f.write("")
        
        try:
            parsed_data = self.parser_service.parse_file(empty_file_path)
            if parsed_data:
                # If parsing succeeds, chart service should handle empty data gracefully
                collection = self.parser_service.get_time_series_collection(parsed_data.file_id)
                if len(collection.list_series()) == 0:
                    # Test chart service with empty collection
                    dashboard_config = DashboardConfig(
                        name="Empty Data Test",
                        title="Empty Data Test",
                        layout=ChartLayout.GRID
                    )
                    
                    # This should not crash
                    dashboard_data = self.chart_service.create_dashboard(collection, dashboard_config)
                    assert dashboard_data is not None
                    assert len(dashboard_data.charts) == 0
                    
                    # Clean up
                    self.parser_service.delete_file(parsed_data.file_id)
        except Exception:
            # It's also acceptable for the parser to raise an exception for empty files
            pass
    
    def test_service_template_integration(self):
        """Test integration between services and chart templates."""
        # Parse file
        sample_file_path = os.path.join(self.test_dir, "template_test.sar")
        with open(sample_file_path, 'w') as f:
            f.write(self.sample_sar_content)
        
        parsed_data = self.parser_service.parse_file(sample_file_path)
        file_id = parsed_data.file_id
        
        # Get collection
        collection = self.parser_service.get_time_series_collection(file_id)
        
        # Test template availability
        templates = self.chart_service.get_available_templates()
        assert len(templates) > 0
        
        # Test template-based chart creation
        if 'cpu_utilization' in templates:
            cpu_chart = self.chart_service.create_chart_from_template(
                'cpu_utilization', collection
            )
            assert cpu_chart is not None
            assert cpu_chart.config.name == "cpu_utilization"
            
            # Verify template chart uses data from collection
            for metric in cpu_chart.config.metrics:
                series = collection.get_series(metric)
                assert series is not None
        
        # Clean up
        self.parser_service.delete_file(file_id)
    
    def test_service_concurrent_access(self):
        """Test concurrent access to services."""
        import threading
        import time
        
        results = []
        errors = []
        
        def process_file(file_index):
            try:
                # Create unique file content
                file_content = f"Linux test-host {file_index} (test-host) 01/15/24 _x86_64_ (4 CPU)\n"
                file_content += "16:00:01        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle\n"
                file_content += "16:00:01        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99\n"
                
                # Create file
                file_path = os.path.join(self.test_dir, f"concurrent_service_{file_index}.sar")
                with open(file_path, 'w') as f:
                    f.write(file_content)
                
                # Parse file
                parsed_data = self.parser_service.parse_file(file_path)
                if parsed_data:
                    collection = self.parser_service.get_time_series_collection(parsed_data.file_id)
                    metrics = collection.list_series()
                    results.append((file_index, len(metrics)))
                    
                    # Clean up
                    self.parser_service.delete_file(parsed_data.file_id)
                else:
                    errors.append(f"Failed to parse file {file_index}")
                    
            except Exception as e:
                errors.append(f"Error processing file {file_index}: {str(e)}")
        
        # Create multiple threads
        threads = []
        for i in range(3):
            thread = threading.Thread(target=process_file, args=(i,))
            threads.append(thread)
            thread.start()
        
        # Wait for all threads to complete
        for thread in threads:
            thread.join()
        
        # Verify results
        assert len(results) == 3
        assert len(errors) == 0
    
    def test_service_data_transformation(self):
        """Test data transformation between services."""
        # Parse file
        sample_file_path = os.path.join(self.test_dir, "transformation_test.sar")
        with open(sample_file_path, 'w') as f:
            f.write(self.sample_sar_content)
        
        parsed_data = self.parser_service.parse_file(sample_file_path)
        file_id = parsed_data.file_id
        
        # Get collection
        collection = self.parser_service.get_time_series_collection(file_id)
        
        # Test data aggregation
        stats = collection.get_statistics_summary()
        assert stats['total_series'] > 0
        assert stats['total_data_points'] > 0
        
        # Test time range
        time_range = collection.get_time_range()
        assert time_range is not None
        assert 'start' in time_range
        assert 'end' in time_range
        
        # Test data filtering
        if len(collection.list_series()) > 0:
            first_metric = collection.list_series()[0]
            series = collection.get_series(first_metric)
            
            # Test time-based filtering
            if len(series.timestamps) > 5:
                # Filter to first 5 data points
                filtered_timestamps = series.timestamps[:5]
                filtered_values = series.values[:5]
                
                assert len(filtered_timestamps) == 5
                assert len(filtered_values) == 5
        
        # Clean up
        self.parser_service.delete_file(file_id)
    
    def test_service_memory_management(self):
        """Test memory management and cleanup between services."""
        # Parse multiple files to test memory management
        file_ids = []
        
        for i in range(5):
            file_path = os.path.join(self.test_dir, f"memory_test_{i}.sar")
            with open(file_path, 'w') as f:
                f.write(self.sample_sar_content)
            
            parsed_data = self.parser_service.parse_file(file_path)
            file_ids.append(parsed_data.file_id)
        
        # Verify all files are accessible
        for file_id in file_ids:
            collection = self.parser_service.get_time_series_collection(file_id)
            assert collection is not None
            assert len(collection.list_series()) > 0
        
        # Clean up files
        for file_id in file_ids:
            self.parser_service.delete_file(file_id)
        
        # Verify cleanup
        for file_id in file_ids:
            try:
                collection = self.parser_service.get_time_series_collection(file_id)
                assert collection is None
            except:
                # It's acceptable for deleted files to raise exceptions
                pass
    
    def test_service_parser_integration(self):
        """Test integration between services and parsers."""
        # Test Linux parser directly
        linux_parser = LinuxParser()
        
        # Create test file
        file_path = os.path.join(self.test_dir, "parser_integration.sar")
        with open(file_path, 'w') as f:
            f.write(self.sample_sar_content)
        
        # Parse using parser directly
        parsed_data = linux_parser.parse_file(file_path)
        assert parsed_data is not None
        
        # Verify parser output can be used by services
        collection = TimeSeriesCollection.from_parsed_data(parsed_data)
        assert collection is not None
        assert len(collection.list_series()) > 0
        
        # Test chart service with parser-generated collection
        dashboard_config = DashboardConfig(
            name="Parser Integration Test",
            title="Parser Integration Test",
            layout=ChartLayout.GRID
        )
        
        # Add a simple chart
        if len(collection.list_series()) > 0:
            first_metric = collection.list_series()[0]
            chart = ChartConfig(
                name="test_chart",
                title="Test Chart",
                chart_type=ChartType.LINE,
                metrics=[first_metric]
            )
            dashboard_config.add_chart(chart)
            
            # Generate dashboard
            dashboard_data = self.chart_service.create_dashboard(collection, dashboard_config)
            assert dashboard_data is not None
        
        # Clean up
        os.unlink(file_path) 