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

public class Log
{
	// name of this log
	public String name;
	
	// the termination symbol included as the last observation in each log
	// indicates that a case has terminated
	// numerical value for termination symbol will always be the largest one
	public final String terminationSymbol = "TERMINATED";
	
	// indicates the number of cases in this log (a case is a single sequence originating from a process)
	private int cases;
	
	// indicates the total number of symbols, summed over all cases
	private int numberOfSymbols;
	
	// indicates the number of unique symbols contained in the log
	private int numberOfUniqueSymbols;
	
	// indicates the number of symbols in the longest case
	private int lengthOfLongestCase;
	
	// an arraylist containing the cases in their original (text-based) form
	private ArrayList<ArrayList<String>> originalLog = new ArrayList<ArrayList<String>>();
	
	// an arraylist containing the cases in a processed, numerical form (easier to use in EM algorithm)
	// each unique string is assinged a corresponding number 
	private ArrayList<ArrayList<Integer>> numericalLog = new ArrayList<ArrayList<Integer>>();
	
	// arraylists documenting the relations between original and numerical log
	private ArrayList<String> symbols = new ArrayList<String>();
	private ArrayList<Integer> numbers = new ArrayList<Integer>();
	
	
	// creates a new log given observations
	public Log(String name, ArrayList<ArrayList<String>> log)
	{	
		this.name = name;
		this.originalLog = log;
		this.cases = this.originalLog.size();
		this.appendTerminationSymbol(); // append termination symbol
		this.createNumericalLog(); // create numerical log and count symbols
		
		// determine number of symbols in longest case
		for (int i = 0 ; i < originalLog.size(); i++)
			if (this.lengthOfLongestCase < originalLog.get(i).size())
				this.lengthOfLongestCase = originalLog.get(i).size();
		
		// determine number of total symbols
		numberOfSymbols = 0;
		for (int i = 0 ; i < originalLog.size(); i++)
			numberOfSymbols += originalLog.get(i).size();
		
	}
	
	public int getNumberOfCases()
	{
		return this.cases;
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
		return this.originalLog.get(caseNumber).size();
	}
	
	public int getLengthOfLongestCase()
	{
		return this.lengthOfLongestCase;
	}
	
	
	public ArrayList<ArrayList<String>> getTextualLog()
	{
		return this.originalLog;
	}
	
	public String getTextualLogEntry(int c, int i)
	{
		return this.originalLog.get(c).get(i);
	}
	
	public ArrayList<ArrayList<Integer>> getNumericalLog()
	{
		return this.numericalLog;
	}
	
	public int getNumericalLogEntry(int c, int i)
	{
		return this.numericalLog.get(c).get(i);
	}
	
	public Map<String,Integer> getSymbolToNumberMapping()
	{
		Map<String,Integer> result = new HashMap<String, Integer>();
		for (int i = 0 ; i < symbols.size(); i++)
			result.put(symbols.get(i),numbers.get(i));
		return result;
	}
	
	public Map<Integer,String> getNumberToSymbolMapping()
	{
		Map<Integer,String> result = new HashMap<Integer,String>();
		for (int i = 0 ; i < symbols.size(); i++)
			result.put(numbers.get(i),symbols.get(i));
		return result;
	}
	
	
	// appends the termination symbol at the end of each case
	private void appendTerminationSymbol()
	{
		// iterate over all cases and append symbol
		for (int i = 0 ; i < originalLog.size(); i++)
			originalLog.get(i).add(this.terminationSymbol);
	}
	
	// counts the number of unique symbols and assigns unqiue integers to them
	private void createNumericalLog()
	{
		// temporarly saves symbols to a set (can determine quickly if we already found it)
		Set<String> symbolsTmp = new HashSet<String>();
		
		// Strategy: iterate over all observations and count smybols to create a mapping
		// Then create the numerical log with this mapping
		
		// iterate cases to identify mapping
		for (int i = 0 ; i < originalLog.size(); i++)
		{
			ArrayList<String> currentCase = originalLog.get(i);
			// iterate symbols of each case
			for (int j = 0 ; j < currentCase.size() ; j++)
			{
				String s = currentCase.get(j);
				if ((symbolsTmp.contains(s) == false) && (s.equals(this.terminationSymbol) == false))
				{
					symbolsTmp.add(s); // add symbol to temporary set
					this.symbols.add(s); // add symbol to arraylist
					this.numbers.add(++this.numberOfUniqueSymbols); // add number to arraylist, then increase
				}
			}
		}
		// termination symbol should get highest numerical value and is appended now
		this.symbols.add(this.terminationSymbol);
		this.numbers.add(++this.numberOfUniqueSymbols);
		
		// now create the numerical log
		Map<String,Integer> map = getSymbolToNumberMapping();
		for (int i = 0 ; i < originalLog.size(); i++)
		{
			ArrayList<String> currentCaseText = originalLog.get(i);
			ArrayList<Integer> currentCaseInt = new ArrayList<Integer>();
			numericalLog.add(currentCaseInt);
			// iterate symbols of each case
			for (int j = 0 ; j < currentCaseText.size() ; j++)
			{
				currentCaseInt.add(map.get(currentCaseText.get(j)));
			}
		}
	}
	
	// delivers a string representing this log
	public String toString()
	{
		String result = "";
		
		result += "Log \"" + this.name + "\" with " + this.cases + " cases and " + this.numberOfUniqueSymbols + " unique symbols. Longest case: " + this.lengthOfLongestCase + " \n";
		for (int i = 0 ; i < cases ; i++)
		{
			result += "Case" + (i+1) + " :";
			ArrayList<String> currentCaseText = this.originalLog.get(i);
			ArrayList<Integer> currentCaseInt = this.numericalLog.get(i);
			for (int j = 0 ; j < currentCaseText.size() ; j++)
			{
				result += " " + currentCaseText.get(j) + "(" + currentCaseInt.get(j) + ")";
			}
			result += "\n";
		}
		
		return result;
	}
	
}