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

package Framework.Algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import Framework.Models.Log.NonRedundantLog;
import Framework.Models.TransitionSystem.TransitionSystem;

public class EmMapResult extends AlgorithmResult {

    public ArrayList<String> protocol;

    public double[] prior;
    public double[][] obsmat;
    public double[][][] transcube;
    public int numberOfStates;
    public double priorStrength;
    public int numberOfSymbols;
    public int numberOfObservations;
    public double AIC;
    public double BIC;
    public double HEU;
    public int iterations;
    public NonRedundantLog log;
    public double loglik;


    // generates a transition system
    // transitions with a probability lower than threshold are not included!
    // the ratio specifies how much more unlikely a transition must be compared to expected probability to be left out
    public TransitionSystem getTransitionSystem(double ratio) {
        TransitionSystem ts = new TransitionSystem(log.name);

        // add states first, as their number is known
        for (int i = 1; i <= numberOfStates; i++) {
            ts.addState("s" + i);
        }
        // specify inital state (is always the first)
        ts.setInitialState(ts.getStateByName("s1"));

        // add transitions now
        // include only those probable enough
        int transitionCounter = 1;
        Map<Integer, String> map = log.getNumberToSymbolMapping();
        Stack<Integer> statesToProcess = new Stack<Integer>();
        Set<Integer> statesProcessed = new HashSet<Integer>();
        statesToProcess.add(new Integer(1));
        Boolean transitionFound = false;

        double threshold = ratio * (1.0 / (numberOfSymbols * numberOfStates));

        while (statesToProcess.size() > 0) {
            // get next state to process
            int currentState = statesToProcess.pop();
            // mark current state as being processed
            statesProcessed.add(new Integer(currentState));

            transitionFound = false;
            for (int i = 0; i < numberOfSymbols; i++) {
                for (int j = 0; j < numberOfStates; j++) {
                    if (obsmat[currentState - 1][i] * transcube[currentState - 1][i][j] > threshold) {
                        transitionFound = true;
                        ts.addTransition("t" + transitionCounter++, map.get(i), "s" + (currentState), "s" + (j + 1));
                        if (!statesProcessed.contains(new Integer(j + 1)) && !statesToProcess.contains(new Integer(j + 1))) {
                            statesToProcess.add(new Integer(j + 1));
                        }
                    }
                }
            }
            // Fallback-Solution: in case no transition was likely enough, use the most likely one
            if (!transitionFound) {
                int max_i = 0;
                int max_j = 0;
                for (int i = 0; i < numberOfSymbols; i++)
                    for (int j = 0; j < numberOfStates; j++) {
                        if (obsmat[currentState - 1][i] * transcube[currentState - 1][i][j] > obsmat[currentState - 1][max_i] * transcube[currentState - 1][max_i][max_j]) {
                            max_i = i;
                            max_j = j;
                        }
                    }
                ts.addTransition("t" + transitionCounter++, map.get(max_i), "s" + (currentState), "s" + (max_j + 1));
                if (!statesProcessed.contains(new Integer(max_j + 1)) && !statesToProcess.contains(new Integer(max_j + 1)))
                    statesToProcess.add(new Integer(max_j + 1));
            }
        }

        ts.removeTransitions(ts.getTransitionsWithLabel(NonRedundantLog.TERMINATION_SYMBOL));

        return ts;
    }


    // --------------------------- //
    // ----- getter / setter ----- //
    // --------------------------- //

    public double getLoglik() {
        return loglik;
    }

    public void setLoglik(double loglik) {
        this.loglik = loglik;
    }

    double[] getPrior() {
        return prior;
    }

    void setPrior(double[] prior) {
        this.prior = prior;
    }

    double[][] getObsmat() {
        return obsmat;
    }

    void setObsmat(double[][] obsmat) {
        this.obsmat = obsmat;
    }

    double[][][] getTranscube() {
        return transcube;
    }

    void setTranscube(double[][][] transcube) {
        this.transcube = transcube;
    }

    public int getNumberOfStates() {
        return numberOfStates;
    }

    public void setNumberOfStates(int numberOfStates) {
        this.numberOfStates = numberOfStates;
    }

