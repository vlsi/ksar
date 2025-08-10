"""
Tests for the ParserService class.
"""

import pytest
import tempfile
import os
from unittest.mock import Mock, patch, mock_open
from datetime import datetime

from app.services.parser_service import ParserService
from app.models.time_series import ParsedData, SystemInfo, MetricData


class TestParserService:
    """Test cases for ParserService"""
    
    def setup_method(self):
        """Set up test fixtures before each test method."""
        self.parser_service = ParserService()
        self.sample_sar_content = """Linux 3.10.0-1160.53.1.el7.x86_64 (test-host.example.com) 04/08/18 _x86_64_ (4 CPU)

16:08:45        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle
16:08:55        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99
16:09:05        all      4.89      0.00      4.67      0.12      0.00      0.00      0.00      0.00      0.00     90.32

16:08:45    kbmemfree   kbmemused  %memused kbbuffers  kbcached  kbcommit   %commit
16:08:55     1234567    8765432      87.65    123456    654321   9876543     98.76
16:09:05     1234568    8765431      87.64    123457    654322   9876542     98.75"""
    
    def test_parse_file_success(self):
        """Test successful file parsing."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                
                assert parsed_data is not None
                assert isinstance(parsed_data, ParsedData)
                assert parsed_data.file_id is not None
                assert parsed_data.system_info.os_type == "Linux"
                assert parsed_data.system_info.hostname == "test-host.example.com"
                assert parsed_data.system_info.nb_cpu == "4"
                assert len(parsed_data.metrics) > 0
                
            finally:
                os.unlink(temp_file.name)
    
    def test_parse_file_not_found(self):
        """Test parsing a non-existent file."""
        with pytest.raises(FileNotFoundError):
            self.parser_service.parse_file("/nonexistent/file.sar")
    
    def test_parse_file_empty(self):
        """Test parsing an empty file."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write("")
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                # Should handle empty file gracefully
                assert parsed_data is not None
            finally:
                os.unlink(temp_file.name)
    
    def test_get_file_info(self):
        """Test retrieving file information."""
        # First parse a file to create file info
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                # Get file info
                file_info = self.parser_service.get_file_info(file_id)
                
                assert file_info is not None
                assert file_info.file_id == file_id
                assert file_info.filename == os.path.basename(temp_file.name)
                assert file_info.upload_time is not None
                assert file_info.status == "parsed"
                
            finally:
                os.unlink(temp_file.name)
    
    def test_get_file_info_not_found(self):
        """Test retrieving file info for non-existent file."""
        file_info = self.parser_service.get_file_info("nonexistent-id")
        assert file_info is None
    
    def test_get_time_series_collection(self):
        """Test getting time series collection."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                collection = self.parser_service.get_time_series_collection(file_id)
                
                assert collection is not None
                assert len(collection.list_series()) > 0
                
                # Check that CPU metrics are present
                cpu_series = [s for s in collection.list_series() if s.startswith('cpu_')]
                assert len(cpu_series) > 0
                
            finally:
                os.unlink(temp_file.name)
    
    def test_get_time_series_collection_not_found(self):
        """Test getting time series collection for non-existent file."""
        collection = self.parser_service.get_time_series_collection("nonexistent-id")
        assert collection is None
    
    def test_delete_file(self):
        """Test file deletion."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                # Verify file exists
                file_info = self.parser_service.get_file_info(file_id)
                assert file_info is not None
                
                # Delete file
                success = self.parser_service.delete_file(file_id)
                assert success is True
                
                # Verify file is deleted
                file_info = self.parser_service.get_file_info(file_id)
                assert file_info is None
                
            finally:
                os.unlink(temp_file.name)
    
    def test_delete_file_not_found(self):
        """Test deleting non-existent file."""
        success = self.parser_service.delete_file("nonexistent-id")
        assert success is False
    
    def test_list_files(self):
        """Test listing all files."""
        # Parse a file first
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                
                # List files
                files = self.parser_service.list_files()
                
                assert isinstance(files, list)
                assert len(files) > 0
                
                # Check that our file is in the list
                file_ids = [f.file_id for f in files]
                assert parsed_data.file_id in file_ids
                
            finally:
                os.unlink(temp_file.name)
    
    def test_get_parse_status(self):
        """Test getting parse status."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                status = self.parser_service.get_parse_status(file_id)
                
                assert status is not None
                assert status["status"] == "completed"
                assert status["file_id"] == file_id
                assert "metrics_count" in status
                assert "samples_count" in status
                
            finally:
                os.unlink(temp_file.name)
    
    def test_get_parse_status_not_found(self):
        """Test getting parse status for non-existent file."""
        status = self.parser_service.get_parse_status("nonexistent-id")
        assert status is None
    
    def test_get_parsed_data(self):
        """Test getting parsed data."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                # Get all data
                data = self.parser_service.get_parsed_data(file_id)
                assert data is not None
                assert "metrics" in data
                assert "system_info" in data
                
                # Get specific series
                data = self.parser_service.get_parsed_data(file_id, series_name="cpu_all")
                assert data is not None
                assert "cpu_all" in data["metrics"]
                
            finally:
                os.unlink(temp_file.name)
    
    def test_get_parsed_data_not_found(self):
        """Test getting parsed data for non-existent file."""
        data = self.parser_service.get_parsed_data("nonexistent-id")
        assert data is None
    
    def test_get_available_metrics(self):
        """Test getting available metrics."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                file_id = parsed_data.file_id
                
                metrics = self.parser_service.get_available_metrics(file_id)
                
                assert isinstance(metrics, list)
                assert len(metrics) > 0
                
                # Check that expected metrics are present
                metric_names = [m["name"] for m in metrics]
                assert any("cpu" in name for name in metric_names)
                
            finally:
                os.unlink(temp_file.name)
    
    def test_get_available_metrics_not_found(self):
        """Test getting available metrics for non-existent file."""
        metrics = self.parser_service.get_available_metrics("nonexistent-id")
        assert metrics == []
    
    def test_parse_file_with_invalid_content(self):
        """Test parsing file with invalid SAR content."""
        invalid_content = "This is not a valid SAR file content\nJust some random text"
        
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(invalid_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                
                # Should handle invalid content gracefully
                assert parsed_data is not None
                # May have minimal data or empty metrics
                
            finally:
                os.unlink(temp_file.name)
    
    def test_parse_file_with_mixed_content(self):
        """Test parsing file with mixed valid and invalid content."""
        mixed_content = """Linux 3.10.0-1160.53.1.el7.x86_64 (test-host.example.com) 04/08/18 _x86_64_ (4 CPU)

16:08:45        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle
16:08:55        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99

Some invalid lines here
Random text that should be ignored
More invalid content

16:09:05        all      4.89      0.00      4.67      0.12      0.00      0.00      0.00      0.00      0.00     90.32"""
        
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(mixed_content)
            temp_file.flush()
            
            try:
                parsed_data = self.parser_service.parse_file(temp_file.name)
                
                assert parsed_data is not None
                assert len(parsed_data.metrics) > 0
                
                # Should still parse valid data despite invalid lines
                cpu_metrics = [k for k in parsed_data.metrics.keys() if 'cpu' in k]
                assert len(cpu_metrics) > 0
                
            finally:
                os.unlink(temp_file.name) 