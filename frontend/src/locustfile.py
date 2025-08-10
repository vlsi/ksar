"""
kSar Performance Testing with Locust
This file creates a Locust-based performance testing tool for the kSar backend
"""

import os
import json
import requests
import time
from typing import Dict, List, Any, Optional

from locust import HttpUser, task, between
from locust import events

class KSarPerformanceUser(HttpUser):
    """
    Performance testing user for kSar backend
    Tests the parsing and data retrieval endpoints
    """
    wait_time = between(1, 3)
    host = "http://localhost:5000"  # Backend API URL
    
    def on_start(self):
        """Called when a user starts"""
        self.client.headers.update({
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        })
        print(f"🚀 Starting performance test user against {self.host}")
    
    @task(3)
    def test_health_check(self):
        """Test the health check endpoint"""
        with self.client.get("/", catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Health check failed: {response.status_code}")
    
    @task(2)
    def test_get_files(self):
        """Test getting the list of uploaded files"""
        with self.client.get("/api/files", catch_response=True) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    if isinstance(data, list):
                        response.success()
                        print(f"📁 Found {len(data)} files")
                    else:
                        response.failure("Expected list of files")
                except json.JSONDecodeError:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"Get files failed: {response.status_code}")
    
    @task(1)
    def test_upload_sample_data(self):
        """Test uploading sample SAR data"""
        sample_sar_data = """Linux 5.4.0-74-generic (test-system) 	12/01/2023 	_x86_64_	(4 CPU)

12:00:01 AM     CPU     %user     %nice   %system   %iowait    %steal     %idle
12:05:01 AM     all      2.50      0.00      1.25      0.25      0.00     96.00
12:10:01 AM     all      3.75      0.00      1.50      0.50      0.00     94.25
12:15:01 AM     all      2.25      0.00      1.00      0.25      0.00     96.50
Average:        all      2.83      0.00      1.25      0.33      0.00     95.58
"""
        
        files = {
            'file': ('test_sar_data.txt', sample_sar_data, 'text/plain')
        }
        
        with self.client.post("/api/upload", files=files, catch_response=True) as response:
            if response.status_code in [200, 201]:
                try:
                    data = response.json()
                    response.success()
                    print(f"📤 Upload successful: {data}")
                except json.JSONDecodeError:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"Upload failed: {response.status_code}")

@events.init_command_line_parser.add_listener
def _(parser):
    """Add custom command line arguments"""
    parser.add_argument("--backend-url", type=str, default="http://localhost:5000", 
                       help="Backend API URL")

@events.init.add_listener
def _(environment, **kwargs):
    """Initialize the test environment"""
    if environment.parsed_options.backend_url:
        KSarPerformanceUser.host = environment.parsed_options.backend_url
    
    print("🔧 kSar Performance Testing Setup")
    print(f"   Backend URL: {KSarPerformanceUser.host}")
    print(f"   Web UI: http://localhost:8080")
    print("   Ready to start testing!")

if __name__ == "__main__":
    # This allows running the file directly for testing
    import sys
    from locust.main import main
    
    sys.argv = [
        "locust",
        "-f", __file__,
        "--web-port", "8080",
        "--host", "http://localhost:5000"
    ]
    main()