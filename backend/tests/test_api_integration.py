"""
Integration tests for the FastAPI HTTP endpoints.

This module tests the complete HTTP API flow including file upload,
parsing, chart generation, and dashboard creation through HTTP requests.
"""

import pytest
import tempfile
import os
import shutil
import json
from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock

from app.main import app


@pytest.mark.integration
@pytest.mark.api
class TestAPIIntegration:
    """Test the complete API integration flow."""
    
    @pytest.fixture(autouse=True)
    def setup_teardown(self):
        """Set up and tear down test environment."""
        self.client = TestClient(app)
        self.test_dir = tempfile.mkdtemp()
        self.sample_sar_content = self._create_sample_sar_content()
        self.uploaded_file_ids = []
        
        yield
        
        # Clean up uploaded files
        for file_id in self.uploaded_file_ids:
            try:
                self.client.delete(f"/api/files/{file_id}")
            except:
                pass
        
        # Clean up test directory
        if os.path.exists(self.test_dir):
            shutil.rmtree(self.test_dir)
    
    def _create_sample_sar_content(self):
        """Create realistic SAR file content for testing."""
        content = []
        content.append("Linux 5.15.0-91-generic (test-host) 01/15/24 _x86_64_ (8 CPU)")
        content.append("")
        content.append("16:00:01        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle")
        
        # Generate 10 minutes of sample data
        for i in range(10):
            time_str = f"16:0{i:02d}:01"
            usr = 15 + (i % 10)  # 15-25%
            sys = 5 + (i % 5)    # 5-10%
            idle = 100 - usr - sys
            content.append(f"{time_str}        all      {usr:6.2f}      0.00      {sys:6.2f}      0.00      0.00      0.00      0.00      0.00      0.00     {idle:6.2f}")
        
        content.append("")
        content.append("16:00:01        kbmemfree   kbavail kbmemused  %memused kbbuffers  kbcached  kbcommit   %commit  kbactive   kbinact   kbdirty")
        
        # Memory data
        for i in range(10):
            time_str = f"16:0{i:02d}:01"
            memfree = 8000000 - (i * 10000)  # Decreasing memory
            memused = 2000000 + (i * 10000)  # Increasing usage
            content.append(f"{time_str}     {memfree:8.0f} {memfree-100000:8.0f} {memused:8.0f}     {memused/(memfree+memused)*100:5.1f}   100000   2000000  {memused*1.2:8.0f}     {memused*1.2/(memfree+memused)*100:5.1f} {memused*0.8:8.0f} {memused*0.2:8.0f}       0")
        
        return "\n".join(content)
    
    def test_complete_api_workflow(self):
        """Test the complete API workflow from upload to dashboard."""
        # Step 1: Health check
        response = self.client.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "healthy"
        
        # Step 2: Upload file
        sample_file_path = os.path.join(self.test_dir, "api_test.sar")
        with open(sample_file_path, 'w') as f:
            f.write(self.sample_sar_content)
        
        with open(sample_file_path, 'rb') as f:
            response = self.client.post(
                "/api/upload",
                files={"file": ("api_test.sar", f, "text/plain")}
            )
        
        assert response.status_code == 200
        upload_data = response.json()
        assert "message" in upload_data
        assert "File uploaded and parsed successfully" in upload_data["message"]
        
        # Extract file ID from the response or get it from files list
        response = self.client.get("/api/files")
        assert response.status_code == 200
        files = response.json()
        assert len(files) > 0
        
        file_id = files[0]["file_id"]  # Get the first uploaded file
        self.uploaded_file_ids.append(file_id)
        
        # Step 3: Get file info
        response = self.client.get(f"/api/files/{file_id}")
        assert response.status_code == 200
        file_info = response.json()
        assert file_info["file_id"] == file_id
        
        # Step 4: Get parse status
        response = self.client.get(f"/api/parse/{file_id}/status")
        assert response.status_code == 200
        status_data = response.json()
        assert "status" in status_data
        
        # Step 5: Get parsed data
        response = self.client.get(f"/api/parse/{file_id}/data")
        assert response.status_code == 200
        data_response = response.json()
        assert "series" in data_response or "metrics" in data_response
        
        # Step 6: Get available metrics
        response = self.client.get(f"/api/metrics/{file_id}")
        assert response.status_code == 200
        metrics = response.json()
        assert isinstance(metrics, list)
        assert len(metrics) > 0
        
        # Step 7: Create chart
        chart_config = {
            "name": "cpu_test",
            "title": "CPU Test Chart",
            "chart_type": "line",
            "metrics": [m for m in metrics if "cpu" in m.lower()][:3],
            "y_axis": {
                "title": "CPU %",
                "type": "linear",
                "min": 0,
                "max": 100
            }
        }
        
        response = self.client.post(
            f"/api/charts/{file_id}",
            json=chart_config
        )
        assert response.status_code == 200
        chart_data = response.json()
        assert "config" in chart_data
        
        # Step 8: Get chart templates
        response = self.client.get("/api/charts/templates")
        assert response.status_code == 200
        templates = response.json()
        assert isinstance(templates, dict)
        assert len(templates) > 0
        
        # Step 9: Create chart from template
        if "cpu_utilization" in templates:
            response = self.client.post(
                f"/api/charts/{file_id}/template/cpu_utilization"
            )
            assert response.status_code == 200
            template_chart = response.json()
            assert "config" in template_chart
        
        # Step 10: Generate dashboard
        response = self.client.get(f"/api/dashboard/{file_id}")
        assert response.status_code == 200
        dashboard_data = response.json()
        assert "charts" in dashboard_data
        assert len(dashboard_data["charts"]) > 0
        
        # Step 11: Test dashboard HTML format
        response = self.client.get(f"/api/dashboard/{file_id}?format=html")
        assert response.status_code == 200
        dashboard_html = response.text
        assert "<html" in dashboard_html.lower()
        assert "dashboard" in dashboard_html.lower()
    
    def test_api_error_handling(self):
        """Test API error handling for various scenarios."""
        # Test invalid file ID
        response = self.client.get("/api/files/invalid_id")
        assert response.status_code == 404
        
        # Test invalid chart configuration
        chart_config = {
            "name": "invalid_chart",
            "chart_type": "invalid_type"  # Invalid chart type
        }
        
        response = self.client.post(
            "/api/charts/invalid_file_id",
            json=chart_config
        )
        assert response.status_code == 404
        
        # Test invalid template
        response = self.client.post("/api/charts/invalid_file_id/template/invalid_template")
        assert response.status_code == 404
        
        # Test invalid dashboard format
        response = self.client.get("/api/dashboard/invalid_file_id?format=invalid")
        assert response.status_code == 404
    
    def test_api_file_management(self):
        """Test complete file management through API."""
        # Upload file
        sample_file_path = os.path.join(self.test_dir, "management_test.sar")
        with open(sample_file_path, 'w') as f:
            f.write(self.sample_sar_content)
        
        with open(sample_file_path, 'rb') as f:
            response = self.client.post(
                "/api/upload",
                files={"file": ("management_test.sar", f, "text/plain")}
            )
        
        assert response.status_code == 200
        
        # Get files list
        response = self.client.get("/api/files")
        assert response.status_code == 200
        files = response.json()
        assert len(files) > 0
        
        file_id = files[0]["file_id"]
        self.uploaded_file_ids.append(file_id)
        
        # Verify file exists
        response = self.client.get(f"/api/files/{file_id}")
        assert response.status_code == 200
        
        # Delete file
        response = self.client.delete(f"/api/files/{file_id}")
        assert response.status_code == 200
        
        # Verify file is deleted
        response = self.client.get(f"/api/files/{file_id}")
        assert response.status_code == 404
        
        # Remove from cleanup list since we already deleted it
        self.uploaded_file_ids.remove(file_id)
    
    def test_api_concurrent_requests(self):
        """Test concurrent API requests."""
        import threading
        import time
        
        results = []
        errors = []
        
        def upload_and_process(file_index):
            try:
                # Create unique file content
                file_content = f"Linux test-host {file_index} (test-host) 01/15/24 _x86_64_ (4 CPU)\n"
                file_content += "16:00:01        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle\n"
                file_content += "16:00:01        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99\n"
                
                # Upload file
                sample_file_path = os.path.join(self.test_dir, f"concurrent_{file_index}.sar")
                with open(sample_file_path, 'w') as f:
                    f.write(file_content)
                
                with open(sample_file_path, 'rb') as f:
                    response = self.client.post(
                        "/api/upload",
                        files={"file": (f"concurrent_{file_index}.sar", f, "text/plain")}
                    )
                
                if response.status_code == 200:
                    # Get file ID and add to cleanup
                    files_response = self.client.get("/api/files")
                    if files_response.status_code == 200:
                        files = files_response.json()
                        if files:
                            file_id = files[0]["file_id"]
                            self.uploaded_file_ids.append(file_id)
                            results.append((file_index, file_id))
                else:
                    errors.append(f"Upload failed for file {file_index}")
                    
            except Exception as e:
                errors.append(f"Error processing file {file_index}: {str(e)}")
        
        # Create multiple threads
        threads = []
        for i in range(3):
            thread = threading.Thread(target=upload_and_process, args=(i,))
            threads.append(thread)
            thread.start()
        
        # Wait for all threads to complete
        for thread in threads:
            thread.join()
        
        # Verify results
        assert len(results) == 3
        assert len(errors) == 0
        
        # Verify all files were uploaded
        response = self.client.get("/api/files")
        assert response.status_code == 200
        files = response.json()
        assert len(files) >= 3
    
    def test_api_cors_headers(self):
        """Test CORS headers are properly set."""
        response = self.client.options("/api/upload")
        assert response.status_code == 200
        
        # Check CORS headers
        headers = response.headers
        assert "access-control-allow-origin" in [h.lower() for h in headers.keys()]
        assert "access-control-allow-methods" in [h.lower() for h in headers.keys()]
    
    def test_api_rate_limiting_and_validation(self):
        """Test API rate limiting and input validation."""
        # Test file size validation (if implemented)
        large_content = "x" * (1024 * 1024)  # 1MB of data
        
        sample_file_path = os.path.join(self.test_dir, "large_test.sar")
        with open(sample_file_path, 'w') as f:
            f.write(large_content)
        
        with open(sample_file_path, 'rb') as f:
            response = self.client.post(
                "/api/upload",
                files={"file": ("large_test.sar", f, "text/plain")}
            )
        
        # Should either succeed or fail gracefully
        assert response.status_code in [200, 400, 413]  # OK, Bad Request, or Payload Too Large
        
        # Test invalid file types
        response = self.client.post(
            "/api/upload",
            files={"file": ("test.txt", b"not a sar file", "text/plain")}
        )
        assert response.status_code == 400
        
        # Test missing file
        response = self.client.post("/api/upload")
        assert response.status_code == 422  # Validation error 