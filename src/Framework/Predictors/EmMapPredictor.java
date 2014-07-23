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

import java.util.Arrays;
import java.util.HashMap;

import Framework.Algorithm.EmMapAlgorithm;
import Framework.Algorithm.EmMapResult;
import Framework.Models.Log.NonRedundantLog;

public class EmMapPredictor extends Predictor {
	
	public EmMapPredictor(NonRedundantLog log, String name) {
		super(log, name);
	}

	public EmMapResult predictor = null;
	
	public static final int HIC_SELECTOR = 1;
	public static final int AIC_SELECTOR = 2;
	public static final int BIC_SELECTOR = 3;
	public static final int TEST_SELECTOR = 4;
	
	public static HashMap<Integer,EmMapPredictor> createPredictors(NonRedundantLog log, int[] gridStates, double[] gridPrior, int numberOfTries, int maxIter, double convergenceThreshold) throws Exception {
		
		HashMap<Integer,EmMapResult> result = new HashMap<Integer,EmMapResult>();
		
		EmMapResult temporarySolution = null; // variable to store the (temporarily) best solution for the current parameter setting
		
		// iterate all values for prior and state number
		for (int i = 0 ; i < gridPrior.length ; i++) {
			double priorStrength = gridPrior[i];
			for (int j = 0 ; j < gridStates.length ; j++) {
				int numberOfStates = gridStates[j];
				temporarySolution = null;
				// make a number of tries to ensure not ending up with a bad local solution
				for (int numberOfTry = 1 ; numberOfTry <= numberOfTries ; numberOfTry++) {
					// create algorithm, set parameters, and run
					EmMapAlgorithm algo = new EmMapAlgorithm(log, numberOfStates, priorStrength);
					algo.setMaximumIterations(maxIter);
					algo.setConvergenceThreshold(convergenceThreshold);
					EmMapResult tmp = algo.runAlgorithm();
					
					// update best solution for this state number and prior value (if needed)
					if (temporarySolution == null)
						temporarySolution = tmp;
					else
						if (tmp.getTrainCrossEntropy() < temporarySolution.getTrainCrossEntropy()) {
							temporarySolution = tmp;
						}
				}
				
				if (result.containsKey(EmMapPredictor.AIC_SELECTOR) == false) {
					result.put(EmMapPredictor.AIC_SELECTOR, temporarySolution);
				}
				else {
					if (temporarySolution.getAIC() < result.get(EmMapPredictor.AIC_SELECTOR).getAIC()) {
						System.out.println("AIC_Improvement: new = " + temporarySolution.getAIC() + " vs. old = " + result.get(EmMapPredictor.AIC_SELECTOR).getAIC());
						result.put(EmMapPredictor.AIC_SELECTOR, temporarySolution);
					}
				}
				if (result.containsKey(EmMapPredictor.HIC_SELECTOR) == false) {
					result.put(EmMapPredictor.HIC_SELECTOR, temporarySolution);
				}
				else {
					if (temporarySolution.getHEU() < result.get(EmMapPredictor.HIC_SELECTOR).getHEU()) {
						System.out.println("HIC_Improvement: new = " + temporarySolution.getHEU() + " vs. old = " + result.get(EmMapPredictor.HIC_SELECTOR).getHEU());
						result.put(EmMapPredictor.HIC_SELECTOR, temporarySolution);
					}
				}
				if (result.containsKey(EmMapPredictor.TEST_SELECTOR) == false) {
					result.put(EmMapPredictor.TEST_SELECTOR, temporarySolution);
				}
				else {
					if (temporarySolution.getTestCrossEntropy() < result.get(EmMapPredictor.TEST_SELECTOR).getTestCrossEntropy()) {
						System.out.println("Test_Improvement: new = " + temporarySolution.getTestCrossEntropy() + " vs. old = " + result.get(EmMapPredictor.TEST_SELECTOR).getTestCrossEntropy());
						result.put(EmMapPredictor.TEST_SELECTOR, temporarySolution);
					}
				}
			}
		}
		
		HashMap<Integer,EmMapPredictor> predictors = new HashMap<Integer,EmMapPredictor>();
		
		EmMapPredictor aicPred = new EmMapPredictor(log,"EmAic");
		aicPred.predictor = result.get(EmMapPredictor.AIC_SELECTOR);
		aicPred.name = aicPred.name + "(k=" + aicPred.predictor.getNumberOfStates() + ",np=" + aicPred.predictor.getPriorStrength() + ")";
		predictors.put(EmMapPredictor.AIC_SELECTOR, aicPred);
		
		EmMapPredictor hicPred = new EmMapPredictor(log,"EmHic");
		hicPred.predictor = result.get(EmMapPredictor.HIC_SELECTOR);
		hicPred.name = hicPred.name + "(k=" + hicPred.predictor.getNumberOfStates() + ",np=" + hicPred.predictor.getPriorStrength() + ")";
		predictors.put(EmMapPredictor.HIC_SELECTOR, hicPred);
		
		EmMapPredictor testPred = new EmMapPredictor(log,"EmTest");
		testPred.predictor = result.get(EmMapPredictor.TEST_SELECTOR);
		testPred.name = testPred.name + "(k=" + testPred.predictor.getNumberOfStates() + ",np=" + testPred.predictor.getPriorStrength() + ")";
		predictors.put(EmMapPredictor.TEST_SELECTOR, testPred);
		
		return predictors;
	}
	
