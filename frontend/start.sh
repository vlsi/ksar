#!/bin/bash

# kSar Frontend Startup Script
echo "🚀 Starting kSar Web Dashboard..."

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "📦 Creating virtual environment..."
    python3 -m venv venv
fi

# Activate virtual environment
echo "🔧 Activating virtual environment..."
source venv/bin/activate

# Install dependencies
echo "📥 Installing dependencies..."
pip install -r requirements.txt

# Start the dashboard
echo "🌐 Starting dashboard on http://localhost:3000"
echo "📊 Backend API: $KSAR_BACKEND_URL"
echo ""
echo "Press Ctrl+C to stop"
echo ""

python main.py "$@" 