# kSar Web Application

[![Build Status](https://github.com/vlsi/ksar/workflows/Test/badge.svg?branch=master)](https://github.com/vlsi/ksar/actions?query=branch%3Amaster)

A modern web-based SAR (System Activity Reporter) graphing and analysis tool built with Python FastAPI backend and Locust-based frontend. This application can parse and visualize SAR files from Linux, macOS, and Solaris systems, providing interactive charts and dashboards using pure Python.

## Features

- **Web-based Interface**: Modern Locust-based frontend with matplotlib charts
- **SAR File Parsing**: Support for Linux, macOS, and Solaris SAR files
- **RESTful API**: FastAPI-based backend with automatic documentation
- **Interactive Charts**: Matplotlib-powered visualizations using pure Python
- **File Upload**: SAR file upload via backend API
- **Real-time Processing**: Asynchronous file parsing and data extraction
- **Docker Support**: Containerized deployment with Docker Compose
- **Pure Python**: No Node.js or JavaScript required

## Prerequisites

- **Python 3.8+** (for backend and frontend)
- **Docker & Docker Compose** (for containerized deployment)
- **PostgreSQL** (for data storage, included in Docker setup)

## Quick Start

### Option 1: Docker Compose (Recommended)

1. **Clone the repository**:
   ```bash
   git clone https://github.com/vlsi/ksar.git
   cd ksar
   ```

2. **Start all services**:
   ```bash
   docker-compose up -d
   ```

3. **Access the application**:
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:5000
   - API Documentation: http://localhost:5000/docs

### Option 2: Local Development

#### Backend Setup

1. **Navigate to backend directory**:
   ```bash
   cd backend
   ```

2. **Create virtual environment**:
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

4. **Set environment variables** (create `.env` file):
   ```bash
   DATABASE_URL=postgresql://user:password@localhost:5432/database
   ```

5. **Start PostgreSQL** (if not using Docker):
   ```bash
   # Using Docker
   docker run -d --name postgres -e POSTGRES_USER=user -e POSTGRES_PASSWORD=password -e POSTGRES_DB=database -p 5432:5432 postgres
   
   # Or install PostgreSQL locally
   ```

6. **Run the backend**:
   ```bash
   uvicorn app.main:app --reload --host 0.0.0.0 --port 5000
   ```

#### Frontend Setup

1. **Navigate to frontend directory**:
   ```bash
   cd frontend
   ```

2. **Create virtual environment**:
   ```bash
   python3 -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

4. **Start the dashboard**:
   ```bash
   python main.py
   ```

## Testing

### Backend Tests

1. **Navigate to backend directory**:
   ```bash
   cd backend
   source venv/bin/activate
   ```

2. **Run all tests**:
   ```bash
   pytest tests/ -v
   ```

3. **Run specific test categories**:
   ```bash
   # Unit tests
   pytest tests/test_models.py -v
   pytest tests/test_parsers.py -v
   pytest tests/test_services.py -v
   
   # Integration tests
   pytest tests/test_integration_full_pipeline.py -v
   pytest tests/test_api_integration.py -v
   pytest tests/test_service_integration.py -v
   
   # Performance tests
   pytest tests/test_performance_integration.py -v
   ```

4. **Run tests with coverage**:
   ```bash
   pytest tests/ --cov=app --cov-report=html
   ```

### Frontend Tests

1. **Navigate to frontend directory**:
   ```bash
   cd frontend
   ```

2. **Install test dependencies**:
   ```bash
   pip install pytest pytest-cov
   ```

3. **Run tests**:
   ```bash
   pytest tests/ -v
   ```

4. **Run tests with coverage**:
   ```bash
   pytest tests/ --cov=src --cov-report=html
   ```

## Building

### Backend Build

The backend is a Python application that doesn't require compilation. For production deployment:

1. **Install production dependencies**:
   ```bash
   cd backend
   source venv/bin/activate
   pip install -r requirements.txt
   ```

2. **Create production environment**:
   ```bash
   # Set production environment variables
   export ENVIRONMENT=production
   export DATABASE_URL=your_production_database_url
   ```

### Frontend Build

The frontend is a pure Python application that doesn't require compilation. For production deployment:

1. **Navigate to frontend directory**:
   ```bash
   cd frontend
   ```

2. **Install production dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

3. **Start the application**:
   ```bash
   python main.py --host 0.0.0.0 --port 3000
   ```

## Deployment

### Docker Deployment

1. **Production build**:
   ```bash
   docker-compose -f docker-compose.prod.yml up -d
   ```

2. **Scale services**:
   ```bash
   docker-compose up -d --scale backend=3
   ```

### Manual Deployment

#### Backend Deployment

1. **Set up production server**:
   ```bash
   # Install system dependencies
   sudo apt-get update
   sudo apt-get install python3 python3-pip postgresql
   
   # Clone repository
   git clone https://github.com/vlsi/ksar.git
   cd ksar/backend
   
   # Set up virtual environment
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   
   # Set up systemd service
   sudo nano /etc/systemd/system/ksar-backend.service
   ```

2. **Systemd service file** (`/etc/systemd/system/ksar-backend.service`):
   ```ini
   [Unit]
   Description=kSar Backend
   After=network.target
   
   [Service]
   User=www-data
   WorkingDirectory=/path/to/ksar/backend
   Environment=PATH=/path/to/ksar/backend/venv/bin
   ExecStart=/path/to/ksar/backend/venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 5000
   Restart=always
   
   [Install]
   WantedBy=multi-user.target
   ```

3. **Enable and start service**:
   ```bash
   sudo systemctl enable ksar-backend
   sudo systemctl start ksar-backend
   ```

#### Frontend Deployment

1. **Set up production server**:
   ```bash
   cd frontend
   
   # Install system dependencies
   sudo apt-get update
   sudo apt-get install python3 python3-pip
   
   # Set up virtual environment
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   ```

2. **Deploy to web server**:
   ```bash
   # Copy application files to web server directory
   sudo cp -r * /var/www/ksar/
   
   # Or deploy with systemd service
   ```

3. **Nginx configuration** (`/etc/nginx/sites-available/ksar`):
   ```nginx
   server {
       listen 80;
       server_name your-domain.com;
       root /var/www/ksar;
       index index.html;
       
       location / {
           try_files $uri $uri/ /index.html;
       }
       
       location /api {
           proxy_pass http://localhost:5000;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
   }
   ```

4. **Enable site**:
   ```bash
   sudo ln -s /etc/nginx/sites-available/ksar /etc/nginx/sites-enabled/
   sudo nginx -t
   sudo systemctl reload nginx
   ```

## Environment Variables

### Backend (.env)

```bash
# Database
DATABASE_URL=postgresql://user:password@localhost:5432/database

# Application
ENVIRONMENT=development
DEBUG=true
LOG_LEVEL=INFO

# Security
SECRET_KEY=your-secret-key-here
```

### Frontend (.env)

```bash
# API Configuration
REACT_APP_API_URL=http://localhost:5000/api

# Environment
REACT_APP_ENVIRONMENT=development
```

## Project Structure

```
ksar/
├── backend/                    # Python FastAPI backend
│   ├── app/
│   │   ├── main.py           # FastAPI application
│   │   ├── api/              # API endpoints
│   │   ├── core/             # Core configuration
│   │   ├── models/           # Data models
│   │   ├── parsers/          # SAR file parsers
│   │   ├── services/         # Business logic
│   │   └── utils/            # Utility functions
│   ├── tests/                # Test suite
│   ├── requirements.txt      # Python dependencies
│   └── README.md            # Backend documentation
├── frontend/                  # React frontend
│   ├── src/
│   │   ├── components/       # React components
│   │   ├── pages/           # Page components
│   │   ├── services/        # API services
│   │   └── types/           # TypeScript types
│   ├── package.json         # Node.js dependencies
│   └── public/              # Static assets
├── docker-compose.yaml       # Docker services
├── Dockerfile-backend        # Backend container
├── Dockerfile-frontend       # Frontend container
└── README.md                # This file
```

## Development

### Adding New Features

1. **Backend**:
   - Add new models in `backend/app/models/`
   - Create parsers in `backend/app/parsers/`
   - Implement services in `backend/app/services/`
   - Add API endpoints in `backend/app/api/`

2. **Frontend**:
   - Create components in `frontend/src/components/`
   - Add pages in `frontend/src/pages/`
   - Implement services in `frontend/src/services/`

### Code Quality

1. **Backend**:
   ```bash
   cd backend
   source venv/bin/activate
   
   # Format code
   black app/ tests/
   
   # Lint code
   flake8 app/ tests/
   
   # Type checking
   mypy app/
   ```

2. **Frontend**:
   ```bash
   cd frontend
   
   # Lint code
   npm run lint
   
   # Type checking
   npm run type-check
   ```

## Troubleshooting

### Common Issues

1. **Database Connection**:
   - Ensure PostgreSQL is running
   - Check `DATABASE_URL` environment variable
   - Verify database credentials

2. **Port Conflicts**:
   - Backend default: 5000
   - Frontend default: 3000
   - Change ports in docker-compose.yaml or environment files

3. **Permission Issues**:
   - Ensure proper file permissions
   - Check user/group ownership
   - Verify virtual environment activation

### Logs

1. **Docker logs**:
   ```bash
   docker-compose logs backend
   docker-compose logs frontend
   docker-compose logs db
   ```

2. **Systemd logs** (manual deployment):
   ```bash
   sudo journalctl -u ksar-backend -f
   ```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the same license as the original kSar project.

## Support

- **Issues**: [GitHub Issues](https://github.com/vlsi/ksar/issues)
- **Discussions**: [GitHub Discussions](https://github.com/vlsi/ksar/discussions)
- **Documentation**: [Backend README](backend/README.md)
