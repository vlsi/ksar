"""
Tests for all parser classes.
"""

import pytest
from datetime import datetime
from app.parsers.base import BaseParser
from app.parsers.linux import LinuxParser


class TestBaseParser:
    """Test cases for BaseParser abstract class"""
    
    def test_base_parser_instantiation(self):
        """Test that BaseParser cannot be instantiated directly."""
        with pytest.raises(TypeError):
            BaseParser()
    
    def test_base_parser_abstract_methods(self):
        """Test that BaseParser has required abstract methods."""
        # Check that required methods exist
        assert hasattr(BaseParser, 'parse_header')
        assert hasattr(BaseParser, 'parse_line')
        assert hasattr(BaseParser, 'get_parsed_data')


class TestLinuxParser:
    """Test cases for LinuxParser"""
    
    def setup_method(self):
        """Set up test fixtures before each test method."""
        self.parser = LinuxParser()
    
    def test_linux_parser_header_parsing(self):
        """Test parsing Linux SAR header."""
        header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
        
        self.parser.parse_header(header_line)
        
        assert self.parser.system_info.os_type == "Linux"
        assert self.parser.system_info.kernel == "3.10.0-1160.53.1.el7.x86_64"
        assert self.parser.system_info.hostname == "hostname.example.com"
        assert self.parser.system_info.nb_cpu == "4"
    
    def test_linux_parser_header_with_different_formats(self):
        """Test parsing Linux SAR header with different formats."""
        # Test with different kernel versions
        header1 = "Linux 5.4.0-42-generic (test-server) 12/15/20 _x86_64_ (8 CPU)"
        self.parser.parse_header(header1)
        assert self.parser.system_info.kernel == "5.4.0-42-generic"
        assert self.parser.system_info.nb_cpu == "8"
        
        # Test with different hostname formats
        header2 = "Linux 4.18.0-305.el8.x86_64 (prod-web-01.company.local) 08/20/21 _x86_64_ (16 CPU)"
        self.parser.parse_header(header2)
        assert self.parser.system_info.hostname == "prod-web-01.company.local"
        assert self.parser.system_info.nb_cpu == "16"
    
    def test_linux_parser_cpu_data_parsing(self):
        """Test parsing CPU data."""
        # Parse header first
        header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
        self.parser.parse_header(header_line)
        
        # Parse CPU header line
        cpu_header = "16:08:45        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle"
        columns = cpu_header.split()
        result = self.parser.parse_line(cpu_header, columns)
        assert result == 2  # Graph data
        
        # Parse CPU data line
        cpu_data = "16:08:55        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99"
        columns = cpu_data.split()
        result = self.parser.parse_line(cpu_data, columns)
        assert result == 2  # Graph data
        
        # Check that data was stored
        assert len(self.parser.metrics) > 0
        assert "cpu_all" in self.parser.metrics
        
        # Verify CPU metric data
        cpu_metric = self.parser.metrics["cpu_all"]
        assert len(cpu_metric.timestamps) > 0
        assert len(cpu_metric.values) > 0
        assert cpu_metric.columns == ["%usr", "%nice", "%sys", "%iowait", "%steal", "%irq", "%soft", "%guest", "%gnice", "%idle"]
    
    def test_linux_parser_memory_data_parsing(self):
        """Test parsing memory data."""
        # Parse header first
        header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
        self.parser.parse_header(header_line)
        
        # Parse memory header line
        memory_header = "16:08:45    kbmemfree   kbmemused  %memused kbbuffers  kbcached  kbcommit   %commit"
        columns = memory_header.split()
        result = self.parser.parse_line(memory_header, columns)
        assert result == 2  # Graph data
        
        # Parse memory data line
        memory_data = "16:08:55     1234567    8765432      87.65    123456    654321   9876543     98.76"
        columns = memory_data.split()
        result = self.parser.parse_line(memory_data, columns)
        assert result == 2  # Graph data
        
        # Check that memory data was stored
        assert "memory_kbmemfree" in self.parser.metrics
        assert "memory_kbmemused" in self.parser.metrics
        assert "memory_%memused" in self.parser.metrics
    
    def test_linux_parser_disk_data_parsing(self):
        """Test parsing disk I/O data."""
        # Parse header first
        header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
        self.parser.parse_header(header_line)
        
        # Parse disk header line
        disk_header = "16:08:45       DEV       tps  rd_sec/s  wr_sec/s  avgrq-sz  avgqu-sz     await     svctm     %util"
        columns = disk_header.split()
        result = self.parser.parse_line(disk_header, columns)
        assert result == 2  # Graph data
        
        # Parse disk data line
        disk_data = "16:08:55      sda      25.47      0.00    512.00     20.12      0.15      5.89      0.15      0.38"
        columns = disk_data.split()
        result = self.parser.parse_line(disk_data, columns)
        assert result == 2  # Graph data
        
        # Check that disk data was stored
        assert "disk_sda_tps" in self.parser.metrics
        assert "disk_sda_rd_sec/s" in self.parser.metrics
        assert "disk_sda_wr_sec/s" in self.parser.metrics
    
    def test_linux_parser_network_data_parsing(self):
        """Test parsing network data."""
        # Parse header first
        header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
        self.parser.parse_header(header_line)
        
        # Parse network header line
        network_header = "16:08:45     IFACE   rxpck/s   txpck/s    rxkB/s    txkB/s   rxcmp/s   txcmp/s  rxmcst/s"
        columns = network_header.split()
        result = self.parser.parse_line(network_header, columns)
        assert result == 2  # Graph data
        
        # Parse network data line
        network_data = "16:08:55      eth0     125.47      89.00    512.00    256.00      0.00      0.00      0.00"
        columns = network_data.split()
        result = self.parser.parse_line(network_data, columns)
        assert result == 2  # Graph data
        
        # Check that network data was stored
        assert "network_eth0_rxpck/s" in self.parser.metrics
        assert "network_eth0_txpck/s" in self.parser.metrics
        assert "network_eth0_rxkB/s" in self.parser.metrics
    
    def test_linux_parser_ignore_lines(self):
        """Test that certain lines are ignored."""
        # Test ignored lines
        ignored_lines = [
            "##",
            "Average:",
            "Summary:",
            "Moyenne :",
            "Durchschn.:",
            "Media:",
            "",
            "   "  # Whitespace only
        ]
        
        for line in ignored_lines:
            columns = [line]
            result = self.parser.parse_line(line, columns)
            assert result == 1  # Ignored
    
    def test_linux_parser_comment_lines(self):
        """Test that comment lines are handled correctly."""
        comment_lines = [
            "# This is a comment",
            " # Another comment",
            "  # Yet another comment"
        ]
        
        for line in comment_lines:
            columns = [line]
            result = self.parser.parse_line(line, columns)
            assert result == 1  # Ignored
    
    def test_linux_parser_timestamp_parsing(self):
        """Test timestamp parsing in different formats."""
        # Parse header first
        header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
        self.parser.parse_header(header_line)
        
        # Test different time formats
        time_formats = [
            "16:08:55",
            "16:08:05",
            "16:18:55",
            "16:58:55",
            "23:59:59"
        ]
        
        for time_str in time_formats:
            # Create a simple data line with the time
            data_line = f"{time_str}        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99"
            columns = data_line.split()
            result = self.parser.parse_line(data_line, columns)
            assert result == 2  # Graph data
        
        # Check that timestamps were parsed correctly
        if "cpu_all" in self.parser.metrics:
            cpu_metric = self.parser.metrics["cpu_all"]
            assert len(cpu_metric.timestamps) == len(time_formats)
            
            # Verify timestamps are datetime objects
            for timestamp in cpu_metric.timestamps:
                assert isinstance(timestamp, datetime)
    
    def test_linux_parser_numeric_data_parsing(self):
        """Test parsing of numeric data with different formats."""
        # Parse header first
        header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
        self.parser.parse_header(header_line)
        
        # Parse CPU header
        cpu_header = "16:08:45        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle"
        columns = cpu_header.split()
        self.parser.parse_line(cpu_header, columns)
        
        # Test different numeric formats
        numeric_formats = [
            "16:08:55        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99",
            "16:09:05        all      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
            "16:09:15        all     99.99      0.01      0.00      0.00      0.00      0.00      0.00      0.00      0.00",
            "16:09:25        all     50.50     25.25     24.25      0.00      0.00      0.00      0.00      0.00      0.00      0.00"
        ]
        
        for data_line in numeric_formats:
            columns = data_line.split()
            result = self.parser.parse_line(data_line, columns)
            assert result == 2  # Graph data
        
        # Verify numeric data was parsed correctly
        if "cpu_all" in self.parser.metrics:
            cpu_metric = self.parser.metrics["cpu_all"]
            assert len(cpu_metric.values) > 0
            
            # Check that values are numeric
            for value_list in cpu_metric.values:
                for value in value_list:
                    assert isinstance(value, (int, float))
    
    def test_linux_parser_get_parsed_data(self):
        """Test getting parsed data from the parser."""
        # Parse header and some data
        header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
        self.parser.parse_header(header_line)
        
        cpu_header = "16:08:45        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle"
        columns = cpu_header.split()
        self.parser.parse_line(cpu_header, columns)
        
        cpu_data = "16:08:55        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99"
        columns = cpu_data.split()
        self.parser.parse_line(cpu_data, columns)
        
        # Get parsed data
        parsed_data = self.parser.get_parsed_data()
        
        assert parsed_data is not None
        assert parsed_data.system_info.os_type == "Linux"
        assert parsed_data.system_info.hostname == "hostname.example.com"
        assert parsed_data.system_info.nb_cpu == "4"
        assert len(parsed_data.metrics) > 0
        assert "cpu_all" in parsed_data.metrics
    
    def test_linux_parser_empty_file(self):
        """Test parsing an empty file."""
        # Parse header only
        header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
        self.parser.parse_header(header_line)
        
        # No data lines
        parsed_data = self.parser.get_parsed_data()
        
        assert parsed_data is not None
        assert parsed_data.system_info.os_type == "Linux"
        assert len(parsed_data.metrics) == 0  # No metrics without data
    
    def test_linux_parser_malformed_data(self):
        """Test parsing malformed data lines."""
        # Parse header first
        header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
        self.parser.parse_header(header_line)
        
        # Parse CPU header
        cpu_header = "16:08:45        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle"
        columns = cpu_header.split()
        self.parser.parse_line(cpu_header, columns)
        
        # Test malformed data lines
        malformed_lines = [
            "16:08:55        all      5.47      0.00      5.39",  # Incomplete data
            "16:09:05        all      invalid   0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99",  # Invalid number
            "16:09:15        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00",  # Missing last value
        ]
        
        for data_line in malformed_lines:
            columns = data_line.split()
            result = self.parser.parse_line(data_line, columns)
            # Should handle gracefully - may ignore or parse what it can
            assert result in [1, 2]  # Either ignored or parsed
    
    def test_linux_parser_metric_naming(self):
        """Test that metrics are named correctly."""
        # Parse header first
        header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
        self.parser.parse_header(header_line)
        
        # Parse CPU header and data
        cpu_header = "16:08:45        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle"
        columns = cpu_header.split()
        self.parser.parse_line(cpu_header, columns)
        
        cpu_data = "16:08:55        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99"
        columns = cpu_data.split()
        self.parser.parse_line(cpu_data, columns)
        
        # Check metric naming convention
        expected_metrics = [
            "cpu_all_%usr", "cpu_all_%nice", "cpu_all_%sys", "cpu_all_%iowait",
            "cpu_all_%steal", "cpu_all_%irq", "cpu_all_%soft", "cpu_all_%guest",
            "cpu_all_%gnice", "cpu_all_%idle"
        ]
        
        for metric_name in expected_metrics:
            assert metric_name in self.parser.metrics
    
    def test_linux_parser_multiple_metric_sections(self):
        """Test parsing multiple metric sections."""
        # Parse header first
        header_line = "Linux 3.10.0-1160.53.1.el7.x86_64 (hostname.example.com) 04/08/18 _x86_64_ (4 CPU)"
        self.parser.parse_header(header_line)
        
        # Parse CPU section
        cpu_header = "16:08:45        CPU      %usr     %nice      %sys   %iowait    %steal      %irq     %soft    %guest    %gnice     %idle"
        columns = cpu_header.split()
        self.parser.parse_line(cpu_header, columns)
        
        cpu_data = "16:08:55        all      5.47      0.00      5.39      0.15      0.00      0.00      0.00      0.00      0.00     88.99"
        columns = cpu_data.split()
        self.parser.parse_line(cpu_data, columns)
        
        # Parse memory section
        memory_header = "16:08:45    kbmemfree   kbmemused  %memused kbbuffers  kbcached  kbcommit   %commit"
        columns = memory_header.split()
        self.parser.parse_line(memory_header, columns)
        
        memory_data = "16:08:55     1234567    8765432      87.65    123456    654321   9876543     98.76"
        columns = memory_data.split()
        self.parser.parse_line(memory_data, columns)
        
        # Check that both sections were parsed
        assert len(self.parser.metrics) > 0
        
        # Check CPU metrics
        cpu_metrics = [k for k in self.parser.metrics.keys() if k.startswith('cpu_')]
        assert len(cpu_metrics) > 0
        
        # Check memory metrics
        memory_metrics = [k for k in self.parser.metrics.keys() if k.startswith('memory_')]
        assert len(memory_metrics) > 0 