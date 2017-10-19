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

package Framework.Predictors;

import java.util.ArrayList;

import Framework.Models.Log.NonRedundantLog;

public class HistoryPredictor extends Predictor {

    public HistoryPredictor(NonRedundantLog log, String name) {
        super(log, name);
        this.createPredictor(log);
    }

    public void createPredictor(NonRedundantLog log) {
        this.log = log;
    }

    // calculates probabilities for each symbol after seeing a history
    public double[] predictProbability(String[] history) {

        // make history numerical for easier processing
        int[] numericalHistory = new int[history.length];
        for (int i = 0; i < history.length; i++)
            numericalHistory[i] = this.log.getSymbolToNumberMapping().get(history[i]);

        // the distribution over symbols (returned in the end)
        double[] symbolDistribution = new double[log.getNumberOfUniqueSymbols()];

        // counts how often each symbol appears after seeing the history
        int[] symbolCounter = new int[log.getNumberOfUniqueSymbols()];
        for (int i = 0; i < log.getNumberOfUniqueSymbols(); i++)
            symbolCounter[i] = 0;

        // iterate cases in the log and record which symbols appear after seeing a history
        for (int c = 0; c < this.log.getNumberOfUniqueCases(); c++) {
            int caseMultiplicity = this.log.getCaseMultiplicity(c);
            ArrayList<Integer> currentCase = this.log.getCase(c);
            // use this case only if it is long enough to make a prediction
            if (currentCase.size() > history.length) {
                // check if this case starts with the history
                boolean historyMatch = true;
                for (int i = 0; i < history.length; i++)
                    if (numericalHistory[i] != currentCase.get(i))
                        historyMatch = false;
                // if we found a match, record the next symbol
                if (historyMatch)
                    symbolCounter[currentCase.get(history.length)] += caseMultiplicity;
            }
        }

        // compute total matches
        int totalMatches = 0;
        for (int i = 0; i < log.getNumberOfUniqueSymbols(); i++)
            totalMatches += symbolCounter[i];

        // if no matches are found, output equal probability for all symbols
        if (totalMatches == 0)
            for (int i = 0; i < symbolDistribution.length; i++)
                symbolDistribution[i] = 1.0 / ((double) log.getNumberOfUniqueSymbols());
        else
            for (int i = 0; i < symbolDistribution.length; i++)
                symbolDistribution[i] = ((double) symbolCounter[i]) / ((double) totalMatches);

        return symbolDistribution;
    }

}
