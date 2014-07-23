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

package Framework.Models.Log;

import java.util.*;

public class NonRedundantLog
{
	// name of this log
	public String name;
	
	// the termination symbol included as the last observation in each log
	// indicates that a case has terminated
	// numerical value for termination symbol will always be the largest one
	public final static String TERMINATION_SYMBOL = "TERMINATED";
	
	// indicates the number of cases in this log (a case is a single sequence originating from a process)
	private int cases;
	
	// indicates the number of unique cases in this log
	private int numberOfUniqueCases;
	
	// indicates the total number of symbols, summed over all cases
	private int numberOfSymbols;
	
	// indicates the number of unique symbols contained in the log
	private int numberOfUniqueSymbols;
	
	// indicates the number of symbols in the longest case
	private int lengthOfLongestCase;
	
	// an arraylist containing the unique cases in a processed, numerical form (easier to use in EM algorithm)
	// each unique symbol is assinged a corresponding number 
	private ArrayList<ArrayList<Integer>> numericalLog = new ArrayList<ArrayList<Integer>>();
	
	// an arraylist documenting the multiplicity of each unique case
	private HashMap<ArrayList<Integer>,Integer> caseMultiplicity = new HashMap<ArrayList<Integer>,Integer>();
	
	// arraylists documenting the relations between original and numerical log
	private HashMap<String,Integer> symbol2number;
	private HashMap<Integer,String> number2symbol;
	
	// hashmap storing for each symbol the frequency with which it appears in the log
	private HashMap<String,Integer> symbolFrequency;
	
	// log used for validation
	public NonRedundantLog validationLog;
	
	// log used for final testing
	public NonRedundantLog testLog;

	
	
	// creates a new log given observations
	public NonRedundantLog(String name, ArrayList<ArrayList<String>> log)
	{	
		this(name, log, null);
	}
	
	public NonRedundantLog(String name, ArrayList<ArrayList<String>> log, HashMap<String,Integer> sym2Num)
	{	
		this.name = name;
		this.cases = log.size();
		if (sym2Num == null) {
			this.initializeNumberSymbolMappings(log);
		}
		else {
			// create reverse mapping
			this.symbol2number = sym2Num;
			this.number2symbol = new HashMap<Integer,String>();
			for (String symbol : symbol2number.keySet()) {
				number2symbol.put(symbol2number.get(symbol), symbol);
			}
			// set number of unique symbols
			this.numberOfUniqueSymbols = symbol2number.size();
			
		}
		this.initializeLog(log); // create numerical log and count symbols
	}
	
	public int getNumberOfCases()
	{
		return this.cases;
	}
	
	public int getNumberOfUniqueCases()
	{
		return this.numberOfUniqueCases;
	}
	
	public int getNumberOfUniqueSymbols()
	{
		return this.numberOfUniqueSymbols;
	}
	
	public int getNumberOfSymbols()
	{
		return this.numberOfSymbols;
	}
	
	public int getLengthOfCase(int caseNumber)
	{
		return this.numericalLog.get(caseNumber).size();
	}
	
	public int getLengthOfLongestCase()
	{
		return this.lengthOfLongestCase;
	}
	
	public ArrayList<ArrayList<Integer>> getNumericalLog()
	{
		return this.numericalLog;
	}
	
	public int[][] getNumericalLogAsArray() {
		int[][] result = new int[this.getNumberOfCases()][];
		
		int caseNumber = 0;
		for (int n = 0 ; n < this.getNumberOfUniqueCases() ; n++) {
			ArrayList<Integer> curCase = this.getCase(n);
			for (int m = 0 ; m < this.getCaseMultiplicity(n) ; m++) {
				result[caseNumber] = new int[curCase.size()];
				for (int i = 0 ; i < curCase.size() ; i++) {
					result[caseNumber][i] = curCase.get(i);
				}
				caseNumber++;
			}
			
		}
		
		return result;
	}
	
	public ArrayList<Integer> getCase(int c) {
		return this.numericalLog.get(c);
	}
	
	public int getNumericalLogEntry(int c, int i)
	{
		return this.numericalLog.get(c).get(i);
	}
	
	public int getCaseMultiplicity(int c) {
		ArrayList<Integer> currentCase = this.getCase(c);
		return this.caseMultiplicity.get(currentCase);
	}
	
	public HashMap<Integer,String> getNumberToSymbolMapping() {
		return this.number2symbol;
	}
	
	public HashMap<String,Integer> getSymbolToNumberMapping() {
		return this.symbol2number;
	}
	
	
	// sets the validation set for this log
	// ensures that all sets of symbols of both logs are merged appropriately
	public void setValidationLog(NonRedundantLog valLog) {
		if (this.validationLog != null) {
			// there is a validation log already
			return;
		}
		
		Set<String> currentSymbols = this.symbol2number.keySet();
		Set<String> validationLogSymbols = valLog.symbol2number.keySet();
		
		for (String s : currentSymbols) {
			if (validationLogSymbols.contains(s) == false)
				valLog.addSymbol(s);
		}
		
		for (String s : validationLogSymbols) {
			if (currentSymbols.contains(s) == false)
				this.addSymbol(s);
		}
		
		this.validationLog = valLog;
		
	}
	
