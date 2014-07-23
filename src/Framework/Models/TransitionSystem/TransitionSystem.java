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

package Framework.Models.TransitionSystem;

import java.util.*;

import org.apache.commons.io.FileUtils;

import java.io.File;

import Framework.Models.Graph.Graph;
import Framework.Models.Graph.Node;
import Framework.Models.PetriNet.PetriNet;
import Framework.Utils.Utilities;

public class TransitionSystem
{

	// all transitions labeled with this symbol are "invisible transitions"
	public static final String invisibleSymbol = "tau";
	
	// internal name of this transition system
	public String name;
	
	// lists of states and transitions associated with this transition system
	public ArrayList<State> states = new ArrayList<State>();
	public ArrayList<Transition> transitions = new ArrayList<Transition>();
	
	// intital state of this transition system
	public State initialState;
	
	// current State in execution
	public State currentState;
	
	// a TS should be able to represent its labels numerically
	public HashMap<String,String> symbol2number = new HashMap<String,String>();
	public HashMap<String,String> number2symbol = new HashMap<String,String>();
	
	// ------------------------ //
	// ----- constructors ----- //
	// ------------------------ //
	
	// cosntructs an empty transition system
	public TransitionSystem(String name)
	{
		this(name, new ArrayList<State>(), new ArrayList<Transition>());
	}
	
	// constructs a transition system with given states and transitions
	public TransitionSystem(String name, ArrayList<State> states, ArrayList<Transition> transitions)
	{
		this.name = name;
		this.states = states;
		this.transitions = transitions;
		this.initializeNumericalLabels();
	}
	
	// construct a transition system being the reachability graph of the given petri net
	public TransitionSystem(PetriNet pNet)
	{
		this(pNet.name);
		
		int[] currentMarking = pNet.getInitialMarking();
		int[] backupMarking = currentMarking;
		State currentState = new State(currentMarking, this);
		HashSet<State> foundStates = new HashSet<State>();
		ArrayList<int[]> statesToProcess = new ArrayList<int[]>();
		foundStates.add(currentState);
		statesToProcess.add(currentMarking);
		this.states.add(currentState);
		this.initialState = currentState;
		
		// iteratively check all transitions for all states.
		// add new states if found
		// add new transitions from currently processed state to other states
		while (statesToProcess.size() > 0)
		{
			currentMarking = statesToProcess.get(0);
			currentState = getStateByMarking(currentMarking);
			pNet.setCurrentMarking(currentMarking);
			ArrayList<Framework.Models.PetriNet.Transition> enabledPNetTransitions = pNet.getEnabledTransitions();
			
			for (Framework.Models.PetriNet.Transition t : enabledPNetTransitions)
			{
				backupMarking = currentMarking;
				pNet.fire(t);
				currentMarking = pNet.getCurrentMarking();
				
				State newState = new State(currentMarking, this);
				// check if this state was already found
				if (foundStates.add(newState) == true)
				{
					// new state found. process new state in some future iteration and add it to 
					statesToProcess.add(currentMarking);
					this.states.add(newState);
					// add transition from current to new state
					this.transitions.add(new Transition(t.name, t.label, this, currentState, newState));
				}
				else
				{
					// state already known. just add additional transition to it
					State targetState = getStateByMarking(currentMarking);
					this.transitions.add(new Transition(t.name, t.label, this, currentState, targetState));
				}
				// restore values
				currentMarking = backupMarking;
				pNet.setCurrentMarking(currentMarking);
			}
			statesToProcess.remove(0);
		}
		
		// complete bidirectional information between states and transitions
		for (Transition t : transitions)
		{
			if (t.sourceState.outgoingTransitions.contains(t) == false)
				t.sourceState.outgoingTransitions.add(t);
			if (t.targetState.incomingTransitions.contains(t) == false)
				t.targetState.incomingTransitions.add(t);
 		}
	}
	
	// -------------------------------- //
	// ----- construction methods ----- //
	// -------------------------------- //
	
	// adds a state
	public Boolean addState(String name)
	{
		return this.addState(name, new ArrayList<Transition>(), new ArrayList<Transition>());
	}
	
	// adds a state connected to transitions
	public Boolean addState(String name, ArrayList<Transition> incomingTransitions, ArrayList<Transition> outgoingTransitions)
	{
		return this.states.add(new State(name, this, incomingTransitions, outgoingTransitions));
	}
	
