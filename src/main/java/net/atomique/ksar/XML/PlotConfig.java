package net.atomique.ksar.XML;

import java.text.NumberFormat;
import net.atomique.ksar.Graph.IEEE1541Number;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.Range;

public class PlotConfig {

    public PlotConfig(String s) {
        Title = s;
    }

    public String[] getHeader() {
        return Header;
    }

    public String getTitle() {
        return Title;
    }

    public void setHeaderStr(String s) {
        this.Header = s.split("\\s+");
        HeaderStr = s;
    }

    public String getHeaderStr() {
        return HeaderStr;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setSize(String s) {
        Integer tmp = new Integer(s);
        if (tmp == null) {
            return;
        }
        this.size = tmp.intValue();
    }


    public NumberAxis getAxis() {
        NumberAxis tmp = new NumberAxis(Title);
        if ("1024".equals(base)) {
            NumberFormat decimalformat1 = new IEEE1541Number(factor.intValue());
            tmp.setNumberFormatOverride(decimalformat1);
        }

        if (range != null) {
            tmp.setRange(range);
        }
        return tmp;
    }

    public void setBase(String s) {
        if (s == null) {
            return;
        }
        base = s;
    }

    public void setFactor(String s) {
        factor = Double.parseDouble(s);
    }

    public void setRange(String s) {
        String[] t = s.split(",");
        if (t.length == 2) {
            Double min = Double.parseDouble(t[0]);
            Double max = Double.parseDouble(t[1]);
            range = new Range(min, max);
        }
    }
    

    private Double factor = null;
    private String base = null;
    private Range range = null;
    private int size = 1;
    private String Title = null;
    private String[] Header = null;
    private String HeaderStr = null;
}
