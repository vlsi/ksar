"""
Chart configuration and data models.
"""

from typing import Dict, List, Optional, Any, Union
from dataclasses import dataclass, field
from enum import Enum


class ChartType(str, Enum):
    """Supported chart types."""
    LINE = "line"
    AREA = "area"
    STACKED_AREA = "stacked_area"
    BAR = "bar"
    SCATTER = "scatter"
    HEATMAP = "heatmap"
    BOX = "box"
    HISTOGRAM = "histogram"


class ChartLayout(str, Enum):
    """Chart layout options."""
    SINGLE = "single"
    GRID = "grid"
    TABS = "tabs"
    CAROUSEL = "carousel"


class YAxisType(str, Enum):
    """Y-axis types."""
    LINEAR = "linear"
    LOG = "log"
    PERCENT = "percent"


@dataclass
class YAxisConfig:
    """Configuration for Y-axis."""
    title: str = "Value"
    type: YAxisType = YAxisType.LINEAR
    min: Optional[float] = None
    max: Optional[float] = None
    grid_color: str = "#e1e5e9"
    show_grid: bool = True
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        return {
            "title": self.title,
            "type": self.type.value,
            "min": self.min,
            "max": self.max,
            "grid_color": self.grid_color,
            "show_grid": self.show_grid
        }


@dataclass
class ChartConfig:
    """Enhanced configuration for a chart."""
    name: str
    title: str
    chart_type: ChartType = ChartType.LINE
    metrics: List[str] = field(default_factory=list)
    layout: ChartLayout = ChartLayout.SINGLE
    y_axis: Optional[YAxisConfig] = None
    options: Dict[str, Any] = field(default_factory=dict)
    
    def __post_init__(self):
        """Set default Y-axis if not provided."""
        if self.y_axis is None:
            self.y_axis = YAxisConfig()
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        return {
            "name": self.name,
            "title": self.title,
            "chart_type": self.chart_type.value,
            "metrics": self.metrics,
            "layout": self.layout.value,
            "y_axis": self.y_axis.to_dict() if self.y_axis else None,
            "options": self.options
        }


@dataclass
class ChartData:
    """Enhanced data for a chart."""
    config: ChartConfig
    data: Dict[str, Any] = field(default_factory=dict)
    metadata: Dict[str, Any] = field(default_factory=dict)
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        return {
            "config": self.config.to_dict(),
            "data": self.data,
            "metadata": self.metadata
        }


@dataclass
class DashboardConfig:
    """Enhanced configuration for a dashboard."""
    name: str
    title: str
    charts: List[ChartConfig] = field(default_factory=list)
    layout: ChartLayout = ChartLayout.GRID
    options: Dict[str, Any] = field(default_factory=dict)
    refresh_interval: Optional[int] = None  # seconds
    auto_refresh: bool = False
    
    def add_chart(self, chart: ChartConfig) -> None:
        """Add a chart to the dashboard."""
        self.charts.append(chart)
    
    def remove_chart(self, chart_name: str) -> bool:
        """Remove a chart from the dashboard."""
        for i, chart in enumerate(self.charts):
            if chart.name == chart_name:
                del self.charts[i]
                return True
        return False
    
    def get_chart(self, chart_name: str) -> Optional[ChartConfig]:
        """Get a chart by name."""
        for chart in self.charts:
            if chart.name == chart_name:
                return chart
        return None
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        return {
            "name": self.name,
            "title": self.title,
            "charts": [chart.to_dict() for chart in self.charts],
            "layout": self.layout.value,
            "options": self.options,
            "refresh_interval": self.refresh_interval,
            "auto_refresh": self.auto_refresh
        }


@dataclass
class DashboardData:
    """Data for a complete dashboard."""
    config: DashboardConfig
    charts: List[ChartData] = field(default_factory=list)
    metadata: Dict[str, Any] = field(default_factory=dict)
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        return {
            "config": self.config.to_dict(),
            "charts": [chart.to_dict() for chart in self.charts],
            "metadata": self.metadata
        }


@dataclass
class ChartTemplate:
    """Template for creating charts."""
    name: str
    description: str
    chart_type: ChartType
    default_metrics: List[str] = field(default_factory=list)
    default_options: Dict[str, Any] = field(default_factory=dict)
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary."""
        return {
            "name": self.name,
            "description": self.description,
            "chart_type": self.chart_type.value,
            "default_metrics": self.default_metrics,
            "default_options": self.default_options
        }


# Predefined chart templates
CHART_TEMPLATES = {
    "cpu_utilization": ChartTemplate(
        name="CPU Utilization",
        description="CPU usage over time",
        chart_type=ChartType.LINE,
        default_metrics=["cpu_user", "cpu_system", "cpu_idle"],
        default_options={"yaxis_title": "CPU %", "yaxis_range": [0, 100]}
    ),
    "memory_usage": ChartTemplate(
        name="Memory Usage",
        description="Memory usage over time",
        chart_type=ChartType.AREA,
        default_metrics=["memory_used", "memory_free"],
        default_options={"yaxis_title": "Memory (KB)"}
    ),
    "disk_io": ChartTemplate(
        name="Disk I/O",
        description="Disk I/O operations over time",
        chart_type=ChartType.LINE,
        default_metrics=["disk_tps", "disk_read", "disk_write"],
        default_options={"yaxis_title": "Operations/sec"}
    ),
    "network_io": ChartTemplate(
        name="Network I/O",
        description="Network I/O over time",
        chart_type=ChartType.LINE,
        default_metrics=["network_rx", "network_tx"],
        default_options={"yaxis_title": "Bytes/sec"}
    ),
    "load_average": ChartTemplate(
        name="Load Average",
        description="System load average over time",
        chart_type=ChartType.LINE,
        default_metrics=["load_1", "load_5", "load_15"],
        default_options={"yaxis_title": "Load Average"}
    )
} 