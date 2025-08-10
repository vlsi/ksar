# kSar Frontend - Pure Python Performance Dashboard

A **pure Python** web dashboard for visualizing SAR (System Activity Reporter) data, built on top of the Locust performance testing framework.

## 🚀 Features

- **Pure Python Implementation**: No Node.js or JavaScript required
- **Locust Integration**: Built-in performance testing capabilities
- **Matplotlib Charts**: Server-side chart generation using matplotlib
- **Real-time Data**: Live updates from SAR file parsing
- **Responsive Design**: Modern, mobile-friendly interface
- **Performance Metrics**: CPU, Memory, Disk, Network, and Load monitoring

## 🏗️ Architecture

The frontend is built using:
- **Locust Framework**: For web server and performance testing
- **Flask**: For web routes and template rendering
- **Matplotlib**: For server-side chart generation
- **Jinja2**: For HTML templating
- **Pure CSS**: No JavaScript dependencies

## 📊 Chart Types

- **CPU Utilization**: Real-time CPU usage over time
- **Memory Usage**: Memory consumption patterns
- **Disk I/O**: Read/write performance metrics
- **Network Activity**: Network I/O monitoring
- **System Load**: Load average trends

## 🛠️ Installation

### Prerequisites
- Python 3.11+
- pip package manager

### Setup
```bash
# Clone the repository
git clone <repository-url>
cd ksar/frontend

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

## 🚀 Running the Dashboard

### Development Mode
```bash
# Start the dashboard
python main.py

# With custom settings
python main.py --host 0.0.0.0 --port 3000 --backend-url http://localhost:5000
```

### Production Mode
```bash
# Using Docker
docker build -t ksar-frontend .
docker run -p 3000:3000 ksar-frontend

# Or with docker-compose
docker-compose up frontend
```

## 🔧 Configuration

### Environment Variables
- `KSAR_BACKEND_URL`: Backend API URL (default: http://localhost:5000)
- `MATPLOTLIB_BACKEND`: Matplotlib backend (default: Agg)

### Command Line Options
- `--host`: Host to bind to (default: 0.0.0.0)
- `--port`: Port to bind to (default: 3000)
- `--backend-url`: Backend API URL
- `--workers`: Number of worker processes (default: 1)

## 📱 Usage

1. **Access Dashboard**: Open http://localhost:3000 in your browser
2. **View Files**: See all uploaded SAR files and their status
3. **Analyze Data**: Click on files to view detailed performance charts
4. **Performance Testing**: Use Locust features for load testing

## 🧪 Testing

### Run Tests
```bash
# Run all tests
python -m pytest

# Run specific test file
python -m pytest tests/test_dashboard.py

# Run with coverage
python -m pytest --cov=src tests/
```

### Test Coverage
The dashboard includes comprehensive tests for:
- Chart generation
- Data parsing
- API endpoints
- Template rendering
- Performance metrics

## 🔍 API Endpoints

- `GET /`: Main dashboard page
- `GET /file/<file_id>`: File detail page with charts
- `GET /api/files`: List all uploaded files
- `GET /api/file/<file_id>/data`: Get file data in JSON format

## 📈 Performance Testing with Locust

The dashboard integrates Locust for performance testing:

```python
# Example Locust user behavior
class KSarUser(HttpUser):
    wait_time = between(1, 3)
    
    @task(3)
    def view_dashboard(self):
        self.client.get("/")
    
    @task(2)
    def view_file_list(self):
        self.client.get("/api/files")
    
    @task(1)
    def view_file_detail(self):
        # View file details
        pass
```

## 🎨 Customization

### Adding New Chart Types
1. Create a new method in `KSarDashboard` class
2. Use matplotlib to generate the chart
3. Return base64 encoded image
4. Update the template to display the chart

### Styling
- Modify `base.html` CSS for global styles
- Use CSS Grid and Flexbox for responsive layouts
- No JavaScript required for styling

## 🐛 Troubleshooting

### Common Issues

**Charts not displaying:**
- Check matplotlib backend is set to 'Agg'
- Verify image data is being generated correctly
- Check browser console for errors

**Performance issues:**
- Reduce chart resolution in matplotlib
- Use fewer data points for large datasets
- Enable caching for static content

**Backend connection:**
- Verify `KSAR_BACKEND_URL` is correct
- Check backend service is running
- Test API endpoints directly

## 📚 Dependencies

### Core Dependencies
- `locust>=2.15.0`: Performance testing framework
- `matplotlib>=3.7.0`: Chart generation
- `pandas>=2.0.0`: Data manipulation
- `numpy>=1.24.0`: Numerical operations
- `jinja2>=3.1.0`: Template engine
- `requests>=2.31.0`: HTTP client
- `flask>=2.3.0`: Web framework

### System Dependencies
- `libfreetype6-dev`: Font rendering
- `libpng-dev`: PNG image support
- `libjpeg-dev`: JPEG image support

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- **Locust Framework**: For the performance testing foundation
- **Matplotlib**: For server-side chart generation
- **Flask**: For the web framework
- **SAR Tools**: For system activity reporting inspiration

---

**Note**: This is a pure Python implementation with no JavaScript dependencies. All charts are generated server-side using matplotlib and embedded as base64 images in the HTML. 