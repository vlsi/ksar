package net.atomique.ksar.xml;

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

    public String getHeaderStr() {
        return HeaderStr;
    }

    public void setHeaderStr(String s) {
        HeaderStr = s;
    }

    public String getStatName() {
        return StatName;
    }

    public boolean canDuplicateTime() {
        return duplicatetime;
    }

    public void setDuplicateTime(String s) {
        if ( "yes".equals(s) || "true".equals(s) ){
            duplicatetime=true;
        }
    }


    private String StatName = null;
    private String GraphName = null;
    private String HeaderStr = null;
    private boolean duplicatetime = false;
}
