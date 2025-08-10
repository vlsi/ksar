"""
Time series data models for chart generation.
"""

from typing import Dict, List, Optional, Any, Union, Tuple
from dataclasses import dataclass, field
from datetime import datetime, timedelta
import pandas as pd
import numpy as np
import json
import logging

logger = logging.getLogger(__name__)


@dataclass
class TimeSeriesData:
    """Enhanced time series data for charting."""
    metric_name: str
    timestamps: List[datetime] = field(default_factory=list)
    values: List[float] = field(default_factory=list)
    columns: List[str] = field(default_factory=list)
    metadata: Dict[str, Any] = field(default_factory=dict)
    
    def __post_init__(self):
        """Validate data after initialization."""
        if len(self.timestamps) != len(self.values):
            # Try to fix the mismatch by truncating to the shorter length
            min_length = min(len(self.timestamps), len(self.values))
            if min_length > 0:
                self.timestamps = self.timestamps[:min_length]
                self.values = self.values[:min_length]
                logger.warning(f"Truncated {self.metric_name} to {min_length} data points due to length mismatch")
            else:
                # If both are empty, that's okay
                if len(self.timestamps) == 0 and len(self.values) == 0:
                    return
                else:
                    raise ValueError(f"Timestamps and values must have the same length for {self.metric_name}. Got {len(self.timestamps)} timestamps and {len(self.values)} values")
    
    def to_dataframe(self) -> pd.DataFrame:
        """Convert to pandas DataFrame."""
        if not self.timestamps or not self.values:
            return pd.DataFrame()
        
        # Reshape values if we have multiple columns
        if len(self.columns) > 1:
            # Values are stored as flat list, need to reshape
            values_per_timestamp = len(self.columns)
            if len(self.values) % values_per_timestamp == 0:
                reshaped_values = np.array(self.values).reshape(-1, values_per_timestamp)
                df = pd.DataFrame(reshaped_values, columns=self.columns, index=self.timestamps)
            else:
                # Fallback: use first column only
                df = pd.DataFrame({'value': self.values}, index=self.timestamps)
        else:
            df = pd.DataFrame({'value': self.values}, index=self.timestamps)
        
        return df
    
    def get_statistics(self) -> Dict[str, float]:
        """Calculate comprehensive statistics for the time series."""
        if not self.values:
            return {}
        
        values = np.array(self.values)
        stats = {
            'min': float(np.min(values)),
            'max': float(np.max(values)),
            'mean': float(np.mean(values)),
            'std': float(np.std(values)),
            'count': len(values),
            'sum': float(np.sum(values))
        }
        
        # Add percentiles
        for p in [1, 5, 10, 25, 50, 75, 90, 95, 99]:
            stats[f'p{p}'] = float(np.percentile(values, p))
        
        # Add time-based statistics
        if self.timestamps:
            time_range = self.get_time_range()
            if time_range:
                stats['duration_hours'] = (time_range['end'] - time_range['start']).total_seconds() / 3600
                stats['data_points_per_hour'] = len(values) / stats['duration_hours'] if stats['duration_hours'] > 0 else 0
        
        return stats
    
    def get_time_range(self) -> Optional[Dict[str, datetime]]:
        """Get the time range of the data."""
        if not self.timestamps:
            return None
        
        return {
            'start': min(self.timestamps),
            'end': max(self.timestamps)
        }
    
    def filter_by_time_range(self, start_time: Optional[datetime] = None, end_time: Optional[datetime] = None) -> 'TimeSeriesData':
        """Filter data by time range."""
        if not self.timestamps:
            return self
        
        filtered_indices = []
        for i, timestamp in enumerate(self.timestamps):
            if start_time and timestamp < start_time:
                continue
            if end_time and timestamp > end_time:
                continue
            filtered_indices.append(i)
        
        if not filtered_indices:
            return TimeSeriesData(self.metric_name)
        
        return TimeSeriesData(
            metric_name=self.metric_name,
            timestamps=[self.timestamps[i] for i in filtered_indices],
            values=[self.values[i] for i in filtered_indices],
            columns=self.columns.copy(),
            metadata=self.metadata.copy()
        )
    
    def resample(self, freq: str = '1H', method: str = 'mean') -> 'TimeSeriesData':
        """Resample the time series data."""
        df = self.to_dataframe()
        if df.empty:
            return self
        
        # Resample based on frequency
        resampled = df.resample(freq)
        
        if method == 'mean':
            resampled_data = resampled.mean()
        elif method == 'sum':
            resampled_data = resampled.sum()
        elif method == 'max':
            resampled_data = resampled.max()
        elif method == 'min':
            resampled_data = resampled.min()
        else:
            resampled_data = resampled.mean()
        
        # Convert back to TimeSeriesData
        timestamps = resampled_data.index.tolist()
        values = resampled_data.values.flatten().tolist()
        
        return TimeSeriesData(
            metric_name=self.metric_name,
            timestamps=timestamps,
            values=values,
            columns=resampled_data.columns.tolist(),
            metadata=self.metadata.copy()
        )
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for JSON serialization."""
        return {
            'metric_name': self.metric_name,
            'timestamps': [ts.isoformat() for ts in self.timestamps],
            'values': self.values,
            'columns': self.columns,
            'metadata': self.metadata,
            'statistics': self.get_statistics(),
            'time_range': self.get_time_range()
        }


@dataclass
class TimeSeriesCollection:
    """Collection of time series data with enhanced functionality."""
    name: str
    series: Dict[str, TimeSeriesData] = field(default_factory=dict)
    metadata: Dict[str, Any] = field(default_factory=dict)
    
    def add_series(self, name: str, series_data: TimeSeriesData) -> None:
        """Add a time series to the collection."""
        self.series[name] = series_data
    
    def get_series(self, name: str) -> Optional[TimeSeriesData]:
        """Get a specific time series by name."""
        return self.series.get(name)
    
    def list_series(self) -> List[str]:
        """List all series names."""
        return list(self.series.keys())
    
    def get_time_range(self) -> Optional[Dict[str, datetime]]:
        """Get the overall time range of all series."""
        if not self.series:
            return None
        
        all_timestamps = []
        for series in self.series.values():
            all_timestamps.extend(series.timestamps)
        
        if not all_timestamps:
            return None
        
        return {
            'start': min(all_timestamps),
            'end': max(all_timestamps)
        }
    
    def get_statistics_summary(self) -> Dict[str, Any]:
        """Get summary statistics for all series."""
        summary = {
            'total_series': len(self.series),
            'time_range': self.get_time_range(),
            'series_stats': {}
        }
        
        for name, series in self.series.items():
            summary['series_stats'][name] = series.get_statistics()
        
        return summary
    
    def filter_by_time_range(self, start_time: Optional[datetime] = None, end_time: Optional[datetime] = None) -> 'TimeSeriesCollection':
        """Filter all series by time range."""
        filtered_collection = TimeSeriesCollection(
            name=self.name,
            metadata=self.metadata.copy()
        )
        
        for name, series in self.series.items():
            filtered_series = series.filter_by_time_range(start_time, end_time)
            if filtered_series.timestamps:  # Only add if there's data
                filtered_collection.add_series(name, filtered_series)
        
        return filtered_collection
    
    def to_dataframe(self, series_name: Optional[str] = None) -> pd.DataFrame:
        """Convert to pandas DataFrame."""
        if series_name:
            series = self.get_series(series_name)
            return series.to_dataframe() if series else pd.DataFrame()
        
        # Combine all series
        if not self.series:
            return pd.DataFrame()
        
        dfs = []
        for name, series in self.series.items():
            df = series.to_dataframe()
            if not df.empty:
                # Add series name as prefix to columns
                df.columns = [f"{name}_{col}" for col in df.columns]
                dfs.append(df)
        
        if not dfs:
            return pd.DataFrame()
        
        # Combine all dataframes
        combined_df = pd.concat(dfs, axis=1)
        return combined_df
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for JSON serialization."""
        return {
            'name': self.name,
            'series': {name: series.to_dict() for name, series in self.series.items()},
            'metadata': self.metadata,
            'statistics_summary': self.get_statistics_summary()
        } 