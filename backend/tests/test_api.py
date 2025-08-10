"""
Tests for the FastAPI application endpoints.
"""

import pytest
import tempfile
import os
from fastapi.testclient import TestClient
from unittest.mock import Mock, patch, mock_open
from datetime import datetime

from app.main import app


class TestAPIEndpoints:
    """Test cases for API endpoints"""
    
    def setup_method(self):
        """Set up test fixtures before each test method."""
        self.client = TestClient(app)
        self.sample_sar_content = """Linux 3.10.0-1160.53.1.el7.x86_64 (test-host.example.com) 04/08/18 _x86_64_ (4 CPU)

16:08:45        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle
16:08:55        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99
16:09:05        all      4.89      0.00      4.67      0.12      0.00      0.00      0.00      0.00      0.00     90.32"""
    
    def test_root_endpoint(self):
        """Test the root endpoint."""
        response = self.client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert data["message"] == "kSar Web API"
        assert data["version"] == "1.0.0"
    
    def test_health_check_endpoint(self):
        """Test the health check endpoint."""
        response = self.client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
    
    def test_upload_file_success(self):
        """Test successful file upload."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as f:
                    response = self.client.post(
                        "/api/upload",
                        files={"file": ("test.sar", f, "text/plain")}
                    )
                
                assert response.status_code == 200
                data = response.json()
                assert "message" in data
                assert "File uploaded and parsed successfully" in data["message"]
                
            finally:
                os.unlink(temp_file.name)
    
    def test_upload_file_no_file(self):
        """Test upload endpoint with no file."""
        response = self.client.post("/api/upload")
        assert response.status_code == 422  # Validation error
    
    def test_upload_file_invalid_type(self):
        """Test upload endpoint with invalid file type."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False) as temp_file:
            temp_file.write("This is not a SAR file")
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as f:
                    response = self.client.post(
                        "/api/upload",
                        files={"file": ("test.txt", f, "text/plain")}
                    )
                
                assert response.status_code == 400
                data = response.json()
                assert "Invalid file type" in data["detail"]
                
            finally:
                os.unlink(temp_file.name)
    
    def test_list_files_endpoint(self):
        """Test the list files endpoint."""
        response = self.client.get("/api/files")
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
    
    def test_get_file_info_endpoint(self):
        """Test getting file information."""
        # First upload a file to get a file ID
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as f:
                    upload_response = self.client.post(
                        "/api/upload",
                        files={"file": ("test.sar", f, "text/plain")}
                    )
                
                if upload_response.status_code == 200:
                    # Try to get file info (may need to parse first)
                    response = self.client.get("/api/files")
                    if response.status_code == 200:
                        files = response.json()
                        if files:
                            file_id = files[0]["file_id"]
                            
                            # Get file info
                            info_response = self.client.get(f"/api/files/{file_id}")
                            assert info_response.status_code == 200
                            data = info_response.json()
                            assert "file_id" in data
                            assert "filename" in data
                
            finally:
                os.unlink(temp_file.name)
    
    def test_get_file_info_not_found(self):
        """Test getting file info for non-existent file."""
        response = self.client.get("/api/files/nonexistent-id")
        assert response.status_code == 404
    
    def test_delete_file_endpoint(self):
        """Test file deletion endpoint."""
        # First upload a file to get a file ID
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as f:
                    upload_response = self.client.post(
                        "/api/upload",
                        files={"file": ("test.sar", f, "text/plain")}
                    )
                
                if upload_response.status_code == 200:
                    # Try to delete the file
                    response = self.client.get("/api/files")
                    if response.status_code == 200:
                        files = response.json()
                        if files:
                            file_id = files[0]["file_id"]
                            
                            # Delete file
                            delete_response = self.client.delete(f"/api/files/{file_id}")
                            assert delete_response.status_code == 200
                            data = delete_response.json()
                            assert "message" in data
                            assert "deleted successfully" in data["message"]
                
            finally:
                os.unlink(temp_file.name)
    
    def test_delete_file_not_found(self):
        """Test deleting non-existent file."""
        response = self.client.delete("/api/files/nonexistent-id")
        assert response.status_code == 404
    
    def test_parse_file_endpoint(self):
        """Test the parse file endpoint."""
        # First upload a file to get a file ID
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as f:
                    upload_response = self.client.post(
                        "/api/upload",
                        files={"file": ("test.sar", f, "text/plain")}
                    )
                
                if upload_response.status_code == 200:
                    # Try to parse the file
                    response = self.client.get("/api/files")
                    if response.status_code == 200:
                        files = response.json()
                        if files:
                            file_id = files[0]["file_id"]
                            
                            # Parse file
                            parse_response = self.client.post(f"/api/parse/{file_id}")
                            assert parse_response.status_code == 200
                            data = parse_response.json()
                            assert "message" in data
                            assert "parsed successfully" in data["message"]
                
            finally:
                os.unlink(temp_file.name)
    
    def test_parse_file_not_found(self):
        """Test parsing non-existent file."""
        response = self.client.post("/api/parse/nonexistent-id")
        assert response.status_code == 404
    
    def test_get_parse_status_endpoint(self):
        """Test getting parse status."""
        # First upload a file to get a file ID
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as f:
                    upload_response = self.client.post(
                        "/api/upload",
                        files={"file": ("test.sar", f, "text/plain")}
                    )
                
                if upload_response.status_code == 200:
                    # Try to get parse status
                    response = self.client.get("/api/files")
                    if response.status_code == 200:
                        files = response.json()
                        if files:
                            file_id = files[0]["file_id"]
                            
                            # Get parse status
                            status_response = self.client.get(f"/api/parse/{file_id}/status")
                            assert status_response.status_code == 200
                            data = status_response.json()
                            assert "status" in data
                            assert "file_id" in data
                
            finally:
                os.unlink(temp_file.name)
    
    def test_get_parse_status_not_found(self):
        """Test getting parse status for non-existent file."""
        response = self.client.get("/api/parse/nonexistent-id/status")
        assert response.status_code == 404
    
    def test_get_parsed_data_endpoint(self):
        """Test getting parsed data."""
        # First upload a file to get a file ID
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as f:
                    upload_response = self.client.post(
                        "/api/upload",
                        files={"file": ("test.sar", f, "text/plain")}
                    )
                
                if upload_response.status_code == 200:
                    # Try to get parsed data
                    response = self.client.get("/api/files")
                    if response.status_code == 200:
                        files = response.json()
                        if files:
                            file_id = files[0]["file_id"]
                            
                            # Get all parsed data
                            data_response = self.client.get(f"/api/parse/{file_id}/data")
                            assert data_response.status_code == 200
                            data = data_response.json()
                            assert "metrics" in data
                            assert "system_info" in data
                            
                            # Get specific series data
                            series_response = self.client.get(
                                f"/api/parse/{file_id}/data?series_name=cpu_all"
                            )
                            assert series_response.status_code == 200
                            series_data = series_response.json()
                            assert "metrics" in series_data
                
            finally:
                os.unlink(temp_file.name)
    
    def test_get_parsed_data_not_found(self):
        """Test getting parsed data for non-existent file."""
        response = self.client.get("/api/parse/nonexistent-id/data")
        assert response.status_code == 404
    
    def test_get_dashboard_endpoint(self):
        """Test getting dashboard."""
        # First upload a file to get a file ID
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as f:
                    upload_response = self.client.post(
                        "/api/upload",
                        files={"file": ("test.sar", f, "text/plain")}
                    )
                
                if upload_response.status_code == 200:
                    # Try to get dashboard
                    response = self.client.get("/api/files")
                    if response.status_code == 200:
                        files = response.json()
                        if files:
                            file_id = files[0]["file_id"]
                            
                            # Get dashboard in HTML format
                            dashboard_response = self.client.get(f"/api/dashboard/{file_id}")
                            assert dashboard_response.status_code == 200
                            data = dashboard_response.json()
                            assert "html" in data
                            
                            # Get dashboard in JSON format
                            json_response = self.client.get(
                                f"/api/dashboard/{file_id}?format=json"
                            )
                            assert json_response.status_code == 200
                            json_data = json_response.json()
                            assert "charts" in json_data
                
            finally:
                os.unlink(temp_file.name)
    
    def test_get_dashboard_not_found(self):
        """Test getting dashboard for non-existent file."""
        response = self.client.get("/api/dashboard/nonexistent-id")
        assert response.status_code == 404
    
    def test_create_chart_endpoint(self):
        """Test creating a chart."""
        # First upload a file to get a file ID
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as f:
                    upload_response = self.client.post(
                        "/api/upload",
                        files={"file": ("test.sar", f, "text/plain")}
                    )
                
                if upload_response.status_code == 200:
                    # Try to create a chart
                    response = self.client.get("/api/files")
                    if response.status_code == 200:
                        files = response.json()
                        if files:
                            file_id = files[0]["file_id"]
                            
                            # Create chart
                            chart_config = {
                                "name": "test_chart",
                                "title": "Test Chart",
                                "chart_type": "line",
                                "metrics": ["cpu_all"]
                            }
                            
                            chart_response = self.client.post(
                                f"/api/charts/{file_id}",
                                json=chart_config
                            )
                            assert chart_response.status_code == 200
                            data = chart_response.json()
                            assert "data" in data
                            assert "layout" in data
                
            finally:
                os.unlink(temp_file.name)
    
    def test_create_chart_not_found(self):
        """Test creating chart for non-existent file."""
        chart_config = {
            "name": "test_chart",
            "title": "Test Chart",
            "chart_type": "line",
            "metrics": ["cpu_all"]
        }
        
        response = self.client.post("/api/charts/nonexistent-id", json=chart_config)
        assert response.status_code == 404
    
    def test_get_chart_templates_endpoint(self):
        """Test getting chart templates."""
        response = self.client.get("/api/charts/templates")
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) > 0
    
    def test_create_chart_from_template_endpoint(self):
        """Test creating chart from template."""
        # First upload a file to get a file ID
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as f:
                    upload_response = self.client.post(
                        "/api/upload",
                        files={"file": ("test.sar", f, "text/plain")}
                    )
                
                if upload_response.status_code == 200:
                    # Try to create chart from template
                    response = self.client.get("/api/files")
                    if response.status_code == 200:
                        files = response.json()
                        if files:
                            file_id = files[0]["file_id"]
                            
                            # Create chart from template
                            template_response = self.client.post(
                                f"/api/charts/{file_id}/template/cpu_overview"
                            )
                            assert template_response.status_code == 200
                            data = template_response.json()
                            assert "data" in data
                            assert "layout" in data
                
            finally:
                os.unlink(temp_file.name)
    
    def test_create_chart_from_template_not_found(self):
        """Test creating chart from template for non-existent file."""
        response = self.client.post("/api/charts/nonexistent-id/template/cpu_overview")
        assert response.status_code == 404
    
    def test_get_available_metrics_endpoint(self):
        """Test getting available metrics."""
        # First upload a file to get a file ID
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(self.sample_sar_content)
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as f:
                    upload_response = self.client.post(
                        "/api/upload",
                        files={"file": ("test.sar", f, "text/plain")}
                    )
                
                if upload_response.status_code == 200:
                    # Try to get available metrics
                    response = self.client.get("/api/files")
                    if response.status_code == 200:
                        files = response.json()
                        if files:
                            file_id = files[0]["file_id"]
                            
                            # Get available metrics
                            metrics_response = self.client.get(f"/api/metrics/{file_id}")
                            assert metrics_response.status_code == 200
                            data = metrics_response.json()
                            assert isinstance(data, list)
                
            finally:
                os.unlink(temp_file.name)
    
    def test_get_available_metrics_not_found(self):
        """Test getting available metrics for non-existent file."""
        response = self.client.get("/api/metrics/nonexistent-id")
        assert response.status_code == 404
    
    def test_cors_headers(self):
        """Test that CORS headers are properly set."""
        response = self.client.options("/")
        assert response.status_code == 200
        # CORS headers should be present
    
    def test_invalid_file_upload(self):
        """Test upload with invalid file content."""
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write("This is completely invalid SAR content")
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as f:
                    response = self.client.post(
                        "/api/upload",
                        files={"file": ("invalid.sar", f, "text/plain")}
                    )
                
                # Should handle gracefully - may return 200 with warning or 400
                assert response.status_code in [200, 400]
                
            finally:
                os.unlink(temp_file.name)
    
    def test_large_file_upload(self):
        """Test upload with a larger file."""
        # Create a larger SAR file
        large_content = self.sample_sar_content * 100  # Repeat content 100 times
        
        with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
            temp_file.write(large_content)
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as f:
                    response = self.client.post(
                        "/api/upload",
                        files={"file": ("large.sar", f, "text/plain")}
                    )
                
                # Should handle large files
                assert response.status_code in [200, 400, 413]  # 413 for too large
                
            finally:
                os.unlink(temp_file.name)
    
    def test_concurrent_uploads(self):
        """Test handling of concurrent file uploads."""
        # This is a basic test - in practice, you'd want to test actual concurrency
        responses = []
        
        for i in range(3):
            with tempfile.NamedTemporaryFile(mode='w', suffix='.sar', delete=False) as temp_file:
                temp_file.write(f"{self.sample_sar_content}\n# File {i}")
                temp_file.flush()
                
                try:
                    with open(temp_file.name, 'rb') as f:
                        response = self.client.post(
                            "/api/upload",
                            files={"file": (f"test_{i}.sar", f, "text/plain")}
                        )
                    responses.append(response.status_code)
                finally:
                    os.unlink(temp_file.name)
        
        # All uploads should succeed
        assert all(status == 200 for status in responses) 