	// sets the test set for this log
	// ensures that all sets of symbols of both logs are merged appropriately
	public void setTestLog(NonRedundantLog testLog) {
		if (this.testLog != null) {
			// there is a validation log already
			return;
		}
		
		Set<String> currentSymbols = this.symbol2number.keySet();
		Set<String> testLogSymbols = testLog.symbol2number.keySet();
		
		for (String s : currentSymbols) {
			if (testLogSymbols.contains(s) == false)
				testLog.addSymbol(s);
		}
		
		for (String s : testLogSymbols) {
			if (currentSymbols.contains(s) == false)
				this.addSymbol(s);
		}
		
		this.testLog = testLog;
		
	}
	
	public NonRedundantLog getValidationLog() {
		return this.validationLog;
	}
	
	public NonRedundantLog getTestLog() {
		return this.testLog;
	}
	
	// adds a symbol to this log
	// note that this symbol is not found in the log data! It is only included as a possible symbols that is never observed though!
	public void addSymbol(String symbol) {
		if (this.symbol2number.get(symbol) != null) {
			// the symbol exists already
			return;
		}
		
		// overwrite termination symbol
		numberOfUniqueSymbols--;
		this.symbol2number.put(symbol,numberOfUniqueSymbols);
		this.number2symbol.put(numberOfUniqueSymbols, symbol);
		this.symbolFrequency.put(symbol,0);
		numberOfUniqueSymbols++;
		
		// termination symbol should get highest numerical value and is appended now
		this.symbol2number.put(NonRedundantLog.TERMINATION_SYMBOL, this.numberOfUniqueSymbols);
		this.number2symbol.put(this.numberOfUniqueSymbols, NonRedundantLog.TERMINATION_SYMBOL);
		this.numberOfUniqueSymbols++;
	}
	
	
	// determines the number of unique symbols and maps symbols to numerical values
	private void initializeNumberSymbolMappings(ArrayList<ArrayList<String>> log) {
		
		// intialize counter. will be increased in the following
		this.numberOfUniqueSymbols = 0;
		
		// initialize hashmaps
		this.symbol2number = new HashMap<String,Integer>();
		this.number2symbol = new HashMap<Integer,String>();
		
		// iterate cases to identify mapping
		for (int i = 0 ; i < log.size(); i++)
		{
			ArrayList<String> currentCase = log.get(i);
			// iterate symbols of each case
			for (int j = 0 ; j < currentCase.size() ; j++)
			{
				String s = currentCase.get(j);
				if ((symbol2number.get(s) == null) && (s.equals(NonRedundantLog.TERMINATION_SYMBOL) == false))
				{
					this.symbol2number.put(s, this.numberOfUniqueSymbols);
					this.number2symbol.put(this.numberOfUniqueSymbols, s);
					this.numberOfUniqueSymbols++;
				}
			}
		}
		
		// termination symbol should get highest numerical value and is appended now
		this.symbol2number.put(NonRedundantLog.TERMINATION_SYMBOL, this.numberOfUniqueSymbols);
		this.number2symbol.put(this.numberOfUniqueSymbols, NonRedundantLog.TERMINATION_SYMBOL);
		this.numberOfUniqueSymbols++;		
	}
	
	// determines frequencies of symbols
	private void initializeSymbolFrequencies(ArrayList<ArrayList<String>> log) {
		this.symbolFrequency = new HashMap<String,Integer>();
		
		for (String s : this.symbol2number.keySet())
			this.symbolFrequency.put(s, new Integer(0));
		
		// iterate cases to identify mapping
		for (int i = 0 ; i < log.size(); i++)
		{
			ArrayList<String> currentCase = log.get(i);
			// iterate symbols of each case
			for (int j = 0 ; j < currentCase.size() ; j++)
			{
				String s = currentCase.get(j);
				this.symbolFrequency.put(s, this.symbolFrequency.get(s)+1);
			}
		}
	}
	
