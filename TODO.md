# Web Application Design Plan: kSar Web

## Overview
A Python FastAPI-based web application that allows users to upload SAR (System Activity Reporter) files, parse them, create interactive graphs, and export the results as PDF reports.

## Architecture

### 1. Backend (FastAPI)

#### Core Components:

**A. Parser Engine**
- **SAR File Parser**: Python implementation of the Java parsers
  - Support for Linux SAR formats
  - Automatic OS detection based on header patterns
  - Date/time parsing with multiple format support
  - Column-based data extraction

**B. Data Processing**
- **Time Series Data Structure**: Store parsed data in time-series format
- **Graph Configuration**: XML-based graph definitions (similar to kSar's XML configs)
- **Statistics Aggregation**: Calculate min, max, average, percentiles

**C. Graph Generation**
- **Chart Engine**: Use Plotly for interactive web charts
- **Graph Types**: Line charts, stacked area charts, bar charts
- **Multiple Y-axes**: Support for different metrics on same timeline
- **Zoom and Pan**: Interactive chart controls

**D. PDF Export**
- **Report Generator**: Use WeasyPrint or ReportLab
- **Template System**: HTML templates for PDF generation
- **Chart Embedding**: Convert Plotly charts to static images for PDF

### 2. Frontend (React/Vue.js)

#### Core Components:

**A. File Upload**
- Drag-and-drop interface
- File validation (SAR format detection)
- Progress indicators
- Multiple file support

**B. Dashboard**
- Overview of parsed data
- System information display
- Time range selection
- Quick navigation to different metrics

**C. Interactive Charts**
- Plotly.js integration
- Responsive design
- Chart customization options
- Export capabilities (PNG, SVG)

**D. Report Builder**
- Graph selection interface
- Layout customization
- PDF preview
- Export options

## Technical Implementation

### 1. Backend Structure

```
backend/
├── app/
│   ├── main.py                 # FastAPI application
│   ├── api/
│   │   ├── __init__.py
│   │   ├── upload.py           # File upload endpoints
│   │   ├── parse.py            # Parsing endpoints
│   │   ├── graphs.py           # Graph generation endpoints
│   │   └── export.py           # PDF export endpoints
│   ├── core/
│   │   ├── __init__.py
│   │   ├── config.py           # Configuration management
│   │   └── database.py         # Database models (if needed)
│   ├── parsers/
│   │   ├── __init__.py
│   │   ├── base.py             # Base parser class
│   │   ├── linux.py            # Linux SAR parser
│   │   ├── aix.py              # AIX SAR parser
│   │   ├── hpux.py             # HPUX SAR parser
│   │   └── solaris.py          # Solaris SAR parser
│   ├── models/
│   │   ├── __init__.py
│   │   ├── sar_data.py         # SAR data models
│   │   └── graphs.py           # Graph configuration models
│   ├── services/
│   │   ├── __init__.py
│   │   ├── parser_service.py   # Parser orchestration
│   │   ├── graph_service.py    # Graph generation service
│   │   └── pdf_service.py      # PDF export service
│   └── utils/
│       ├── __init__.py
│       ├── date_utils.py       # Date parsing utilities
│       └── file_utils.py       # File handling utilities
├── requirements.txt
└── Dockerfile
```

### 2. Frontend Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── FileUpload/
│   │   ├── Dashboard/
│   │   ├── Charts/
│   │   └── ReportBuilder/
│   ├── services/
│   │   ├── api.js
│   │   └── chartService.js
│   ├── utils/
│   │   ├── dateUtils.js
│   │   └── fileUtils.js
│   └── App.js
├── package.json
└── Dockerfile
```

## Key Features

### 1. File Upload & Parsing
- **Supported Formats**: Linux, SAR files
- **Automatic Detection**: OS type detection from file headers
- **Validation**: File format validation and error reporting
- **Progress Tracking**: Real-time parsing progress

### 2. Interactive Dashboard
- **System Overview**: Hostname, OS version, CPU count, etc.
- **Time Range**: Data time span with zoom capabilities
- **Metric Categories**: CPU, Memory, Disk, Network, etc.
- **Quick Stats**: Min, max, average values

### 3. Graph Generation
- **Multiple Chart Types**: Line, area, stacked area, bar charts
- **Interactive Features**: Zoom, pan, hover tooltips
- **Customization**: Color schemes, chart layouts
- **Export Options**: PNG, SVG, PDF

### 4. PDF Export
- **Report Templates**: Professional PDF layouts
- **Chart Inclusion**: High-quality chart images
- **Customizable**: Page size, orientation, margins
- **Table of Contents**: Auto-generated navigation

## API Endpoints

### File Management
```
POST /api/upload              # Upload SAR file
GET  /api/files/{file_id}     # Get file info
DELETE /api/files/{file_id}   # Delete file
```

### Parsing
```
POST /api/parse/{file_id}     # Parse uploaded file
GET  /api/parse/{file_id}/status  # Get parsing status
GET  /api/parse/{file_id}/data    # Get parsed data
```

### Graphs
```
GET  /api/graphs/{file_id}           # Get available graphs
POST /api/graphs/{file_id}/generate  # Generate specific graph
GET  /api/graphs/{file_id}/{graph_id} # Get graph data
```

### Export
```
POST /api/export/{file_id}/pdf       # Generate PDF report
GET  /api/export/{file_id}/pdf/{report_id} # Download PDF
```

## Data Models

### SAR Data Structure
```python
class SARData:
    file_id: str
    os_type: str
    hostname: str
    start_time: datetime
    end_time: datetime
    metrics: Dict[str, TimeSeriesData]
    system_info: Dict[str, str]

class TimeSeriesData:
    metric_name: str
    timestamps: List[datetime]
    values: List[float]
    columns: List[str]
    metadata: Dict[str, Any]
```

### Graph Configuration
```python
class GraphConfig:
    name: str
    title: str
    type: str  # line, area, stacked, bar
    metrics: List[str]
    y_axis: List[YAxisConfig]
    options: Dict[str, Any]
```

## Technology Stack

### Backend
- **FastAPI**: Web framework
- **Pydantic**: Data validation
- **Plotly**: Chart generation
- **WeasyPrint/ReportLab**: PDF generation
- **Redis**: Caching and job queues
- **Celery**: Background task processing

### Frontend
- **React/Vue.js**: UI framework
- **Plotly.js**: Interactive charts
- **Axios**: HTTP client
- **Tailwind CSS**: Styling

### Infrastructure
- **Docker**: Containerization
- **Nginx**: Reverse proxy
- **PostgreSQL**: Data storage (optional)
- **Redis**: Session storage and caching

## Development Phases

### Phase 1: Core Parser
1. Implement base parser framework
2. Port Linux SAR parser from Java
3. Create basic API endpoints
4. Simple file upload and parsing

### Phase 2: Graph Generation
1. Implement time series data structures
2. Create graph configuration system
3. Integrate Plotly for chart generation
4. Build basic dashboard

### Phase 3: Interactive UI
1. Develop frontend components
2. Implement file upload interface
3. Create interactive charts
4. Add dashboard functionality

### Phase 4: PDF Export
1. Design PDF templates
2. Implement PDF generation service
3. Add chart-to-image conversion
4. Create report builder interface

### Phase 5: Advanced Features
1. Support for additional OS types
2. Advanced chart customization
3. Batch processing capabilities
4. Performance optimizations

## Benefits Over Original kSar

1. **Web-based**: No Java installation required
2. **Interactive**: Real-time chart interaction
3. **Collaborative**: Multiple users can access
4. **Scalable**: Cloud deployment possible
5. **Modern UI**: Responsive design
6. **API-first**: Programmatic access
7. **Extensible**: Easy to add new features

This design maintains the core functionality of kSar while modernizing it for web deployment and adding interactive capabilities that weren't possible in the original Java Swing application.
