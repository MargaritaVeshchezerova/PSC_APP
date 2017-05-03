/**
 * @author Antoine
 */
package org.core.proximityweight;

// NB: the MapReduce implementation lives outside of this project
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import javafx.util.Pair;
import org.core.Globals;

public class ProximityWeight {

    public static HashMap<Pair<Integer, Integer>, Float> proximityWeight = null;

    private static Integer mail2id(String mail) {
        String[] toks = mail.split("@");
        if (toks.length < 2) {
            return -1;
        } else {
            return Integer.valueOf(toks[0]);
        }
    }

    /**
     * Computes the proximity weights for a mission.
     *
     * If a proximityWeights.db file already exists in the folder, assumes it is
     * sane and loads the data from it.
     *
     * Otherwise, computes the proximity weights from the list of mails stored
     * in Globals, then saves to proximityWeight.db to speed up future queries.
     *
     * FIXME: this code is a one-shot mess, document it a bit more. But it
     * should work just fine for now.
     */
    public static boolean computeProximityWeights() {

        if (Globals.mainDirectory == null) {
            System.out.println("No missions have been loaded !");
            return false;
        }

        // If the database file exists, load the data
        File f = new File(Globals.mainDirectory + "proximityWeights.db");
        if (f.exists() && !f.isDirectory()) {
            System.out.println("Found a data file ! Now loading from hard disk...");
            try {
                //use buffering
                InputStream file = new FileInputStream(Globals.mainDirectory + "proximityWeights.db");
                ObjectInput input = new ObjectInputStream(new BufferedInputStream(file));
                try {
                    proximityWeight = (HashMap<Pair<Integer, Integer>, Float>) input.readObject();
                } finally {
                    input.close();
                }
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
                return false;
            } catch (IOException ex) {
                ex.printStackTrace();
                return false;
            }

            System.out.println("Done loading from hard disk !");
            return true;
        } else {
            System.out.println("File " + Globals.mainDirectory + "proximityWeights.db" + " not found, computing proximity weights...");

            if (Globals.mails == null) {
                System.out.println("No emails have been loaded yet !");
                return false;
            }

            HashMap<Integer, Integer> mailCounter = new HashMap<>();

            proximityWeight = new HashMap<>();

            Globals.mails.forEach((m) -> {
                Integer auth = mail2id(m.getKey());
                LinkedList<String> dest = m.getValue();
                int n = dest.size();

                if (!mailCounter.containsKey(auth)) {
                    mailCounter.put(auth, 0);
                }
                mailCounter.put(auth, mailCounter.get(auth) + 1);

                // If n == 0, I don't know what's going on
                if (n != 0) {

                    /**
                     * Add the contribution of this mail to s_auth,d and
                     * s_d,auth for each receiver d.
                     */
                    for (String d : dest) {

                        Integer intd = mail2id(d);
                        // Increment the mail counter of d
                        if (!mailCounter.containsKey(intd)) {
                            mailCounter.put(intd, 0);
                        }
                        mailCounter.put(intd, mailCounter.get(intd) + 1);

                        Pair<Integer, Integer> keyd = new Pair(auth, intd);
                        Pair<Integer, Integer> keyr = new Pair(intd, auth);

                        if (!proximityWeight.containsKey(keyd)) {
                            proximityWeight.put(keyd, new Float(0.0));
                        }

                        proximityWeight.put(keyd, proximityWeight.get(keyd) + 1 / (float) n);

                        if (!proximityWeight.containsKey(keyr)) {
                            proximityWeight.put(keyr, new Float(0.0));
                        }

                        proximityWeight.put(keyr, proximityWeight.get(keyr) + 1 / (float) n);
                    }

                    ArrayList<String> destArray = new ArrayList<String>(dest);

                    for (int i = 0; i < destArray.size(); ++i) {
                        Integer inti = mail2id(destArray.get(i));
                        for (int j = i + 1; j < destArray.size(); ++j) {
                            Integer intj = mail2id(destArray.get(i));
                            final Pair<Integer, Integer> keyd = new Pair(inti, intj), keyr = new Pair(intj, inti);

                            if (!proximityWeight.containsKey(keyd)) {
                                proximityWeight.put(keyd, new Float(0.0));
                            }

                            proximityWeight.put(keyd, proximityWeight.get(keyd) + 1 / (float) n);

                            if (!proximityWeight.containsKey(keyr)) {
                                proximityWeight.put(keyr, new Float(0.0));
                            }

                            proximityWeight.put(keyr, proximityWeight.get(keyr) + 1 / (float) n);
                        }
                    }
                }
            });

            for (Pair<Integer, Integer> key : proximityWeight.keySet()) {
                proximityWeight.put(key, proximityWeight.get(key) / (float) mailCounter.get(key.getKey()));
            }

            System.out.println("Done computing proximity weights ! Now saving to hard disk...");

            try {
                OutputStream buffer = new BufferedOutputStream(new FileOutputStream(Globals.mainDirectory + "proximityWeights.db"));
                ObjectOutput output = new ObjectOutputStream(buffer);
                try {
                    output.writeObject(proximityWeight);
                } finally {
                    output.close();
                }
            } catch (IOException ex) {
                return false;
            }

            System.out.println("Done saving to hard disk !");
            return true;
        }
    }

    /**
     * For a given user i (be careful, you need to convert it to an integer
     * first !), return
     * @param i an user
     * @return the list of (peer, proximity) ordered by proximity (first = highet proximity)
     */
    public static ArrayList<Pair<Integer, Float>> getOrderedPeers(Integer i) {
        ArrayList<Pair<Integer, Float>> ret = new ArrayList<>();

        for (Pair<Integer, Integer> p : proximityWeight.keySet()) {
            // If it is related to the user that is being considered
            if (p.getKey() == i) {
                ret.add(new Pair<Integer, Float>(p.getValue(), proximityWeight.get(p)));
            }
        }
        Collections.sort(ret, new Comparator<Pair<Integer, Float>>() {
            @Override
            public int compare(Pair<Integer, Float> lhs, Pair<Integer, Float> rhs) {
                if (lhs.getValue() > rhs.getValue()) {
                    return -1;
                }
                if (lhs.getValue() > rhs.getValue()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        return ret;
    }
}
