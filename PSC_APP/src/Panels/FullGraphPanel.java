/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Panels;

/**
 *
 * @author Margarita
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JPanel;
import org.core.Globals;
import org.graphstream.graph.Graph;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;

public class FullGraphPanel extends JPanel
{
    public View full_graph_vis;
    
    public FullGraphPanel()
    {
        initialisation();
    }
//    public JPanel jPanel1;
    
    public void initialisation()
    {
        Viewer vue = new Viewer(Globals.collapsedGraph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        vue.enableAutoLayout();
        this.full_graph_vis = vue.addDefaultView(false);
        validate();
    }    
}
