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

package Framework.Models.TransitionSystem;

import java.util.*;

public class State {

    public String name;
    public TransitionSystem ts;
    public ArrayList<Transition> incomingTransitions = new ArrayList<Transition>();
    public ArrayList<Transition> outgoingTransitions = new ArrayList<Transition>();

    private volatile int hashCode = 0;

    public State(String name) {
        this.name = name;
    }

    public State(String name, TransitionSystem ts, ArrayList<Transition> incomingTransitions, ArrayList<Transition> outgoingTransitions) {
        this.name = name;
        this.ts = ts;
        this.incomingTransitions = incomingTransitions;
        this.outgoingTransitions = outgoingTransitions;
    }

    // creates a state for a petri net marking (during reachability graph construction)
    public State(int[] marking, TransitionSystem ts) {
        String name = "";
        for (int i = 0; i < marking.length; i++) name += marking[i];
        this.name = name;
        this.ts = ts;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof State)) return false;
        State s = (State) obj;
        return (s.name.equals(this.name));
    }

    public int hashCode() {
        if (this.hashCode == 0) this.hashCode = this.name.hashCode();
        return this.hashCode;
    }


}