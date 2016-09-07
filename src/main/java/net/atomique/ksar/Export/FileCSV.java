package net.atomique.ksar.Export;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import net.atomique.ksar.Graph.Graph;
import net.atomique.ksar.Graph.List;
import net.atomique.ksar.UI.ParentNodeInfo;
import net.atomique.ksar.UI.SortedTreeNode;
import net.atomique.ksar.UI.TreeNodeInfo;
import net.atomique.ksar.kSar;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;

public class FileCSV implements Runnable {

    public FileCSV(String filename, kSar hissar) {
        csvfilename = filename;
        mysar = hissar;

    }

    public FileCSV(String filename, kSar hissar, JProgressBar g, JDialog d) {
        csvfilename = filename;
        mysar = hissar;
        progress_bar = g;
        dialog = d;
    }

    public void run() {
        // open file
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(csvfilename));
        } catch (IOException e) {
            out = null;
        }

        // print header
        tmpcsv.append("Date;");
        
        export_treenode_header(mysar.graphtree);
        tmpcsv.append("\n");
        Iterator<LocalDateTime> ite = mysar.myparser.getDateSamples().iterator();
        while (ite.hasNext()) {
            LocalDateTime tmpLDT = ite.next();

            Second tmp = new Second(tmpLDT.getSecond(),
                    tmpLDT.getMinute(),
                    tmpLDT.getHour(),
                    tmpLDT.getDayOfMonth(),
                    tmpLDT.getMonthValue(),
                    tmpLDT.getYear());

            export_treenode_data(mysar.graphtree, tmp);
            tmpcsv.append("\n");
        }
        try {
            out.write(tmpcsv.toString());
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(FileCSV.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        if (dialog != null) {
            dialog.dispose();
        }



    }


    public void export_treenode_header(SortedTreeNode node) {
        int num = node.getChildCount();

        if (num > 0) {
            Object obj1 = node.getUserObject();
            if (obj1 instanceof ParentNodeInfo) {
                ParentNodeInfo tmpnode = (ParentNodeInfo) obj1;
                List nodeobj = tmpnode.getNode_object();                
            }
            for (int i = 0; i < num; i++) {
                SortedTreeNode l = (SortedTreeNode) node.getChildAt(i);
                export_treenode_header(l);
            }
        } else {
            Object obj1 = node.getUserObject();
            if (obj1 instanceof TreeNodeInfo) {
                TreeNodeInfo tmpnode = (TreeNodeInfo) obj1;
                Graph nodeobj = tmpnode.getNode_object();
                if ( nodeobj.doPrint()) {
                    tmpcsv.append(nodeobj.getCsvHeader());
                    
                }
            }
        }
    }

     public void export_treenode_data(SortedTreeNode node, RegularTimePeriod time) {
        int num = node.getChildCount();

        if (num > 0) {
            Object obj1 = node.getUserObject();
            if (obj1 instanceof ParentNodeInfo) {
                ParentNodeInfo tmpnode = (ParentNodeInfo) obj1;
                List nodeobj = tmpnode.getNode_object();
            }
            for (int i = 0; i < num; i++) {
                SortedTreeNode l = (SortedTreeNode) node.getChildAt(i);
                export_treenode_data(l, time);
            }
        } else {
            Object obj1 = node.getUserObject();
            if (obj1 instanceof TreeNodeInfo) {
                TreeNodeInfo tmpnode = (TreeNodeInfo) obj1;
                Graph nodeobj = tmpnode.getNode_object();
                if ( nodeobj.doPrint()) {
                    tmpcsv.append(nodeobj.getCsvLine(time));
                    update_ui();

                }
            }
        }
    }
     
    private void update_ui() {
        if (progress_bar != null) {
            progress_bar.setValue(++progress_info);
            progress_bar.repaint();
        }

    }

    
    private StringBuilder tmpcsv = new StringBuilder();
    private int progress_info =0;
    private String csvfilename = null;
    private kSar mysar = null;
    private JProgressBar progress_bar = null;
    private JDialog dialog = null;
}
