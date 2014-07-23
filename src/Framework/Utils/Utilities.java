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

package Framework.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import Framework.Models.Log.NonRedundantLog;

public class Utilities {
	
	// add two logarithmic probabilities
	// i.e., caluclates log(x+y) if a=log(x) and b=log(y) are given 
	public static double addLogSpace (double a, double b) {
		if (a == Double.NEGATIVE_INFINITY) 
			return b;
		else if (b == Double.NEGATIVE_INFINITY)
			return a;
		else if (b < a)
			return a + Math.log (1 + Math.exp(b-a));
		else
			return b + Math.log (1 + Math.exp(a-b));
	}
	
	public static double log2(double a) {
		return Math.log(a) / Math.log(2);
	}
	
	// maps symbols to numerical values
	// interger values start at 0
	public static HashMap<String,Integer> createNumberSymbolMapping(ArrayList<ArrayList<String>> log, String terminationSymbol) {
		
		// intialize counter. will be increased in the following
		int numberOfUniqueSymbols = 0;
		
		// initialize hashmap
		HashMap<String,Integer> symbol2number = new HashMap<String,Integer>();
		
		// iterate cases to identify mapping
		for (int i = 0 ; i < log.size(); i++)
		{
			ArrayList<String> currentCase = log.get(i);
			// iterate symbols of each case
			for (int j = 0 ; j < currentCase.size() ; j++)
			{
				String s = currentCase.get(j);
				if ((symbol2number.get(s) == null) && (s.equals(terminationSymbol) == false))
				{
					symbol2number.put(s, numberOfUniqueSymbols);
					numberOfUniqueSymbols++;
				}
			}
		}
		
		// termination symbol should get highest numerical value and is appended now
		symbol2number.put(terminationSymbol, numberOfUniqueSymbols);
		
		return symbol2number;
	}
	
	// creates a log with validationSet attached. Uses "learningFraction" for learning, rest for validation
	public static NonRedundantLog createTrainValidationLogs(String name, ArrayList<ArrayList<String>> samples, float learningFraction) {
		
		// random permutation
		Collections.shuffle(samples);
		
		int N = samples.size();
		int learningCases = Math.round(N * learningFraction);
		
		// create mapping of symbols and numbers
		HashMap<String,Integer> sym2num = Utilities.createNumberSymbolMapping(samples, NonRedundantLog.TERMINATION_SYMBOL);
		
		
		// get learning samples
		ArrayList<ArrayList<String>> learningSamples = new ArrayList<ArrayList<String>>();
		for (int i = 0 ; i < learningCases ; i++) {
			learningSamples.add(samples.get(i));
		}
		// get validation samples
		ArrayList<ArrayList<String>> validationSamples = new ArrayList<ArrayList<String>>();
		for (int i = learningCases ; i < N ; i++) {
			validationSamples.add(samples.get(i));
		}
		
		// create logs
		NonRedundantLog result = new NonRedundantLog(name, learningSamples, sym2num);
		NonRedundantLog validationLog = new NonRedundantLog(name, validationSamples, sym2num);
		result.setValidationLog(validationLog);
		
		return result;
	}
	
	// creates a log with validation and test set attached. fractions are given by the fractions-array (learning / validation / testing), must sum to one
	public static NonRedundantLog createTrainValidationTestLogs(String name, ArrayList<ArrayList<String>> samples, float[] fractions) {
		
		// random permutation
		Collections.shuffle(samples);
		
		int N = samples.size();
		int learningCases = Math.round(N * fractions[0]);
		int validationCases = Math.round(N * fractions[1]);
		
		// create mapping of symbols and numbers
		HashMap<String,Integer> sym2num = Utilities.createNumberSymbolMapping(samples, NonRedundantLog.TERMINATION_SYMBOL);
		
		
		// get learning samples
		ArrayList<ArrayList<String>> learningSamples = new ArrayList<ArrayList<String>>();
		for (int i = 0 ; i < learningCases ; i++) {
			learningSamples.add(samples.get(i));
		}
		// get validation samples
		ArrayList<ArrayList<String>> validationSamples = new ArrayList<ArrayList<String>>();
		for (int i = learningCases ; i < learningCases + validationCases ; i++) {
			validationSamples.add(samples.get(i));
		}
		// get test samples
		ArrayList<ArrayList<String>> testSamples = new ArrayList<ArrayList<String>>();
		for (int i = learningCases + validationCases ; i < N ; i++) {
			testSamples.add(samples.get(i));
		}
		
		// create logs
		NonRedundantLog result = new NonRedundantLog(name, learningSamples, sym2num);
		NonRedundantLog validationLog = new NonRedundantLog(name, validationSamples, sym2num);
		NonRedundantLog testLog = new NonRedundantLog(name, testSamples, sym2num);
		result.setValidationLog(validationLog);
		result.setTestLog(testLog);
		
		return result;
	}
	
	// returns the unqiue symbols in this sample
	public static HashSet<String> getUniqueSymbols(ArrayList<ArrayList<String>> samples) {
		HashSet<String> symbols = new HashSet<String>();
		for (ArrayList<String> sample : samples) {
			for (String s : sample) {
				symbols.add(s);
			}
		}
		return symbols;
	}
	
	// changes each symbol in the sample with probability "noiseProbability" to a random one
	public static ArrayList<ArrayList<String>> applyNoise(ArrayList<ArrayList<String>> samples, double noiseProbability) {
		HashSet<String> symbolSet = Utilities.getUniqueSymbols(samples);
		String[] symbols = new String[symbolSet.size()]; 
		symbols = symbolSet.toArray(symbols);
		Random generator = new Random();
		
		ArrayList<ArrayList<String>> noisySampels = new ArrayList<ArrayList<String>>();
		for (ArrayList<String> sampleOrig : samples) {
			ArrayList<String> sampleNoise = new ArrayList<String>();
			noisySampels.add(sampleNoise);
			for (String s : sampleOrig) {
				if (Math.random() < noiseProbability) {
					// draw new random symbol
					int r = generator.nextInt(symbols.length);
					sampleNoise.add(symbols[r]);
				}
				else {
					// leave symbol s as it is
					sampleNoise.add(s);
				}
				
			}
		}
		return noisySampels;
	}
	
	// transforms the samples into numerical form w.r.t. to given mapping of symbols to numbers
	// the resulting 2-D array can be used with the PDIA sampler 
	public int[][] toNumericalSample(ArrayList<ArrayList<String>> samples, HashMap<String,Integer> sym2num) {
		int[][] result = new int[samples.size()][];
		int counter = 0;
		for (ArrayList<String> sample : samples) {
			result[counter] = new int[sample.size()];
			for (int i = 0 ; i < sample.size() ; i++)
				result[counter][i] = sym2num.get(sample.get(i));
		}
		return result;
	}
}
