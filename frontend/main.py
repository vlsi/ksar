#!/usr/bin/env python3
"""
kSar Frontend - Main Entry Point
A Locust-based web dashboard for visualizing SAR data
"""

import os
import sys
import argparse
from pathlib import Path

# Add src directory to Python path
src_path = Path(__file__).parent / "src"
sys.path.insert(0, str(src_path))

def main():
    """Main entry point for the kSar frontend"""
    parser = argparse.ArgumentParser(description="kSar Web Dashboard")
    parser.add_argument(
        "--host", 
        default="0.0.0.0", 
        help="Host to bind to (default: 0.0.0.0)"
    )
    parser.add_argument(
        "--port", 
        type=int, 
        default=3000, 
        help="Port to bind to (default: 3000)"
    )
    parser.add_argument(
        "--backend-url", 
        default="http://localhost:5000", 
        help="Backend API URL (default: http://localhost:5000)"
    )
    parser.add_argument(
        "--workers", 
        type=int, 
        default=1, 
        help="Number of worker processes (default: 1)"
    )
    
    args = parser.parse_args()
    
    # Set environment variables
    os.environ["KSAR_BACKEND_URL"] = args.backend_url
    
    print(f"🚀 Starting kSar Web Dashboard...")
    print(f"   Frontend: http://{args.host}:{args.port}")
    print(f"   Backend: {args.backend_url}")
    print(f"   Workers: {args.workers}")
    print()
    
    try:
        # Import and run Locust
        from locust import main as locust_main
        
        # Set Locust arguments
        sys.argv = [
            "locust",
            "--host", args.backend_url,
            "--web-host", args.host,
            "--web-port", str(args.port),
            "--locustfile", str(src_path / "locustfile.py"),
            "--workers", str(args.workers)
        ]
        
        # Run Locust
        locust_main()
        
    except ImportError as e:
        print(f"❌ Error importing Locust: {e}")
        print("Please install required dependencies:")
        print("  pip install -r requirements.txt")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Error starting dashboard: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main() 