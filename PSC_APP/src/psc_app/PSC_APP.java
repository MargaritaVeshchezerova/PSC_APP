/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package psc_app;

import core.Globals.All_Frames_and_Panels;
import org.core.Globals;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.RandomGenerator;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

/**
 *
 * @author Margarita
 */
public class PSC_APP {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

    Globals.collapsedGraph = new SingleGraph("Random");
    Generator gen = new RandomGenerator(2);
    gen.addSink(Globals.collapsedGraph);
    gen.begin();
    for(int i=0; i<100; i++)
        gen.nextEvents();
    gen.end();
    
        
        new All_Frames_and_Panels();
        All_Frames_and_Panels.WF.setVisible(true);

        }

    }