	public EmMapResult createPredictor(int[] gridStates, double[] gridPrior, int numberOfTries, int maxIter, double convergenceThreshold, int selectionCriterion) throws Exception {
		
		EmMapResult temporarySolution = null; // variable to store the (temporarily) best solution for the current parameter setting
		
		// iterate all values for prior and state number
		for (int i = 0 ; i < gridPrior.length ; i++) {
			double priorStrength = gridPrior[i];
			for (int j = 0 ; j < gridStates.length ; j++) {
				int numberOfStates = gridStates[j];
				temporarySolution = null;
				// make a number of tries to ensure not ending up with a bad local solution
				for (int numberOfTry = 1 ; numberOfTry <= numberOfTries ; numberOfTry++) {
					// create algorithm, set parameters, and run
					EmMapAlgorithm algo = new EmMapAlgorithm(log, numberOfStates, priorStrength);
					algo.setMaximumIterations(maxIter);
					algo.setConvergenceThreshold(convergenceThreshold);
					EmMapResult tmp = algo.runAlgorithm();
					
					// update best solution for this state number and prior value (if needed)
					if (temporarySolution == null)
						temporarySolution = tmp;
					else
					{
						if (this.predictor == null) {
							this.predictor = temporarySolution;
						}
						switch (selectionCriterion) {
						case 1:
							if (temporarySolution.getHEU() < this.predictor.getHEU()) {
								System.out.println("_Improvement: new = " + this.predictor.getHEU() + " vs. old = " + temporarySolution.getHEU());
								this.predictor = temporarySolution;
							}
							break;
						case 2:
							if (temporarySolution.getAIC() < this.predictor.getAIC()) {
								this.predictor = temporarySolution;
							}
							break;
						case 3:
							if (temporarySolution.getBIC() < this.predictor.getBIC()) {
								this.predictor = temporarySolution;
							}
							break;
						case 4:
							if (temporarySolution.getTestCrossEntropy() < this.predictor.getTestCrossEntropy()) {
								this.predictor = temporarySolution;
							}
							break;
						default:
							throw new Exception("Unknown selection criterion criterion");
						}
					}	
				}
			}
		}
		
		return temporarySolution;
		
	}
	
	// ------------------------------ //
	// ----- prediction methods ----- //
	// ------------------------------ //
	
	// calculates probabilities for each symbol after seeing a history
	public double[] predictProbability(String[] history) {
		
		// the distribution over symbols (returned in the end)
		double[] symbolDistribution = new double[predictor.numberOfSymbols];
		
		// prior contains the initial state distribution
		double[] stateDistribution = this.updateStateDistribution(predictor.prior, history);
		
		// compute distribution over symbols given current state distribution
		for (int i = 0 ; i < predictor.numberOfSymbols ; i++) {
			symbolDistribution[i] = 0;
			for (int j = 0 ; j < predictor.numberOfStates ; j++) {
				symbolDistribution[i] += stateDistribution[j] * predictor.obsmat[j][i];
			}
		}
		
		return symbolDistribution;
	}
	
	// calculates probabilities for a particular symbol after seeing a history, for each of the subsequent steps
	public double[] getSymbolProbabilitiesBySteps(String[] history, int numberOfSteps, String symbol) {
		
		if ((numberOfSteps < 1) || (symbol == null) || (predictor.log.getSymbolToNumberMapping().get(symbol) == null))
			throw new IllegalArgumentException("problem with arguments while computing symbol probabilities");
		
		double[] symbolProbabilities = new double[numberOfSteps];
		int numericalSymbol = predictor.log.getSymbolToNumberMapping().get(symbol);
				
		double[] currentStateDistribution = this.updateStateDistribution(predictor.prior, history);
		
		for (int i = 0 ; i < numberOfSteps ; i++) {
			// compute symbol probabilitiy
			symbolProbabilities[i] = 0.0;
			for (int j = 0 ; j < predictor.numberOfStates ; j++) {
				symbolProbabilities[i] += currentStateDistribution[j] * predictor.obsmat[j][numericalSymbol];
			}
			
			// update state distribution
			double[] oldStateDistribution = Arrays.copyOf(currentStateDistribution, currentStateDistribution.length);
			for (int j = 0 ; j < predictor.numberOfStates ; j++) {
				currentStateDistribution[j] = 0.0;
				for (int k = 0 ; k < predictor.numberOfStates ; k++) {
					for (int l = 0 ; l < predictor.numberOfSymbols ; l++) {
						currentStateDistribution[j] += oldStateDistribution[k] * predictor.obsmat[k][l] * predictor.transcube[k][l][j];
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
		}
		else {
			// convert history into numerical format
			int[] numericalHistory = new int[history.length];
			for (int i = 0 ; i < history.length ; i++) {
				
				if (predictor.log.getSymbolToNumberMapping().get(history[i]) == null) {
					throw new IllegalArgumentException("History contains an event not found of the original log!");
				}
				numericalHistory[i] = predictor.log.getSymbolToNumberMapping().get(history[i]);
			}
			
			// update state distribution up to end of history
			for (int i = 0 ; i < numericalHistory.length ; i++) {
				// save current distribution temporarily
				double[] oldStateDistribution = Arrays.copyOf(stateDistribution, stateDistribution.length);
				
				// update the distribution
				double tmpSum = 0.0;
				for (int j = 0 ; j < predictor.numberOfStates ; j++) {
					stateDistribution[j] = 0.0;
					for (int k = 0 ; k < predictor.numberOfStates ; k++) {
						stateDistribution[j] += oldStateDistribution[k] * predictor.transcube[k][numericalHistory[i]][j];
					}
					tmpSum += stateDistribution[j];
				}
				// renormalize to avoid rounding errors
				//System.out.println("tmpSum: " + tmpSum);
				for (int j = 0 ; j < predictor.numberOfStates ; j++) {
					stateDistribution[j] = (stateDistribution[j] / tmpSum);
				}
			}
		}
		
		return stateDistribution;
		
	}
	
}