    public double getPriorStrength() {
        return priorStrength;
    }

    public void setPriorStrength(double prior) {
        this.priorStrength = prior;
    }

    public int getNumberOfSymbols() {
        return numberOfSymbols;
    }

    public void setNumberOfSymbols(int numberOfSymbols) {
        this.numberOfSymbols = numberOfSymbols;
    }

    public int getNumberOfObservations() {
        return numberOfObservations;
    }

    public void setNumberOfObservations(int numberOfObservations) {
        this.numberOfObservations = numberOfObservations;
    }

    public double getAIC() {
        return AIC;
    }

    public void setAIC(double aic) {
        AIC = aic;
    }

    public double getBIC() {
        return BIC;
    }

    public void setBIC(double bic) {
        BIC = bic;
    }

    public double getHEU() {
        return HEU;
    }

    public void setHEU(double heu) {
        HEU = heu;
    }

    public NonRedundantLog getLog() {
        return log;
    }

    public void setLog(NonRedundantLog log) {
        this.log = log;
    }

    public int getIterations() {
        return this.iterations;
    }

    public void setIterations(int i) {
        this.iterations = i;
    }


    // ------------------------------ //
    // ----- prediction methods ----- //
    // ------------------------------ //

    // calculates probabilities for each symbol after seeing a history
    public double[] getDistributionOverNextSymbol(String[] history) {

        // the distribution over symbols (returned in the end)
        double[] symbolDistribution = new double[this.numberOfSymbols];

        // prior contains the initial state distribution
        double[] stateDistribution = this.updateStateDistribution(this.prior, history);

        // compute distribution over symbols given current state distribution
        for (int i = 0; i < this.numberOfSymbols; i++) {
            symbolDistribution[i] = 0;
            for (int j = 0; j < this.numberOfStates; j++) {
                symbolDistribution[i] += stateDistribution[j] * this.obsmat[j][i];
            }
        }

        return symbolDistribution;
    }

    // calculates probabilities for a particular symbol after seeing a history, for each of the subsequent steps
    public double[] getSymbolProbabilitiesBySteps(String[] history, int numberOfSteps, String symbol) {

        if ((numberOfSteps < 1) || (symbol == null) || (this.log.getSymbolToNumberMapping().get(symbol) == null))
            throw new IllegalArgumentException("problem with arguments while computing symbol probabilities");

        double[] symbolProbabilities = new double[numberOfSteps];
        int numericalSymbol = this.log.getSymbolToNumberMapping().get(symbol);

        double[] currentStateDistribution = this.updateStateDistribution(this.prior, history);

        for (int i = 0; i < numberOfSteps; i++) {
            // compute symbol probabilitiy
            symbolProbabilities[i] = 0.0;
            for (int j = 0; j < this.numberOfStates; j++) {
                symbolProbabilities[i] += currentStateDistribution[j] * this.obsmat[j][numericalSymbol];
            }

            // update state distribution
            double[] oldStateDistribution = Arrays.copyOf(currentStateDistribution, currentStateDistribution.length);
            for (int j = 0; j < this.numberOfStates; j++) {
                currentStateDistribution[j] = 0.0;
                for (int k = 0; k < this.numberOfStates; k++) {
                    for (int l = 0; l < this.numberOfSymbols; l++) {
                        currentStateDistribution[j] += oldStateDistribution[k] * this.obsmat[k][l] * this.transcube[k][l][j];
                    }
                }
            }
        }


        return symbolProbabilities;
    }

