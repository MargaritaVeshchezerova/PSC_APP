/**
 * @author Antoine
 *
 * Analysis that do not require much insight go here.
 */

package org.core;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import static org.graphstream.algorithm.Toolkit.degreeDistribution;
import org.graphstream.graph.Graph;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class GenericAnalysis {
    public static void degreeDistributionFromGivenGraph(Graph mg) {
        int[] deg = degreeDistribution(mg);
        SimpleRegression sr = new SimpleRegression(true);
        XYSeriesCollection result = new XYSeriesCollection();
        XYSeries series = new XYSeries("Users");
        
        for (int i = 0; i < deg.length; ++i) {
            series.add(i, deg[i]);
            if(i >= 1 && deg[i] >= 1 && i <= 100){
                sr.addData(Math.log10(i), Math.log10(deg[i]));
            }
        }
        sr.regress();
        double slope = sr.getSlope();
        double intercept = sr.getIntercept();
        result.addSeries(series);
        
        XYItemRenderer scatterRenderer = new XYLineAndShapeRenderer(false, true);
        scatterRenderer.setSeriesPaint(0, new Color(0, 128, 0));
        scatterRenderer.setSeriesShape(0, new Ellipse2D.Double(-1, -1, 2, 2));
        
        LogAxis xAxis = new LogAxis("Degree");
        LogAxis yAxis = new LogAxis("Number of users");
        xAxis.setRange(1, Math.pow(10, 5));
        yAxis.setRange(1, Math.pow(10, 5));

        XYPlot plot = new XYPlot();
        plot.setDataset(0, result);
        plot.setRenderer(0, scatterRenderer);
        plot.setDomainAxis(0, xAxis);
        plot.setRangeAxis(0, yAxis);
        
        XYSeriesCollection linereg = new XYSeriesCollection();
        XYSeries clq = new XYSeries("Regression (alpha = " + new DecimalFormat("#.###").format(slope) + ")");
        clq.add(1, Math.pow(10,intercept));
        clq.add(Math.pow(10, 3), Math.pow(10,intercept) * Math.pow(Math.pow(10,3),slope));
        linereg.addSeries(clq);
        XYItemRenderer renderer2 = new XYLineAndShapeRenderer(true, false);
        renderer2.setSeriesStroke(0, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{6.0f, 6.0f}, 0.0f));
        renderer2.setSeriesPaint(0, Color.RED);
        plot.setDataset(1, linereg);
        plot.setRenderer(1, renderer2);
        
        JFreeChart chart = new JFreeChart("Degree analysis", JFreeChart.DEFAULT_TITLE_FONT, plot, true);

        // FIXME: delegate display to caller
        ChartFrame frame = new ChartFrame("Degree analysis", chart);
        frame.pack();
        frame.setVisible(true);
    }
}