	// sets initial state
	public void setInitialState(State s)
	{
		this.initialState = s;
	}
	
	// add transition from one state to another
	public Transition addTransition(String name, String label, String source, String target)
	{
		// check if adding is possible
		if (getStateByName(name) != null) return null; // transition already exists
		State sourceState = getStateByName(source);
		State targetState = getStateByName(target);
		if ((sourceState == null) || (targetState == null)) return null;
		
		// add and maintain collections
		Transition t = new Transition(name, label, this, sourceState, targetState);
		this.transitions.add(t);
		sourceState.outgoingTransitions.add(t);
		targetState.incomingTransitions.add(t);
		
		return t;
	}
	
	// add transition from one state to another
	public Transition addTransition(String name, String label, State sourceState, State targetState)
	{
		// check if adding is possible
		if (getStateByName(name) != null) return null; // transition already exists
		if ((sourceState == null) || (targetState == null)) return null;
		
		// add and maintain collections
		Transition t = new Transition(name, label, this, sourceState, targetState);
		this.transitions.add(t);
		sourceState.outgoingTransitions.add(t);
		targetState.incomingTransitions.add(t);
		
		return t;
	}
	
	// removes a transition
	public Boolean removeTransition(Transition t)
	{
		if (this.transitions.contains(t) == false)	return false; // transition does not exist in this TS
		
		// remove transition
		this.transitions.remove(this.transitions.indexOf(t));
		t.sourceState.outgoingTransitions.remove(t.sourceState.outgoingTransitions.indexOf(t));
		t.targetState.incomingTransitions.remove(t.targetState.incomingTransitions.indexOf(t));
		
		return true;
	}
	
	public Boolean removeTransitions(ArrayList<Transition> trans)
	{
		Boolean result = false;
		for (Transition t : trans)
			result = result | this.removeTransition(t);
		return result;
	}
	
	// -------------------------- //
	// ----- getter methods ----- //
	// -------------------------- //
	
	public State getStateByName(String name)
	{
		for (State s : this.states) if (s.name.equals(name)) return s;
		return null;
	}
	
	public Transition getTransitionByName(String name)
	{
		for (Transition t : this.transitions)
			if (t.name.equals(name)) 
				return t;
		return null;
	}
	
	public HashMap<String,String> getNumberToSymbolMapping() {
		return this.number2symbol;
	}
	
	public ArrayList<Transition> getTransitionsWithLabel(String label)
	{
		ArrayList<Transition> result = new ArrayList<Transition>();
		
		for (Transition t : this.transitions)
			if (t.label.equals(label))
				result.add(t);
		
		return result;
	}
	
	// needed during reachability graph generation
	public State getStateByMarking(int[] marking)
	{
		String name = "";
		for (int i = 0 ; i < marking.length ; i++) name += marking[i];
		
		for (State s : this.states) if (s.name.equals(name)) return s;
		return null;
	}
	
	// ----------------------------- //
	// ----- execution methods ----- //
	// ----------------------------- //
	
	
	// sets the current state to the inital state
	public void reset() {
		this.currentState = this.initialState;
	}
	
	public State getCurrentState() {
		return this.currentState;
	}
	
	// fires a transition and returns the label
	// does not ensure that the transition system is in the source state!
	public String fireTransition(Transition trans) {
		this.currentState = trans.targetState;
		return trans.label;
	}
	
	public ArrayList<String> sample(int maxSize, boolean showInvisible) {
		
		State backupState = this.currentState;
		this.reset();
		
		ArrayList<String> sample = new ArrayList<String>();
		this.reset();
		
		for (int i = 0 ; i < maxSize ; i++)
		{
			// choose a transition at random, if possible
			int activeTransitions = this.currentState.outgoingTransitions.size();
			if (activeTransitions == 0) {
				break;
			}
			int tmp = (int)Math.floor(Math.random() * activeTransitions);
			Transition trans = this.currentState.outgoingTransitions.get(tmp);
			String symbol = fireTransition(trans);
			if ((!symbol.equals(TransitionSystem.invisibleSymbol)) || (showInvisible == true))
				sample.add(symbol);
		}
		
		this.currentState = backupState;
		
		return sample;
	}
	
