/*
 * Copyright 2018 The kSAR Project. All rights reserved.
 * See the LICENSE file in the project root for more information.
 */

package net.atomique.ksar;

import net.atomique.ksar.xml.CnxHistory;
import net.atomique.ksar.xml.ColumnConfig;
import net.atomique.ksar.xml.GraphConfig;
import net.atomique.ksar.xml.HostInfo;
import net.atomique.ksar.xml.OSConfig;
import net.atomique.ksar.xml.PlotStackConfig;
import net.atomique.ksar.xml.StatConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class XMLConfig extends DefaultHandler {
  private static final String KSAR_DTD_PREFIX = "-//NET/ATOMIQUE/KSAR/";

  private static final Logger log = LoggerFactory.getLogger(XMLConfig.class);

  public InputSource resolveEntity(String publicId, String systemId)
      throws IOException {

    if (publicId == null || !publicId.startsWith(KSAR_DTD_PREFIX)) {
      return null;
    }

    String dtdFile = publicId.substring(KSAR_DTD_PREFIX.length() - 1);
    InputStream inputStream = getClass().getResourceAsStream(dtdFile);
    if (inputStream == null) {
      throw new FileNotFoundException("File " + publicId + " is not found in kSar resources");
    }
    return new InputSource(inputStream);
  }

  void loadFromResources(String fileName) {
    try (InputStream is = getClass().getResourceAsStream(fileName)) {
      if (is == null) {
        throw new FileNotFoundException("File " + fileName + " is not found in kSar resources");
      }
      InputSource source = new InputSource(is);
      source.setPublicId(KSAR_DTD_PREFIX + fileName);
      loadConfig(source);
    } catch (IOException e) {
      log.warn("XML error while parsing " + fileName, e);
    }
  }

  void loadConfig(File file) {
    InputSource source = new InputSource(file.toURI().toASCIIString());
    loadConfig(source);
  }

  private void loadConfig(InputSource source) {
    SAXParserFactory fabric;
    SAXParser parser;
    String id = source.getSystemId() + " (" + source.getPublicId() + ")";
    try {
      fabric = SAXParserFactory.newInstance();
      parser = fabric.newSAXParser();
      parser.parse(source, this);

    } catch (ParserConfigurationException | SAXException ex) {
      log.warn("XML error while parsing " + id, ex);
    } catch (IOException ioe) {
      String msg = "IO exception while parsing " + id;
      log.error(msg, ioe);
      throw new IllegalArgumentException(msg, ioe);
    }
  }

  void dump_XML() {

    GlobalOptions.getOSlist().keySet().forEach((String item) -> {

      OSConfig tmp = GlobalOptions.getOSlist().get(item);
      log.trace("-OS-{}", tmp.getOsName());

      tmp.getStatHash().keySet().forEach((String stat) -> {

        StatConfig tmp2 = tmp.getStatHash().get(stat);
        log.trace("--STAT-- "
            + tmp2.getStatName() + "=> "
            + tmp2.getGraphName() + " "
            + tmp2.getHeaderStr());
      });

      tmp.getGraphHash().keySet().forEach((String graph) -> {

        GraphConfig tmp3 = tmp.getGraphHash().get(graph);
        log.trace("---GRAPH--- "
            + tmp3.getName() + "=> "
            + tmp3.getTitle());

        tmp3.getPlotlist().keySet().forEach((String plot) -> {

          PlotStackConfig tmp4 = tmp3.getPlotlist().get(plot);
          log.trace("----PLOT---- "
              + tmp4.getTitle() + "=> "
              + tmp4.getHeaderStr());

        });
      });
    });

  }

  public void characters(char[] ch, int start, int length) throws SAXException {
    tempval = new String(ch, start, length);
  }

  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {

    /*
    if ("ConfiG".equals(qName)) {
      // config found
    }
    */
    if ("colors".equals(qName)) {
      in_colors = true;
    }
    if ("OS".equals(qName)) {
      in_OS = true;
    }

    if ("History".equals(qName)) {
      in_history = true;
    }

    if ("HostInfo".equals(qName)) {
      in_hostinfo = true;
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
        in_host = true;
      }
    }

    // OS
    if (in_OS) {
      if ("OSType".equals(qName)) {
        currentOS = GlobalOptions.getOSlist().get(attributes.getValue("name"));
        if (currentOS == null) {
          currentOS = new OSConfig(attributes.getValue("name"));
          GlobalOptions.getOSlist().put(currentOS.getOsName(), currentOS);
        }
      }
      if (currentOS != null) {
        if ("Stat".equals(qName)) {
          currentStat = new StatConfig(attributes.getValue("name"));
          currentOS.addStat(currentStat);
        }
        if ("Graph".equals(qName)) {
          currentGraph = new GraphConfig(attributes.getValue("name"), attributes.getValue("Title"),
              attributes.getValue("type"));
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
        //log.error("Err: {}", currentColor.getError_message());
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
        log.error("Err cnx is not valid");
        currentCnx = null;
      }
    }
    if (in_hostinfo) {
      if ("alias".equals(qName)) {
        currentHost.setAlias(tempval);
      }
      if ("description".equals(qName)) {
        currentHost.setDescription(tempval);
      }
      if ("memblocksize".equals(qName)) {
        currentHost.setMemBlockSize(tempval);
      }
    }
    if ("host".equals(qName)) {
      GlobalOptions.getHostInfoList().put(currentHost.getHostname(), currentHost);
      currentHost = null;
    }
  }


  private boolean beenparse = false;
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

