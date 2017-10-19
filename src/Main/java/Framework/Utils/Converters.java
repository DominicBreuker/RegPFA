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

import java.util.HashMap;

public class Converters {

    public static dk.brics.automaton.Automaton getBricsAutomaton(Framework.Models.TransitionSystem.TransitionSystem ts, HashMap<String, Character> labelMapping) {
        dk.brics.automaton.Automaton a = new dk.brics.automaton.Automaton();

        // keep a mapping between the states of both automatas (a and ts)
        HashMap<Framework.Models.TransitionSystem.State, dk.brics.automaton.State> stateMapping = new HashMap<Framework.Models.TransitionSystem.State, dk.brics.automaton.State>();
        // initial state has been created by constructor of a --> map it to the initial state of ts
        stateMapping.put(ts.initialState, a.getInitialState());

        // create states (and include them in mapping)
        for (Framework.Models.TransitionSystem.State s : ts.states) {
            getBricsAutomatonState(s, stateMapping);
        }

        // create transitions
        for (Framework.Models.TransitionSystem.State s : ts.states) {
            dk.brics.automaton.State sourceState = getBricsAutomatonState(s, stateMapping);
            for (Framework.Models.TransitionSystem.Transition t : s.outgoingTransitions) {
                dk.brics.automaton.State targetState = getBricsAutomatonState(t.targetState, stateMapping);
                Character bricsLabel = labelMapping.get(t.label);
                sourceState.addTransition(new dk.brics.automaton.Transition(bricsLabel, targetState));
            }
            if (s.outgoingTransitions.size() == 0)
                sourceState.setAccept(true);
            else
                sourceState.setAccept(false);
        }

        // return result
        return a;
    }

    private static dk.brics.automaton.State getBricsAutomatonState(Framework.Models.TransitionSystem.State tsState, HashMap<Framework.Models.TransitionSystem.State, dk.brics.automaton.State> stateMapping) {
        dk.brics.automaton.State result = null;
        result = stateMapping.get(tsState);
        // if there is no mapped state yet, then create one, include it in the mapping, and return it
        if (result == null) {
            result = new dk.brics.automaton.State();
            stateMapping.put(tsState, result);
        }
        return result;
    }

    public static HashMap<String, Character> createCharLabelMapping(Framework.Models.TransitionSystem.TransitionSystem ts) {
        HashMap<String, Character> mapping = new HashMap<String, Character>();

        char c = 'a';
        for (Framework.Models.TransitionSystem.Transition t : ts.transitions) {
            // if label is not yet mapped, do it
            if (mapping.containsKey(t.label) == false) {
                mapping.put(t.label, c++);
            }
        }

        return mapping;
    }

}
