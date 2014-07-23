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

package Framework.Models.Graph.Matcher;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import Framework.Models.Graph.Edge;
import Framework.Models.Graph.Graph;
import Framework.Models.Graph.Node;

public class VF2Matcher {
	
	// finds all subgraph isomorphisms and prints them to the console
	// modelGraph is the big graph
	// patternGraph is the small graph which is searched for in the big one
	public void match(Graph modelGraph, Graph patternGraph) {
		
		State state = new State(modelGraph, patternGraph);
		this.matchInternal(state, modelGraph, patternGraph); 
	}
	
	// internal method for finding subgraphs. called recursively
	private void matchInternal(State s, Graph modelGraph, Graph patternGraph) {
		
		// abort search if we reached the final level of the search tree 
		if (s.depth == patternGraph.nodes.size()) {
			s.printMapping(); // all pattern nodes matched -> print solution
		}
		else
		{	
			// get candidate pairs
			Map<Integer,Integer> candiatePairs = this.getCandidatePairs(s, modelGraph, patternGraph);
			
			// iterate through candidate pairs
			for (Integer n : candiatePairs.keySet()) {
				int m = candiatePairs.get(n);
				
				// check if candidate pair (n,m) is feasible 
				if (checkFeasibility(s,n,m)) {
					
					//System.out.println("Associating : " + n + " - " + m);
					
					
					s.match(n, m); // extend mapping
					
					//System.out.println(s.getSetContent());
					
					matchInternal(s, modelGraph, patternGraph); // recursive call
					
					//System.out.println("Backtrack: " + n + " - " + m);
					
					s.backtrack(n, m); // remove (n,m) from the mapping
					
					//System.out.println(s.getSetContent());
					
				}
			}
		}
	}
	
	// determines all candidate pairs to be checked for feasibility
	private Map<Integer,Integer> getCandidatePairs(State s, Graph m, Graph p) {
		
		// if Tout sets are non-empty, choose nodes currently mapped nodes point to
		if ((s.T1out.size() > 0) && (s.T2out.size() > 0)) {
			return this.pairGenerator(s.T1out, s.T2out);
		}
		// if Tin sets are non-empty, choose nodes pointing towards currently mapped nodes
		else if ((s.T1in.size() > 0) && (s.T2in.size() > 0)) {
			return this.pairGenerator(s.T1in, s.T2in);	
		}
		// if no such pairs exist, choose any pair of yet unmapped nodes
		else {
			return this.pairGenerator(s.unmapped1, s.unmapped2);
		}
		
	}
	
	// generates pairs of nodes
	// outputs a map from model nodes to pattern nodes
	private Map<Integer,Integer> pairGenerator(Collection<Integer> modelNodes , Collection<Integer> patternNodes) {
		
		TreeMap<Integer,Integer> map = new TreeMap<Integer,Integer>(); // the map storing candidate pairs
		
		// find the largest among all pattern nodes (the one with the largest ID)!
		// Note: it does not matter how to choose a node here. The only important thing is to have a total order, i.e., to uniquely choose one node. If you do not do this, you might get multiple redundant states having the same pairs of nodes mapped. The only difference will be the order in which these pairs have been included (but the order does not change the result, so these states are all the same!).
		int nextPatternNode = -1;
		for (Integer i : patternNodes)
			nextPatternNode = Math.max(nextPatternNode, i);
		
		// generate pairs of all model graph nodes with the designated pattern graph node
		for (Integer i : modelNodes)
			map.put(i, nextPatternNode);
		
		return map; // return node pairs
	}
	
	// checks whether or not it makes sense to extend the mapping by the pair (n,m)
	// n is a model graph node
	// m is a pattern graph node
	private Boolean checkFeasibility(State s , int n , int m) {
		
		Boolean passed = true;
		
		passed = passed && checkSemanticFeasibility(s,n,m); // check equality of node labels
		//if (!passed) System.out.println("Semantic feasibility violated: " + n + " - " + m);
		
		passed = passed && checkRpredAndRsucc(s,n,m); // check Rpred / Rsucc conditions (subgraph isomorphism definition)
		//if (!passed) System.out.println("Rpred || Rsucc violated: " + n + " - " + m);
		
		passed = passed && checkRinAndRout(s,n,m); // check Rin / Rout conditions (1-look-ahead)
		//if (!passed) System.out.println("Rin || Rout violated: " + n + " - " + m);
		
		passed = passed && checkRnew(s,n,m); // checks Rnew conditions (2-look-ahead)
		//if (!passed) System.out.println("Rnew violated: " + n + " - " + m);
		
		return passed; // return result
	}
	
