/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar.xml;

import java.util.Arrays;
import java.util.HashMap;

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
        //this is called for each line of source file

        String[] s1 = Arrays.copyOfRange(columns, firstdatacolumn, columns.length);
        String header = String.join(" ", s1);

        //cache Mapping of HeaderStr to StatName - get StatHash more efficiently
        createCacheForMappingOfHeaderStr2StatName();
        String statName = MappingStatHeaderName.get(header);

        if (statName !=null)
            return StatHash.get(statName).getGraphName();
        else
            return null;

    }

    private void createCacheForMappingOfHeaderStr2StatName() {
        //do this only once - all Stats are known - create reverse maaping
        if (MappingStatHeaderName == null) {

            MappingStatHeaderName = new HashMap<>();

            StatHash.forEach((k,v) -> {
                String statName = v.getStatName();
                String graphHeader = v.getHeaderStr();

                MappingStatHeaderName.put(graphHeader, statName);
            });
        }
    }

    public StatConfig getStat(String statName) {
        if (StatHash.isEmpty()) {
            return null;
        }

        StatConfig[] result = {null};
        StatHash.keySet().forEach((String item) -> {
            StatConfig tmp = StatHash.get(item);
            if ( tmp.getGraphName().equals(statName)) result[0] = tmp;
        });
        return result[0];
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

    private HashMap<String, String> MappingStatHeaderName;

}
