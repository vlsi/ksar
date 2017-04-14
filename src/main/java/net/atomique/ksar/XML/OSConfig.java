package net.atomique.ksar.XML;

import java.util.HashMap;
import java.util.Iterator;

public class OSConfig {

    public OSConfig(String s) {
        OsName = s;
    }

    public void addStat(StatConfig s) {
        StatHash.put(s.getStatName(), s);
    }

    public void addGraph(GraphConfig s) {
        GraphHash.put(s.getName(), s);
    }

    public String getOsName() {
        return OsName;
    }

    public String getStat(String[] columns, int firstdatacolumn) {

        StringBuffer tmpbuf = new StringBuffer();
        int num = 0;
        for (int i = firstdatacolumn; i < columns.length; i++) {
            if (tmpbuf.length() != 0) {
                tmpbuf.append(" ");
            }
            tmpbuf.append(columns[i]);
            num++;
        }

        Iterator<String> ite = StatHash.keySet().iterator();
        while (ite.hasNext()) {
            String tmptitle = ite.next();
            StatConfig tmp = (StatConfig) StatHash.get(tmptitle);
            if (tmp.check_Header(tmpbuf.toString(), num)) {
                return tmp.getGraphName();
            }
        }
        return null;
    }

    public StatConfig getStat(String statName) {
        if (StatHash.isEmpty()) {
            return null;
        }
        Iterator<String> ite = StatHash.keySet().iterator();
        while (ite.hasNext()) {
            String tmptitle = ite.next();
            StatConfig tmp = (StatConfig) StatHash.get(tmptitle);
            if ( tmp.getGraphName().equals(statName)) {
                return tmp;
            }
        }
        return null;
    }

    public GraphConfig getGraphConfig(String s) {
        if (GraphHash.isEmpty()) {
            return null;
        }
        return GraphHash.get(s);
    }

    public HashMap<String, StatConfig> getStatHash() {
        return StatHash;
    }

    public HashMap<String, GraphConfig> getGraphHash() {
        return GraphHash;
    }
    private String OsName = null;
    private HashMap<String, StatConfig> StatHash = new HashMap<>();
    private HashMap<String, GraphConfig> GraphHash = new HashMap<>();
}
