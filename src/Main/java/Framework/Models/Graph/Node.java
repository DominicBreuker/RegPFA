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


package Framework.Models.Graph;

import java.util.ArrayList;

public class Node {

    public Graph graph; // the graph to which the node belongs

    public int id; // a unique id - running number
    public String label; // for semantic feasibility checks

    public ArrayList<Edge> outgoingEdges = new ArrayList<Edge>(); // edges of which this node is the origin
    public ArrayList<Edge> incomingEdges = new ArrayList<Edge>(); // edges of which this node is the destination

    public Node(Graph g, int id, String label) {
        this.graph = g;
        this.id = id;
        this.label = label;
    }

    public Node(Graph g, int id) {
        this(g, id, "none");
    }


    public ArrayList<Node> getSuccessors() {
        ArrayList<Node> result = new ArrayList<Node>();

        for (Edge e : outgoingEdges)
            result.add(e.target);

        return result;
    }

    public ArrayList<Integer> getSuccessorsId() {
        ArrayList<Integer> result = new ArrayList<Integer>();

        for (Edge e : outgoingEdges)
            result.add(e.target.id);

        return result;
    }

    public ArrayList<Node> getPredecessors() {
        ArrayList<Node> result = new ArrayList<Node>();

        for (Edge e : incomingEdges)
            result.add(e.source);

        return result;
    }

    public ArrayList<Integer> getPredecessorsId() {
        ArrayList<Integer> result = new ArrayList<Integer>();

        for (Edge e : incomingEdges)
            result.add(e.source.id);

        return result;
    }


}
