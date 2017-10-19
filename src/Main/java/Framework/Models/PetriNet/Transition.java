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

package Framework.Models.PetriNet;

import java.util.*;

public class Transition {

    public String name;
    public String label;
    public Character charLabel;
    public PetriNet pNet;
    public ArrayList<Place> inputPlaces = new ArrayList<Place>();
    public ArrayList<Place> outputPlaces = new ArrayList<Place>();

    public Transition(String name, String label, PetriNet pNet, ArrayList<Place> inputPlaces, ArrayList<Place> outputPlaces) {
        this.name = name;
        this.pNet = pNet;
        this.label = label;
        this.charLabel = this.pNet.getCharLabel(this.label); // mapping is maintained by petri net
        this.inputPlaces = inputPlaces;
        this.outputPlaces = outputPlaces;
    }

    public Boolean addInputPlace(String inputPlace) {
        Place p = this.pNet.getPlaceByName(inputPlace);
        if (p != null) {
            return addInputPlace(p);
        } else
            return false;
    }

    public Boolean addInputPlace(Place inputPlace) {
        if (this.pNet.places.contains(inputPlace)) {
            this.inputPlaces.add(inputPlace);
            inputPlace.outgoingTransitions.add(this);
            return true;
        } else {
            return false;
        }
    }

    public Boolean removeInputPlace(Place inputPlace) {
        int i = this.inputPlaces.indexOf(inputPlace);
        int j = inputPlace.outgoingTransitions.indexOf(this);
        if ((i != -1) && (j != -1)) {
            this.inputPlaces.remove(i);
            inputPlace.outgoingTransitions.remove(j);
            return true;
        } else {
            System.out.println("error removing input place p" + inputPlace.name + "from t" + this.name);
            return false;
        }
    }

    public Boolean addOutputPlace(String outputPlace) {
        Place p = this.pNet.getPlaceByName(outputPlace);
        if (p != null) {
            return addOutputPlace(p);
        } else
            return false;
    }

    public Boolean addOutputPlace(Place outputPlace) {
        if (this.pNet.places.contains(outputPlace)) {
            this.outputPlaces.add(outputPlace);
            outputPlace.incomingTransitions.add(this);
            return true;
        } else {
            return false;
        }
    }

    public Boolean removeOutputPlace(Place outputPlace) {
        int i = this.outputPlaces.indexOf(outputPlace);
        int j = outputPlace.incomingTransitions.indexOf(this);
        if ((i != -1) && (j != -1)) {
            this.outputPlaces.remove(i);
            outputPlace.incomingTransitions.remove(j);
            return true;
        } else {
            System.out.println("error removing output place p" + outputPlace.name + "from t" + this.name);
            return false;
        }
    }

}
