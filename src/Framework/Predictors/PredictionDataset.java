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

public class PredictionDataset {
	
	public ArrayList<ArrayList<String>> features = new ArrayList<ArrayList<String>>();
	public ArrayList<String> targets = new ArrayList<String>();
	public ArrayList<Integer> weights = new ArrayList<Integer>();
	public int size = 0;
	
	// creates a dataset with the first n symbols of each case as features and the symbol n+1 as target variable
	public PredictionDataset(NonRedundantLog log, int n) {
		for (int c = 0 ; c < log.getNumberOfUniqueCases() ; c++) {
			ArrayList<Integer> currentCase = log.getCase(c);
			if (currentCase.size() > n) {
				ArrayList<String> currentFeature = new ArrayList<String>();
				for (int i = 0 ; i < n ; i++) {
					currentFeature.add(log.getNumberToSymbolMapping().get(currentCase.get(i)));
				}
				features.add(currentFeature);
				targets.add(log.getNumberToSymbolMapping().get(currentCase.get(n)));
				weights.add(log.getCaseMultiplicity(c));
				this.size += log.getCaseMultiplicity(c);
			}
		}
	}
	
	public HashMap<String,Integer> getTargetSymbolCounts() {
		HashMap<String,Integer> result = new HashMap<String,Integer>();
		for (int i = 0 ; i < targets.size() ; i++) {
			if (result.containsKey(targets.get(i)))
					result.put(targets.get(i), result.get(targets.get(i)) + weights.get(i));
			else
				result.put(targets.get(i), weights.get(i));
		}
		return result;
	}
	
}
