/**
 * @author Antoine
 *
 * Contrary to DatabaseLoader, the goal is to load various data from a folder
 * containing standard csv files.
 * In actual facts, for speed purposes, it's the preferred input format even
 * though it's considerably harder to parse.
 *
 * NB: contrary to DatabaseLoader (that needs to connect to a database, something
 *     we do one and for all in the constructor), here all the methods are declared
 *     static.
 */
package org.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;
import javafx.util.Pair;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.graph.implementations.SingleGraph;

public class MissionLoader {

    private static class EdgeMaker {

        private final HashMap<String, Integer> num = new HashMap<>();

        public String retrieveEdgeName(String orig, String dest) {
            String k = orig + "->" + dest;

            if (num.containsKey(k)) {
                int n = num.get(k);
                num.put(k, n + 1);
                return k + String.valueOf(n + 1);
            } else {
                num.put(k, 1);
                return k + "1";
            }
        }
    }

    // FIXME: this can be parallelized quite easily
    public static boolean loadWholeGraphFromDirectory(String directory) {
        String recipients = directory + "/EmlRecipients.csv";
        String attachments = directory + "/EmlAttachments.csv";
        BufferedReader br = null;
        String line = "";

        Globals.mainDirectory = directory;

        MultiGraph ret = new MultiGraph("Whole graph");
        ret.setAutoCreate(true);
        ret.setStrict(false);

        // msgid -> ("20/01/2017", [(("Isak", "from"), ("Sicheng", "to"), ("Florian", "cc")])
        HashMap<String, Pair<String, ArrayList<Pair<String, String>>>> msg = new HashMap<>();

        /**
         ******************************************************************
         ******************* Loads data from EmlRecipients ****************
         * *****************************************************************
         */
        try {
            br = new BufferedReader(new FileReader(recipients));

            while ((line = br.readLine()) != null) {
                String[] toks = line.split(";");
                String msgid = toks[0], who = toks[1], date = toks[2], status = toks[3];

                if (!msg.containsKey(msgid)) {
                    msg.put(msgid, new Pair(date, new ArrayList<>()));
                }

                msg.get(msgid).getValue().add(new Pair(who, status));
            }
        } catch (FileNotFoundException ex) {
            System.out.println("File " + recipients + " not found !");
            return false;
        } catch (IOException ex) {
            System.out.println("Fatal error while reading " + recipients + ": " + ex);
            return false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * ****************************************************************
         * **************** Loads data from EmlAttachments ****************
         * ****************************************************************
         */
        HashMap<String, ArrayList<String>> attach = new HashMap<>();
        try {
            br = new BufferedReader(new FileReader(attachments));

            while ((line = br.readLine()) != null) {
                String[] toks = line.split(";");
                String msgid = toks[0], filename = toks[1];

                String ext = filename.split("\\.")[1].toLowerCase();
                if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png") && !ext.equals("gif")) {
                    if (!attach.containsKey(msgid)) {
                        attach.put(msgid, new ArrayList<>());
                    }
                    attach.get(msgid).add(ext);
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println("File " + attachments + " not found !");
            return false;
        } catch (IOException ex) {
            System.out.println("Fatal error while reading " + attachments + ": " + ex);
            return false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * ****************************************************************
         * ********************* Builds the whole graph *******************
         * ****************************************************************
         */
        EdgeMaker em = new EdgeMaker();
        Globals.mails = new LinkedList<>();

        for (String msgid : msg.keySet()) {
            String author = null;
            Stack<Pair<String, String>> dest = new Stack<>();
            HashSet<String> desttrunc = new HashSet<>();
            boolean authorFound = false;

            for (Pair<String, String> ps : msg.get(msgid).getValue()) {
                if (!authorFound && ps.getValue().equals("from")) {
                    authorFound = true;
                    author = ps.getKey();
                } else {
                    dest.push(ps);
                }

                desttrunc.add(ps.getKey());
            }

            if (author != null) {
                Globals.mails.add(new Pair(author, new LinkedList(desttrunc)));

                for (Pair<String, String> t : dest) {
                    Edge e = ret.addEdge(em.retrieveEdgeName(author, t.getKey()), author, t.getKey());
                    e.addAttribute("date", msg.get(msgid).getKey());
                    e.addAttribute("type", t.getValue());
                }
            }
        }

        // Don't forget to set the global variable accordingly !
        Globals.wholeGraph = ret;
        return true;
    }

    public static void loadCollapsedGraphFromDirectory(String directory) {
        String recipients = directory + "/EmlRecipients.csv";
        String attachments = directory + "/EmlAttachments.csv";
        BufferedReader br = null;
        String line = "";

        Globals.mainDirectory = directory;

        SingleGraph ret = new SingleGraph("Collapsed graph");
        ret.setAutoCreate(true);
        ret.setStrict(false);

        // msgid -> ("20/01/2017", [(("Isak", "from"), ("Sicheng", "to"), ("Florian", "cc")])
        HashMap<String, Pair<String, ArrayList<Pair<String, String>>>> msg = new HashMap<>();

        // "Isak" -> {"Sicheng" -> 100 ; "Florian" -> 14 ; "Antoine" -> 12}
        HashMap<String, HashMap<String, Integer>> counts = new HashMap<>();

        /**
         ******************************************************************
         ******************* Loads data from EmlRecipients ****************
         * *****************************************************************
         */
        try {
            br = new BufferedReader(new FileReader(recipients));

            while ((line = br.readLine()) != null) {
                String[] toks = line.split(";");
                String msgid = toks[0], who = toks[1], date = toks[2], status = toks[3];

                if (!msg.containsKey(msgid)) {
                    msg.put(msgid, new Pair(date, new ArrayList<>()));
                }

                msg.get(msgid).getValue().add(new Pair(who, status));
            }
        } catch (FileNotFoundException ex) {
            System.out.println("File " + recipients + " not found !");
        } catch (IOException ex) {
            System.out.println("Fatal error while reading " + recipients + ": " + ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Globals.mails = new LinkedList<>();

        for (String msgid : msg.keySet()) {
            String author = null;
            Stack<String> dest = new Stack<>();
            HashSet<String> desttrunc = new HashSet<>();
            boolean authorFound = false;

            for (Pair<String, String> ps : msg.get(msgid).getValue()) {
                if (!authorFound && ps.getValue().equals("from")) {
                    authorFound = true;
                    author = ps.getKey();
                } else {
                    dest.push(ps.getKey());
                    desttrunc.add(ps.getKey());
                }
            }

            if (author != null) {
                Globals.mails.add(new Pair(author, new LinkedList(desttrunc)));

                for (String pax : dest) {
                    
                    if (!counts.containsKey(author)) {
                        counts.put(author, new HashMap<>());
                    }

                    if (!counts.get(author).containsKey(pax)) {
                        counts.get(author).put(pax, 0);
                    }

                    counts.get(author).put(pax, counts.get(author).get(pax) + 1);
                }
            }
        }

        for (String author : counts.keySet()) {
            for (String pax : counts.get(author).keySet()) {
                Edge e = ret.addEdge(author + "->" + pax, author, pax,true);
                e.setAttribute("weight", String.valueOf(counts.get(author).get(pax)));
            }
        }
        Globals.collapsedGraph = ret;
    }

}
