"""
Chart service for generating charts and dashboards.
"""

import json
import logging
from typing import Dict, List, Optional, Any
import plotly.graph_objects as go
import plotly.express as px
from plotly.subplots import make_subplots
import pandas as pd
from datetime import datetime

from ..models.chart_models import (
    ChartConfig, ChartData, DashboardConfig, ChartType, ChartLayout, 
    YAxisConfig, YAxisType, DashboardData, CHART_TEMPLATES
)
from ..models.time_series import TimeSeriesCollection, TimeSeriesData

logger = logging.getLogger(__name__)


class ChartService:
    """Enhanced service for generating charts and dashboards."""
    
    def __init__(self):
        """Initialize the chart service."""
        self.default_colors = [
            '#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd',
            '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf'
        ]
    
    def create_chart(self, collection: TimeSeriesCollection, config: ChartConfig) -> ChartData:
        """
        Create a chart based on configuration.
        
        Args:
            collection: Time series collection
            config: Chart configuration
            
        Returns:
            ChartData: Chart data with Plotly figure
        """
        try:
            if config.chart_type == ChartType.LINE:
                fig = self._create_line_chart(collection, config)
            elif config.chart_type == ChartType.AREA:
                fig = self._create_area_chart(collection, config)
            elif config.chart_type == ChartType.STACKED_AREA:
                fig = self._create_stacked_area_chart(collection, config)
            elif config.chart_type == ChartType.BAR:
                fig = self._create_bar_chart(collection, config)
            elif config.chart_type == ChartType.SCATTER:
                fig = self._create_scatter_chart(collection, config)
            elif config.chart_type == ChartType.HEATMAP:
                fig = self._create_heatmap_chart(collection, config)
            elif config.chart_type == ChartType.BOX:
                fig = self._create_box_chart(collection, config)
            elif config.chart_type == ChartType.HISTOGRAM:
                fig = self._create_histogram_chart(collection, config)
            else:
                raise ValueError(f"Unsupported chart type: {config.chart_type}")
            
            # Apply Y-axis configuration
            self._apply_y_axis_config(fig, config.y_axis)
            
            # Convert figure to JSON
            chart_json = fig.to_json()
            chart_data = json.loads(chart_json)
            
            return ChartData(
                config=config,
                data=chart_data,
                metadata={
                    "series_count": len(config.metrics),
                    "data_points": self._count_data_points(collection, config.metrics),
                    "time_range": collection.get_time_range()
                }
            )
            
        except Exception as e:
            logger.error(f"Error creating chart {config.name}: {e}")
            raise
    
    def create_dashboard(self, collection: TimeSeriesCollection, config: DashboardConfig) -> DashboardData:
        """
        Create a dashboard with multiple charts.
        
        Args:
            collection: Time series collection
            config: Dashboard configuration
            
        Returns:
            DashboardData: Complete dashboard data
        """
        try:
            charts = []
            for chart_config in config.charts:
                chart_data = self.create_chart(collection, chart_config)
                charts.append(chart_data)
            
            return DashboardData(
                config=config,
                charts=charts,
                metadata={
                    "chart_count": len(charts),
                    "time_range": collection.get_time_range(),
                    "statistics_summary": collection.get_statistics_summary()
                }
            )
            
        except Exception as e:
            logger.error(f"Error creating dashboard {config.name}: {e}")
            raise
    
    def create_dashboard_html(self, dashboard_data: DashboardData) -> str:
        """
        Create HTML for the dashboard.
        
        Args:
            dashboard_data: Dashboard data
            
        Returns:
            str: Complete HTML for the dashboard
        """
        return self._create_dashboard_html(dashboard_data.config, [chart.to_dict() for chart in dashboard_data.charts])
    
    def get_available_templates(self) -> Dict[str, Any]:
        """Get available chart templates."""
        return {name: template.to_dict() for name, template in CHART_TEMPLATES.items()}
    
    def create_chart_from_template(self, template_name: str, collection: TimeSeriesCollection, 
                                 custom_metrics: Optional[List[str]] = None) -> ChartData:
        """
        Create a chart from a template.
        
        Args:
            template_name: Name of the template
            collection: Time series collection
            custom_metrics: Custom metrics to override template defaults
            
        Returns:
            ChartData: Chart data
        """
        if template_name not in CHART_TEMPLATES:
            raise ValueError(f"Template {template_name} not found")
        
        template = CHART_TEMPLATES[template_name]
        metrics = custom_metrics if custom_metrics else template.default_metrics
        
        # Filter metrics to only include those available in the collection
        available_metrics = collection.list_series()
        filtered_metrics = [m for m in metrics if m in available_metrics]
        
        if not filtered_metrics:
            # Try to find similar metrics
            for metric in metrics:
                for available in available_metrics:
                    if metric.lower() in available.lower() or available.lower() in metric.lower():
                        filtered_metrics.append(available)
                        break
        
        config = ChartConfig(
            name=template_name,
            title=template.name,
            chart_type=template.chart_type,
            metrics=filtered_metrics,
            options=template.default_options
        )
        
        return self.create_chart(collection, config)
    
    def _create_line_chart(self, collection: TimeSeriesCollection, config: ChartConfig) -> go.Figure:
        """Create a line chart."""
        fig = go.Figure()
        
        for i, metric_name in enumerate(config.metrics):
            series = collection.get_series(metric_name)
            if not series or not series.timestamps:
                continue
            
            df = series.to_dataframe()
            if df.empty:
                continue
            
            color = self.default_colors[i % len(self.default_colors)]
            
            for column in df.columns:
                fig.add_trace(
                    go.Scatter(
                        x=df.index,
                        y=df[column],
                        mode='lines',
                        name=f"{metric_name}_{column}",
                        line=dict(color=color, width=2),
                        hovertemplate='<b>%{x}</b><br>%{y:.2f}<extra></extra>'
                    )
                )
        
        fig.update_layout(
            title=config.title,
            xaxis_title="Time",
            yaxis_title=config.y_axis.title if config.y_axis else "Value",
            hovermode='x unified',
            showlegend=True,
            plot_bgcolor='white',
            paper_bgcolor='white'
        )
        
        return fig
    
    def _create_area_chart(self, collection: TimeSeriesCollection, config: ChartConfig) -> go.Figure:
        """Create an area chart."""
        fig = go.Figure()
        
        for i, metric_name in enumerate(config.metrics):
            series = collection.get_series(metric_name)
            if not series or not series.timestamps:
                continue
            
            df = series.to_dataframe()
            if df.empty:
                continue
            
            color = self.default_colors[i % len(self.default_colors)]
            
            for column in df.columns:
                fig.add_trace(
                    go.Scatter(
                        x=df.index,
                        y=df[column],
                        mode='lines',
                        fill='tonexty',
                        name=f"{metric_name}_{column}",
                        line=dict(color=color, width=1),
                        fillcolor=color.replace(')', ', 0.3)').replace('rgb', 'rgba'),
                        hovertemplate='<b>%{x}</b><br>%{y:.2f}<extra></extra>'
                    )
                )
        
        fig.update_layout(
            title=config.title,
            xaxis_title="Time",
            yaxis_title=config.y_axis.title if config.y_axis else "Value",
            hovermode='x unified',
            showlegend=True,
            plot_bgcolor='white',
            paper_bgcolor='white'
        )
        
        return fig
    
    def _create_stacked_area_chart(self, collection: TimeSeriesCollection, config: ChartConfig) -> go.Figure:
        """Create a stacked area chart."""
        fig = go.Figure()
        
        # Collect all data first
        all_data = []
        for metric_name in config.metrics:
            series = collection.get_series(metric_name)
            if not series or not series.timestamps:
                continue
            
            df = series.to_dataframe()
            if not df.empty:
                all_data.append((metric_name, df))
        
        if not all_data:
            return go.Figure()
        
        # Create stacked area chart
        for i, (metric_name, df) in enumerate(all_data):
            color = self.default_colors[i % len(self.default_colors)]
            
            for column in df.columns:
                fig.add_trace(
                    go.Scatter(
                        x=df.index,
                        y=df[column],
                        mode='lines',
                        fill='tonexty',
                        name=f"{metric_name}_{column}",
                        line=dict(color=color, width=1),
                        fillcolor=color.replace(')', ', 0.3)').replace('rgb', 'rgba'),
                        stackgroup='one',
                        hovertemplate='<b>%{x}</b><br>%{y:.2f}<extra></extra>'
                    )
                )
        
        fig.update_layout(
            title=config.title,
            xaxis_title="Time",
            yaxis_title=config.y_axis.title if config.y_axis else "Value",
            hovermode='x unified',
            showlegend=True,
            plot_bgcolor='white',
            paper_bgcolor='white'
        )
        
        return fig
    
    def _create_bar_chart(self, collection: TimeSeriesCollection, config: ChartConfig) -> go.Figure:
        """Create a bar chart."""
        fig = go.Figure()
        
        for i, metric_name in enumerate(config.metrics):
            series = collection.get_series(metric_name)
            if not series or not series.timestamps:
                continue
            
            df = series.to_dataframe()
            if df.empty:
                continue
            
            color = self.default_colors[i % len(self.default_colors)]
            
            for column in df.columns:
                fig.add_trace(
                    go.Bar(
                        x=df.index,
                        y=df[column],
                        name=f"{metric_name}_{column}",
                        marker_color=color,
                        hovertemplate='<b>%{x}</b><br>%{y:.2f}<extra></extra>'
                    )
                )
        
        fig.update_layout(
            title=config.title,
            xaxis_title="Time",
            yaxis_title=config.y_axis.title if config.y_axis else "Value",
            showlegend=True,
            plot_bgcolor='white',
            paper_bgcolor='white'
        )
        
        return fig
    
    def _create_scatter_chart(self, collection: TimeSeriesCollection, config: ChartConfig) -> go.Figure:
        """Create a scatter chart."""
        fig = go.Figure()
        
        for i, metric_name in enumerate(config.metrics):
            series = collection.get_series(metric_name)
            if not series or not series.timestamps:
                continue
            
            df = series.to_dataframe()
            if df.empty:
                continue
            
            color = self.default_colors[i % len(self.default_colors)]
            
            for column in df.columns:
                fig.add_trace(
                    go.Scatter(
                        x=df.index,
                        y=df[column],
                        mode='markers',
                        name=f"{metric_name}_{column}",
                        marker=dict(color=color, size=6),
                        hovertemplate='<b>%{x}</b><br>%{y:.2f}<extra></extra>'
                    )
                )
        
        fig.update_layout(
            title=config.title,
            xaxis_title="Time",
            yaxis_title=config.y_axis.title if config.y_axis else "Value",
            showlegend=True,
            plot_bgcolor='white',
            paper_bgcolor='white'
        )
        
        return fig
    
    def _create_heatmap_chart(self, collection: TimeSeriesCollection, config: ChartConfig) -> go.Figure:
        """Create a heatmap chart."""
        # This is a simplified heatmap implementation
        # In practice, you might want to aggregate data by time periods
        fig = go.Figure()
        
        # Collect data for heatmap
        all_data = []
        for metric_name in config.metrics:
            series = collection.get_series(metric_name)
            if not series or not series.timestamps:
                continue
            
            df = series.to_dataframe()
            if not df.empty:
                all_data.append(df)
        
        if not all_data:
            return go.Figure()
        
        # Combine data for heatmap
        combined_df = pd.concat(all_data, axis=1)
        
        fig.add_trace(
            go.Heatmap(
                z=combined_df.values.T,
                x=combined_df.index,
                y=combined_df.columns,
                colorscale='Viridis',
                hovertemplate='<b>%{y}</b><br>%{x}<br>%{z:.2f}<extra></extra>'
            )
        )
        
        fig.update_layout(
            title=config.title,
            xaxis_title="Time",
            yaxis_title="Metrics",
            plot_bgcolor='white',
            paper_bgcolor='white'
        )
        
        return fig
    
    def _create_box_chart(self, collection: TimeSeriesCollection, config: ChartConfig) -> go.Figure:
        """Create a box chart."""
        fig = go.Figure()
        
        for i, metric_name in enumerate(config.metrics):
            series = collection.get_series(metric_name)
            if not series or not series.timestamps:
                continue
            
            df = series.to_dataframe()
            if df.empty:
                continue
            
            color = self.default_colors[i % len(self.default_colors)]
            
            for column in df.columns:
                fig.add_trace(
                    go.Box(
                        y=df[column],
                        name=f"{metric_name}_{column}",
                        marker_color=color,
                        boxpoints='outliers'
                    )
                )
        
        fig.update_layout(
            title=config.title,
            yaxis_title=config.y_axis.title if config.y_axis else "Value",
            showlegend=True,
            plot_bgcolor='white',
            paper_bgcolor='white'
        )
        
        return fig
    
    def _create_histogram_chart(self, collection: TimeSeriesCollection, config: ChartConfig) -> go.Figure:
        """Create a histogram chart."""
        fig = go.Figure()
        
        for i, metric_name in enumerate(config.metrics):
            series = collection.get_series(metric_name)
            if not series or not series.timestamps:
                continue
            
            df = series.to_dataframe()
            if df.empty:
                continue
            
            color = self.default_colors[i % len(self.default_colors)]
            
            for column in df.columns:
                fig.add_trace(
                    go.Histogram(
                        x=df[column],
                        name=f"{metric_name}_{column}",
                        marker_color=color,
                        opacity=0.7,
                        nbinsx=30
                    )
                )
        
        fig.update_layout(
            title=config.title,
            xaxis_title=config.y_axis.title if config.y_axis else "Value",
            yaxis_title="Frequency",
            showlegend=True,
            plot_bgcolor='white',
            paper_bgcolor='white'
        )
        
        return fig
    
    def _apply_y_axis_config(self, fig: go.Figure, y_axis_config: Optional[YAxisConfig]) -> None:
        """Apply Y-axis configuration to the figure."""
        if not y_axis_config:
            return
        
        fig.update_layout(
            yaxis=dict(
                title=y_axis_config.title,
                type=y_axis_config.type.value,
                range=[y_axis_config.min, y_axis_config.max] if y_axis_config.min is not None and y_axis_config.max is not None else None,
                gridcolor=y_axis_config.grid_color if y_axis_config.show_grid else None,
                showgrid=y_axis_config.show_grid
            )
        )
    
    def _count_data_points(self, collection: TimeSeriesCollection, metrics: List[str]) -> int:
        """Count total data points for specified metrics."""
        total_points = 0
        for metric_name in metrics:
            series = collection.get_series(metric_name)
            if series:
                total_points += len(series.timestamps)
        return total_points
    
    def _create_dashboard_html(self, config: DashboardConfig, charts: List[Dict]) -> str:
        """Create HTML for the dashboard."""
        html_parts = []
        
        # Add Plotly.js
        html_parts.append("""
        <!DOCTYPE html>
        <html>
        <head>
            <title>kSar Dashboard</title>
            <script src="https://cdn.plot.ly/plotly-latest.min.js"></script>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; }
                .chart-container { margin: 20px 0; }
                .chart-title { font-size: 18px; font-weight: bold; margin-bottom: 10px; }
            </style>
        </head>
        <body>
        """)
        
        # Add charts
        for chart in charts:
            html_parts.append(f"""
            <div class="chart-container">
                <div class="chart-title">{chart['config']['title']}</div>
                <div id="chart_{chart['config']['name']}" style="width:100%; height:400px;"></div>
            </div>
            """)
        
        # Add JavaScript for charts
        html_parts.append("<script>")
        for chart in charts:
            chart_name = chart['config']['name']
            # Handle the new chart data structure
            if 'data' in chart and isinstance(chart['data'], dict):
                if 'data' in chart['data'] and 'layout' in chart['data']:
                    # New structure
                    data = chart['data']['data']
                    layout = chart['data']['layout']
                else:
                    # Fallback structure
                    data = chart['data'].get('data', [])
                    layout = chart['data'].get('layout', {})
            else:
                # Old structure
                data = chart.get('data', {}).get('figure', {}).get('data', [])
                layout = chart.get('data', {}).get('figure', {}).get('layout', {})
            
            html_parts.append(f"""
            Plotly.newPlot('chart_{chart_name}', {json.dumps(data)}, {json.dumps(layout)});
            """)
        
        html_parts.append("</script></body></html>")
        
        return "\n".join(html_parts) 