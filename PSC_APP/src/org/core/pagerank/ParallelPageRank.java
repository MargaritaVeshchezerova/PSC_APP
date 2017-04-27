/**
 * @author Antoine
 *
 * Parallel PageRank with double-barrier synchronization to prevent race
 * conditions.
 */
package org.core.pagerank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.core.Globals;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

public class ParallelPageRank {

    // Has the algorithm converged yet ?
    static boolean converged = false;

    static final int Nthreads = Runtime.getRuntime().availableProcessors();
    static final int Nnodes = Globals.collapsedGraph.getNodeCount();

    // This is where the computed pageranks are stored
    static final ConcurrentHashMap<String, Double> pagerank = new ConcurrentHashMap<>(Nnodes + 1, 1);

    // We store in memory the previously computed PageRank
    static final ConcurrentHashMap<String, Double> previousPagerank = new ConcurrentHashMap<>(Nnodes + 1, 1);

    // How many interations have been done ?
    static Integer counter = new Integer(0);

    static double d = 0.1, threshold = 0.1;

    // Constant
    static final double b = 0.25;
    

    public ParallelPageRank(double damping, double thr) {
        d = damping;
        threshold = thr;

        for (Node n : Globals.collapsedGraph.getNodeSet()) {
            pagerank.put(n.getId(), 1 / (double) Nnodes);
        }
    }

    private static class Worker implements Runnable {

        CyclicBarrier computationBarrier, copyBarrier;
        final HashMap<String, Double> tmpPagerank = new HashMap<>(Nthreads / Nnodes + 2, 1);

        final HashMap<String, Double> prevtmpPagerank = new HashMap<>(Nthreads / Nnodes + 2, 1);

        Worker(CyclicBarrier compBarrier, CyclicBarrier copBarrier, HashSet<String> nodePool) {
            this.computationBarrier = compBarrier;
            this.copyBarrier = copBarrier;

            for (String s : nodePool) {
                tmpPagerank.put(s, 1 / (double) Nnodes);
                prevtmpPagerank.put(s, 1 / (double) Nnodes);
            }
        }

        @Override
        public void run() {
            while (!converged) {

                /**
                 * Computation time ! Iterate over our assigned nodes and
                 * compute their new pagerank based on what we computed at the
                 * previous iteration (which is stored in the global variable
                 * `pagerank`.
                 *
                 * Locally store the newly computed pageranks in tmpPagerank for
                 * future update.
                 */
                for (String s : tmpPagerank.keySet()) {
                    prevtmpPagerank.put(s, pagerank.get(s));

                    double S = (1.0 - d) / (double) Nnodes;
                    Node p = Globals.collapsedGraph.getNode(s);

                    // Entering
                    double tmps1 = 0.0;
                    double inweight = 0;
                    boolean open = false;

                    for (Edge e : p.getEnteringEdgeSet()) {
                        Node q = e.getSourceNode();

                        // Avoid self referencing edges !
                        if (!q.equals(p)) {
                            open = true;
                            double w = Double.valueOf(e.getAttribute("weight"));
                            inweight += w;

                            tmps1 += pagerank.get(q.getId()) * w / ((double) q.getOutDegree());
                        }
                    }

                    if (open) {
                        tmps1 *= (d * (1 - b) / inweight);
                    }

                    // Leaving
                    double tmps2 = 0.0;
                    double outweight = 0.0;
                    boolean exit = false;

                    for (Edge e : p.getLeavingEdgeSet()) {
                        Node q = e.getTargetNode();

                        if (!q.equals(p)) {
                            exit = true;

                            double w = Double.valueOf(e.getAttribute("weight").toString());
                            outweight += w;

                            tmps2 += pagerank.get(q.getId()) * w / (double) q.getInDegree();
                        }
                    }

                    if (exit) {
                        tmps2 *= (d * b / outweight);
                    }

                    S += tmps1 + tmps2;
                    
                    //System.out.println(S);

                    prevtmpPagerank.put(s, tmpPagerank.get(s));
                    tmpPagerank.put(s, S);
                }

                try {
                    computationBarrier.await();
                } catch (InterruptedException | BrokenBarrierException ex) {
                    return;
                }

                /**
                 * Update time ! Now that everybody is done computing, copy the
                 * newly computed Pageranks in the global pagerank map.
                 */

                // CHECKITOUT: hopefully it does what is intended.
                // CHECKED: it does.
                previousPagerank.putAll(prevtmpPagerank);
                pagerank.putAll(tmpPagerank);

                try {
                    copyBarrier.await();
                } catch (InterruptedException | BrokenBarrierException ex) {
                    return;
                }
            }
        }

    }

    // Splits a HashSet into `count` equilibrated HashSets
    public static ArrayList<HashSet<String>> split(HashSet<String> original, int count) {
        ArrayList<HashSet<String>> l = new ArrayList<>(count);
        Iterator<String> it = original.iterator();

        int each = original.size() / count;
        int rmnd = original.size() % count;

        // Loop for each new set.
        for (int i = 0; i < count; i++) {
            int a = (rmnd == 0 ? 0 : 1);
            HashSet<String> s = new HashSet<String>(original.size() / count + 2);
            l.add(s);

            for (int j = 0; j < (each + a) && it.hasNext(); j++) {
                s.add(it.next());
            }

            rmnd -= 1;
        }
        return l;
    }

    public static void doPageRank() {
        HashSet<String> hsh = new HashSet<>();
        Thread[] t = new Thread[Nthreads];

        for (Node n : Globals.collapsedGraph.getNodeSet()) {
            hsh.add(n.getId());
            pagerank.put(n.getId(), 1 / (double) Nnodes);
        }

        ArrayList<HashSet<String>> partition = split(hsh, Nthreads);

        CyclicBarrier computationBarrier = new CyclicBarrier(Nthreads, new Runnable() {
            @Override
            public void run() {
            }
        });

        CyclicBarrier copyBarrier = new CyclicBarrier(Nthreads, new Runnable() {
            @Override
            public void run() {
                counter += 1;

                if (counter % 10 == 0) {
                    double err = 0.0;

                    for(String s : previousPagerank.keySet()) {
                        err += Math.abs(previousPagerank.get(s) - pagerank.get(s));
                    }
                    
                    System.out.println("Error value after " + counter + " iterations: " + err);
                    
                    if(err < threshold) {
                        converged = true;
                        System.out.println("Pagerank converged, terminating processes...");
                    }
                }
            }
        });


        // Creating threads, assigning each their own working set
        for (int i = 0; i < Nthreads; ++i) {
            t[i] = new Thread(new Worker(computationBarrier, copyBarrier, partition.get(i)));
        }

        // Starting the parallel computations
        for (int i = 0; i < Nthreads; ++i) {
            t[i].start();
        }
        
        // Waiting for the processes to end
        for (int i = 0; i < Nthreads; ++i) {
            try {
                t[i].join();
            } catch (InterruptedException ex) {
                Logger.getLogger(ParallelPageRank.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