	public ArrayList<ArrayList<String>> sample(int numberOfSamples, int maxSize, boolean showInvisible) {
		ArrayList<ArrayList<String>> samples = new ArrayList<ArrayList<String>>();
		
		for (int i = 0 ; i < numberOfSamples ; i++) {
			samples.add(this.sample(maxSize, showInvisible));
		}
		
		return samples;
	}
	
	// returns the log_2 probability that the given sample sequence is sampled from this transition system
	public double scoreSample(ArrayList<String> sample) {
		double loglik = 0;
		
		HashMap<State,Double> stateLogProbs = new HashMap<State,Double>();
		stateLogProbs.put(this.initialState, Math.log(1.0));
		
		for (String symbol : sample) {
			
			HashMap<State,Double> nextStateLogProbs = new HashMap<State,Double>();
			for (State s : stateLogProbs.keySet()) {
				double currentStateLogProb = stateLogProbs.get(s);
				for (Transition t : s.outgoingTransitions) {
					if (t.label.equals(symbol)) {
						State nextState = t.targetState;
						Double nextStateLogProb = nextStateLogProbs.get(nextState);
						if (nextStateLogProb == null) {
							nextStateLogProbs.put(nextState, currentStateLogProb + Math.log(t.probability));
						}
						else {
							nextStateLogProbs.put(nextState, currentStateLogProb + Utilities.addLogSpace(nextStateLogProb, t.probability));
						}
					}
				}
			}
			stateLogProbs = nextStateLogProbs;
		}
		
		// check which of the states the TS might be in after seeing the samples is a final state (i.e., one without outgoing transitions)
		for (State state : stateLogProbs.keySet()) {
			if (state.outgoingTransitions.size() == 0)
				loglik += stateLogProbs.get(state);
		}
		
		// convert to log_2
		loglik = loglik / Math.log(2.0);
		
		return loglik;
	}
	
	// returns the log_2 probability that the given sample sequences are sampled from this transition system
	public ArrayList<Double> scoreSamples(ArrayList<ArrayList<String>> samples) {
		ArrayList<Double> scores = new ArrayList<Double>();
		
		for (ArrayList<String> sample : samples) {
			scores.add(this.scoreSample(sample));
		}
		
		return scores;
	}
	
	
	// ---------------------------- //
	// ----- file i/o methods ----- //
	// ---------------------------- //
	
