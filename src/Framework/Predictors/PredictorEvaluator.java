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
import java.util.Set;

import Framework.Models.Log.NonRedundantLog;

public class PredictorEvaluator {
	
	Set<String> allSymbols = null;
	
	public HashMap<Predictor,Double> averageAccuracy = new HashMap<Predictor,Double>();
	public HashMap<Predictor,ArrayList<Double>> accuraciesByPosition = new HashMap<Predictor,ArrayList<Double>>();
	public ArrayList<Integer> sizeByPosition = new ArrayList<Integer>();
	
	public HashMap<Predictor,Double> averageSensitivity  = new HashMap<Predictor,Double>(); // average over all symbols
	public HashMap<Predictor,HashMap<String,Double>> sensitivityBySymbol = new HashMap<Predictor,HashMap<String,Double>>();
	public HashMap<Predictor,HashMap<String,Integer>> truePositivesBySymbol = new HashMap<Predictor,HashMap<String,Integer>>();
	
	public HashMap<Predictor,Double> averageSpecitivity  = new HashMap<Predictor,Double>(); // average over all symbols
	public HashMap<Predictor,HashMap<String,Double>> specitivityBySymbol = new HashMap<Predictor,HashMap<String,Double>>();
	public HashMap<Predictor,HashMap<String,Integer>> falsePositivesBySymbol = new HashMap<Predictor,HashMap<String,Integer>>();
	public HashMap<String,Integer> positivesBySymbol = new HashMap<String,Integer>();
	
	public HashMap<Predictor,Double> crossEntropies = new HashMap<Predictor,Double>();
	
	public int sizeTotal = 0;
	
	public ArrayList<Predictor> predictors = null;
	
	public PredictorEvaluator(NonRedundantLog testLog, ArrayList<Predictor> predictors) {
		
		this.predictors = predictors;
		
		this.allSymbols = testLog.getSymbolToNumberMapping().keySet();
		
		for (String symbol : allSymbols)
			positivesBySymbol.put(symbol, 0);
		
		for (Predictor pred : predictors) {
			accuraciesByPosition.put(pred, new ArrayList<Double>());
			sensitivityBySymbol.put(pred, new HashMap<String,Double>());
			specitivityBySymbol.put(pred, new HashMap<String,Double>());
			truePositivesBySymbol.put(pred, new HashMap<String,Integer>());
			falsePositivesBySymbol.put(pred, new HashMap<String,Integer>());
			for (String symbol : allSymbols) {
				truePositivesBySymbol.get(pred).put(symbol, 0);
				falsePositivesBySymbol.get(pred).put(symbol, 0);
			}
		}
		
		int maxLength = testLog.getLengthOfLongestCase();
		// iterate through the log by position
		for (int n = 1 ; n < maxLength ; n++) {
			PredictionDataset data = new PredictionDataset(testLog, n);
			// record size at this position and update sizes for symbols
			sizeByPosition.add(data.size);
			HashMap<String,Integer> currentSymbolCounts = data.getTargetSymbolCounts();
			for (String symbol : currentSymbolCounts.keySet())
				positivesBySymbol.put(symbol, positivesBySymbol.get(symbol) + currentSymbolCounts.get(symbol));
			this.sizeTotal += data.size;
			
			// iterate the predictors to generate their scores
			for (Predictor pred : predictors) {
				// score accuracy for this predictor and add value to the list  
				accuraciesByPosition.get(pred).add(pred.scoreAccuracy(data));
				// count true positives / false positives
				HashMap<String,Integer> currentTruePositivesBySymbol = pred.getTruePositivesBySymbol(data);
				for (String symbol : currentTruePositivesBySymbol.keySet())
					truePositivesBySymbol.get(pred).put(symbol, truePositivesBySymbol.get(pred).get(symbol) + currentTruePositivesBySymbol.get(symbol));
				HashMap<String,Integer> currentFalsePositivesBySymbol = pred.getFalsePositivesBySymbol(data);
				for (String symbol : currentFalsePositivesBySymbol.keySet())
					falsePositivesBySymbol.get(pred).put(symbol, falsePositivesBySymbol.get(pred).get(symbol) + currentFalsePositivesBySymbol.get(symbol));
			}
		}
		// calculate final scores for each symbol
		for (Predictor pred: predictors)
			for (String symbol : allSymbols) {
				if (symbol.equals("SUBMITTED(complete)"))
					System.out.println("bin da");
				int currentTruePositives = truePositivesBySymbol.get(pred).get(symbol);
				int currentFalsePositvies = falsePositivesBySymbol.get(pred).get(symbol);
				int currentPositives = positivesBySymbol.get(symbol);
				int currentNegatives = this.sizeTotal - currentPositives;
				sensitivityBySymbol.get(pred).put(symbol, ((double)currentTruePositives) / ((double)currentPositives) );
				specitivityBySymbol.get(pred).put(symbol, ((double)(currentNegatives - currentFalsePositvies)) / ((double)currentNegatives) );
			}
		
		// calculate averages
		for (Predictor pred : predictors) {
			// calculate average accuracy regardless of position
			double numerator = 0.0;
			double denominator = 0.0;
			for (int i = 0 ; i < accuraciesByPosition.get(pred).size() ; i++) {
				double currentSize = (double)sizeByPosition.get(i);
				double currentAccuracy = accuraciesByPosition.get(pred).get(i);
				numerator += currentAccuracy * currentSize;
				denominator += currentSize;
			}
			this.averageAccuracy.put(pred, numerator/denominator);
			
			// average sensitivity over all symbols
			numerator = 0.0;
			denominator = 0.0;
			for (String symbol : this.sensitivityBySymbol.get(pred).keySet()) {
				if (Double.isNaN(this.sensitivityBySymbol.get(pred).get(symbol)))
					continue; // continue if sensitivity is NaN. can  happen if it is always the first symbol in the process (i.e., if it is never to be predicted)
				numerator += this.sensitivityBySymbol.get(pred).get(symbol);
				denominator += 1;
			}
			this.averageSensitivity.put(pred, numerator/denominator);
			
			// average specitivity over all symbols
			numerator = 0.0;
			denominator = 0.0;
			for (String symbol : this.specitivityBySymbol.get(pred).keySet()) {
				if (Double.isNaN(this.specitivityBySymbol.get(pred).get(symbol)))
					continue;
				numerator += this.specitivityBySymbol.get(pred).get(symbol);
				denominator += 1;
			}
			this.averageSpecitivity.put(pred, numerator/denominator);
			
			// cross entropies
			this.crossEntropies.put(pred, pred.getCrossEntropy(testLog));
		}
		
	}
	
