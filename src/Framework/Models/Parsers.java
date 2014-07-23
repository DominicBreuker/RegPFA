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

package Framework.Models;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import Framework.Models.PetriNet.PetriNet;
import Framework.Models.PetriNet.Place;
import Framework.Models.PetriNet.Transition;

public class Parsers {
	
	// ---------------------------- //
	// ----- PetriNet parsers ----- //
	// ---------------------------- //
	
	// reads a petri net from a simple string format. Example:
	// Name: MyPetriNet
	// # list of places and initial tokens (prefix "p" required)
	// p1 1
	// p2 0
	// p3 0
	// # list of transitions with labels (prefix t required)
	// t1 A
	// t2 B
	// t3 C
	// # list of arcs (first source, then target)
	// p1 -> t1
	// t1 -> p2
	// t1 -> p3
	public static PetriNet readPetriNetFromString(String s) throws Exception
	{
		ArrayList<String> lines = new ArrayList<String>(Arrays.asList(s.split("[\\r\\n]+")));
		
		String name = "Nameless Petri Net";
		ArrayList<String> places = new ArrayList<String>();
		ArrayList<Integer> placeMarkings = new ArrayList<Integer>();
		ArrayList<String> transitions = new ArrayList<String>();
		ArrayList<String> transitionLabels = new ArrayList<String>();
		ArrayList<String> arcSources = new ArrayList<String>();
		ArrayList<String> arcTargets = new ArrayList<String>();
		
		// parse all lines first
		for (String line : lines)
		{
			String[] tmp = line.split(" ");
			if (tmp[0].equals("Name:"))
			{
				name = tmp[1];
			}
			else if ((tmp.length == 2) && tmp[0].startsWith("p"))
			{
				places.add(tmp[0]);
				try
				{
					placeMarkings.add(Integer.parseInt(tmp[1].trim()));
				}
				catch (NumberFormatException e)
				{
					placeMarkings.add(0);
				}
			}
			else if ((tmp.length == 2) && tmp[0].startsWith("t"))
			{
				transitions.add(tmp[0]);
				transitionLabels.add(tmp[1]);
			}
			else if ((tmp.length == 3) && (tmp[1].equals("->")) && (places.contains(tmp[0]) || transitions.contains(tmp[0])) && (places.contains(tmp[2]) || transitions.contains(tmp[2])))
			{
				arcSources.add(tmp[0]);
				arcTargets.add(tmp[2]);
			}
			else if (tmp[0].startsWith("#"))
			{
				// comment - do nothing
			}
			else
			{
				throw new Exception("Error parsing Petri Net! problematic line: " + line);
			}
		}
		
		// then construct petri net
		
		PetriNet result = new PetriNet(name);
		
		for (int i = 0 ; i < places.size() ; i++)
		{
			result.addPlace(places.get(i), placeMarkings.get(i));
		}
		for (int i = 0 ; i < transitions.size() ; i++)
		{
			result.addTransition(transitions.get(i), transitionLabels.get(i));
		}
		for (int i = 0 ; i < arcSources.size() ; i++)
		{
			if (arcSources.get(i).startsWith("p"))
			{
				result.getTransitionByName(arcTargets.get(i)).addInputPlace(arcSources.get(i));
			}
			else
			{
				result.getTransitionByName(arcSources.get(i)).addOutputPlace(arcTargets.get(i));
			}
		}
		
		return result;
	}
	
