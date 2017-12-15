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

import java.util.ArrayList;
import java.util.HashMap;

public class LogFilter {


    // removes all events from the log that are in the array list "eventsToFilter"
    public static ArrayList<ArrayList<String>> filterLog(ArrayList<ArrayList<String>> log, ArrayList<String> eventsToFilter) {

        ArrayList<ArrayList<String>> filteredLog = new ArrayList<ArrayList<String>>();

        for (ArrayList<String> currentCase : log) {
            ArrayList<String> currentFilteredCase = new ArrayList<String>();
            for (String event : currentCase) {
                if (eventsToFilter.contains(event) == false)
                    currentFilteredCase.add(event);
            }
            if (currentFilteredCase.size() > 0)
                filteredLog.add(currentFilteredCase);
        }

        return filteredLog;
    }

    // assumes events are named "eventType(lifecycleType)"
    // creates a new event log which contains only events of lifecycleType=complete
    public static ArrayList<ArrayList<String>> filterOnlyComplete(ArrayList<ArrayList<String>> log) {
        ArrayList<ArrayList<String>> filteredLog = new ArrayList<ArrayList<String>>();

        for (ArrayList<String> currentCase : log) {
            ArrayList<String> currentFilteredCase = new ArrayList<String>();
            for (String event : currentCase) {
                if (event.split("\\(")[1].toLowerCase().equals("complete)"))
                    currentFilteredCase.add(event);
            }
            if (currentFilteredCase.size() > 0)
                filteredLog.add(currentFilteredCase);
        }

        return filteredLog;
    }

    // groups all events of a type occurring less often than specified by minRelativeFrequency into a single new event type, called "groupName"
    public static ArrayList<ArrayList<String>> groupInfrequentSymbols(ArrayList<ArrayList<String>> log, double minRelativeFrequency, String groupName) {

        ArrayList<String> eventTypesToReplace = new ArrayList<String>();

        // get frequencies of event types
        HashMap<String, Integer> frequencies = new HashMap<String, Integer>();
        int totalNumberOfEvents = 0;
        for (ArrayList<String> currentCase : log) {
            for (String event : currentCase) {
                totalNumberOfEvents++;
                if (frequencies.containsKey(event))
                    frequencies.put(event, frequencies.get(event) + 1);
                else
                    frequencies.put(event, 1);
            }
        }

        // find event types to replace
        for (String eventType : frequencies.keySet()) {
            int eventTypeFrequency = frequencies.get(eventType);
            if ((((double) eventTypeFrequency) / ((double) totalNumberOfEvents)) < minRelativeFrequency)
                eventTypesToReplace.add(eventType);
        }

        // construct new log
        ArrayList<ArrayList<String>> filteredLog = new ArrayList<ArrayList<String>>();
        for (ArrayList<String> currentCase : log) {
            ArrayList<String> currentFilteredCase = new ArrayList<String>();
            for (String event : currentCase) {
                if (eventTypesToReplace.contains(event))
                    currentFilteredCase.add(groupName);
                else
                    currentFilteredCase.add(event);
            }
            filteredLog.add(currentFilteredCase);
        }

        return filteredLog;
    }

}