	// creates a numerical version of the log
	// duplicate cases are merged and weighted with their multiplicity
	private void createNumericalLog(ArrayList<ArrayList<String>> log) {
		
		this.numberOfUniqueCases = 0;
		
		// temporary data structures to identify redundant cases
		HashMap<String,ArrayList<Integer>> cases2logEntries = new HashMap<String,ArrayList<Integer>>();
		String tmpCase;
		
		for (int i = 0 ; i < log.size(); i++)
		{
			
			// get current case in textual form and construct numerical form
			ArrayList<String> currentCaseText = log.get(i);
			ArrayList<Integer> currentCaseInt = new ArrayList<Integer>();
			
			// construct numerical form and "characteristic string" (tmpCase)
			tmpCase = "";
			for (int j = 0 ; j < currentCaseText.size() ; j++)
			{
				int numericalEntry = this.symbol2number.get(currentCaseText.get(j));
				currentCaseInt.add(numericalEntry);
				tmpCase += numericalEntry;
			}
			
			ArrayList<Integer> currentCase = cases2logEntries.get(tmpCase);
			// if current case did not exist yet, create it
			if (currentCase == null) {
				// append termination symbol
				currentCaseInt.add(this.symbol2number.get(NonRedundantLog.TERMINATION_SYMBOL));
				// add case to log
				this.numericalLog.add(currentCaseInt);
				this.caseMultiplicity.put(currentCaseInt, new Integer(1));
				// put case to temporary hashmap to document that it exists
				cases2logEntries.put(tmpCase, currentCaseInt);
				// increase number of unique cases
				this.numberOfUniqueCases++;
			}
			// if current case exists already, increase its multiplicity
			else {
				Integer multiplicity = this.caseMultiplicity.get(currentCase);
				multiplicity++;
				this.caseMultiplicity.put(currentCase, multiplicity);
			}
		}
	}
	
	// counts the number of unique symbols and assigns unique integers to them
	private void initializeLog(ArrayList<ArrayList<String>> log)
	{
		this.initializeSymbolFrequencies(log);
		this.createNumericalLog(log);
		
		// determine number of symbols in longest case
		for (int i = 0 ; i < this.numericalLog.size(); i++)
			if (this.lengthOfLongestCase < this.getLengthOfCase(i))
				this.lengthOfLongestCase = this.getLengthOfCase(i);
		
		// determine number of total symbols
		this.numberOfSymbols = 0;
		for (int i = 0 ; i < log.size(); i++)
			this.numberOfSymbols += log.get(i).size();
	}
	
	public String toMxml() {
		StringBuffer result = new StringBuffer();
		result.append("<WorkflowLog xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://is.tm.tue.nl/research/processmining/WorkflowLog.xsd\">\n");
		result.append(String.format("<Process id=\"%s\">\n",this.name));
		for (int i = 0 ; i < this.numberOfUniqueCases ; i++)
		{
			int caseMultiplicity = this.getCaseMultiplicity(i);
			ArrayList<Integer> currentCase = this.numericalLog.get(i);
			
			for (int j = 0 ; j < caseMultiplicity ; j++) {
				result.append(String.format("<ProcessInstance id=\"%s\">\n",i + "(" + j + ")"));
				
				for (int k = 0 ; k < currentCase.size() ; k++) {
					String[] event = this.number2symbol.get(currentCase.get(k)).split("\\(");
					if (event[0].equals(NonRedundantLog.TERMINATION_SYMBOL) == false) {
						String eventType = event[0];
						String eventLifecycleType = event[1].substring(0,event[1].length()-1);
						result.append("<AuditTrailEntry>\n");
						result.append(String.format("<WorkflowModelElement>%s</WorkflowModelElement>\n", eventType));
						result.append(String.format("<EventType>%s</EventType>\n", eventLifecycleType));
						result.append("</AuditTrailEntry>\n");
					}
				}
				result.append("</ProcessInstance>\n");
			}
		}
		result.append("</Process>\n");
		result.append("</WorkflowLog>");
		
		return result.toString();
	}
	
	// delivers a string representing this log
	public String toString()
	{
		String result = "";
		
		result += "Log \"" + this.name + "\" with " + this.cases + " cases (" + this.numberOfUniqueCases + " unique) and " + this.numberOfUniqueSymbols + " unique symbols. Longest case: " + this.lengthOfLongestCase + " \n";
		for (int i = 0 ; i < this.numberOfUniqueCases ; i++)
		{
			result += "Case" + (i+1) + " :";
			ArrayList<Integer> currentCase = this.numericalLog.get(i);
			for (int j = 0 ; j < currentCase.size() ; j++)
			{
				result += " " + this.number2symbol.get(currentCase.get(j)) + "(" + currentCase.get(j) + ")";
			}
			result += " Multiplicity: " + this.caseMultiplicity.get(currentCase);
			result += "\n";
		}
		
		return result;
	}
	
	public String toSummaryString() {
		String result = "";
		result += "Log \"" + this.name + "\" with " + this.cases + " cases (" + this.numberOfUniqueCases + " unique) and " + this.numberOfUniqueSymbols + " unique symbols. Longest case: " + this.lengthOfLongestCase + " \n";
		result += "Symbol frequencies:\n";
		for (String s : this.symbolFrequency.keySet()) {
			result += s + ":" + this.symbolFrequency.get(s) + "\n";
		}
		return result;
	}
	
}