	// checks if extending the mapping by the pair (n,m) would violate the subgraph isomorphism definition
	private Boolean checkRpredAndRsucc(State s , int n , int m) {
		
		Boolean passed = true;
		
		// check if the structure of the (partial) model graph is also present in the (partial) pattern graph 
		// if a predecessor of n has been mapped to a node n' before, then n' must be mapped to a predecessor of m 
		Node nTmp = s.modelGraph.nodes.get(n);
		for (Edge e : nTmp.incomingEdges) {
			if (s.core_1[e.source.id] > -1) {
				passed = passed && (s.patternGraph.getAdjacencyMatrix()[s.core_1[e.source.id]][m] == 1);
			}
		}
		// if a successor of n has been mapped to a node n' before, then n' must be mapped to a successor of m
		for (Edge e : nTmp.outgoingEdges) {
			if (s.core_1[e.target.id] > -1) {
				passed = passed && (s.patternGraph.getAdjacencyMatrix()[m][s.core_1[e.target.id]] == 1);
			}
		}
		
		// check if the structure of the (partial) pattern graph is also present in the (partial) model graph
		// if a predecessor of m has been mapped to a node m' before, then m' must be mapped to a predecessor of n
		Node mTmp = s.patternGraph.nodes.get(m);
		for (Edge e : mTmp.incomingEdges) {
			if (s.core_2[e.source.id] > -1) {
				passed = passed && (s.modelGraph.getAdjacencyMatrix()[s.core_2[e.source.id]][n] == 1);
			}
		}
		// if a successor of m has been mapped to a node m' before, then m' must be mapped to a successor of n
		for (Edge e : mTmp.outgoingEdges) {
			if (s.core_2[e.target.id] > -1) {
				passed = passed && (s.modelGraph.getAdjacencyMatrix()[n][s.core_2[e.target.id]] == 1);
			}
		}
		
		return passed; // return the result
	}
	
	// checks if the 1-look-ahead conditions are satisfied
	private Boolean checkRinAndRout(State s , int n , int m) {
		
		Node nTmp = s.modelGraph.nodes.get(n);
		Node mTmp = s.patternGraph.nodes.get(m);
		
		Boolean passed = true;

		// ----------------- //
		// --- Check Rin --- //
		// ----------------- //
		
		// count successors of n which are in T1in
		int succNT1in = 0;
		for (Edge e : nTmp.outgoingEdges)
			if (s.inT1in(e.target.id))
				succNT1in++;

		// count successors of m which are in T2in
		int succMT2in = 0;
		for (Edge e : mTmp.outgoingEdges)
			if (s.inT2in(e.target.id))
				succMT2in++;

		passed = passed && (succNT1in >= succMT2in);
		
		// count predecessors of n which are in T1in
		int predNT1in = 0;
		for (Edge e : nTmp.incomingEdges)
			if (s.inT1in(e.source.id))
				predNT1in++;
		
		// count predecessors of m which are in T2in
		int predMT2in = 0;
		for (Edge e : mTmp.incomingEdges)
			if (s.inT2in(e.source.id))
				predMT2in++;

		passed = passed && (predNT1in >= predMT2in);
		

		// ------------------ //
		// --- Check Rout --- //
		// ------------------ //
		
		// count successors of n which are in T1out
		int succNT1out = 0;
		for (Edge e : nTmp.outgoingEdges)
			if (s.inT1out(e.target.id))
				succNT1out++;

		// count successors of m which are in T2out
		int succMT2out = 0;
		for (Edge e : mTmp.outgoingEdges)
			if (s.inT2out(e.target.id))
				succMT2out++;

		passed = passed && (succNT1out >= succMT2out);
		
		// count predecessors of n which are in T1out
		int predNT1out = 0;
		for (Edge e : nTmp.incomingEdges)
			if (s.inT1out(e.source.id))
				predNT1out++;
		
		// count predecessors of m which are in T2out
		int predMT2out = 0;
		for (Edge e : mTmp.incomingEdges)
			if (s.inT2out(e.source.id))
				predMT2out++;

		passed = passed && (predNT1out >= predMT2out);
		
		return passed;
	}
	
	// checks of the 2-look-ahead contitions are satisfied
	private Boolean checkRnew(State s , int n , int m) {
		
		Node nTmp = s.modelGraph.nodes.get(n);
		Node mTmp = s.patternGraph.nodes.get(m);
		
		Boolean passed = true;
		
		// count predecessors of n which are in N1tilde
		int predNN1tilde = 0;
		for (Edge e : nTmp.incomingEdges)
			if (s.inN1tilde(e.source.id))
				predNN1tilde++;

		// count predecessors of m which are in N2tilde
		int predMN2tilde = 0;
		for (Edge e : mTmp.incomingEdges)
			if (s.inT2in(e.source.id))
				predMN2tilde++;

		passed = passed && (predNN1tilde >= predMN2tilde);
		
		// count successors of n which are in N1tilde
		int succNN1tilde = 0;
		for (Edge e : nTmp.outgoingEdges)
			if (s.inN1tilde(e.target.id))
				succNN1tilde++;

		// count successors of m which are in N2tilde
		int succMN2tilde = 0;
		for (Edge e : mTmp.outgoingEdges)
			if (s.inT2in(e.target.id))
				succMN2tilde++;

		passed = passed && (succNN1tilde >= succMN2tilde);
		
		return passed;
	}
	
	// checks if the labels of two nodes match 
	private Boolean checkSemanticFeasibility(State s , int n , int m) {
		
		String nLabel = s.modelGraph.nodes.get(n).label;
		String mLabel = s.patternGraph.nodes.get(m).label;
		
		return nLabel.equals(mLabel); // simple string equality test
	}
	
}
