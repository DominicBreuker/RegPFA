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

import Framework.Models.Graph.Edge;
import Framework.Models.Graph.Graph;
import Framework.Models.Graph.Node;

import java.util.Collection;
import java.util.HashSet;

public class State {
	
	public int[] core_1; // stores for each model graph node to which pattern graph node it maps ("-1" indicates no mapping)
	public int[] core_2; // stores for each pattern graph node to which model graph node it maps ("-1" indicates no mapping)
	
	public int[] in_1; // stores for each model graph node the depth in the search tree at which it entered "T_1 in" or the mapping ("-1" indicates that the node is not part of the set)
	public int[] in_2; // stores for each pattern graph node the depth in the search tree at which it entered "T_2 in" or the mapping ("-1" indicates that the node is not part of the set)
	public int[] out_1; // stores for each model graph node the depth in the search tree at which it entered "T_1 out" or the mapping ("-1" indicates that the node is not part of the set)
	public int[] out_2; // stores for each pattern graph node the depth in the search tree at which it entered "T_2 out" or the mapping ("-1" indicates that the node is not part of the set)
	
	
	// sets mentioned in the paper
	public HashSet<Integer> T1in;
	public HashSet<Integer> T1out;
	public HashSet<Integer> T2in;
	public HashSet<Integer> T2out;
	
	// sets storing yet unmapped nodes
	public HashSet<Integer> unmapped1;
	public HashSet<Integer> unmapped2;
	
	public int depth = 0; // current depth of the search tree
	
	public Graph modelGraph;
	public Graph patternGraph;
	
	// initializes a new state
	public State(Graph modelGraph, Graph patternGraph) {
		
		this.modelGraph = modelGraph;
		this.patternGraph = patternGraph;
		
		int modelSize = modelGraph.nodes.size();
		int patternSize = patternGraph.nodes.size();
		
		T1in = new HashSet<Integer>(modelSize * 2);
		T1out = new HashSet<Integer>(modelSize * 2);
		T2in = new HashSet<Integer>(modelSize * 2);
		T2out = new HashSet<Integer>(modelSize * 2);
		
		unmapped1 = new HashSet<Integer>(modelSize * 2);
		unmapped2 = new HashSet<Integer>(patternSize * 2);
		
		core_1 = new int[modelSize];
		core_2 = new int[patternSize];
		
		in_1 = new int[modelSize];
		in_2 = new int[patternSize];
		out_1 = new int[modelSize];
		out_2 = new int[patternSize];
		
		// initialize values ("-1" means no mapping / not contained in the set)
		// initially, all sets are empty and no nodes are mapped
		for (int i = 0 ; i < modelSize ; i++) {
			core_1[i] = -1;
			in_1[i] = -1;
			out_1[i] = -1;
			unmapped1.add(i);
		}
		for (int i = 0 ; i < patternSize ; i++) {
			core_2[i] = -1;
			in_2[i] = -1;
			out_2[i] = -1;
			unmapped2.add(i);
		}
	}
	
	
	public String getSetContent() {
		String s = "";
		s += "T1in:  " + setContent(T1in) + "\n";
		s += "T1out: " + setContent(T1out) + "\n";
		s += "T2in:  " + setContent(T2in) + "\n";
		s += "T2out: " + setContent(T2out) + "\n";
		s += "un1:   " + setContent(unmapped1) + "\n";
		s += "un2:   " + setContent(unmapped2) + "\n";
		return s;
	}
	
	// temporary methods
	private String setContent(Collection<Integer> c) {
		String s = "";
		for (Integer i : c)
			s += i + " ";
		return s;
	}
	
	// set membership tests
	public Boolean inM1(int nodeId) {
		return (core_1[nodeId] > -1);
	}
	
	public Boolean inM2(int nodeId) {
		return (core_2[nodeId] > -1);
	}
	
	public Boolean inT1in(int nodeId) {
		return ((core_1[nodeId] == -1) && (in_1[nodeId] > -1));
	}
	
	public Boolean inT2in(int nodeId) {
		return ((core_2[nodeId] == -1) && (in_2[nodeId] > -1));
	}
	
	public Boolean inT1out(int nodeId) {
		return ((core_1[nodeId] == -1) && (out_1[nodeId] > -1));
	}
	
	public Boolean inT2out(int nodeId) {
		return ((core_2[nodeId] == -1) && (out_2[nodeId] > -1));
	}
	
