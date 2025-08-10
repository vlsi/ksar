"""
Integration tests for the complete kSar pipeline.

This module tests the entire flow from file upload through parsing,
data processing, chart generation, and dashboard creation.
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


@pytest.mark.integration
@pytest.mark.slow
class TestFullPipelineIntegration:
    """Test the complete kSar pipeline integration."""
    
    @pytest.fixture(autouse=True)
    def setup_teardown(self):
        """Set up and tear down test environment."""
        # Create temporary directory for test files
        self.test_dir = tempfile.mkdtemp()
        self.sample_sar_content = self._create_sample_sar_content()
        
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
        content.append("16:00:01        LINUX RESTART")
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
    
    def test_complete_file_processing_pipeline(self):
        """Test the complete file processing pipeline."""
        # Create services
        parser_service = ParserService()
        chart_service = ChartService()
        
        # Step 1: Create and save sample SAR file
        sample_file_path = os.path.join(self.test_dir, "sample.sar")
        with open(sample_file_path, 'w') as f:
            f.write(self.sample_sar_content)
        
        assert os.path.exists(sample_file_path)
        
        # Step 2: Parse the file
        parsed_data = parser_service.parse_file(sample_file_path)
        assert parsed_data is not None
        assert hasattr(parsed_data, 'file_id')
        file_id = parsed_data.file_id
        
        # Step 3: Verify file info
        file_info = parser_service.get_file_info(file_id)
        assert file_info is not None
        assert file_info["file_id"] == file_id
        
        # Step 4: Get time series collection
        collection = parser_service.get_time_series_collection(file_id)
        assert collection is not None
        assert isinstance(collection, TimeSeriesCollection)
        
        # Step 5: Verify available metrics
        available_metrics = collection.list_series()
        print(f"Available metrics: {available_metrics}")
        assert len(available_metrics) > 0
        assert any('cpu' in metric.lower() for metric in available_metrics)
        assert any('memory' in metric.lower() for metric in available_metrics)
        assert any('disk' in metric.lower() for metric in available_metrics)
        
        # Step 6: Test data access
        for metric in available_metrics[:3]:  # Test first 3 metrics
            series = collection.get_series(metric)
            assert series is not None
            assert len(series.timestamps) > 0
            assert len(series.values) > 0
        
        # Step 7: Test statistics
        stats = collection.get_statistics_summary()
        assert stats['total_series'] > 0
        assert stats['total_data_points'] > 0
        
        # Step 8: Test time range
        time_range = collection.get_time_range()
        assert time_range is not None
        assert 'start' in time_range
        assert 'end' in time_range
        
        # Step 9: Create dashboard configuration
        dashboard_config = DashboardConfig(
            name=f"Integration Test Dashboard - {file_id}",
            title="Integration Test Dashboard",
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
        
        # Add memory chart
        memory_metrics = [m for m in available_metrics if 'memory' in m.lower()]
        if memory_metrics:
            memory_chart = ChartConfig(
                name="memory_usage",
                title="Memory Usage",
                chart_type=ChartType.AREA,
                metrics=memory_metrics,
                y_axis=YAxisConfig(title="Memory (KB)")
            )
            dashboard_config.add_chart(memory_chart)
        
        # Step 10: Generate dashboard
        dashboard_data = chart_service.create_dashboard(collection, dashboard_config)
        assert dashboard_data is not None
        assert len(dashboard_data.charts) > 0
        
        # Step 11: Test chart templates
        templates = chart_service.get_available_templates()
        assert len(templates) > 0
        
        # Step 12: Test template-based chart creation
        if 'cpu_utilization' in templates:
            template_chart = chart_service.create_chart_from_template(
                'cpu_utilization', collection
            )
            assert template_chart is not None
        
        # Step 13: Generate HTML dashboard
        dashboard_html = chart_service.create_dashboard_html(dashboard_data)
        assert dashboard_html is not None
        assert len(dashboard_html) > 0
        assert '<html' in dashboard_html.lower()
        
        # Step 14: Test file cleanup
        parser_service.delete_parsed_file(file_id)
        file_info_after_delete = parser_service.get_file_info(file_id)
        assert file_info_after_delete is None
    
    def test_error_handling_integration(self):
        """Test error handling across the entire pipeline."""
        parser_service = ParserService()
        chart_service = ChartService()
        
        # Test with invalid file
        parsed_data = parser_service.parse_file("nonexistent_file.sar")
        assert parsed_data is None
        
        # Test with empty file
        empty_file_path = os.path.join(self.test_dir, "empty.sar")
        with open(empty_file_path, 'w') as f:
            f.write("")
        
        # This should handle empty files gracefully
        try:
            parsed_data = parser_service.parse_file(empty_file_path)
            # If it doesn't raise an exception, the data should be None or empty
            if parsed_data:
                assert len(parser_service.get_time_series_collection(parsed_data.file_id).list_series()) == 0
        except Exception:
            # It's also acceptable for the parser to raise an exception for empty files
            pass
    
    def test_concurrent_processing_integration(self):
        """Test concurrent processing of multiple files."""
        import threading
        import time
        
        parser_service = ParserService()
        chart_service = ChartService()
        
        results = []
        errors = []
        
        def process_file(file_path, file_id):
            try:
                # Parse file
                parsed_data = parser_service.parse_file(file_path)
                if parsed_data:
                    collection = parser_service.get_time_series_collection(parsed_data.file_id)
                    metrics = collection.list_series()
                    results.append((file_id, len(metrics)))
                else:
                    errors.append(f"Failed to parse {file_id}")
            except Exception as e:
                errors.append(f"Error processing {file_id}: {str(e)}")
        
        # Create multiple test files
        threads = []
        for i in range(3):
            file_path = os.path.join(self.test_dir, f"concurrent_{i}.sar")
            with open(file_path, 'w') as f:
                f.write(self.sample_sar_content)
            
            thread = threading.Thread(
                target=process_file, 
                args=(file_path, f"concurrent_{i}")
            )
            threads.append(thread)
            thread.start()
        
        # Wait for all threads to complete
        for thread in threads:
            thread.join()
        
        # Verify results
        assert len(results) == 3
        assert len(errors) == 0
        
        # Clean up
        for i in range(3):
            file_id = f"concurrent_{i}"
            try:
                parser_service.delete_parsed_file(file_id)
            except:
                pass
    
    def test_data_persistence_integration(self):
        """Test that data persists within the same service instance."""
        # Create service instance
        parser_service = ParserService()
        
        # Create and parse file
        sample_file_path = os.path.join(self.test_dir, "persistence.sar")
        with open(sample_file_path, 'w') as f:
            f.write(self.sample_sar_content)
        
        parsed_data = parser_service.parse_file(sample_file_path)
        file_id = parsed_data.file_id
        
        # Verify data exists
        collection1 = parser_service.get_time_series_collection(file_id)
        metrics1 = collection1.list_series()
        assert len(metrics1) > 0
        
        # Verify data persists within the same instance
        collection2 = parser_service.get_time_series_collection(file_id)
        metrics2 = collection2.list_series()
        assert len(metrics2) == len(metrics1)
        assert set(metrics2) == set(metrics1)
        
        # Clean up
        parser_service.delete_parsed_file(file_id) 