	public static PetriNet readPetriNetFromDOTFile(File f)
	{
		//////// System.out.println("Reading: " + f.getPath() + " --- " + f.getName());
		PetriNet result = new PetriNet(f.getName());
		try
		{
			// prescan file: check for lines defining places, transitions and arcs
			List<String> contents = FileUtils.readLines(f);
			ArrayList<String> places = new ArrayList<String>();
			ArrayList<String> transitions = new ArrayList<String>();
			ArrayList<String> arcs = new ArrayList<String>();
			for (String line : contents)
			{
				///////////System.out.println("1: " + line);
				// remove the "nunknown:normal" stuff from the labels
				line = line.replace("\\nunknown:normal","");
				
				String[] tmp = line.split(" ");
				if (!(tmp[0].equals("digraph") || tmp[0].equals("edge") || tmp[0].equals("node") || tmp[0].equals("}")))
				{
					if (tmp.length > 1 && tmp[1].startsWith("->"))
						arcs.add(line);
					else
					{
						if (tmp[0].startsWith("t"))
							transitions.add(line);
						else if (tmp[0].startsWith("p"))
							places.add(line);
						else if (tmp[0].startsWith("subgraph"))
							; // subgraphs group duplicate transitions --> ignore them
						else if (tmp[0].startsWith("//"))
							; // these lines seem to be comments -> ignore them
						else
						{
							// unknown format encountered...
							throw new Exception("ERROR parsing file:" + line);
						}
					}
				}
			}
			
			// construct petri net according to prescanned lines
			for (String place : places)
			{
				String name = place.split(" ")[0];
				result.addPlace(name);
				// check if label is a "." if yes, interpret is as an initial marking of one token for this place 
				if (place.contains("label=\"")) {
					String label = place.split("label=\"")[1].split("\"")[0];
					if (label.equals(".")) {
						result.getPlaceByName(name).initialMarking = 1;
					}
				}
			}
			for (String transition : transitions)
			{
				String name = transition.split(" ")[0];
				String label = transition.split("label=\"")[1].split("\"")[0];
				if (label.equals(""))
					label = result.invisibleSymbol;
				result.addTransition(name, label);
			}
			for (String arc : arcs)
			{
				if (arc.startsWith("t"))
				{
					String t = arc.split(" -> ")[0];
					String p = arc.split(" -> ")[1].split("\\[")[0];
					result.getTransitionByName(t).addOutputPlace(p);
				}
				else
				{
					String p = arc.split(" -> ")[0];
					String t = arc.split(" -> ")[1].split("\\[")[0];
					result.getTransitionByName(t).addInputPlace(p);
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("ERROR while processing: " + f.getName());
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static PetriNet readPetriNetFromPetrifyFile(File f, HashMap<String,String> number2symbol)
	{
		PetriNet result = new PetriNet(f.getName());
		try
		{
			// prescan file: check for lines defining places, transitions and arcs
			List<String> contents = FileUtils.readLines(f);
			Set<String> places = new HashSet<String>();
			Set<String> markedPlaces = new HashSet<String>();
			Set<String> transitions = new HashSet<String>();
			Set<String> arcsPT= new HashSet<String>();
			Set<String> arcsTP = new HashSet<String>();
			
			// indicates the section of the file we are currently in
			// 0 = header
			// 1 = graph specification section
			// 2 = end
			int currentArea = 0;
			
			// contains the tokens of which a line consists
			String[] tokens; 
			
			// contains the labels transitions could have
			String[] labels = new String[1];
			
			for (String line : contents)
			{
				// first thing to search for are outputs (there may be transitions with the same output)
				// e.g., "outputs A B C"
				switch (currentArea)
				{
				case 0: // header
					if (line.startsWith(".outputs"))
					{
						// now we identify all possible labels transitions may have
						tokens = line.replaceAll(" +", " ").split(" ");
						labels = new String[tokens.length-1];
						for (int i = 0 ; i < labels.length ; i++)
							labels[i] = tokens[i+1];
					}
					else if (line.startsWith(".graph"))
						currentArea = 1; // petri net specification section reached
					break;
				case 1:
					if (line.startsWith(".marking"))
					{
						// read initial marking
						tokens = line.replaceAll("\\{","").replaceAll("\\}","").replaceAll(" +"," ").replaceAll("/", "§").split(" ");
						for (int i = 1 ; i < tokens.length ; i++)
							markedPlaces.add(tokens[i]);
						
					}
					else if (line.startsWith(".end"))
					{
						currentArea = 2;
						break; // end is reached!
					}
					else
					{
						tokens = line.replaceAll("/","\\§").split(" ");
						
						// identify if first token is place or transition
						if (isTransition(tokens[0],labels))
						{
							// first token is transition
							for (int i = 1 ; i < tokens.length ; i++)
							{
								if (isTransition(tokens[i],labels))
								{
									// implicit place found: add transitions and place!
									String t1 = tokens[0];
									String t2 = tokens[i];
									String implicitPlace = "<" + tokens[0] + "," + tokens[i] + ">";
									
									transitions.add(t1);
									transitions.add(t2);
									places.add(implicitPlace);
									arcsTP.add(t1 + " " + implicitPlace);
									arcsPT.add(implicitPlace + " " + t2);
									
								}
								else
								{
									// arc from transition to place found
									String t = tokens[0];
									String p = tokens[i];
									
									transitions.add(t);
									places.add(p);
									arcsTP.add(t + " " + p);
								}
							}
						}
						else
						{
							// first token is place
							for (int i = 1 ; i < tokens.length ; i++)
							{
								// all subsequent tokens must be transitions
								String p = tokens[0];
								String t = tokens[i];
								
								places.add(p);
								transitions.add(t);
								arcsPT.add(p + " " + t);
							}
						}
					}
				}
			}
			
			// data is read! now construct petri net
			
			for (String p : places)
				result.addPlace(p);
			for (String t : transitions)
				result.addTransition(t, number2symbol.get(t.split("\\§")[0]));
			for (String arc : arcsPT)
			{
				String[] tmp = arc.split(" ");
				result.getTransitionByName(tmp[1]).addInputPlace(tmp[0]);
			}
			for (String arc : arcsTP)
			{
				String[] tmp = arc.split(" ");
				result.getTransitionByName(tmp[0]).addOutputPlace(tmp[1]);
			}
			for (String m : markedPlaces)
				result.getPlaceByName(m).initialMarking = 1;
				
		}
		catch (Exception e)
		{
			System.out.println("ERROR while processing: " + f.getName());
			e.printStackTrace();
		}
		
		return result;
	}
	
	// reads a petri net from a PNML string
	// i used this method to parse petri net from the BIT process library
	// other PNML files do not necessarily work, as the standard is sometimes implemented differently
	// PNML files may contain many petri nets. This method will only parse the first one
	public static PetriNet readPetriNetFromPNML(String s) throws Exception {
		
		ArrayList<String> lines = new ArrayList<String>(Arrays.asList(s.split("[\\r\\n]+")));
		
		String name = "Nameless Petri Net";
		
		HashMap<String,String> places = new HashMap<String,String>();
		String currentPlaceId = null;
		HashMap<String,Integer> placeMarkings = new HashMap<String,Integer>();
		
		HashMap<String,String> transitions = new HashMap<String,String>();
		String currentTransitionId = null;
		
		ArrayList<String> arcSources = new ArrayList<String>();
		ArrayList<String> arcTargets = new ArrayList<String>();
		
		String position = "outside"; // we start outside the petri net
		for (String line : lines) {
			
			line.trim(); // remove leading spaces
			switch (position) {
			case "outside":
				if (line.startsWith("<pnml>"))
					position = "inside"; // entering PNML definition part
				break;
				
			case "inside":
				if (line.indexOf("<net") > -1) // net definition entered
					position = "net";
				else if (line.indexOf("</pnml>") > -1) // pnml definition left
					position = "outside";
				break;
				
			case "net":
				if (line.indexOf("<place") > -1) { // place found
					position = "place";
					// parse place ID
					int indexStart = line.indexOf("\"") + 1;
					int indexEnd = line.indexOf("\"", indexStart);
					currentPlaceId = (line.substring(indexStart, indexEnd));
					places.put(currentPlaceId, "");
				}
				else if (line.indexOf("<transition") > -1) { // transition found
					position = "transition";
					// parse transition ID
					int indexStart = line.indexOf("\"") + 1;
					int indexEnd = line.indexOf("\"", indexStart);
					currentTransitionId = (line.substring(indexStart, indexEnd));
					transitions.put(currentTransitionId, "");
				}
				else if (line.indexOf("<arc") > -1) { // arc found
					position = "arc";
					// identify source id
					int startIndex = line.indexOf("source=\"") + 8;
					int endIndex =  line.indexOf("\"", startIndex);
					String sourceId = line.substring(startIndex, endIndex);
					// identify target id
					startIndex = line.indexOf("target=\"") + 8;
					endIndex =  line.indexOf("\"", startIndex);
					String targetId = line.substring(startIndex, endIndex);
					arcSources.add(sourceId);
					arcTargets.add(targetId);
				}
				
				break;
				
			case "place":
				if (line.indexOf("<name>") > -1) // name definition entered
					position = "placeName";
				else if (line.indexOf("</place>") > -1){ // place definition left
					position = "net";
					currentPlaceId = null;
				}
				else if (line.indexOf("<initialMarking>") > -1) //marking definition entered
					position = "placeMarking";
				break;
				
			case "placeName":
				if (line.indexOf("<text>") > -1) { // place name found
					int indexStart = line.indexOf("<text>") + 6;
					int indexEnd = line.indexOf("</text>");
					String placename = line.substring(indexStart, indexEnd);
					places.put(currentPlaceId, placename);
				}
				else if (line.indexOf("</name>") > -1) // place name definition left
					position = "place";
				break;
				
			case "placeMarking":
				if (line.indexOf("<text>") > -1) { // initial marking found
					int indexStart = line.indexOf("<text>") + 6;
					int indexEnd = line.indexOf("</text>");
					placeMarkings.put(currentPlaceId, Integer.parseInt(line.substring(indexStart, indexEnd)));
				}
				else if (line.indexOf("</initialMarking>") > -1) {
					position = "place";
				}
				break;
				
			case "transition":
				if (line.indexOf("<name>") > -1) // name definition entered
					position = "transitionName";
				else if (line.indexOf("</transition>") > -1){ // transition definition left
					position = "net";
					currentTransitionId = null;
				}
				break;
				
			case "transitionName":
				if (line.indexOf("<text>") > -1) { // transition name found
					int indexStart = line.indexOf("<text>") + 6;
					int indexEnd = line.indexOf("</text>");
					String transitionname = line.substring(indexStart, indexEnd);
					transitions.put(currentTransitionId, transitionname);
				}
				else if (line.indexOf("</name>") > -1) // transition name definition left
					position = "transition";
				break;
				
			case "arc":
				if (line.indexOf("</arc>") > -1) {
					position = "net";
				}
				break;
			}
		}
		
		PetriNet pn = new PetriNet(name);
		
		// add places and their markings
		for (String pid : places.keySet())
		{
			String placeName = places.get(pid);
			Integer initalMarking = placeMarkings.get(pid);
			if (initalMarking == null)
				initalMarking = 0;
			pn.addPlace(pid, initalMarking).description = placeName;
			
		}
		
		// add transitions
		for (String tid : transitions.keySet())
		{
			String transitionname = transitions.get(tid);
			pn.addTransition(tid, transitionname);
		}
		
		// add arcs
		for (int i = 0 ; i < arcSources.size() ; i++)
		{
			// get source and target
			String sid = arcSources.get(i);
			String tid = arcTargets.get(i);
			
			// try finding a place for source sid
			Place place = pn.getPlaceByName(sid);
			if (place != null) {
				// arc from place to transition
				Transition trans = pn.getTransitionByName(tid);
				trans.addInputPlace(place);
			}
			else {
				// arc from transition to place
				place = pn.getPlaceByName(tid);
				Transition trans = pn.getTransitionByName(sid);
				trans.addOutputPlace(place);
			}
			
			
		}
		
		return pn;
	}
	
	
	// reads petri net from a PNML file
	public static PetriNet readPetriNetFromPNML(File f) throws Exception {
		String pnml = FileUtils.readFileToString(f);
		PetriNet pn = Parsers.readPetriNetFromPNML(pnml);
		pn.name = FilenameUtils.removeExtension(f.getName());
		return pn;
	}
	
	// -------------------------- //
	// ----- helper methods ----- //
	// -------------------------- //
	
	private static boolean isTransition(String token, String[] symbols)
	{
		boolean result = false;
		
		for (int i = 0 ; i < symbols.length ; i++)
			if (token.startsWith(symbols[i]))
			{
				result = true;
				break;
			}
		
		return result;
	}
	
}
