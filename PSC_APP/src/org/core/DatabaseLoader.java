/*
 * @author: Antoine
 * 
 * Adds loading capabilities.
 */
package org.core;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import org.graphstream.graph.implementations.MultiGraph;

public class DatabaseLoader {

    Connection c;

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

    public DatabaseLoader(String file) {
        File f = new File(file);

        if (!f.exists()) {
            System.out.println("File " + file + " not found. Exiting...");
            System.exit(1);
        }

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:" + file);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Successfully connected to database " + file + " !");
    }

    /*
     * Loads the whole graph from the database and stores it in Globals.wholeGraph
     */
    public void loadWholeGraphFromDB() {
        Statement stmt;
        MultiGraph ret = new MultiGraph("Whole graph");

        ret.setAutoCreate(true);
        ret.setStrict(false);

        EdgeMaker em = new EdgeMaker();

        try {
            int cnt = 0;

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT messages.authorEmailAddress AS auth, GROUP_CONCAT(recipients.emailAddress) AS paxs, recipients.type FROM recipients,messages WHERE messages.id = recipients.messageId GROUP BY recipients.messageId");

            while (rs.next()) {
                String auth = rs.getString("auth");
                //ret.addNode(auth);
                for (String pax : rs.getString("paxs").split(",")) {
                    //ret.addNode(pax);
                    ret.addEdge(em.retrieveEdgeName(auth, pax), auth, pax);
                }
            }

            Globals.wholeGraph = ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
