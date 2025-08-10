"""
Main FastAPI application for kSar Web.

This module provides the main FastAPI application with all the endpoints.
"""

from fastapi import FastAPI, HTTPException, UploadFile, File, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, HTMLResponse
from fastapi.staticfiles import StaticFiles
import tempfile
import os
import logging
from typing import List, Dict, Optional
from datetime import datetime

from .services.parser_service import ParserService
from .services.chart_service import ChartService
from .models.chart_models import (
    ChartConfig, DashboardConfig, ChartType, ChartLayout, 
    YAxisConfig, YAxisType, CHART_TEMPLATES
)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Create FastAPI app
app = FastAPI(
    title="kSar Web API",
    description="Web API for parsing and analyzing SAR files",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure this properly for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize services
parser_service = ParserService()
chart_service = ChartService()


@app.get("/")
async def root():
    """Root endpoint."""
    return {"message": "kSar Web API", "version": "1.0.0"}


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy"}


@app.post("/api/upload")
async def upload_file(file: UploadFile = File(...)):
    """
    Upload and parse a SAR file.
    
    Args:
        file: The SAR file to upload
        
    Returns:
        Dict: File information and parsing status
    """
    if not file.filename:
        raise HTTPException(status_code=400, detail="No file provided")
    
    # Check file extension
    if not file.filename.endswith(('.txt', '.sar', '')):
        raise HTTPException(status_code=400, detail="Invalid file type. Please upload a SAR file.")
    
    try:
        # Create temporary file
        with tempfile.NamedTemporaryFile(delete=False, suffix='.sar') as temp_file:
            # Write uploaded file to temporary file
            content = await file.read()
            temp_file.write(content)
            temp_file.flush()
            
            # Parse the file
            parsed_data = parser_service.parse_file(temp_file.name)
            
            if not parsed_data:
                raise HTTPException(status_code=400, detail="Failed to parse SAR file")
            
            # Clean up temporary file
            os.unlink(temp_file.name)
            
            # Return file information
            file_info = parser_service.get_file_info(parsed_data.file_id)
            
            return {
                "message": "File uploaded and parsed successfully",
                "file_id": parsed_data.file_id,
                "file_info": file_info
            }
            
    except Exception as e:
        logger.error(f"Error uploading file: {e}")
        raise HTTPException(status_code=500, detail=f"Error uploading file: {str(e)}")


@app.get("/api/files")
async def list_files():
    """
    List all parsed files.
    
    Returns:
        List: List of file information
    """
    try:
        files = parser_service.list_files()
        return {"files": files}
    except Exception as e:
        logger.error(f"Error listing files: {e}")
        raise HTTPException(status_code=500, detail=f"Error listing files: {str(e)}")


@app.get("/api/files/{file_id}")
async def get_file_info(file_id: str):
    """
    Get information about a specific file.
    
    Args:
        file_id: The file ID
        
    Returns:
        Dict: File information
    """
    try:
        file_info = parser_service.get_file_info(file_id)
        if not file_info:
            raise HTTPException(status_code=404, detail="File not found")
        
        return file_info
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error getting file info: {e}")
        raise HTTPException(status_code=500, detail=f"Error getting file info: {str(e)}")


@app.delete("/api/files/{file_id}")
async def delete_file(file_id: str):
    """
    Delete a parsed file.
    
    Args:
        file_id: The file ID
        
    Returns:
        Dict: Deletion status
    """
    try:
        success = parser_service.delete_file(file_id)
        if not success:
            raise HTTPException(status_code=404, detail="File not found")
        
        return {"message": "File deleted successfully"}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error deleting file: {e}")
        raise HTTPException(status_code=500, detail=f"Error deleting file: {str(e)}")


@app.post("/api/parse/{file_id}")
async def parse_file(file_id: str):
    """
    Parse a file.
    
    Args:
        file_id: The file ID
        
    Returns:
        Dict: Parsing status
    """
    try:
        parsed_data = parser_service.parse_file(file_id)
        if not parsed_data:
            raise HTTPException(status_code=404, detail="File not found")
        
        return {
            "message": "File parsed successfully",
            "file_id": parsed_data.file_id,
            "parsed_data": parsed_data.to_dict()
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error parsing file: {e}")
        raise HTTPException(status_code=500, detail=f"Error parsing file: {str(e)}")


@app.get("/api/parse/{file_id}/status")
async def get_parse_status(file_id: str):
    """
    Get parsing status for a file.
    
    Args:
        file_id: The file ID
        
    Returns:
        Dict: Parsing status
    """
    try:
        collection = parser_service.get_time_series_collection(file_id)
        if not collection:
            raise HTTPException(status_code=404, detail="File not found")
        
        return {
            "file_id": file_id,
            "status": "parsed",
            "series_count": len(collection.list_series()),
            "time_range": collection.get_time_range(),
            "statistics_summary": collection.get_statistics_summary()
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error getting parse status: {e}")
        raise HTTPException(status_code=500, detail=f"Error getting parse status: {str(e)}")


@app.get("/api/parse/{file_id}/data")
async def get_parsed_data(file_id: str, series_name: Optional[str] = None):
    """
    Get parsed data for a file.
    
    Args:
        file_id: The file ID
        series_name: Optional specific series name
        
    Returns:
        Dict: Parsed data
    """
    try:
        collection = parser_service.get_time_series_collection(file_id)
        if not collection:
            raise HTTPException(status_code=404, detail="File not found")
        
        if series_name:
            series = collection.get_series(series_name)
            if not series:
                raise HTTPException(status_code=404, detail="Series not found")
            return series.to_dict()
        else:
            return collection.to_dict()
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error getting parsed data: {e}")
        raise HTTPException(status_code=500, detail=f"Error getting parsed data: {str(e)}")


@app.get("/api/dashboard/{file_id}")
async def get_dashboard(file_id: str, format: str = Query("html", description="Output format: html or json")):
    """
    Get a complete dashboard for a file.
    
    Args:
        file_id: The file ID
        format: Output format (html or json)
        
    Returns:
        HTMLResponse or JSONResponse: Dashboard data
    """
    try:
        # Get the time series collection
        collection = parser_service.get_time_series_collection(file_id)
        if not collection:
            raise HTTPException(status_code=404, detail="File not found")
        
        # Create dashboard configuration
        dashboard_config = DashboardConfig(
            name=f"SAR Dashboard - {file_id}",
            title=f"SAR Analysis Dashboard - {collection.metadata.get('system_info', {}).get('hostname', file_id)}",
            layout=ChartLayout.GRID
        )
        
        # Add CPU chart
        cpu_metrics = [name for name in collection.list_series() if name.startswith('cpu_')]
        if cpu_metrics:
            cpu_chart = ChartConfig(
                name="cpu_utilization",
                title="CPU Utilization",
                chart_type=ChartType.LINE,
                metrics=cpu_metrics[:5],  # Limit to first 5 CPU metrics
                y_axis=YAxisConfig(title="CPU %", type=YAxisType.LINEAR, min=0, max=100)
            )
            dashboard_config.add_chart(cpu_chart)
        
        # Add memory chart
        memory_metrics = [name for name in collection.list_series() if name.startswith('memory')]
        if memory_metrics:
            memory_chart = ChartConfig(
                name="memory_usage",
                title="Memory Usage",
                chart_type=ChartType.AREA,
                metrics=memory_metrics,
                y_axis=YAxisConfig(title="Memory (KB)")
            )
            dashboard_config.add_chart(memory_chart)
        
        # Add disk chart
        disk_metrics = [name for name in collection.list_series() if name.startswith('disk_')]
        if disk_metrics:
            disk_chart = ChartConfig(
                name="disk_io",
                title="Disk I/O",
                chart_type=ChartType.LINE,
                metrics=disk_metrics[:3],  # Limit to first 3 disk metrics
                y_axis=YAxisConfig(title="Operations/sec")
            )
            dashboard_config.add_chart(disk_chart)
        
        # Add network chart
        network_metrics = [name for name in collection.list_series() if name.startswith('network_')]
        if network_metrics:
            network_chart = ChartConfig(
                name="network_io",
                title="Network I/O",
                chart_type=ChartType.LINE,
                metrics=network_metrics[:3],  # Limit to first 3 network metrics
                y_axis=YAxisConfig(title="Bytes/sec")
            )
            dashboard_config.add_chart(network_chart)
        
        # Generate dashboard
        dashboard_data = chart_service.create_dashboard(collection, dashboard_config)
        
        if format.lower() == "json":
            return dashboard_data.to_dict()
        else:
            # Create complete HTML page
            dashboard_html = chart_service.create_dashboard_html(dashboard_data)
            
            html_content = f"""
            <!DOCTYPE html>
            <html>
            <head>
                <title>kSar Dashboard - {file_id}</title>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body {{
                        font-family: Arial, sans-serif;
                        margin: 0;
                        padding: 20px;
                        background-color: #f5f5f5;
                    }}
                    .header {{
                        background-color: #fff;
                        padding: 20px;
                        border-radius: 5px;
                        margin-bottom: 20px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }}
                    .header h1 {{
                        color: #333;
                        margin: 0 0 10px 0;
                    }}
                    .file-info {{
                        color: #666;
                        font-size: 14px;
                    }}
                    .file-info p {{
                        margin: 5px 0;
                    }}
                    .dashboard-content {{
                        background-color: #fff;
                        border-radius: 5px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        overflow: hidden;
                    }}
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>kSar Dashboard</h1>
                    <div class="file-info">
                        <p><strong>File ID:</strong> {file_id}</p>
                        <p><strong>Hostname:</strong> {collection.metadata.get('system_info', {}).get('hostname', 'N/A')}</p>
                        <p><strong>OS:</strong> {collection.metadata.get('system_info', {}).get('os_type', 'N/A')} {collection.metadata.get('system_info', {}).get('kernel', '')}</p>
                        <p><strong>Time Range:</strong> {collection.get_time_range()['start'] if collection.get_time_range() else 'N/A'} to {collection.get_time_range()['end'] if collection.get_time_range() else 'N/A'}</p>
                    </div>
                </div>
                <div class="dashboard-content">
                    {dashboard_html}
                </div>
            </body>
            </html>
            """
            
            return HTMLResponse(content=html_content)
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error creating dashboard: {e}")
        raise HTTPException(status_code=500, detail=f"Error creating dashboard: {str(e)}")


@app.post("/api/charts/{file_id}")
async def create_chart(file_id: str, chart_config: ChartConfig):
    """
    Create a specific chart for a file.
    
    Args:
        file_id: The file ID
        chart_config: Chart configuration
        
    Returns:
        Dict: Chart data
    """
    try:
        collection = parser_service.get_time_series_collection(file_id)
        if not collection:
            raise HTTPException(status_code=404, detail="File not found")
        
        chart_data = chart_service.create_chart(collection, chart_config)
        return chart_data.to_dict()
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error creating chart: {e}")
        raise HTTPException(status_code=500, detail=f"Error creating chart: {str(e)}")


@app.get("/api/charts/templates")
async def get_chart_templates():
    """
    Get available chart templates.
    
    Returns:
        Dict: Available chart templates
    """
    try:
        return chart_service.get_available_templates()
    except Exception as e:
        logger.error(f"Error getting chart templates: {e}")
        raise HTTPException(status_code=500, detail=f"Error getting chart templates: {str(e)}")


@app.post("/api/charts/{file_id}/template/{template_name}")
async def create_chart_from_template(file_id: str, template_name: str, custom_metrics: Optional[List[str]] = None):
    """
    Create a chart from a template.
    
    Args:
        file_id: The file ID
        template_name: Template name
        custom_metrics: Custom metrics to override template defaults
        
    Returns:
        Dict: Chart data
    """
    try:
        collection = parser_service.get_time_series_collection(file_id)
        if not collection:
            raise HTTPException(status_code=404, detail="File not found")
        
        chart_data = chart_service.create_chart_from_template(template_name, collection, custom_metrics)
        return chart_data.to_dict()
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error(f"Error creating chart from template: {e}")
        raise HTTPException(status_code=500, detail=f"Error creating chart from template: {str(e)}")


@app.get("/api/metrics/{file_id}")
async def get_available_metrics(file_id: str):
    """
    Get available metrics for a file.
    
    Args:
        file_id: The file ID
        
    Returns:
        Dict: Available metrics
    """
    try:
        collection = parser_service.get_time_series_collection(file_id)
        if not collection:
            raise HTTPException(status_code=404, detail="File not found")
        
        metrics = {}
        for series_name in collection.list_series():
            series = collection.get_series(series_name)
            if series:
                metrics[series_name] = {
                    "name": series_name,
                    "data_points": len(series.timestamps),
                    "time_range": series.get_time_range(),
                    "statistics": series.get_statistics(),
                    "columns": series.columns
                }
        
        return {
            "file_id": file_id,
            "metrics": metrics,
            "total_metrics": len(metrics)
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error getting metrics: {e}")
        raise HTTPException(status_code=500, detail=f"Error getting metrics: {str(e)}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000) 