    private double[] updateStateDistribution(double[] currentStateDistribution, String[] history) {

        // prior contains the initial state distribution
        double[] stateDistribution = Arrays.copyOf(currentStateDistribution, currentStateDistribution.length);

        if ((history == null) || (history.length == 0)) {
            // state distribution is up to date already as there is no history to update it with
            return stateDistribution;
        } else {
            // convert history into numerical format
            int[] numericalHistory = new int[history.length];
            for (int i = 0; i < history.length; i++) {

                if (this.log.getSymbolToNumberMapping().get(history[i]) == null) {
                    throw new IllegalArgumentException("History contains an event not found of the original log!");
                }
                numericalHistory[i] = this.log.getSymbolToNumberMapping().get(history[i]);
            }

            // update state distribution up to end of history
            for (int i = 0; i < numericalHistory.length; i++) {
                // save current distribution temporarily
                double[] oldStateDistribution = Arrays.copyOf(stateDistribution, stateDistribution.length);

                // update the distribution
                double tmpSum = 0.0;
                for (int j = 0; j < this.numberOfStates; j++) {
                    stateDistribution[j] = 0.0;
                    for (int k = 0; k < this.numberOfStates; k++) {
                        stateDistribution[j] += oldStateDistribution[k] * this.transcube[k][numericalHistory[i]][j];
                    }
                    tmpSum += stateDistribution[j];
                }
                // renormalize to avoid rounding errors
                //System.out.println("tmpSum: " + tmpSum);
                for (int j = 0; j < this.numberOfStates; j++) {
                    stateDistribution[j] = (stateDistribution[j] / tmpSum);
                }
            }
        }

        return stateDistribution;

    }

    // calculates the cross entropy of the samples in this log and this object's model
    public double getCrossEntropy(NonRedundantLog log) {
        double crossEntropy = 0.0;
        int numberOfCases = log.getNumberOfCases();

        for (int c = 0; c < log.getNumberOfUniqueCases(); c++) {

            // determine number of symbols in this case
            int N = log.getNumericalLog().get(c).size();

            // determine multiplicity of this case
            double caseMultiplicity = (double) log.getCaseMultiplicity(c);

            // calculate case probability
            double logCaseProbability = 0.0;
            double[] stateDistribution = Arrays.copyOf(this.prior, this.prior.length);

            for (int s = 0; s < N; s++) {
                // determine current symbol (as the number it has in the learning log)
                int cur_obs = log.getNumericalLogEntry(c, s);

                // calculate probability of seeing it (w.r.t. current state distribution)
                double prob = 0.0;
                for (int i = 0; i < this.numberOfStates; i++) {
                    prob += stateDistribution[i] * this.obsmat[i][cur_obs];
                }

                // update logarithm of caseProbability
                logCaseProbability += Framework.Utils.Utilities.log2(prob);

                // update state distribution
                double[] oldStateDistribution = Arrays.copyOf(stateDistribution, stateDistribution.length);
                double tmpSum = 0.0;
                for (int i = 0; i < this.numberOfStates; i++) {
                    stateDistribution[i] = 0.0;
                    for (int j = 0; j < this.numberOfStates; j++) {
                        stateDistribution[i] += oldStateDistribution[j] * this.transcube[j][cur_obs][i];
                    }
                    tmpSum += stateDistribution[i];
                }

                // renormalize to avoid rounding errors over time
                for (int i = 0; i < this.numberOfStates; i++) {
                    stateDistribution[i] = stateDistribution[i] / tmpSum;
                }
            }

            crossEntropy -= (caseMultiplicity / (double) (numberOfCases)) * logCaseProbability;
        }

        return crossEntropy;
    }

    // calculate perplexity on test set
    public double getTrainCrossEntropy() {
        return this.getCrossEntropy(this.log);
    }

    // calculate perplexity on test set
    public double getTestCrossEntropy() {
        return this.getCrossEntropy(this.log.getValidationLog());
    }

    // calculate perplexity on test set
    public double getTrainPerplextiy() {
        return Math.pow(2.0, this.getTrainCrossEntropy());
    }

    // calculate perplexity on test set
    public double getTestPerplexity() {
        return Math.pow(2.0, this.getTestCrossEntropy());
    }

    // ------------------------- //
    // ----- other methods ----- //
    // ------------------------- //

    public String toString() {
        String result = "";
        result += "Learning \"" + this.log.name + "\" from " + this.numberOfObservations + " observations\n";
        result += "States: " + this.numberOfStates + " / Symbols: " + this.numberOfSymbols + " \n";
        result += "log-likelihood: " + this.loglik + " --- (AIC: " + Math.round(this.AIC * 100) / 100.0 + " / BIC: " + Math.round(this.BIC * 100) / 100.0 + " / HEU" + Math.round(this.HEU * 100) / 100.0 + ")\n";

        return result;
    }

}
