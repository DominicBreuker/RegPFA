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
import java.util.Arrays;
import java.util.HashMap;

import Framework.Models.Log.NonRedundantLog;

public class nGramPredictor extends Predictor {
	
	ArrayList<HashMap<String,double[]>> nGrams = new ArrayList<HashMap<String,double[]>>();
	double[] intitalDistribution = null;
	int n = -1;
	
	public nGramPredictor(NonRedundantLog log, String name) {
		super(log, name);
	}
	
	public void createPredictor(int n) {
		this.n = n;
		int symbolNum = log.getNumberOfUniqueSymbols();
		ArrayList<HashMap<String,int[]>> nGramCounters = new ArrayList<HashMap<String,int[]>>();
		int[] intitalDistributionCounter = new int[symbolNum];
		for (int i = 0 ; i < symbolNum ; i++)
			intitalDistributionCounter[i] = 0;
		
		// create nGrams up to n-value given to this function
		for (int i = 1 ; i <= n ; i++) {
			
			HashMap<String,int[]> currentnGramCounter = new HashMap<String,int[]>();
			
			// iterate unique cases
			for (int c = 0 ; c < log.getNumberOfUniqueCases() ; c++) {
				int caseMultiplicity = log.getCaseMultiplicity(c);
				ArrayList<Integer> currentCase = log.getCase(c);
				intitalDistributionCounter[currentCase.get(0)] += caseMultiplicity;
				// get all nGrams and count the symbols that follow on them
				for (int e = i ; e < currentCase.size(); e++) {
					String[] featureSymbols = new String[i];
					for (int j = i ; j > 0 ; j--) {
						featureSymbols[i-j] = log.getNumberToSymbolMapping().get(currentCase.get(e-j));
					}
					String feature = Arrays.toString(featureSymbols);
					int target = currentCase.get(e);
					if (currentnGramCounter.containsKey(feature))
						currentnGramCounter.get(feature)[target] += caseMultiplicity;
					else {
						int[] tmp = new int[symbolNum];
						for (int tmpI = 0 ; tmpI < symbolNum ; tmpI++)
							tmp[tmpI] = 0;
						tmp[target] = caseMultiplicity;
						currentnGramCounter.put(feature, tmp);
					}
				}
			}
			nGramCounters.add(currentnGramCounter);
		}
		
		// use counters to create distributions
		int intitalSumTmp = 0;
		intitalDistribution = new double[symbolNum];
		for (int i = 0 ; i < symbolNum ; i++) {
			intitalSumTmp += intitalDistributionCounter[i];
			intitalDistribution[i] = 0;
		}
		for (int i = 0 ; i < symbolNum ; i++) 
			intitalDistribution[i] = ((double)intitalDistributionCounter[i]) / ((double)intitalSumTmp);
					
		for (HashMap<String,int[]> nGramCounter : nGramCounters) {
			HashMap<String,double[]> currentnGram = new HashMap<String,double[]>();
			for (String feature : nGramCounter.keySet()) {
				double[] dist = new double[symbolNum];
				int[] counter = nGramCounter.get(feature);
				int totalSum = 0;
				for (int i = 0 ; i < symbolNum ; i++)
					totalSum += counter[i];
				for (int i = 0 ; i < symbolNum ; i++)
					dist[i] = ((double)counter[i]) / ((double)totalSum);
				currentnGram.put(feature, dist);
			}
			this.nGrams.add(currentnGram);
		}
	}

	@Override
	public double[] predictProbability(String[] history) {
		
		HashMap<String,double[]> nGram = null;
		String[] feature = null;
		if (history.length > n) {
			nGram = this.nGrams.get(n-1);
			feature = new String[n];
		}
		else if (history.length == 0) {
			return this.intitalDistribution;
		}
		else {
			nGram = this.nGrams.get(history.length-1);
			feature = new String[history.length];
		}
		for (int i = 0 ; i < feature.length ; i++) {
			feature[feature.length-1-i] = history[history.length-1-i];
		}
		
		double[] prediction = nGram.get(Arrays.toString(feature));
		if (prediction == null) {
			prediction = new double[log.getNumberOfUniqueSymbols()];
			for (int i = 0 ; i < log.getNumberOfUniqueSymbols() ; i++) {
				prediction[i] = 1.0 / ((double)log.getNumberOfUniqueSymbols());
			}
		}
		
		return prediction;
	}
	
	public void printnGrams() {
		for (HashMap<String,double[]> nGram : this.nGrams) {
			for (String key : nGram.keySet()) {
				String tmp = key + " -> ";
				double[] targetDistribution = nGram.get(key);
				for (int i = 0 ; i < targetDistribution.length ; i++)
					tmp += String.format("%s : %f | ",log.getNumberToSymbolMapping().get(i), targetDistribution[i]);
				System.out.println(tmp);
			}
		}
	}
	
	
}