	public String toCsv() {
		StringBuffer result = new StringBuffer();
		
		// append header
		result.append("Algorithm;AvgAcc;AvgSens;AvgSpec;CE");
		for (int position = 1 ; position <= this.sizeByPosition.size() ; position++)
			result.append(";Acc_P_" + position);
		for (String symbol : this.allSymbols)
			result.append(";Sens_" + symbol);
		for (String symbol : this.allSymbols)
			result.append(";Spec_" + symbol);
		result.append("\n");
		
		// append one row for each predictor
		for (Predictor pred : predictors) {
			result.append(pred.name + ";" + this.averageAccuracy.get(pred) + ";" + this.averageSensitivity.get(pred) + ";" + this.averageSpecitivity.get(pred) + ";" + this.crossEntropies.get(pred));
		for (int position = 0 ; position < accuraciesByPosition.get(pred).size() ; position++)
			result.append(";" + this.accuraciesByPosition.get(pred).get(position));
		for (String symbol : this.allSymbols)
			result.append(";" + this.sensitivityBySymbol.get(pred).get(symbol));
		for (String symbol : this.allSymbols)
			result.append(";" + this.specitivityBySymbol.get(pred).get(symbol));
		result.append("\n");
		}
		return result.toString().replace(".", ",");
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		
		result.append(String.format("Total Size = %d \n", this.sizeTotal));
		
		for (Predictor pred : predictors) {
			
			result.append(String.format("%15s Accs (%4.2f) by Position: ", pred.name, this.averageAccuracy.get(pred)));
			for (int i = 0 ; i < accuraciesByPosition.get(pred).size() ; i++)
				result.append(String.format("%4.2f | ", accuraciesByPosition.get(pred).get(i)));
			result.append("\n");
		}
		
		for (Predictor pred : predictors) {
			
			result.append(String.format("%15s Sensitivity (%4.2f) by Symbol: ", pred.name, this.averageSensitivity.get(pred)));
			for (String symbol : sensitivityBySymbol.get(pred).keySet())
				result.append(String.format("%s : %4.2f | ", symbol, sensitivityBySymbol.get(pred).get(symbol)));
			result.append("\n");
		}
		
		for (Predictor pred : predictors) {
			
			result.append(String.format("%15s Specitivity (%4.2f) by Symbol: ", pred.name, this.averageSpecitivity.get(pred)));
			for (String symbol : specitivityBySymbol.get(pred).keySet())
				result.append(String.format("%s : %4.2f | ", symbol, specitivityBySymbol.get(pred).get(symbol)));
			result.append("\n");
		}
		
		result.append("\n");
		for (Predictor pred : predictors) {
			result.append(pred.name + " CE: " + this.crossEntropies.get(pred) + "\n");
		}
		
		
		return result.toString();
	}
	
}
