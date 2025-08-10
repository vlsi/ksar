"""
Performance and stress tests for the kSar system.

This module tests system performance under various load conditions
including large files, concurrent processing, and memory usage.
"""

import pytest
import tempfile
import os
import shutil
import time
import threading
import psutil
import gc
from unittest.mock import patch, MagicMock
from datetime import datetime, timedelta

from app.services.parser_service import ParserService
from app.services.chart_service import ChartService
from app.models.chart_models import (
    ChartConfig, DashboardConfig, ChartType, ChartLayout, 
    YAxisConfig, YAxisType
)


@pytest.mark.integration
@pytest.mark.performance
@pytest.mark.slow
class TestPerformanceIntegration:
    """Test system performance under various load conditions."""
    
    @pytest.fixture(autouse=True)
    def setup_teardown(self):
        """Set up and tear down test environment."""
        self.test_dir = tempfile.mkdtemp()
        self.parser_service = ParserService()
        self.chart_service = ChartService()
        
        yield
        
        # Clean up
        if os.path.exists(self.test_dir):
            shutil.rmtree(self.test_dir)
        
        # Force garbage collection
        gc.collect()
    
    def _create_large_sar_content(self, num_data_points=1000):
        """Create large SAR file content for performance testing."""
        content = []
        content.append("Linux 5.15.0-91-generic (test-host) 01/15/24 _x86_64_ (8 CPU)")
        content.append("")
        content.append("16:00:01        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle")
        
        # Generate large amount of data
        base_time = datetime.now().replace(hour=16, minute=0, second=1, microsecond=0)
        for i in range(num_data_points):
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
        for i in range(num_data_points):
            timestamp = base_time + timedelta(minutes=i)
            time_str = timestamp.strftime("%H:%M:%S")
            memfree = 8000000 - (i * 100)  # Decreasing memory
            memused = 2000000 + (i * 100)  # Increasing usage
            content.append(f"{time_str}     {memfree:8.0f} {memfree-100000:8.0f} {memused:8.0f}     {memused/(memfree+memused)*100:5.1f}   100000   2000000  {memused*1.2:8.0f}     {memused*1.2/(memfree+memused)*100:5.1f} {memused*0.8:8.0f} {memused*0.2:8.0f}       0")
        
        return "\n".join(content)
    
    def test_large_file_processing_performance(self):
        """Test performance with large SAR files."""
        # Create large file (1000 data points)
        large_content = self._create_large_sar_content(1000)
        large_file_path = os.path.join(self.test_dir, "large_performance.sar")
        
        with open(large_file_path, 'w') as f:
            f.write(large_content)
        
        # Measure file size
        file_size = os.path.getsize(large_file_path)
        print(f"Large file size: {file_size / 1024:.2f} KB")
        
        # Measure parsing time
        start_time = time.time()
        parsed_data = self.parser_service.parse_file(large_file_path)
        parsing_time = time.time() - start_time
        
        print(f"Parsing time for large file: {parsing_time:.2f} seconds")
        
        # Performance assertions
        assert parsing_time < 10.0  # Should parse in under 10 seconds
        assert parsed_data is not None
        
        file_id = parsed_data.file_id
        
        # Measure collection access time
        start_time = time.time()
        collection = self.parser_service.get_time_series_collection(file_id)
        access_time = time.time() - start_time
        
        print(f"Collection access time: {access_time:.2f} seconds")
        assert access_time < 1.0  # Should access in under 1 second
        
        # Measure dashboard generation time
        dashboard_config = DashboardConfig(
            name="Performance Test Dashboard",
            title="Performance Test",
            layout=ChartLayout.GRID
        )
        
        # Add multiple charts
        available_metrics = collection.list_series()
        for i, metric in enumerate(available_metrics[:5]):  # First 5 metrics
            chart = ChartConfig(
                name=f"chart_{i}",
                title=f"Chart {i}",
                chart_type=ChartType.LINE,
                metrics=[metric]
            )
            dashboard_config.add_chart(chart)
        
        start_time = time.time()
        dashboard_data = self.chart_service.create_dashboard(collection, dashboard_config)
        dashboard_time = time.time() - start_time
        
        print(f"Dashboard generation time: {dashboard_time:.2f} seconds")
        assert dashboard_time < 5.0  # Should generate in under 5 seconds
        
        # Clean up
        self.parser_service.delete_file(file_id)
    
    def test_concurrent_processing_performance(self):
        """Test performance under concurrent load."""
        num_concurrent_files = 10
        results = []
        errors = []
        processing_times = []
        
        def process_file_concurrent(file_index):
            try:
                # Create unique file content
                file_content = f"Linux test-host {file_index} (test-host) 01/15/24 _x86_64_ (4 CPU)\n"
                file_content += "16:00:01        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle\n"
                
                # Add 100 data points for each file
                for i in range(100):
                    time_str = f"16:{i//60:02d}:{i%60:02d}:01"
                    usr = 15 + (i % 10)
                    sys = 5 + (i % 5)
                    idle = 100 - usr - sys
                    file_content += f"{time_str}        all      {usr:6.2f}      0.00      {sys:6.2f}      0.00      0.00      0.00      0.00      0.00      0.00     {idle:6.2f}\n"
                
                # Create file
                file_path = os.path.join(self.test_dir, f"concurrent_perf_{file_index}.sar")
                with open(file_path, 'w') as f:
                    f.write(file_content)
                
                # Measure processing time
                start_time = time.time()
                parsed_data = self.parser_service.parse_file(file_path)
                processing_time = time.time() - start_time
                
                if parsed_data:
                    collection = self.parser_service.get_time_series_collection(parsed_data.file_id)
                    metrics = collection.list_series()
                    results.append((file_index, len(metrics)))
                    processing_times.append(processing_time)
                    
                    # Clean up
                    self.parser_service.delete_file(parsed_data.file_id)
                else:
                    errors.append(f"Failed to parse file {file_index}")
                    
            except Exception as e:
                errors.append(f"Error processing file {file_index}: {str(e)}")
        
        # Start concurrent processing
        start_time = time.time()
        threads = []
        for i in range(num_concurrent_files):
            thread = threading.Thread(target=process_file_concurrent, args=(i,))
            threads.append(thread)
            thread.start()
        
        # Wait for all threads to complete
        for thread in threads:
            thread.join()
        
        total_time = time.time() - start_time
        
        # Performance analysis
        print(f"Total concurrent processing time: {total_time:.2f} seconds")
        print(f"Average processing time per file: {sum(processing_times) / len(processing_times):.2f} seconds")
        print(f"Files processed successfully: {len(results)}")
        print(f"Errors: {len(errors)}")
        
        # Performance assertions
        assert len(results) == num_concurrent_files
        assert len(errors) == 0
        assert total_time < 30.0  # Should complete in under 30 seconds
        assert all(t < 5.0 for t in processing_times)  # Each file should process in under 5 seconds
    
    def test_memory_usage_performance(self):
        """Test memory usage under load."""
        # Get initial memory usage
        process = psutil.Process()
        initial_memory = process.memory_info().rss / 1024 / 1024  # MB
        print(f"Initial memory usage: {initial_memory:.2f} MB")
        
        # Process multiple files to test memory usage
        file_ids = []
        memory_usage = []
        
        for i in range(5):
            # Create file
            file_content = self._create_large_sar_content(500)  # 500 data points
            file_path = os.path.join(self.test_dir, f"memory_test_{i}.sar")
            
            with open(file_path, 'w') as f:
                f.write(file_content)
            
            # Parse file
            parsed_data = self.parser_service.parse_file(file_path)
            file_ids.append(parsed_data.file_id)
            
            # Check memory usage
            current_memory = process.memory_info().rss / 1024 / 1024
            memory_usage.append(current_memory)
            print(f"Memory after file {i}: {current_memory:.2f} MB")
        
        # Memory usage assertions
        max_memory = max(memory_usage)
        memory_increase = max_memory - initial_memory
        
        print(f"Maximum memory usage: {max_memory:.2f} MB")
        print(f"Memory increase: {memory_increase:.2f} MB")
        
        # Memory should not increase excessively (less than 100MB for 5 files)
        assert memory_increase < 100.0
        
        # Clean up files
        for file_id in file_ids:
            self.parser_service.delete_file(file_id)
        
        # Check memory after cleanup
        final_memory = process.memory_info().rss / 1024 / 1024
        print(f"Final memory usage: {final_memory:.2f} MB")
        
        # Memory should decrease after cleanup
        assert final_memory < max_memory
    
    def test_chart_generation_performance(self):
        """Test chart generation performance with large datasets."""
        # Create large file
        large_content = self._create_large_sar_content(800)
        file_path = os.path.join(self.test_dir, "chart_performance.sar")
        
        with open(file_path, 'w') as f:
            f.write(large_content)
        
        # Parse file
        parsed_data = self.parser_service.parse_file(file_path)
        file_id = parsed_data.file_id
        
        # Get collection
        collection = self.parser_service.get_time_series_collection(file_id)
        available_metrics = collection.list_series()
        
        # Create complex dashboard with many charts
        dashboard_config = DashboardConfig(
            name="Performance Chart Test",
            title="Performance Chart Test",
            layout=ChartLayout.GRID
        )
        
        # Add multiple charts with different configurations
        for i, metric in enumerate(available_metrics[:10]):  # First 10 metrics
            chart_type = ChartType.LINE if i % 2 == 0 else ChartType.AREA
            chart = ChartConfig(
                name=f"perf_chart_{i}",
                title=f"Performance Chart {i}",
                chart_type=chart_type,
                metrics=[metric],
                y_axis=YAxisConfig(title=f"Metric {i}", type=YAxisType.LINEAR)
            )
            dashboard_config.add_chart(chart)
        
        # Measure dashboard generation time
        start_time = time.time()
        dashboard_data = self.chart_service.create_dashboard(collection, dashboard_config)
        generation_time = time.time() - start_time
        
        print(f"Complex dashboard generation time: {generation_time:.2f} seconds")
        print(f"Number of charts generated: {len(dashboard_data.charts)}")
        
        # Performance assertions
        assert generation_time < 10.0  # Should generate in under 10 seconds
        assert len(dashboard_data.charts) == 10
        
        # Test HTML generation performance
        start_time = time.time()
        dashboard_html = self.chart_service.create_dashboard_html(dashboard_data)
        html_time = time.time() - start_time
        
        print(f"HTML generation time: {html_time:.2f} seconds")
        assert html_time < 3.0  # Should generate HTML in under 3 seconds
        
        # Clean up
        self.parser_service.delete_file(file_id)
    
    def test_stress_test_under_load(self):
        """Stress test the system under heavy load."""
        num_files = 20
        file_ids = []
        start_time = time.time()
        
        try:
            # Create and process many files rapidly
            for i in range(num_files):
                # Create file with moderate size
                file_content = self._create_large_sar_content(200)
                file_path = os.path.join(self.test_dir, f"stress_test_{i}.sar")
                
                with open(file_path, 'w') as f:
                    f.write(file_content)
                
                # Parse file
                parsed_data = self.parser_service.parse_file(file_path)
                file_ids.append(parsed_data.file_id)
                
                # Create simple chart
                collection = self.parser_service.get_time_series_collection(parsed_data.file_id)
                dashboard_config = DashboardConfig(
                    name=f"Stress Test {i}",
                    title=f"Stress Test {i}",
                    layout=ChartLayout.GRID
                )
                
                # Add a simple chart
                if len(collection.list_series()) > 0:
                    first_metric = collection.list_series()[0]
                    chart = ChartConfig(
                        name=f"stress_chart_{i}",
                        title=f"Stress Chart {i}",
                        chart_type=ChartType.LINE,
                        metrics=[first_metric]
                    )
                    dashboard_config.add_chart(chart)
                    
                    # Generate dashboard
                    dashboard_data = self.chart_service.create_dashboard(collection, dashboard_config)
                    assert dashboard_data is not None
                
                # Progress indicator
                if (i + 1) % 5 == 0:
                    print(f"Processed {i + 1}/{num_files} files...")
            
            total_time = time.time() - start_time
            print(f"Stress test completed in {total_time:.2f} seconds")
            print(f"Average time per file: {total_time / num_files:.2f} seconds")
            
            # Performance assertions
            assert total_time < 120.0  # Should complete in under 2 minutes
            assert len(file_ids) == num_files
            
        finally:
            # Clean up all files
            for file_id in file_ids:
                try:
                    self.parser_service.delete_file(file_id)
                except:
                    pass
    
    def test_error_recovery_performance(self):
        """Test system performance during error recovery."""
        # Create valid file
        valid_content = self._create_large_sar_content(300)
        valid_file_path = os.path.join(self.test_dir, "valid_recovery.sar")
        
        with open(valid_file_path, 'w') as f:
            f.write(valid_content)
        
        # Process valid file
        start_time = time.time()
        parsed_data = self.parser_service.parse_file(valid_file_path)
        valid_processing_time = time.time() - start_time
        
        print(f"Valid file processing time: {valid_processing_time:.2f} seconds")
        
        # Try to process invalid files (should fail fast)
        invalid_files = [
            "nonexistent_file.sar",
            os.path.join(self.test_dir, "empty.sar"),
            os.path.join(self.test_dir, "corrupted.sar")
        ]
        
        # Create empty and corrupted files
        with open(os.path.join(self.test_dir, "empty.sar"), 'w') as f:
            f.write("")
        
        with open(os.path.join(self.test_dir, "corrupted.sar"), 'w') as f:
            f.write("This is not a valid SAR file\n" * 100)
        
        error_processing_times = []
        
        for invalid_file in invalid_files:
            try:
                start_time = time.time()
                self.parser_service.parse_file(invalid_file)
                # If we get here, it means the file was processed (which is unexpected)
                error_processing_times.append(time.time() - start_time)
            except Exception:
                # Expected error
                error_processing_times.append(time.time() - start_time)
        
        # Error processing should be fast (fail fast principle)
        avg_error_time = sum(error_processing_times) / len(error_processing_times)
        print(f"Average error processing time: {avg_error_time:.2f} seconds")
        
        # Error processing should be much faster than valid processing
        assert avg_error_time < valid_processing_time * 0.1  # 10x faster
        
        # Clean up
        if parsed_data:
            self.parser_service.delete_file(parsed_data.file_id) 