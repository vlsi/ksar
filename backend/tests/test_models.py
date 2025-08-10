import pytest
import pandas as pd
from datetime import datetime, timedelta
from app.models.time_series import TimeSeriesData, TimeSeriesCollection
from app.models.chart_models import (
    ChartConfig, DashboardConfig, YAxisConfig, ChartType, 
    ChartLayout, YAxisType, ChartTemplate
)


class TestTimeSeriesData:
    """Test cases for TimeSeriesData model"""
    
    def test_create_valid_timeseries(self):
        """Test creating a valid TimeSeriesData instance"""
        timestamps = [
            datetime(2024, 1, 1, 10, 0, 0),
            datetime(2024, 1, 1, 10, 1, 0),
            datetime(2024, 1, 1, 10, 2, 0)
        ]
        values = [10.5, 15.2, 12.8]
        
        ts_data = TimeSeriesData(
            name="cpu_usage",
            timestamps=timestamps,
            values=values,
            unit="%"
        )
        
        assert ts_data.name == "cpu_usage"
        assert ts_data.timestamps == timestamps
        assert ts_data.values == values
        assert ts_data.unit == "%"
        assert ts_data.metadata == {}
    
    def test_create_with_metadata(self):
        """Test creating TimeSeriesData with metadata"""
        timestamps = [datetime(2024, 1, 1, 10, 0, 0)]
        values = [10.5]
        metadata = {"source": "sar", "host": "test-server"}
        
        ts_data = TimeSeriesData(
            name="cpu_usage",
            timestamps=timestamps,
            values=values,
            unit="%",
            metadata=metadata
        )
        
        assert ts_data.metadata == metadata
    
    def test_handle_mismatched_lengths(self):
        """Test handling of mismatched timestamp and value lengths"""
        timestamps = [
            datetime(2024, 1, 1, 10, 0, 0),
            datetime(2024, 1, 1, 10, 1, 0)
        ]
        values = [10.5, 15.2, 12.8]  # One extra value
        
        ts_data = TimeSeriesData(
            name="cpu_usage",
            timestamps=timestamps,
            values=values,
            unit="%"
        )
        
        # Should truncate to minimum length
        assert len(ts_data.timestamps) == 2
        assert len(ts_data.values) == 2
        assert ts_data.timestamps == timestamps
        assert ts_data.values == [10.5, 15.2]
    
    def test_get_statistics(self):
        """Test statistics calculation"""
        timestamps = [
            datetime(2024, 1, 1, 10, 0, 0),
            datetime(2024, 1, 1, 10, 1, 0),
            datetime(2024, 1, 1, 10, 2, 0)
        ]
        values = [10.5, 15.2, 12.8]
        
        ts_data = TimeSeriesData(
            name="cpu_usage",
            timestamps=timestamps,
            values=values,
            unit="%"
        )
        
        stats = ts_data.get_statistics()
        
        assert stats["count"] == 3
        assert stats["min"] == 10.5
        assert stats["max"] == 15.2
        assert stats["mean"] == pytest.approx(12.83, rel=1e-2)
        assert stats["std"] == pytest.approx(2.35, rel=1e-2)
        assert "p50" in stats
        assert "p95" in stats
        assert "p99" in stats
    
    def test_filter_by_time_range(self):
        """Test filtering by time range"""
        timestamps = [
            datetime(2024, 1, 1, 10, 0, 0),
            datetime(2024, 1, 1, 10, 1, 0),
            datetime(2024, 1, 1, 10, 2, 0),
            datetime(2024, 1, 1, 10, 3, 0)
        ]
        values = [10.5, 15.2, 12.8, 18.1]
        
        ts_data = TimeSeriesData(
            name="cpu_usage",
            timestamps=timestamps,
            values=values,
            unit="%"
        )
        
        start_time = datetime(2024, 1, 1, 10, 1, 0)
        end_time = datetime(2024, 1, 1, 10, 2, 30)
        
        filtered = ts_data.filter_by_time_range(start_time, end_time)
        
        assert len(filtered.timestamps) == 2
        assert filtered.timestamps[0] == datetime(2024, 1, 1, 10, 1, 0)
        assert filtered.timestamps[1] == datetime(2024, 1, 1, 10, 2, 0)
        assert filtered.values == [15.2, 12.8]
    
    def test_resample(self):
        """Test resampling functionality"""
        timestamps = [
            datetime(2024, 1, 1, 10, 0, 0),
            datetime(2024, 1, 1, 10, 0, 30),
            datetime(2024, 1, 1, 10, 1, 0),
            datetime(2024, 1, 1, 10, 1, 30)
        ]
        values = [10.5, 12.1, 15.2, 14.8]
        
        ts_data = TimeSeriesData(
            name="cpu_usage",
            timestamps=timestamps,
            values=values,
            unit="%"
        )
        
        resampled = ts_data.resample("1min", "mean")
        
        assert len(resampled.timestamps) == 2
        assert resampled.timestamps[0] == datetime(2024, 1, 1, 10, 0, 0)
        assert resampled.timestamps[1] == datetime(2024, 1, 1, 10, 1, 0)
        assert resampled.values[0] == pytest.approx(11.3, rel=1e-2)  # (10.5 + 12.1) / 2
        assert resampled.values[1] == pytest.approx(15.0, rel=1e-2)  # (15.2 + 14.8) / 2
    
    def test_to_dict(self):
        """Test conversion to dictionary"""
        timestamps = [
            datetime(2024, 1, 1, 10, 0, 0),
            datetime(2024, 1, 1, 10, 1, 0)
        ]
        values = [10.5, 15.2]
        metadata = {"source": "sar"}
        
        ts_data = TimeSeriesData(
            name="cpu_usage",
            timestamps=timestamps,
            values=values,
            unit="%",
            metadata=metadata
        )
        
        data_dict = ts_data.to_dict()
        
        assert data_dict["name"] == "cpu_usage"
        assert data_dict["unit"] == "%"
        assert data_dict["metadata"] == metadata
        assert len(data_dict["timestamps"]) == 2
        assert len(data_dict["values"]) == 2


