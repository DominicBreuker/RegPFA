package Main.java;/*
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

import java.util.ArrayList;

import Framework.Miners.RegPfaMiner;
import Framework.Models.Parsers;
import Framework.Models.Visualizer;
import Framework.Models.Log.NonRedundantLog;
import Framework.Models.PetriNet.PetriNet;
import Framework.Predictors.EmMapPredictor;
import Framework.Utils.Utilities;


public class Examples {

    public static String input1 = "Name: XOR(4)\n" +
            "# list of places and initial tokens (prefix \"p\" required)\n" +
            "p1 1\n" +
            "p2 0\n" +
            "p3 0\n" +
            "p4 0\n" +
            "# list of transitions with labels (prefix t required)\n" +
            "t1 A\n" +
            "t2 B\n" +
            "t3 C\n" +
            "t4 D\n" +
            "# list of arcs (first source, then target)\n" +
            "p1 -> t1\n" +
            "t1 -> p2\n" +
            "p2 -> t2\n" +
            "p2 -> t3\n" +
            "t2 -> p3\n" +
            "t3 -> p3\n" +
            "p3 -> t4\n" +
            "t4 -> p4\n";

    public static String input2 = "Name: Parallel(6)\n" +
            "# list of places and initial tokens (prefix \"p\" required)\n" +
            "p1 1\n" +
            "p2 0\n" +
            "p3 0\n" +
            "p4 0\n" +
            "p5 0\n" +
            "p6 0\n" +
            "# list of transitions with labels (prefix t required)\n" +
            "t1 A\n" +
            "t2 B\n" +
            "t3 C\n" +
            "t4 D\n" +
            "# list of arcs (first source, then target)\n" +
            "p1 -> t1\n" +
            "t1 -> p2\n" +
            "t1 -> p3\n" +
            "p2 -> t2\n" +
            "p3 -> t3\n" +
            "t2 -> p4\n" +
            "t3 -> p5\n" +
            "p4 -> t4\n" +
            "p5 -> t4\n" +
            "t4 -> p6\n";

    public static String input3 = "Name: LoopWithDuplicate(5)\n" +
            "# list of places and initial tokens (prefix \"p\" required)\n" +
            "p1 1\n" +
            "p2 0\n" +
            "p3 0\n" +
            "p4 0\n" +
            "p5 0\n" +
            "# list of transitions with labels (prefix t required)\n" +
            "t1 A\n" +
            "t2 B\n" +
            "t3 C\n" +
            "t4 D\n" +
            "t5 A\n" +
            "# list of arcs (first source, then target)\n" +
            "p1 -> t1\n" +
            "t1 -> p2\n" +
            "p2 -> t2\n" +
            "p2 -> t3\n" +
            "t2 -> p3\n" +
            "p3 -> t4\n" +
            "t4 -> p2\n" +
            "t3 -> p4\n" +
            "p4 -> t5\n" +
            "t5 -> p5\n";

    public static String input4 = "Name: DuplicateTraces(_)\n" +
            "# list of places and initial tokens (prefix \"p\" required)\n" +
            "p1 1\n" +
            "p2 0\n" +
            "p3 0\n" +
            "p4 0\n" +
            "p5 0\n" +
            "p6 0\n" +
            "# list of transitions with labels (prefix t required)\n" +
            "t1 A\n" +
            "t2 B\n" +
            "t3 C\n" +
            "t4 D\n" +
            "t5 A\n" +
            "t6 C\n" +
            "t7 A\n" +
            "# list of arcs (first source, then target)\n" +
            "p1 -> t1\n" +
            "t1 -> p2\n" +
            "p2 -> t2\n" +
            "p2 -> t3\n" +
            "t2 -> p3\n" +
            "p3 -> t4\n" +
            "t4 -> p2\n" +
            "t3 -> p4\n" +
            "p4 -> t5\n" +
            "t5 -> p5\n" +
            "p2 -> t6\n" +
            "t6 -> p6\n" +
            "p6 -> t7\n" +
            "t7 -> p2\n" +
            "";


    public static void main(String[] args) throws Exception {
        // get a Petri net
        PetriNet example = Parsers.readPetriNetFromString(input4);

        // sample an event log with 1000 cases - training log with 50%, validation log with 25%, test log with 25%
        ArrayList<ArrayList<String>> samples = example.sample(1000, 9999);
        float[] fractions = {0.5f, 0.25f, 0.25f};
        NonRedundantLog eventLog = Utilities.createTrainValidationTestLogs("input4Log", samples, fractions);
        System.out.println("Training log:\n" + eventLog.toSummaryString());
        System.out.println("Validation log:\n" + eventLog.getValidationLog().toSummaryString());
        System.out.println("Test log:\n" + eventLog.getTestLog().toSummaryString());

        // train a RegPFA model
        double[] gridPrior = {0.0, 0.1, 0.2, 0.3};
        int[] gridStates = {2, 4, 8, 10};
        double convergenceThreshold = 0.001;
        int maxIter = 5000;
        int numberOfTries = 5;
        int modelSelectionCriterion = EmMapPredictor.HIC_SELECTOR;
        RegPfaMiner miner = new RegPfaMiner();
        PetriNet minedNet = miner.minePetriNet(eventLog);

        Visualizer.showPetriNet(example);
        Visualizer.showPetriNet(minedNet);
    }
}