	// writes DOT file for this transition system
	public Boolean writeDotFile(File file)
	{
		try
		{
			ArrayList<String> newContents = new ArrayList<String>();
			newContents.add("digraph TS {");
			for (Transition t : this.transitions)
			{
				if (t.probability > 0.0) newContents.add("\"" + t.sourceState.name + "\" -> \"" + t.targetState.name + "\" [label=\"" + t.label + "(" + t.probability + ")\"];");
				else newContents.add("\"" + t.sourceState.name + "\" -> \"" + t.targetState.name + "\" [label=\"" + t.label + "\"];");
			}
			newContents.add("}");
			FileUtils.writeLines(file, newContents);
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	// writes the transition system to a file "petrify" can understand
	public Boolean writePetrifyFile(File file)
	{
		try
		{
			ArrayList<String> newContents = new ArrayList<String>();
			
			// add a line containing all possible outputs (i.e., labeles of transitions)
			// e.g., ".outputs t0 t1 t2 t3 t4"
			String tmp = ".outputs ";
			
			for (String s : this.number2symbol.keySet())
				tmp += s + " ";
			newContents.add(tmp.substring(0,tmp.length()-1)); // removes last space
			
			// add a line indicating this is a state graph
			// must always be ".state graph"
			newContents.add(".state graph");
			
			// add a line for each transition, indicating its source and target state
			// probabilities are not considered here!
			// e.g., "s0 t0 s1"
			for (Transition t : this.transitions)
				newContents.add("s" + t.sourceState.name + " " + this.symbol2number.get(t.label) + " s" + t.targetState.name);
				
			// add a line indicating the initial marking (if there is such a marking)
			// e.g., ".marking{s0}"
			newContents.add(".marking{s" + this.initialState.name + "}");
			
			// add a line indicating the end of the file
			// must always be ".end"
			newContents.add(".end");
			
			FileUtils.writeLines(file, newContents);
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	// writes the adjacency matrix of this transition system to a file
	public Boolean writeAdjacencyMatrix()
	{
		return writeAdjacencyMatrix("ModelsResult/TS_Adja/");
	}
	
	// writes the adjacency matrix of this transition system to a file
	public Boolean writeAdjacencyMatrix(String path)
	{
		try
		{
			ArrayList<String> newContents = new ArrayList<String>();
			newContents.add("name: " + this.name);
			newContents.add("initial State: " + this.states.indexOf(this.initialState));
			int n = this.states.size();
			int[][] adjacencyMatrix = new int[n][n];
			int k = 0;
			String[] labels = new String[this.transitions.size()];
			for (Transition t : this.transitions)
			{
				labels[k] = t.label;
				adjacencyMatrix[this.states.indexOf(t.sourceState)][this.states.indexOf(t.targetState)] = ++k;
			}
			for (int i = 0 ; i < n ; i++)
			{
				String line = "";
				for (int j = 0 ; j < n ; j++)
				{
					line += "" + adjacencyMatrix[i][j];
					if (j < n-1) line += ";";
				}
				newContents.add(line);
			}
			for (int i = 0 ; i < this.transitions.size() ; i++)
			{
				newContents.add((i+1) + " - " + labels[i]);
			}
			FileUtils.writeLines(new File(path + ".adj"), newContents);
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	
	// ------------------------- //
	// ----- other methods ----- //
	// ------------------------- //
	
	// returns a graph object for this transition system
	public Graph getGraph() {
		Graph result = new Graph(this.name);
		
		HashMap<Transition,Node> transitionNodes = new HashMap<Transition,Node>();
		for (Transition t : this.transitions)
			transitionNodes.put(t, result.addNode(t.label));
		
		HashMap<State,Node> stateNodes = new HashMap<State,Node>();
		for (State s : this.states)
			stateNodes.put(s, result.addNode("state"));
		
		for (Transition t : this.transitions) {
		
			Node transitionNode = transitionNodes.get(t);
			Node sourceStateNode = stateNodes.get(stateNodes.get(t.sourceState));
			Node targetStateNode = stateNodes.get(stateNodes.get(t.targetState));
			
			result.addEdge(sourceStateNode, transitionNode);
			result.addEdge(transitionNode, targetStateNode);
			
		}
		
		return result;
	}
	
	// initialize numerical representation of transition labels
	public void initializeNumericalLabels() {
		
		int numberOfUniqueLabels = 0;
		
		// initialize hashmaps
		this.symbol2number = new HashMap<String,String>();
		this.number2symbol = new HashMap<String,String>();
		
		// iterate transitions to identify mapping
		for (int i = 0 ; i < this.transitions.size(); i++)
		{
			Transition t = this.transitions.get(i);
			if (this.symbol2number.get(t.label) == null) {
				// new symbol found
				this.symbol2number.put(t.label, "t" + numberOfUniqueLabels);
				this.number2symbol.put("t" + numberOfUniqueLabels, t.label);
				numberOfUniqueLabels++;
			}
		}
	}
	
	// print TS into string
	public String toString()
	{
		String result = "";
		result += "Transition System: " + this.name +"\n";
		result += "States (" + states.size() + "):"+"\n";
		for (State s : states)
		{
			result += "s" + s.name + " - ";
		}
		result += "\n";
		result += "Transitions (" + transitions.size() + "):\n";
		for (Transition t : transitions)
		{
			result += t.sourceState.name + " (" + t.label + ") " + t.targetState.name + " | ";
		}
		return result;
	}
	
	
	// add probabilities to TS (needed for my similartiy metric)
	// it assigns a uniform distribution over transitions to each state
	public void addProbabilities()
	{
		for (State s : states)
		{
			int transitions = s.outgoingTransitions.size();
			if (transitions > 0)
			{
				for (Transition t : s.outgoingTransitions) t.probability = ((1.0)/(new Double(transitions)));
			}
		}
	}
	
	// prints a marking as a string
	public String markingToString(int[] marking)
	{
		String name = "";
		for (int i = 0 ; i < marking.length ; i++) name +=  " " + marking[i];
		return name;
	}
	
	public void numberTransitions()
	{
		int i = 1;
		for (Transition t : transitions) t.label = "" + i++;
	}
}