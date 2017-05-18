package net.atomique.ksar.parser;

import static org.junit.Assert.*;

import net.atomique.ksar.Config;
import net.atomique.ksar.kSar;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RunWith(Parameterized.class)
public class LinuxHeaderTest {
  private String datetimeformat;
  private String header;
  private String sarString;
  private LocalDateTime expectedDate;
  private Linux sut;

  public LinuxHeaderTest(String header, String sarString, LocalDateTime expectedDate) {
    this.header = header;
    this.sarString = sarString;
    this.expectedDate = expectedDate;
  }

  @Before
  public void setUp() throws Exception {
    datetimeformat = "Automatic Detection";
    Config.setLinuxDateFormat(datetimeformat);
    sut = new Linux();
  }

  @Test
  public void test() {
    String[] columns = sarString.split("\\s+");
    kSar ksar = new kSar();
    sut.init(ksar, header);
    sut.parse(sarString, columns);
    assertEquals(expectedDate, sut.get_startofgraph());
  }

  @Parameters
  public static Collection testValues() {

    // current Byte, expected value, expected unit, test name
    String str = "2016-03-28 09:10:01";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime dateTime = LocalDateTime.parse(str, formatter);

    return Arrays.asList(new Object[][] {
        { "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  03/28/16  _x86_64_  (48 CPU)",
            "09:10:01          6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
            dateTime },
        { "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  03/28/2016  _x86_64_  (48 CPU)",
            "09:10:01          6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
            dateTime },
        { "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  20160328  _x86_64_  (48 CPU)",
            "09:10:01          6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
            dateTime },
        { "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  2016-03-28  _x86_64_  (48 CPU)",
            "09:10:01          6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
            dateTime },
        { "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  03/28/16  _x86_64_  (48 CPU)",
            "09:10:01    AM      6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
            dateTime },
        { "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  03/28/2016  _x86_64_  (48 CPU)",
            "09:10:01    AM      6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
            dateTime },
        { "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  20160328  _x86_64_  (48 CPU)",
            "09:10:01     AM     6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
            dateTime },
        { "Linux 3.10.0-327.el7.x86_64 (hostname.example.com)  2016-03-28  _x86_64_  (48 CPU)",
            "09:10:01    AM      6      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00    100.00",
            dateTime }, });
  }

}
