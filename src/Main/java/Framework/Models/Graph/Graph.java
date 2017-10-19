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

// class representing a graph
// construct graphs only with the methods provided in this class
public class Graph {

    public String name; // name of the graph
    public ArrayList<Node> nodes = new ArrayList<Node>(); // list of all nodes
    public ArrayList<Edge> edges = new ArrayList<Edge>(); // list of all edges

    private int[][] adjacencyMatrix; // stores graph structure as adjacecy matrix
    private boolean adjacencyMatrixUpdateNeeded = true; // indicates if the adjacency matrix needs an update
    private int nodeCounter = 0; // counts nodes for assigning id's

    public Graph(String name) {
        this.name = name;
    }

    // add nodes

    public Node addNode(String label) {
        Node newNode = new Node(this, nodeCounter, label);
        nodes.add(newNode);
        nodeCounter++;
        this.adjacencyMatrixUpdateNeeded = true;
        return newNode;
    }

    public Node addNode() {
        Node newNode = new Node(this, nodeCounter);
        nodes.add(newNode);
        nodeCounter++;
        this.adjacencyMatrixUpdateNeeded = true;
        return newNode;
    }

    // add edges
    public void addEdge(Node source, Node target) {
        edges.add(new Edge(this, source, target));
        this.adjacencyMatrixUpdateNeeded = true;
    }

    public void addEdge(int sourceId, int targetId) {
        this.addEdge(this.nodes.get(sourceId), this.nodes.get(targetId));
    }

    // get the adjacency matrix
    // reconstruct it if it needs an update
    public int[][] getAdjacencyMatrix() {

        if (this.adjacencyMatrixUpdateNeeded) {

            int k = this.nodes.size();
            this.adjacencyMatrix = new int[k][k];
            for (int i = 0; i < k; i++)
                for (int j = 0; j < k; j++)
                    this.adjacencyMatrix[i][j] = 0; // initialize entries to 0

            for (Edge e : this.edges) {
                this.adjacencyMatrix[e.source.id][e.target.id] = 1; // change entries to 1 if there is an edge
            }
            this.adjacencyMatrixUpdateNeeded = false;
        }
        return this.adjacencyMatrix;
    }


    // prints adjacency matrix to console
    public void printGraph() {
        int[][] a = this.getAdjacencyMatrix();
        int k = a.length;

        System.out.print(this.name + " - Nodes: ");
        for (Node n : nodes) System.out.print(n.id + " ");
        System.out.println();
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                System.out.print(a[i][j] + " ");
            }
            System.out.println();
        }
    }

}
