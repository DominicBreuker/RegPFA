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
import java.util.HashMap;

import Framework.Models.Log.NonRedundantLog;

public abstract class Predictor {

    public NonRedundantLog log;
    public String name = "noName";

    public Predictor(NonRedundantLog log, String name) {
        this.log = log;
        this.name = name;
    }

    public abstract double[] predictProbability(String[] history);

    public String predict(String[] history) {
        // the distribution over symbols (returned in the end)
        double[] symbolDistribution = this.predictProbability(history);

        // find symbol with highest probability
        int maxIndex = -1;
        for (int i = 0; i < symbolDistribution.length; i++) {
            if (maxIndex < 0)
                maxIndex = i;
            if (symbolDistribution[i] > symbolDistribution[maxIndex])
                maxIndex = i;
        }

        // return the symbol
        return this.log.getNumberToSymbolMapping().get(maxIndex);
    }

    public double scoreAccuracy(PredictionDataset data) {
        int uniqueCases = data.features.size();
        int hits = 0;
        int misses = 0;
        for (int i = 0; i < uniqueCases; i++) {
            ArrayList<String> currentFeature = data.features.get(i);
            String[] currentFeatureArray = new String[currentFeature.size()];
            currentFeatureArray = currentFeature.toArray(currentFeatureArray);
            String currentTarget = data.targets.get(i);
            int currentWeight = data.weights.get(i);

            String prediction = this.predict(currentFeatureArray);

            if (prediction.equals(currentTarget))
                hits += currentWeight;
            else
                misses += currentWeight;
        }
        return ((double) hits) / ((double) hits + misses);
    }

    public HashMap<String, Integer> getTruePositivesBySymbol(PredictionDataset data) {
        int uniqueCases = data.features.size();
        HashMap<String, Integer> truePositivesBySymbol = new HashMap<String, Integer>();
        for (int i = 0; i < uniqueCases; i++) {
            ArrayList<String> currentFeature = data.features.get(i);
            String[] currentFeatureArray = new String[currentFeature.size()];
            currentFeatureArray = currentFeature.toArray(currentFeatureArray);
            String currentTarget = data.targets.get(i);
            int currentWeight = data.weights.get(i);

            String prediction = this.predict(currentFeatureArray);

            if (prediction.equals(currentTarget)) {
                if (truePositivesBySymbol.containsKey(prediction))
                    truePositivesBySymbol.put(prediction, truePositivesBySymbol.get(prediction) + currentWeight);
                else
                    truePositivesBySymbol.put(prediction, currentWeight);
            }
        }
        return truePositivesBySymbol;
    }

    public HashMap<String, Integer> getFalsePositivesBySymbol(PredictionDataset data) {
        int uniqueCases = data.features.size();
        HashMap<String, Integer> falsePositives = new HashMap<String, Integer>();
        for (int i = 0; i < uniqueCases; i++) {
            ArrayList<String> currentFeature = data.features.get(i);
            String[] currentFeatureArray = new String[currentFeature.size()];
            currentFeatureArray = currentFeature.toArray(currentFeatureArray);
            String currentTarget = data.targets.get(i);
            int currentWeight = data.weights.get(i);

            String prediction = this.predict(currentFeatureArray);

            if (prediction.equals(currentTarget) == false)
                if (falsePositives.containsKey(prediction))
                    falsePositives.put(prediction, falsePositives.get(prediction) + currentWeight);
                else
                    falsePositives.put(prediction, currentWeight);
        }
        return falsePositives;
    }

    public HashMap<String, Double> sensitivityBySymbol(PredictionDataset data) {
        HashMap<String, Integer> truePositivesBySymbol = this.getTruePositivesBySymbol(data);
        HashMap<String, Integer> positivesBySymbol = data.getTargetSymbolCounts();

        HashMap<String, Double> sensitivityBySymbol = new HashMap<String, Double>();
        for (String symbol : positivesBySymbol.keySet()) {
            int currentTruePositives = 0;
            int currentPositives = positivesBySymbol.get(symbol);
            if (truePositivesBySymbol.containsKey(symbol))
                currentTruePositives += truePositivesBySymbol.get(symbol);
            sensitivityBySymbol.put(symbol, ((double) currentTruePositives) / ((double) currentPositives));
        }
        return sensitivityBySymbol;
    }

    public HashMap<String, Double> specitivityBySymbol(PredictionDataset data) {
        HashMap<String, Integer> falsePositivesBySymbol = this.getFalsePositivesBySymbol(data);
        HashMap<String, Integer> positivesBySymbol = data.getTargetSymbolCounts();

        HashMap<String, Double> specitivityBySymbol = new HashMap<String, Double>();
        for (String symbol : positivesBySymbol.keySet()) {
            int currentNegatives = data.size - positivesBySymbol.get(symbol);
            int currentTrueNegatives = currentNegatives;
            if (falsePositivesBySymbol.containsKey(symbol))
                currentTrueNegatives -= falsePositivesBySymbol.get(symbol);
            specitivityBySymbol.put(symbol, ((double) currentTrueNegatives) / ((double) currentNegatives));
        }
        return specitivityBySymbol;
    }

    public double getCrossEntropy(NonRedundantLog log) {

        // use dedicated method for em predictors (more efficient)
        if (this instanceof Framework.Predictors.EmMapPredictor) {
            EmMapPredictor emPred = (EmMapPredictor) this;
            return emPred.predictor.getCrossEntropy(log);
        }
        //return -1;


        // use this as default for other models
        double crossEntropy = 0.0;
        int numberOfCases = log.getNumberOfCases();

        for (int c = 0; c < log.getNumberOfUniqueCases(); c++) {

            // determine number of symbols in this case
            int N = log.getNumericalLog().get(c).size();

            // determine multiplicity of this case
            double caseMultiplicity = (double) log.getCaseMultiplicity(c);

            // calculate case probability
            double CaseProbability = 1.0;

            // get string version of this case
            ArrayList<String> currentCaseSymbolic = new ArrayList<String>();
            for (int s = 0; s < N; s++) {
                currentCaseSymbolic.add(log.getNumberToSymbolMapping().get(log.getNumericalLogEntry(c, s)));
            }

            ArrayList<String> historyUpToS = new ArrayList<String>();
            for (int s = 0; s < N; s++) {
                String currentTarget = currentCaseSymbolic.get(s);
                String[] history = new String[historyUpToS.size()];
                for (int i = 0; i < historyUpToS.size(); i++)
                    history[i] = historyUpToS.get(i);
                double probOfTarget = this.predictProbability(history)[log.getSymbolToNumberMapping().get(currentTarget)];
                CaseProbability = CaseProbability * probOfTarget;
                historyUpToS.add(currentTarget);
            }

            crossEntropy -= (caseMultiplicity / (double) (numberOfCases)) * Framework.Utils.Utilities.log2(CaseProbability);

        }

        return crossEntropy;
    }
}