	public Boolean inT1(int nodeId) {
		return (this.inT1in(nodeId) || this.inT1out(nodeId));
	}
	
	public Boolean inT2(int nodeId) {
		return (this.inT2in(nodeId) || this.inT2out(nodeId));
	}
	
	public Boolean inN1tilde(int nodeId) {
		return ((core_1[nodeId] == -1) && (in_1[nodeId] == -1) && (out_1[nodeId] == -1));
	}
	
	public Boolean inN2tilde(int nodeId) {
		return ((core_2[nodeId] == -1) && (in_2[nodeId] == -1) && (out_2[nodeId] == -1));
	}
	
	// extends the current matching by the pair (n,m) -> going down one level in the search tree
	// n is the id of a model graph node
	// m is the id of a pattern graph node
	public void match(int n, int m) {
		
		// include pair (n,m) into the mapping 
		core_1[n] = m;
		core_2[m] = n;
		unmapped1.remove(n);
		unmapped2.remove(m);
		
		T1in.remove(n);
		T1out.remove(n);
		T2in.remove(m);
		T2out.remove(m);
		
		depth++; // increase depth (we moved down one level in the search tree)
		
		// update in/out arrays
		// updates needed for nodes entering Tin/Tout sets on this level
		// no updates needed for nodes which entered these sets before
		Node nTmp = modelGraph.nodes.get(n);
		Node mTmp = patternGraph.nodes.get(m);
		// cycle through nodes pointing towards n
		for (Edge e : nTmp.incomingEdges) {
			if (in_1[e.source.id] == -1){
				in_1[e.source.id] = depth; // update in_1
				if (!inM1(e.source.id))
					T1in.add(e.source.id); // update T1in
			}
		}
		// cycle through nodes n points to
		for (Edge e : nTmp.outgoingEdges) {
			if (out_1[e.target.id] == -1){
				out_1[e.target.id] = depth; // update out_1
				if (!inM1(e.target.id))
					T1out.add(e.target.id); // update T1out
			}
		}
		// cycle through nodes pointing towards m
		for (Edge e : mTmp.incomingEdges) {
			if (in_2[e.source.id] == -1){
				in_2[e.source.id] = depth; // update in_2
				if (!inM2(e.source.id))
					T2in.add(e.source.id); // update T2in
			}
		}
		// cycle through nodes m points to
		for (Edge e : mTmp.outgoingEdges) {
			if (out_2[e.target.id] == -1){
				out_2[e.target.id] = depth; // update out_2
				if (!inM2(e.target.id))
					T2out.add(e.target.id); // update T2out
			}
		}
			
	}
	
	// removes the pair (n,m) from the current mapping -> going up one level in the search tree
	// n is the id of a model graph node
	// m is the id of a pattern graph node
	public void backtrack(int n, int m) {
		
		// remove mapping for pair (n,m)
		core_1[n] = -1;
		core_2[m] = -1;
		unmapped1.add(n);
		unmapped2.add(m);
		
		// if any node entered Tin/Tout sets when the mapping was extended by pair (n,m), undo this
		for (int i = 0 ; i < core_1.length ; i++) {
			if (in_1[i] == depth) {
				in_1[i] = -1;
				T1in.remove(i);
			}
			if (out_1[i] == depth) {
				out_1[i] = -1;
				T1out.remove(i);
			}
		}
		for (int i = 0 ; i < core_2.length ; i++) {
			if (in_2[i] == depth) {
				in_2[i] = -1;
				T2in.remove(i);
			}
			if (out_2[i] == depth) {
				out_2[i] = -1;
				T2out.remove(i);
			}
		}
		
		// put n / m back into Tin and Tout sets if necessary
		if (inT1in(n))
			T1in.add(n);
		if (inT1out(n))
			T1out.add(n);
		if (inT2in(m))
			T2in.add(m);
		if (inT2out(m))
			T2out.add(m);
		
		depth--; // decrease depth of search tree -> we move up one level
	}
	
	// keep track of mappings found by the algorithm
	// simply prints these mappings to the console
	// format: ([modelNode],[patternNode]) ...
	public void printMapping() {
		System.out.print("Mapping found: ");
		for (int i = 0 ; i < core_2.length ; i++) {
			System.out.print("(" + core_2[i] + "-" + i + ") ");
		}
		System.out.println();
	}
}
