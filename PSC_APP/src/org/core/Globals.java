/**
 * @author Antoine
 *
 * Stores program-wide constants.
 */
package org.core;

import java.util.LinkedList;
import javafx.util.Pair;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.graph.implementations.SingleGraph;

public class Globals {

    // The graph where 1 edge from A to B = 1 mail from A to B
    public static MultiGraph wholeGraph = null;

    // A collapsed version of wholeGraph in which edges are weighted
    public static SingleGraph collapsedGraph = null;

    public static String mainDirectory = null;

    /**
     * Comprehensive list of mails, stored in memory for performance reasons in
     * subsequent analysis.
     * Format: 
     * 
     * [("Isak", ["Sicheng", "Florian"]), ...]
     */
    public static LinkedList<Pair<String, LinkedList<String>>> mails = null;
}
