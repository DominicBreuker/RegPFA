/*
 * Copyright (c) 2014, Dominic Breuker
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *    
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *    
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package Framework.Models.PetriNet;

import java.util.*;

import org.apache.commons.io.FileUtils;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import Framework.Models.Graph.Graph;
import Framework.Models.Graph.Node;
import Framework.Models.Log.NonRedundantLog;
import Framework.Models.TransitionSystem.TransitionSystem;
import Framework.Utils.Utilities;

import java.io.File;

public class PetriNet {

    // all transitions labeled with this symbol are "invisible transitions"
    // when sampling from a petri net, you can choose if you want to include these labels in the sample
    public final String invisibleSymbol = "tau";
    // internal name of this petri net
    public String name;
    // lists containing places and transitions associated with this petri net
    public ArrayList<Place> places = new ArrayList<Place>();
    public ArrayList<Transition> transitions = new ArrayList<Transition>();
    // mapping for readable labels to char labels
    HashMap<String, Character> stringToCharLabelMapping = new HashMap<String, Character>();
    Character firstChar = 'a'; // first character to use if mapping is automatically created

    // ------------------------ //
    // ----- constructors ----- //
    // ------------------------ //

    public PetriNet() {
        this.name = "NamelessPetriNet";
    }

    // construct empty petri net with name as specified
    public PetriNet(String name) {
        this.name = name;
    }

    // -------------------------------- //
    // ----- construction methods ----- //
    // -------------------------------- //

    // ----- methods for places ----- //

    // adds a place (with inital marking zero)
    public Place addPlace(String name) {
        return addPlace(name, 0);
    }

    // adds a place and specifies its initial marking
    public Place addPlace(String name, int initialMarking) {
        // ensure that places do have unique names
        Place p = getPlaceByName(name);
        if (p == null) {
            p = new Place(name, initialMarking);
            this.places.add(p);
            return p;
        } else {
            System.out.println("Duplicate place added! " + name);
            return p;
        }
    }

    // removes a given place
    public Boolean removePlace(Place p) {
        int i = this.places.indexOf(p);
        if (i == -1) return false;
        this.places.remove(i);
        return true;
    }

    // ----- methods for transitions ----- //

    // adds a transition (not connected to any places)
    public Transition addTransition(String name, String label) {
        return addTransition(name, label, new ArrayList<Place>(), new ArrayList<Place>());
    }

    // adds a transition with given input and output places
    public Transition addTransition(String name, String label, ArrayList<Place> inputPlaces, ArrayList<Place> outputPlaces) {
        // ensure that transition names are unique
        Transition t = getTransitionByName(name);
        if (t == null) {
            // check if in- and output places exist in this petri net
            for (Place p : inputPlaces) {
                if (places.contains(p) == false) {
                    System.out.println("ERROR: input place does not exist in petri net: " + p.name);
                    return null;
                }
            }
            for (Place p : outputPlaces) {
                if (places.contains(p) == false) {
                    System.out.println("ERROR: output place does not exist in petri net: " + p.name);
                    return null;
                }
            }

            t = new Transition(name, label, this, inputPlaces, outputPlaces);
            this.transitions.add(t);
            return t;
        } else {
            System.out.println("Duplicate transition added! " + name);
            return t;
        }
    }

    // removes a given transition
    public Boolean removeTransition(Transition t) {
        int i = this.transitions.indexOf(t);
        if (i == -1) return false;
        for (Place p : t.inputPlaces) p.outgoingTransitions.remove(t);
        for (Place p : t.outputPlaces) p.incomingTransitions.remove(t);
        this.transitions.remove(i);
        return true;
    }


    // -------------------------- //
    // ----- getter methods ----- //
    // -------------------------- //

    // finds a place by its internal name
    public Place getPlaceByName(String name) {
        for (Place p : places) {
            if (p.name.equals(name)) return p;
        }
        return null;
    }

    // finds a transition by its internal name
    public Transition getTransitionByName(String name) {
        for (Transition t : transitions) {
            if (t.name.equals(name)) return t;
        }
        return null;
    }

    // returns the char label given the string label
    // or a new char label for this string if none existed yet
    public Character getCharLabel(String stringLabel) {
        Character c = this.stringToCharLabelMapping.get(stringLabel); // return label according to mapping
        if (c == null) {
            // add one, but make sure it has not been used yet
            if (stringLabel.length() == 1 && this.stringToCharLabelMapping.values().contains(stringLabel.charAt(0)) == false) {
                this.stringToCharLabelMapping.put(stringLabel, stringLabel.charAt(0));
                c = stringLabel.charAt(0);
            } else {
                while (this.stringToCharLabelMapping.get(this.firstChar.toString()) != null)
                    this.firstChar++;
                this.stringToCharLabelMapping.put(stringLabel, this.firstChar);
                c = this.firstChar;
                this.firstChar++;
            }

        }
        return c;
    }


    // ----------------------------- //
    // ----- execution methods ----- //
    // ----------------------------- //

    // sets current marking to initial marking
    public void reset() {
        for (Place p : places) p.currentMarking = p.initialMarking;
    }

    // returns current marking
    public int[] getInitialMarking() {
        int[] result = new int[places.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = places.get(i).initialMarking;
        }
        return result;
    }

    // returns current marking
    public int[] getCurrentMarking() {
        int[] result = new int[places.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = places.get(i).currentMarking;
        }
        return result;
    }

    // sets current marking to arbitrary value
    public Boolean setCurrentMarking(int[] marking) {
        if (marking.length != places.size()) return false;
        for (int i = 0; i < marking.length; i++) {
            places.get(i).currentMarking = marking[i];
        }
        return true;
    }

    // returns true if t is enabled
    // interprets the petri net as k-bounded, i.e., no transition is enabled if firing it produces a place with more than k tokens
    public Boolean isEnabledK(Transition t, int k) {
        Boolean enabled = true;
        for (Place p : t.inputPlaces) {
            if (p.currentMarking < 1) enabled = false;
        }
        // enforce k-bounded petri net --> do not fire if the result is an illegitimate state
        for (Place p : t.outputPlaces) {
            if (p.currentMarking >= k) enabled = false;
        }
        return enabled;
    }

    // returns true if t is enabled
    // enforces that the petri net is safe
    public Boolean isEnabled(Transition t) {
        return isEnabledK(t, 1);
    }

    // returns all enabled transitions
    public ArrayList<Transition> getEnabledTransitions() {
        return this.getEnabledTransitionsK(1);
    }

    // returns all enabled transitions under the assumption of k-boundedness
    public ArrayList<Transition> getEnabledTransitionsK(int k) {
        ArrayList<Transition> result = new ArrayList<Transition>();
        for (Transition t : transitions) {
            if (isEnabledK(t, k) == true) result.add(t);
        }
        return result;
    }

    // fires transition t if possible
    public Boolean fire(Transition t) {
        // check if t is enabled
        if (isEnabledK(t, Integer.MAX_VALUE) == false) return false;

        // update marking
        for (Place p : t.inputPlaces) p.currentMarking--;
        for (Place p : t.outputPlaces) p.currentMarking++;

        return true;
    }

    // fires a random transition chosen uniformly from all enabled transitions
    // returns the fired transition
    public Transition fireRandom() {
        ArrayList<Transition> enabledTransitions = getEnabledTransitionsK(Integer.MAX_VALUE);
        int n = enabledTransitions.size();
        if (n == 0) return null;

        double r = Math.random() * n;
        int tIndex = (int) Math.floor(r);
        Transition t = enabledTransitions.get(tIndex);

        if (fire(t) == false)
            System.out.println("THIS SHOULD HAVE NEVER HAPPENED!!! ERROR WHILE SAMPLING!!!");

        return t;
    }

    // returns a sequence of labels sampled from the petri net
    // show invisible determines if the sample contains invisible transition labels
    public ArrayList<String> sample(int maxSize, boolean showInvisible) {
        ArrayList<String> sample = new ArrayList<String>();
        this.reset();
        for (int i = 0; i < maxSize; i++) {
            Transition t = fireRandom();
            if (t == null) return sample;
            if ((showInvisible == true) || t.label.equals(invisibleSymbol) == false) sample.add(t.label);

        }
        return sample;
    }

    // returns a sequence of labels sampled from the petri net
    public ArrayList<String> sample(int maxSize) {
        return sample(maxSize, false);
    }

    // returns a complete sample, multiple cases each with given maximum number of labels
    public ArrayList<ArrayList<String>> sample(int cases, int maxSize) {
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        for (int i = 1; i <= cases; i++) {
            result.add(this.sample(maxSize));
        }

        return result;
    }


    // --------------------------------------- //
    // -----------Scoring Methods ------------ //
    // --------------------------------------- //

    // returns cross entropy of this petri net and the given log
    private double getCrossEntropy(NonRedundantLog log) {
        double crossEntropy = 0.0;

        int numberOfCases = log.getNumberOfCases();

        for (int c = 0; c < log.getNumberOfUniqueCases(); c++) {

            // determine multiplicity of this case
            double caseMultiplicity = (double) log.getCaseMultiplicity(c);

            // translate case from numerical to symbolic
            ArrayList<String> symbolicCase = new ArrayList<String>();
            int N = log.getNumericalLog().get(c).size(); // number of symbols in this case
            for (int i = 0; i < N; i++) {
                int numericalEvent = log.getNumericalLogEntry(c, i);
                String symbolicEvent = log.getNumberToSymbolMapping().get(numericalEvent);
                symbolicCase.add(symbolicEvent);
            }
            // calculate case probability
            double logCaseProbability = this.scoreSample(symbolicCase);

            crossEntropy -= (caseMultiplicity / (double) (numberOfCases)) * logCaseProbability;
        }

        return crossEntropy;
    }

    // returns cross entropy of this petri net and training set of this log
    public double getCrossEntropyTrain(NonRedundantLog log) {
        return this.getCrossEntropy(log);
    }

    // returns cross entropy of this petri net and test set of this log
    public double getCrossEntropyTest(NonRedundantLog log) {
        return this.getCrossEntropy(log.getValidationLog());
    }

    // returns the log_2 probability that the given sample sequences are sampled from this petri net
    public ArrayList<Double> scoreSamples(ArrayList<ArrayList<String>> samples) {
        ArrayList<Double> scores = new ArrayList<Double>();

        for (ArrayList<String> sample : samples) {
            scores.add(this.scoreSample(sample));
        }

        return scores;
    }

    // returns the log_2 probability that the given sample sequence is sampled from this petri net
    public double scoreSample(ArrayList<String> sample) {
        double logProb = Double.NEGATIVE_INFINITY;
        int[] backupMarking = this.getCurrentMarking(); // save marking to restore it later
        HashMap<int[], Double> stateDistribution = new HashMap<int[], Double>(); // stores the current distribution over states (log_2 probabilities)

        this.reset(); // sets current marking to inital marking;
        stateDistribution.put(this.getInitialMarking(), 0.0); // current state is inital state with probability 1 (log2(1) = 0)

        for (String s : sample) {
            // iterate samples to see if and with which probability they could originate from this petri net

            HashMap<int[], Double> newStateDistribution = new HashMap<int[], Double>(); // stores the current distribution over states (log_2 probabilities)

            for (int[] currentState : stateDistribution.keySet()) {
                // iterate the states we may be in
                double currentStateProb = stateDistribution.get(currentState);

                this.setCurrentMarking(currentState); // set state of this petri net to the one we look at
                ArrayList<Transition> trans = this.getEnabledTransitionsK(Integer.MAX_VALUE); // get all enabled transitions
                double numberOfEnabledTrans = (double) trans.size();

                if (trans.size() == 0) {
                    // if not transition is available, we are at a dead end
                } else {
                    double transProb = Math.log(1.0 / numberOfEnabledTrans); // probability of each transition to be fired (uniform over all enabled transitions)
                    ArrayList<Transition> transWithCorrectLabel = filterByLabel(trans, s); // take from all enabled transitions those producing the correct label

                    if (transWithCorrectLabel.size() == 0) {
                        // if there is not current transition available, we are at a dead end too
                    } else {
                        for (Transition t : transWithCorrectLabel) {
                            // iterate all transitions that could fire to produce the next symbol of the sample
                            this.fire(t);
                            newStateDistribution.put(this.getCurrentMarking(), currentStateProb + transProb);
                            this.setCurrentMarking(currentState);
                        }
                    }
                }
            }
            // at this point, we determined all new states we might get into
            if (newStateDistribution.size() == 0) {
                break; // the sequence cannot be generated from this petri net. Probability = 0 (log2(0) = -inf)
            }

            stateDistribution = newStateDistribution; // replace old distribution with new one
        }

        // at this point, we have a list of states the petri net might be in after having observed the sequence
        // we need to check which of these states is final, i.e., which one has no enabled transitions
        for (int[] currentState : stateDistribution.keySet()) {
            this.setCurrentMarking(currentState);
            if (this.getEnabledTransitionsK(Integer.MAX_VALUE).size() == 0) {
                // for all deadlock states, add up their probabilities
                logProb = Utilities.addLogSpace(logProb, stateDistribution.get(currentState));
            }
        }

        this.setCurrentMarking(backupMarking); // restore marking to whatever it was before
        return logProb / Math.log(2); // return logarithm of probability with base 2
    }

    // returns an array list of transitions with all transitions from the set it is provided with that have a specified label
    private ArrayList<Transition> filterByLabel(ArrayList<Transition> transitions, String label) {
        ArrayList<Transition> result = new ArrayList<Transition>();

        for (Transition t : transitions) {
            if (t.label.equals(label))
                result.add(t);
        }

        return result;
    }

    // -------------------------------- //
    // ----- model transformation ----- //
    // -------------------------------- //


    // returns a reachability graph of this petri net
    // treats the petri net as if it would be k-bounded
    public Automaton getReachabilityGraph(int bound) {

        // maps markings of the petri net to states of the fsm
        HashMap<String, State> stateMapping = new HashMap<String, State>();

        Automaton fsm = new Automaton();
        fsm.setDeterministic(false); // automaton may be nondeterministic, so we set false to be on the safe side

        int[] saveMarking = this.getCurrentMarking(); // save petri net's current marking and restore later
        this.reset(); // set current marking to initial marking
        stateMapping.put(Arrays.toString(this.getCurrentMarking()), fsm.getInitialState()); // map initial state

        Stack<int[]> markingsToDo = new Stack<int[]>(); // stores all markings that still need processing
        markingsToDo.push(this.getCurrentMarking()); // current (initial) marking is the first to work on

        int[] currentMarking;
        int[] nextMarking;
        Runtime r = Runtime.getRuntime();
        while ((markingsToDo.isEmpty()) == false) {
            if ((float) r.totalMemory() >= (((float) r.maxMemory()) * 0.8f)) {
                //System.out.println("approaching overflow! mem: " + r.totalMemory() + " - max: " + r.maxMemory());
                r.gc();
                r.freeMemory();
                r.gc();
                //System.out.println("now: " + r.totalMemory() + " - max: " + r.maxMemory());
                return null;
            }

            currentMarking = markingsToDo.pop();
            //System.out.println("processing: " + Arrays.toString(currentMarking));
            //System.out.println("#todo: " + markingsToDo.size() + " - mapped: " + stateMapping.size());
            State currentState = stateMapping.get(Arrays.toString(currentMarking)); // must exist at this point
            this.setCurrentMarking(currentMarking); // set marking of the petri net to the one currently processes

            ArrayList<Transition> enabledTransitions = this.getEnabledTransitionsK(bound); // get all transitions enabled in this state
            if (enabledTransitions.size() == 0) {
                currentState.setAccept(true);
            } else {
                currentState.setAccept(false);
                for (Transition t : enabledTransitions) {
                    // iterate all transitions
                    this.fire(t); // fire transition to change the marking of this petri net
                    nextMarking = this.getCurrentMarking(); // what the next marking would be
                    State targetState = stateMapping.get(Arrays.toString(nextMarking)); // has this marking been discovered before?
                    if (targetState == null) {
                        // if state does not exist yet, create it
                        targetState = new State();
                        // map it
                        stateMapping.put(Arrays.toString(nextMarking), targetState);
                        // and add marking to the list of markings to process
                        markingsToDo.push(nextMarking);
                    }
                    // add new transition from current to target state
                    Character c = this.stringToCharLabelMapping.get(t.label);
                    dk.brics.automaton.Transition fsmTrans = new dk.brics.automaton.Transition(c, targetState);
                    currentState.addTransition(fsmTrans);

                    this.setCurrentMarking(currentMarking); // restore marking of this net to process next transition
                }
            }


        }


        this.setCurrentMarking(saveMarking); // restore the marking of this net
        return fsm;
    }

    // returns a transition system for this petri net that is equipped with probabilities
    // treats the petri net as if it would be k-bounded
    public TransitionSystem getTransitionSystem(int bound) {

        String markingDelimiter = "_";

        // maps markings of the petri net to states of the fsm
        HashMap<String, Framework.Models.TransitionSystem.State> stateMapping = new HashMap<String, Framework.Models.TransitionSystem.State>();

        TransitionSystem ts = new TransitionSystem(this.name);

        int[] saveMarking = this.getCurrentMarking(); // save petri net's current marking and restore later
        this.reset(); // set current marking to initial marking

        String currentMarkingString = Arrays.toString(this.getCurrentMarking()).replace("[", "").replace("]", "").replace(", ", markingDelimiter);
        ts.addState(currentMarkingString);
        ts.setInitialState(ts.getStateByName(currentMarkingString));

        stateMapping.put(currentMarkingString, ts.initialState); // map initial state

        Stack<int[]> markingsToDo = new Stack<int[]>(); // stores all markings that still need processing
        markingsToDo.push(this.getCurrentMarking()); // current (initial) marking is the first to work on

        int tsCounter = 0;
        int[] currentMarking;
        int[] nextMarking;
        Runtime r = Runtime.getRuntime();
        while ((markingsToDo.isEmpty()) == false) {
            if ((float) r.totalMemory() >= (((float) r.maxMemory()) * 0.8f)) {
                //System.out.println("approaching overflow! mem: " + r.totalMemory() + " - max: " + r.maxMemory());
                r.gc();
                r.freeMemory();
                r.gc();
                //System.out.println("now: " + r.totalMemory() + " - max: " + r.maxMemory());
                return null;
            }

            currentMarking = markingsToDo.pop();
            //System.out.println("processing: " + Arrays.toString(currentMarking));
            //System.out.println("#todo: " + markingsToDo.size() + " - mapped: " + stateMapping.size());
            Framework.Models.TransitionSystem.State currentState = stateMapping.get(Arrays.toString(currentMarking).replace("[", "").replace("]", "").replace(", ", markingDelimiter)); // must exist at this point
            this.setCurrentMarking(currentMarking); // set marking of the petri net to the one currently processes

            ArrayList<Transition> enabledTransitions = this.getEnabledTransitionsK(bound); // get all transitions enabled in this state
            double transitionProb = 1.0 / ((double) enabledTransitions.size());

            for (Transition t : enabledTransitions) {
                // iterate all transitions
                this.fire(t); // fire transition to change the marking of this petri net
                nextMarking = this.getCurrentMarking(); // what the next marking would be
                Framework.Models.TransitionSystem.State targetState = stateMapping.get(Arrays.toString(nextMarking).replace("[", "").replace("]", "").replace(", ", markingDelimiter)); // has this marking been discovered before?
                if (targetState == null) {
                    String nextMarkingString = Arrays.toString(nextMarking).replace("[", "").replace("]", "").replace(", ", markingDelimiter);
                    // if state does not exist yet, create it
                    ts.addState(nextMarkingString);
                    targetState = ts.getStateByName(nextMarkingString);
                    // map it
                    stateMapping.put(nextMarkingString, targetState);
                    // and add marking to the list of markings to process
                    markingsToDo.push(nextMarking);
                }
                // add new transition from current to target state
                Framework.Models.TransitionSystem.Transition transTs = ts.addTransition("" + tsCounter++, t.label, currentState, targetState);
                transTs.probability = transitionProb;

                this.setCurrentMarking(currentMarking); // restore marking of this net to process next transition
            }


        }


        this.setCurrentMarking(saveMarking); // restore the marking of this net
        return ts;
    }

    // returns a graph object for this petri net
    public Graph getGraph() {
        Graph result = new Graph(this.name);

        HashMap<Transition, Node> transitionNodes = new HashMap<Transition, Node>();
        for (Transition t : this.transitions)
            transitionNodes.put(t, result.addNode(t.label));

        HashMap<Place, Node> placeNodes = new HashMap<Place, Node>();
        for (Place p : this.places)
            placeNodes.put(p, result.addNode("place"));

        for (Transition t : this.transitions) {
            for (Place p : t.inputPlaces) {
                Node transitionNode = transitionNodes.get(t);
                Node placeNode = placeNodes.get(p);
                result.addEdge(placeNode, transitionNode);
            }
            for (Place p : t.outputPlaces) {
                Node transitionNode = transitionNodes.get(t);
                Node placeNode = placeNodes.get(p);
                result.addEdge(transitionNode, placeNode);
            }
        }

        return result;
    }

    // ---------------------------- //
    // ----- file i/o methods ----- //
    // ---------------------------- //

    // writes DOT file for this petri net
    public Boolean writeDotFile(File file) {
        try {
            ArrayList<String> newContents = new ArrayList<String>();
            newContents.add("digraph G {ranksep=\".3\"; fontsize=\"8\"; remincross=true; margin=\"0.0,0.0\"; fontname=\"Arial\";rankdir=\"LR\"; ");
            newContents.add("edge [arrowsize=\"0.5\"];");
            newContents.add("node [height=\".2\",width=\".2\",fontname=\"Arial\",fontsize=\"8\"];");

            for (Transition t : this.transitions) {
                if (t.label.equals(invisibleSymbol) == false)
                    newContents.add(t.name + " [shape=\"box\",label=\"" + t.label + "\"];");
                else
                    newContents.add(t.name + " [shape=\"box\",label=\"\",style=\"filled\"];");
            }
            for (Place p : this.places) {
                newContents.add(p.name + " [shape=\"circle\",label=\"\"" + ((p.initialMarking > 0) ? ",style=filled,color=black" : "") + "];");
            }
            for (Transition t : this.transitions) {
                for (Place p : t.inputPlaces) {
                    newContents.add(p.name + " -> " + t.name + "[label=\"\"];");
                }
                for (Place p : t.outputPlaces) {
                    newContents.add(t.name + " -> " + p.name + "[label=\"\"];");
                }
            }

            newContents.add("}");
            FileUtils.writeLines(file, newContents);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    // ------------------------- //
    // ----- other methods ----- //
    // ------------------------- //

    // if loaded from a file, the inital marking of a petri net will be unknown!
    // this method guesses an initial marking if possible. in case that there is a unique place p such that there is no transition for which p is the output place, p's initial marking is set to 1 and all other places initial marking is set to 0
    public Boolean guessInitialMarking() {
        ArrayList<Place> candidates = new ArrayList<Place>();
        for (Place p : places) candidates.add(p);
        for (Transition t : transitions) {
            for (Place p : t.outputPlaces) {
                int i = candidates.indexOf(p);
                if (i > -1) candidates.remove(i);
            }
        }

        if (candidates.size() == 1) {
            for (Place p : places) {
                p.initialMarking = 0;
                p.currentMarking = 0;
            }
            candidates.get(0).initialMarking = 1;
            candidates.get(0).currentMarking = 1;
            return true;
        } else {
            return false;
        }

    }

    // prints the petri net to a string
    public String toString() {
        String result = "";
        result += "Petri Net: " + this.name + "\n";
        result += "Transitions:\n";
        for (int i = 0; i < this.transitions.size(); i++) {
            result += this.transitions.get(i).name + "(" + i + ") - " + this.transitions.get(i).label + " (" + this.transitions.get(i).inputPlaces.size() + "-" + this.transitions.get(i).outputPlaces.size() + ")\n";
        }
        result += "\n";
        result += "Places:\n";
        for (int i = 0; i < this.places.size(); i++) {
            result += this.places.get(i).name + "(" + i + "-" + this.places.get(i).currentMarking + ")" + " --- (" + this.places.get(i).incomingTransitions.size() + "-" + this.places.get(i).outgoingTransitions.size() + ")\n";
        }
        result += "\n";

        result += "\n";
        result += "Arcs:\n";
        for (int i = 0; i < this.transitions.size(); i++) {
            Transition t = this.transitions.get(i);
            for (Place p : t.inputPlaces) {
                result += p.name + " -> " + t.name + " --- ";
            }
            for (Place p : t.outputPlaces) {
                result += t.name + " -> " + p.name + " --- ";
            }
        }
        return result;
    }

    // checks if all the arraylists are properly filled
    public void checkArrays() {
        for (Transition t : this.transitions) {
            for (Place p : t.inputPlaces) {
                if (p.outgoingTransitions.indexOf(t) == -1)
                    System.out.println("Error at t" + t.name + "and input place p" + p.name);
                if (this.transitions.indexOf(t) == -1) System.out.println("no t" + t.name);
            }

            for (Place p : t.outputPlaces) {
                if (p.incomingTransitions.indexOf(t) == -1)
                    System.out.println("Error at t" + t.name + "and output place p" + p.name);
                if (this.transitions.indexOf(t) == -1) System.out.println("no t" + t.name);
            }
        }

        for (Place p : this.places) {
            for (Transition t : p.incomingTransitions) {
                if (t.outputPlaces.indexOf(p) == -1)
                    System.out.println("Error at p" + p.name + "and incoming transition place t" + t.name);
                if (this.places.indexOf(p) == -1) System.out.println("no p" + p.name);
            }

            for (Transition t : p.outgoingTransitions) {
                if (t.inputPlaces.indexOf(p) == -1)
                    System.out.println("Error at p" + p.name + "and outgoing transition place t" + t.name);
                if (this.places.indexOf(p) == -1) System.out.println("no p" + p.name);
            }
        }

    }
}