class TestTimeSeriesCollection:
    """Test cases for TimeSeriesCollection model"""
    
    def test_create_collection(self):
        """Test creating a TimeSeriesCollection"""
        ts1 = TimeSeriesData(
            name="cpu_user",
            timestamps=[datetime(2024, 1, 1, 10, 0, 0)],
            values=[10.5],
            unit="%"
        )
        ts2 = TimeSeriesData(
            name="cpu_system",
            timestamps=[datetime(2024, 1, 1, 10, 0, 0)],
            values=[5.2],
            unit="%"
        )
        
        collection = TimeSeriesCollection(
            name="cpu_metrics",
            series=[ts1, ts2],
            metadata={"host": "test-server"}
        )
        
        assert collection.name == "cpu_metrics"
        assert len(collection.series) == 2
        assert collection.metadata == {"host": "test-server"}
    
    def test_get_statistics_summary(self):
        """Test getting statistics summary for collection"""
        ts1 = TimeSeriesData(
            name="cpu_user",
            timestamps=[datetime(2024, 1, 1, 10, 0, 0), datetime(2024, 1, 1, 10, 1, 0)],
            values=[10.5, 15.2],
            unit="%"
        )
        ts2 = TimeSeriesData(
            name="cpu_system",
            timestamps=[datetime(2024, 1, 1, 10, 0, 0), datetime(2024, 1, 1, 10, 1, 0)],
            values=[5.2, 8.1],
            unit="%"
        )
        
        collection = TimeSeriesCollection(
            name="cpu_metrics",
            series=[ts1, ts2]
        )
        
        summary = collection.get_statistics_summary()
        
        assert "cpu_user" in summary
        assert "cpu_system" in summary
        assert summary["cpu_user"]["count"] == 2
        assert summary["cpu_user"]["mean"] == pytest.approx(12.85, rel=1e-2)
        assert summary["cpu_system"]["count"] == 2
        assert summary["cpu_system"]["mean"] == pytest.approx(6.65, rel=1e-2)
    
    def test_filter_by_time_range(self):
        """Test filtering collection by time range"""
        ts1 = TimeSeriesData(
            name="cpu_user",
            timestamps=[
                datetime(2024, 1, 1, 10, 0, 0),
                datetime(2024, 1, 1, 10, 1, 0),
                datetime(2024, 1, 1, 10, 2, 0)
            ],
            values=[10.5, 15.2, 12.8],
            unit="%"
        )
        
        collection = TimeSeriesCollection(
            name="cpu_metrics",
            series=[ts1]
        )
        
        start_time = datetime(2024, 1, 1, 10, 0, 30)
        end_time = datetime(2024, 1, 1, 10, 1, 30)
        
        filtered = collection.filter_by_time_range(start_time, end_time)
        
        assert len(filtered.series) == 1
        assert len(filtered.series[0].timestamps) == 1
        assert filtered.series[0].timestamps[0] == datetime(2024, 1, 1, 10, 1, 0)
        assert filtered.series[0].values[0] == 15.2
    
    def test_to_dict(self):
        """Test conversion to dictionary"""
        ts1 = TimeSeriesData(
            name="cpu_user",
            timestamps=[datetime(2024, 1, 1, 10, 0, 0)],
            values=[10.5],
            unit="%"
        )
        
        collection = TimeSeriesCollection(
            name="cpu_metrics",
            series=[ts1],
            metadata={"host": "test-server"}
        )
        
        data_dict = collection.to_dict()
        
        assert data_dict["name"] == "cpu_metrics"
        assert data_dict["metadata"] == {"host": "test-server"}
        assert len(data_dict["series"]) == 1
        assert data_dict["series"][0]["name"] == "cpu_user"


