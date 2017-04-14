package net.atomique.ksar.XML;

public class StatConfig {

    public StatConfig(String s) {
        StatName = s;
    }

    public String getGraphName() {
        return GraphName;
    }

    public void setGraphName(String GraphName) {
        this.GraphName = GraphName;
    }

    public String[] getHeader() {
        return Header;
    }

    public String getHeaderStr() {
        return HeaderStr;
    }

    public void setHeaderStr(String s) {
        HeaderStr = s;
        this.Header = HeaderStr.split("\\s+");
        HeaderNum = Header.length;
    }

    public String getStatName() {
        return StatName;
    }

    public boolean check_Header(String c, int i) {
        if (!compare_Header(i)) {
            return false;
        }

        if (HeaderStr.equals(c)) {
            return true;
        }
        return false;
    }

    public boolean compare_Header(int i) {
        if (i == HeaderNum) {
            return true;
        }
        return false;
    }

    public boolean canDuplicateTime() {
        return duplicatetime;
    }

    public void setDuplicateTime(String s) {
        if ( "yes".equals(s) || "true".equals(s) ){
            duplicatetime=true;
        }
    }


    private int HeaderNum = 0;
    private String StatName = null;
    private String GraphName = null;
    private String Header[] = null;
    private String HeaderStr = null;
    private boolean duplicatetime = false;
}
