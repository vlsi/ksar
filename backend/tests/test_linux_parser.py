"""
Test for Linux SAR parser.
"""

import pytest
from datetime import datetime
from app.parsers.linux import LinuxParser


def test_linux_parser_header():
    """Test parsing Linux SAR header."""
    parser = LinuxParser()
    header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
    
    parser.parse_header(header_line)
    
    assert parser.system_info.os_type == "Linux"
    assert parser.system_info.kernel == "3.10.0-1160.53.1.el7.x86_64"
    assert parser.system_info.hostname == "hostname.example.com"
    assert parser.system_info.nb_cpu == "4"


def test_linux_parser_cpu_data():
    """Test parsing CPU data."""
    parser = LinuxParser()
    
    # Parse header first
    header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
    parser.parse_header(header_line)
    
    # Parse CPU header line
    cpu_header = "16:08:45        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle"
    columns = cpu_header.split()
    result = parser.parse_line(cpu_header, columns)
    assert result == 2  # Graph data
    
    # Parse CPU data line
    cpu_data = "16:08:55        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99"
    columns = cpu_data.split()
    result = parser.parse_line(cpu_data, columns)
    assert result == 2  # Graph data
    
    # Check that data was stored
    assert len(parser.metrics) > 0
    assert "cpu_all" in parser.metrics


def test_linux_parser_ignore_lines():
    """Test that certain lines are ignored."""
    parser = LinuxParser()
    
    # Test ignored lines
    ignored_lines = [
        "##",
        "Average:",
        "Summary:",
        "Moyenne :",
        "Durchschn.:",
        "Media:"
    ]
    
    for line in ignored_lines:
        columns = [line]
        result = parser.parse_line(line, columns)
        assert result == 1  # Ignored


if __name__ == "__main__":
    pytest.main([__file__]) 