class TestChartModels:
    """Test cases for chart configuration models"""
    
    def test_chart_config_creation(self):
        """Test creating ChartConfig"""
        config = ChartConfig(
            name="cpu_chart",
            title="CPU Usage",
            chart_type=ChartType.LINE,
            metrics=["%user", "%system"],
            y_axis=YAxisConfig(title="CPU %", type=YAxisType.LINEAR)
        )
        
        assert config.name == "cpu_chart"
        assert config.title == "CPU Usage"
        assert config.chart_type == ChartType.LINE
        assert config.metrics == ["%user", "%system"]
        assert config.y_axis.title == "CPU %"
        assert config.y_axis.type == YAxisType.LINEAR
    
    def test_dashboard_config_creation(self):
        """Test creating DashboardConfig"""
        chart_config = ChartConfig(
            name="cpu_chart",
            title="CPU Usage",
            chart_type=ChartType.LINE,
            metrics=["%user", "%system"]
        )
        
        dashboard = DashboardConfig(
            name="system_dashboard",
            title="System Performance",
            layout=ChartLayout.GRID,
            charts=[chart_config]
        )
        
        assert dashboard.name == "system_dashboard"
        assert dashboard.title == "System Performance"
        assert dashboard.layout == ChartLayout.GRID
        assert len(dashboard.charts) == 1
        assert dashboard.charts[0].name == "cpu_chart"
    
    def test_y_axis_config_defaults(self):
        """Test YAxisConfig default values"""
        config = ChartConfig(
            name="test_chart",
            title="Test Chart",
            chart_type=ChartType.LINE,
            metrics=["metric1"]
        )
        
        # Should have default Y-axis config
        assert config.y_axis is not None
        assert config.y_axis.title == "Value"
        assert config.y_axis.type == YAxisType.LINEAR
    
    def test_chart_template(self):
        """Test ChartTemplate model"""
        template = ChartTemplate(
            name="cpu_overview",
            title="CPU Overview",
            description="Complete CPU utilization breakdown",
            chart_type=ChartType.AREA,
            metrics=["%user", "%system", "%idle"],
            y_axis_title="CPU %"
        )
        
        assert template.name == "cpu_overview"
        assert template.title == "CPU Overview"
        assert template.chart_type == ChartType.AREA
        assert len(template.metrics) == 3
        assert template.y_axis_title == "CPU %"


if __name__ == "__main__":
    pytest.main([__file__]) 