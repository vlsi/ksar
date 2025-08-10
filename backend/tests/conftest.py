"""
Pytest configuration and common fixtures for kSar tests.

This module provides common fixtures and configuration for all tests,
especially integration tests.
"""

import pytest
import tempfile
import os
import shutil
from unittest.mock import patch, MagicMock
from datetime import datetime, timedelta

from app.services.parser_service import ParserService
from app.services.chart_service import ChartService


@pytest.fixture(scope="session")
def sample_sar_content():
    """Provide sample SAR content for testing."""
    content = []
    content.append("Linux 5.15.0-91-generic (test-host) 01/15/24 _x86_64_ (8 CPU)")
    content.append("")
    content.append("16:00:01        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle")
    
    # Generate 10 minutes of sample data
    base_time = datetime.now().replace(hour=16, minute=0, second=1, microsecond=0)
    for i in range(10):
        timestamp = base_time + timedelta(minutes=i)
        time_str = timestamp.strftime("%H:%M:%S")
        # Simulate realistic CPU values
        usr = 15 + (i % 10)  # 15-25%
        sys = 5 + (i % 5)    # 5-10%
        idle = 100 - usr - sys
        content.append(f"{time_str}        all      {usr:6.2f}      0.00      {sys:6.2f}      0.00      0.00      0.00      0.00      0.00      0.00     {idle:6.2f}")
    
    content.append("")
    content.append("16:00:01        kbmemfree   kbavail kbmemused  %memused kbbuffers  kbcached  kbcommit   %commit  kbactive   kbinact   kbdirty")
    
    # Memory data
    for i in range(10):
        timestamp = base_time + timedelta(minutes=i)
        time_str = timestamp.strftime("%H:%M:%S")
        memfree = 8000000 - (i * 10000)  # Decreasing memory
        memused = 2000000 + (i * 10000)  # Increasing usage
        content.append(f"{time_str}     {memfree:8.0f} {memfree-100000:8.0f} {memused:8.0f}     {memused/(memfree+memused)*100:5.1f}   100000   2000000  {memused*1.2:8.0f}     {memused*1.2/(memfree+memused)*100:5.1f} {memused*0.8:8.0f} {memused*0.2:8.0f}       0")
    
    content.append("")
    content.append("16:00:01        DEV       tps     rkB/s     wkB/s   rMB/s   wMB/s avgrq-sz avgqu-sz     await     svctm     %util")
    
    # Disk I/O data
    for i in range(10):
        timestamp = base_time + timedelta(minutes=i)
        time_str = timestamp.strftime("%H:%M:%S")
        tps = 50 + (i % 20)  # 50-70 transactions per second
        rkb = 1000 + (i * 100)  # Increasing read KB
        wkb = 500 + (i * 50)   # Increasing write KB
        content.append(f"{time_str}        sda     {tps:6.0f} {rkb:8.0f} {wkb:8.0f}   {rkb/1024:5.1f}   {wkb/1024:5.1f}     {512 + i*10:6.0f}     {0.1 + i*0.01:6.2f}     {2.0 + i*0.1:6.1f}     {1.0 + i*0.05:6.2f}     {5.0 + i*0.5:5.1f}")
    
    return "\n".join(content)


@pytest.fixture(scope="function")
def temp_test_dir():
    """Provide a temporary directory for each test."""
    test_dir = tempfile.mkdtemp()
    yield test_dir
    # Clean up
    if os.path.exists(test_dir):
        shutil.rmtree(test_dir)


@pytest.fixture(scope="function")
def parser_service():
    """Provide a ParserService instance for each test."""
    return ParserService()


@pytest.fixture(scope="function")
def chart_service():
    """Provide a ChartService instance for each test."""
    return ChartService()


@pytest.fixture(scope="function")
def sample_sar_file(temp_test_dir, sample_sar_content):
    """Provide a sample SAR file for testing."""
    file_path = os.path.join(temp_test_dir, "sample.sar")
    with open(file_path, 'w') as f:
        f.write(sample_sar_content)
    return file_path


@pytest.fixture(scope="function")
def parsed_data(parser_service, sample_sar_file):
    """Provide parsed data from a sample SAR file."""
    return parser_service.parse_file(sample_sar_file)


@pytest.fixture(scope="function")
def time_series_collection(parser_service, parsed_data):
    """Provide a time series collection for testing."""
    return parser_service.get_time_series_collection(parsed_data.file_id)


@pytest.fixture(scope="function")
def dashboard_config():
    """Provide a basic dashboard configuration for testing."""
    from app.models.chart_models import DashboardConfig, ChartLayout
    
    return DashboardConfig(
        name="Test Dashboard",
        title="Test Dashboard",
        layout=ChartLayout.GRID
    )


@pytest.fixture(scope="function")
def chart_config():
    """Provide a basic chart configuration for testing."""
    from app.models.chart_models import ChartConfig, ChartType, YAxisConfig, YAxisType
    
    return ChartConfig(
        name="test_chart",
        title="Test Chart",
        chart_type=ChartType.LINE,
        metrics=["cpu_usr"],
        y_axis=YAxisConfig(title="CPU %", type=YAxisType.LINEAR, min=0, max=100)
    )


# Markers for different test types
def pytest_configure(config):
    """Configure pytest with custom markers."""
    config.addinivalue_line(
        "markers", "integration: marks tests as integration tests"
    )
    config.addinivalue_line(
        "markers", "api: marks tests as API tests"
    )
    config.addinivalue_line(
        "markers", "services: marks tests as service layer tests"
    )
    config.addinivalue_line(
        "markers", "performance: marks tests as performance tests"
    )
    config.addinivalue_line(
        "markers", "slow: marks tests as slow running tests"
    )
    config.addinivalue_line(
        "markers", "unit: marks tests as unit tests"
    )


# Skip slow tests by default unless --runslow is passed
def pytest_addoption(parser):
    """Add custom command line options."""
    parser.addoption(
        "--runslow", action="store_true", default=False, help="run slow tests"
    )


def pytest_collection_modifyitems(config, items):
    """Modify test collection based on command line options."""
    if not config.getoption("--runslow"):
        skip_slow = pytest.mark.skip(reason="need --runslow option to run")
        for item in items:
            if "slow" in item.keywords:
                item.add_marker(skip_slow) 