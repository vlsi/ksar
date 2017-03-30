/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.atomique.ksar;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.atomique.ksar.XML.CnxHistory;
import net.atomique.ksar.XML.ColumnConfig;
import net.atomique.ksar.XML.GraphConfig;
import net.atomique.ksar.XML.HostInfo;
import net.atomique.ksar.XML.OSConfig;
import net.atomique.ksar.XML.PlotStackConfig;
import net.atomique.ksar.XML.StatConfig;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Max
 */
public class XMLConfig extends DefaultHandler {

    public XMLConfig(String filename) {
        load_config(filename);
        
    }

    public XMLConfig(InputStream is) {
        load_config(is);
    }

    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {

            InputSource inputSource = null;
            try {
                String dtdFile = systemId.substring(systemId.lastIndexOf("/"));
                InputStream inputStream = EntityResolver.class.getResourceAsStream(dtdFile);
                inputSource = new InputSource(inputStream);
            } catch (Exception e) {
                // No action; just let the null InputSource pass through
            }

            // If nothing found, null is returned, for normal processing
            return inputSource;
        }
    
    public void load_config(InputStream is) {
        SAXParserFactory fabric = null;
        SAXParser parser = null;
        XMLReader reader = null;
        try {
            fabric = SAXParserFactory.newInstance();
            parser = fabric.newSAXParser();
            parser.parse(is, this);

        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }
        //dump_XML();
        try {
            is.close();
        } catch (IOException ex) {
            Logger.getLogger(XMLConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void load_config(String xmlfile) {
        SAXParserFactory fabric = null;
        SAXParser parser = null;
        try {
            fabric = SAXParserFactory.newInstance();
            parser = fabric.newSAXParser();
            parser.parse(xmlfile, this);

        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    public void dump_XML() {
        SortedSet<String> sortedset = new TreeSet<String>(GlobalOptions.getOSlist().keySet());
        Iterator<String> it = sortedset.iterator();
        while (it.hasNext()) {
            OSConfig tmp = (OSConfig) GlobalOptions.getOSlist().get(it.next());
            System.out.println("-OS-" + tmp.getOSname());
            SortedSet<String> sortedset2 = new TreeSet<String>(tmp.getStatHash().keySet());
            Iterator<String> it2 = sortedset2.iterator();
            while (it2.hasNext()) {
                StatConfig tmp2 = (StatConfig) tmp.getStatHash().get(it2.next());
                System.out.println("--STAT-- "
                        + tmp2.getStatName() + "=> "
                        + tmp2.getGraphName() + " "
                        + tmp2.getHeaderStr());
            }
            SortedSet<String> sortedset3 = new TreeSet<String>(tmp.getGraphHash().keySet());
            Iterator<String> it3 = sortedset3.iterator();
            while (it3.hasNext()) {
                GraphConfig tmp3 = (GraphConfig) tmp.getGraphHash().get(it3.next());
                System.out.println("---GRAPH--- "
                        + tmp3.getName() + "=> "
                        + tmp3.getTitle());
                SortedSet<String> sortedset4 = new TreeSet<String>(tmp3.getPlotlist().keySet());
                Iterator<String> it4 = sortedset4.iterator();
                while (it4.hasNext()) {
                    PlotStackConfig tmp4 = (PlotStackConfig) tmp3.getPlotlist().get(it4.next());
                    System.out.println("----PLOT---- "
                            + tmp4.getTitle() + "=> "
                            + tmp4.getHeaderStr());

                }
            }
        }

    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        tempval = new String(ch, start, length);
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        if ("ConfiG".equals(qName)) {
            // config found
        }
        if ("colors".equals(qName)) {
            in_colors = true;
        }
        if ("OS".equals(qName)) {
            in_OS = true;
        }

        if ("History".equals(qName)) {
            in_history = true;
        }

        if ( "HostInfo".equals(qName)) {
            in_hostinfo=true;
        }
        
        // COLORS
        if (in_colors) {
            if ("itemcolor".equals(qName)) {
                currentColor = new ColumnConfig(attributes.getValue("name"));
                in_color = true;
            }
        }

        // history
        if (in_history) {
            if ("cnx".equals(qName)) {
                currentCnx = new CnxHistory(attributes.getValue("link"));
                in_cnx = true;
            }
        }
        // hostinfo
        if (in_hostinfo) {
            if ("host".equals(qName)) {
                currentHost = new HostInfo(attributes.getValue("name"));
                in_host=true;
            }
        }

        // OS
        if (in_OS) {
            if ("OSType".equals(qName)) {
                currentOS = GlobalOptions.getOSlist().get(attributes.getValue("name"));
                if (currentOS == null) {
                    currentOS = new OSConfig(attributes.getValue("name"));
                    GlobalOptions.getOSlist().put(currentOS.getOSname(), currentOS);
                }
            }
            if (currentOS != null) {
                if ("Stat".equals(qName)) {
                    currentStat = new StatConfig(attributes.getValue("name"));
                    currentOS.addStat(currentStat);
                }
                if ("Graph".equals(qName)) {
                    currentGraph = new GraphConfig(attributes.getValue("name"), attributes.getValue("Title"), attributes.getValue("type"));
                    currentOS.addGraph(currentGraph);
                }
                if (currentGraph != null) {
                    if ("Plot".equals(qName)) {
                        currentPlot = new PlotStackConfig(attributes.getValue("Title"));
                        String size_tmp = attributes.getValue("size");
                        if (size_tmp != null) {
                            currentPlot.setSize(size_tmp);
                        }
                        currentGraph.addPlot(currentPlot);
                    }
                    if ("Stack".equals(qName)) {
                        currentStack = new PlotStackConfig(attributes.getValue("Title"));
                        String size_tmp = attributes.getValue("size");
                        if (size_tmp != null) {
                            currentStack.setSize(size_tmp);
                        }
                        currentGraph.addStack(currentStack);
                    }

                    if (currentPlot != null) {
                        if ("format".equals(qName)) {
                            currentPlot.setBase(attributes.getValue("base"));
                            currentPlot.setFactor(attributes.getValue("factor"));
                        }
                    }
                    if (currentStack != null) {
                        if ("format".equals(qName)) {
                            currentStack.setBase(attributes.getValue("base"));
                            currentStack.setFactor(attributes.getValue("factor"));
                        }
                    }
                }

            }
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {

        // clean up tempval;
        tempval = tempval.trim();
        if ("ConfiG".equals(qName)) {
            beenparse = true;
        }
        if ("colors".equals(qName)) {
            in_colors = false;
        }
        if ("OSType".equals(qName)) {
            currentOS = null;
        }
        if ("Stat".equals(qName)) {
            currentStat = null;
        }
        if ("Graph".equals(qName)) {
            currentGraph = null;
        }
        if ("Cnx".equals(qName)) {
            currentCnx = null;
        }
        if ("Plot".equals(qName)) {
            currentPlot = null;
        }
        if ("Stack".equals(qName)) {
            currentStack = null;
        }
        if ("HostInfo".equals(qName)) {
            in_hostinfo = false;
        }
        


        if (currentStat != null) {
            if ("headerstr".equals(qName)) {
                currentStat.setHeaderStr(tempval);
            }
            if ("graphname".equals(qName)) {
                currentStat.setGraphName(tempval);
            }
            if ("duplicate".equals(qName)) {
                currentStat.setDuplicateTime(tempval);
            }
        }
        
        if ("cols".equals(qName)) {
            if (currentPlot != null) {
                currentPlot.setHeaderStr(tempval);

            }
            if (currentStack != null) {
                currentStack.setHeaderStr(tempval);
            }
        }
        if ("range".equals(qName)) {
            if (currentPlot != null) {
                currentPlot.setRange(tempval);

            }
            if (currentStack != null) {
                currentStack.setRange(tempval);
            }
        }


        if ("itemcolor".equals(qName)) {
            if (currentColor.is_valid()) {
                GlobalOptions.getColorlist().put(currentColor.getData_title(), currentColor);
            } else {
                System.err.println("Err: " + currentColor.getError_message());
                currentColor = null;
            }
            in_color = false;
        }

        if (in_color) {
            if ("color".equals(qName) && currentColor != null) {
                currentColor.setData_color(tempval);
            }
        }

        if (in_cnx) {
            if ("command".equals(qName) && currentCnx != null) {
                currentCnx.addCommand(tempval);
            }
        }
        if ("cnx".equals(qName)) {
            if (currentCnx.isValid()) {
                GlobalOptions.getHistoryList().put(currentCnx.getLink(), currentCnx);
            } else {
                System.err.println("Err cnx is not valid");
                currentCnx = null;
            }
        }
        if ( in_hostinfo ) {
            if ( "alias".equals(qName)) {
                currentHost.setAlias(tempval);
            }
            if ( "description".equals(qName)) {
                currentHost.setDescription(tempval);
            }
            if ( "memblocksize".equals(qName)) {
                currentHost.setMemBlockSize(tempval);
            }
        }
        if ( "host".equals(qName)) {
            GlobalOptions.getHostInfoList().put(currentHost.getHostname(), currentHost);
            currentHost=null;
        }
    }

    
    public boolean beenparse = false;
    private String tempval;
    private boolean in_color = false;
    private boolean in_colors = false;
    private boolean in_OS = false;
    private boolean in_history = false;
    private boolean in_cnx = false;
    private boolean in_hostinfo = false;
    private boolean in_host = false;
    private ColumnConfig currentColor = null;
    private OSConfig currentOS = null;
    private StatConfig currentStat = null;
    private GraphConfig currentGraph = null;
    private PlotStackConfig currentPlot = null;
    private PlotStackConfig currentStack = null;
    private CnxHistory currentCnx = null;
    private HostInfo currentHost = null;

    
}

