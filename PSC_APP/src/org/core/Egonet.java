/**
 * @author Antoine
 */
package org.core;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Egonet {

    public static SingleGraph loadEgonetFromWholeGraph(String egoUser) {
        Node n = Globals.wholeGraph.getNode(egoUser);

        if (n == null) {
            return null;
        }

        SingleGraph egonet = new SingleGraph(egoUser + "-egonet");
        egonet.setAutoCreate(true);
        egonet.setStrict(false);

        egonet.addAttribute("ui.stylesheet", "node {text-size: 11px;} node#\"" + egoUser + "\" {  fill-color: red; text-color: red; text-style: bold; text-size: 14px;} edge {fill-color: rgba(175,175,175,160);}");
        HashSet<String> nodes = new HashSet<>();

        // Adds all the nodes that are adjacent to the user and the subsequents edges
        for (Edge e : n.getEdgeSet()) {
            Node t = e.getTargetNode(), s = e.getSourceNode();
            String tid = t.getId(), sid = s.getId();
            nodes.add(tid);
            nodes.add(sid);
            
            Edge ee = egonet.addEdge(sid + "-" + e.getId(), sid, tid);
            
            if(ee != null) {
                ee.addAttribute("ui.style", "fill-color: rgba(0,0,0,128); size: 2px;");
            }
            egonet.getNode(tid).setAttribute("ui.label", tid);
            egonet.getNode(sid).setAttribute("ui.label", sid);
        }

        egonet.addAttribute("ui.antialias");
        //egonet.getNode(egoUser).setAttribute("ui.color", "red");
        nodes.remove(n.getId());

        ArrayList<String> lst = new ArrayList<>(nodes);

        // Then add all the edges that connect two nodes in the user's neighbourhood
        for (int i = 0; i < lst.size(); ++i) {
            // Strict egonet (no self referencing edes)
            for (int j = i + 1; j < lst.size(); ++j) {
                if (Globals.wholeGraph.getNode(lst.get(i)).hasEdgeBetween(lst.get(j))) {
                    egonet.addEdge(lst.get(i) + "-" + lst.get(j), lst.get(i), lst.get(j));
                }
            }
        }

        return egonet;
    }

    private static int innerEdges(HashMap<String, HashSet<String>> neighbourhood, String k) {
        HashSet<String> neighs = neighbourhood.get(k);
        ArrayList<String> aneighs = new ArrayList<>(neighs);

        int cnt = 0;
        for (int i = 0; i < aneighs.size(); ++i) {
            for (int j = i + 1; j < aneighs.size(); ++j) {
                if (neighbourhood.get(aneighs.get(i)).contains(aneighs.get(j))) {
                    cnt += 1;
                }
            }
        }

        return neighs.size() + cnt;
    }

    /*
     * A Java version of my stylish Python code.
     * Does an EDPL regression based on
     * OddBall: Spotting Anomalies in Weighted Graphs, Akoglu et al., Carnegie Mellon
     */
    public static ChartFrame edplAnalysisFromWholeGraph() {
        HashMap<String, HashSet<String>> neighbourhood = new HashMap<>();

        for (Edge e : Globals.wholeGraph.getEdgeSet()) {
            String s = e.getSourceNode().getId();
            String t = e.getTargetNode().getId();

            if (!neighbourhood.containsKey(s)) {
                neighbourhood.put(s, new HashSet<>());
            }
            if (!neighbourhood.containsKey(t)) {
                neighbourhood.put(t, new HashSet<>());
            }

            neighbourhood.get(s).add(t);
            neighbourhood.get(t).add(s);
        }

        ArrayList<Integer> N = new ArrayList<>();
        ArrayList<Integer> E = new ArrayList<>();

        for (String k : neighbourhood.keySet()) {
            neighbourhood.get(k).remove(k);
            N.add(neighbourhood.get(k).size());
            int e = innerEdges(neighbourhood, k);
            E.add(e == 0 ? 1 : e);
        }

        XYSeriesCollection result = new XYSeriesCollection();
        XYSeries series = new XYSeries("Users");

        for (int i = 0; i < N.size(); ++i) {
            series.add(N.get(i), E.get(i));
        }

        result.addSeries(series);

        XYPlot plot = new XYPlot();

        XYItemRenderer scatterRenderer = new XYLineAndShapeRenderer(false, true);
        LogAxis xAxis = new LogAxis("|N|");
        LogAxis yAxis = new LogAxis("|E|");

        scatterRenderer.setSeriesPaint(0, new Color(0, 128, 0));
        scatterRenderer.setSeriesShape(0, new Ellipse2D.Double(-1, -1, 2, 2));

        xAxis.setRange(1, Math.pow(10, 4));

        plot.setDataset(0, result);
        plot.setRenderer(0, scatterRenderer);
        plot.setDomainAxis(0, xAxis);
        plot.setRangeAxis(0, yAxis);

        plot.mapDatasetToDomainAxis(0, 0);
        plot.mapDatasetToRangeAxis(0, 0);

        XYSeriesCollection lineclique = new XYSeriesCollection();
        XYSeries clq = new XYSeries("Clique pure");
        clq.add(1, 1);
        clq.add(Math.pow(10, 5), Math.pow(10, 5) * (Math.pow(10, 5) + 1) / 2);
        lineclique.addSeries(clq);
        XYItemRenderer renderer2 = new XYLineAndShapeRenderer(true, false);
        renderer2.setSeriesStroke(0, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{6.0f, 6.0f}, 0.0f));
        renderer2.setSeriesPaint(0, Color.RED);
        plot.setDataset(1, lineclique);
        plot.setRenderer(1, renderer2);

        XYSeriesCollection linestar = new XYSeriesCollection();
        XYSeries str = new XYSeries("Étoile pure");
        str.add(1, 1);
        str.add(Math.pow(10, 5), Math.pow(10, 5));
        linestar.addSeries(str);
        XYItemRenderer renderer3 = new XYLineAndShapeRenderer(true, false);
        renderer3.setSeriesStroke(0, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{6.0f, 6.0f}, 0.0f));
        renderer3.setSeriesPaint(0, Color.BLUE);
        plot.setDataset(2, linestar);
        plot.setRenderer(2, renderer3);

        JFreeChart chart = new JFreeChart("EDPL Analysis", JFreeChart.DEFAULT_TITLE_FONT, plot, true);

        // FIXME: delegate display to caller
        ChartFrame frame = new ChartFrame("First", chart);
        frame.pack();
        frame.setVisible(true);

        return frame;

    }
    
    // Same function, but operating on a collapsed graph
    public static ChartFrame edplAnalysisFromCollapsedGraph() {
        HashMap<String, HashSet<String>> neighbourhood = new HashMap<>();

        for (Edge e : Globals.collapsedGraph.getEdgeSet()) {
            String s = e.getSourceNode().getId();
            String t = e.getTargetNode().getId();

            if (!neighbourhood.containsKey(s)) {
                neighbourhood.put(s, new HashSet<>());
            }
            if (!neighbourhood.containsKey(t)) {
                neighbourhood.put(t, new HashSet<>());
            }

            neighbourhood.get(s).add(t);
            neighbourhood.get(t).add(s);
        }
        
        ArrayList<Integer> N = new ArrayList<>();
        ArrayList<Integer> E = new ArrayList<>();

        for (String k : neighbourhood.keySet()) {
            neighbourhood.get(k).remove(k);
            N.add(neighbourhood.get(k).size());
            int e = innerEdges(neighbourhood, k);
            E.add(e == 0 ? 1 : e);
        }

        XYSeriesCollection result = new XYSeriesCollection();
        XYSeries series = new XYSeries("Users");

        for (int i = 0; i < N.size(); ++i) {
            series.add(N.get(i), E.get(i));
        }

        result.addSeries(series);

        XYPlot plot = new XYPlot();

        XYItemRenderer scatterRenderer = new XYLineAndShapeRenderer(false, true);
        LogAxis xAxis = new LogAxis("|N|");
        LogAxis yAxis = new LogAxis("|E|");

        scatterRenderer.setSeriesPaint(0, new Color(0, 128, 0));
        scatterRenderer.setSeriesShape(0, new Ellipse2D.Double(-1, -1, 2, 2));

        xAxis.setRange(1, Math.pow(10, 4));

        plot.setDataset(0, result);
        plot.setRenderer(0, scatterRenderer);
        plot.setDomainAxis(0, xAxis);
        plot.setRangeAxis(0, yAxis);

        plot.mapDatasetToDomainAxis(0, 0);
        plot.mapDatasetToRangeAxis(0, 0);

        XYSeriesCollection lineclique = new XYSeriesCollection();
        XYSeries clq = new XYSeries("Clique pure");
        clq.add(1, 1);
        clq.add(Math.pow(10, 5), Math.pow(10, 5) * (Math.pow(10, 5) + 1) / 2);
        lineclique.addSeries(clq);
        XYItemRenderer renderer2 = new XYLineAndShapeRenderer(true, false);
        renderer2.setSeriesStroke(0, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{6.0f, 6.0f}, 0.0f));
        renderer2.setSeriesPaint(0, Color.RED);
        plot.setDataset(1, lineclique);
        plot.setRenderer(1, renderer2);

        XYSeriesCollection linestar = new XYSeriesCollection();
        XYSeries str = new XYSeries("Étoile pure");
        str.add(1, 1);
        str.add(Math.pow(10, 5), Math.pow(10, 5));
        linestar.addSeries(str);
        XYItemRenderer renderer3 = new XYLineAndShapeRenderer(true, false);
        renderer3.setSeriesStroke(0, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{6.0f, 6.0f}, 0.0f));
        renderer3.setSeriesPaint(0, Color.BLUE);
        plot.setDataset(2, linestar);
        plot.setRenderer(2, renderer3);

        JFreeChart chart = new JFreeChart("EDPL Analysis", JFreeChart.DEFAULT_TITLE_FONT, plot, true);

        // FIXME: delegate display to caller
        ChartFrame frame = new ChartFrame("First", chart);
        frame.pack();
        frame.setVisible(true);

        return frame;
    }
}
