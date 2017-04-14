package net.atomique.ksar.XML;

import java.util.Arrays;
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

        final String[] result = {null};

        String[] s1 = Arrays.copyOfRange(columns, firstdatacolumn, columns.length);
        String header = String.join(" ", s1);

        StatHash.keySet().forEach((String item) -> {
            StatConfig tmp = StatHash.get(item);
            if (tmp.check_Header(header, s1.length)) result[0] = tmp.getGraphName();
        });
        return result[0];

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
