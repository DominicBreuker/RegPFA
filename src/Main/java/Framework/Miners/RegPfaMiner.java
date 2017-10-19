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

package Framework.Miners;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import Framework.Algorithm.EmMapAlgorithm;
import Framework.Algorithm.EmMapResult;
import Framework.Models.Log.NonRedundantLog;
import Framework.Models.PetriNet.PetriNet;
import Framework.Models.TransitionSystem.TransitionSystem;
import Framework.PetrifyConnector.Petrify;
import Framework.Predictors.EmMapPredictor;

public class RegPfaMiner extends Miner {

    @Override
    public PetriNet minePetriNet(NonRedundantLog log) throws Exception {
        int[] stateGrid = {2, 4, 6, 8, 10};
        double[] priorStrengthGrid = {0.0, 0.1, 0.2};
        int numberOfTries = 5;
        int maxIter = 5000;
        double convergenceThreshold = 0.001;
        int modelSelectionCriterion = EmMapPredictor.HIC_SELECTOR;
        double pruningRatio = 1.5;
        return this.minePetriNet(log, stateGrid, priorStrengthGrid, numberOfTries, maxIter, convergenceThreshold, modelSelectionCriterion, pruningRatio);
    }

    public TransitionSystem mineTransitionSystem(NonRedundantLog log, int[] stateGrid, double[] priorStrengthGrid, int numberOfTries, int maxIter, double convergenceThreshold, int modelSelectionCriterion, double pruningRatio) throws Exception {
        EmMapPredictor predictor = new EmMapPredictor(log, log.name);
        EmMapResult finalResult = predictor.createPredictor(stateGrid, priorStrengthGrid, numberOfTries, maxIter, convergenceThreshold, modelSelectionCriterion);
        return finalResult.getTransitionSystem(pruningRatio);
    }

    public PetriNet minePetriNet(NonRedundantLog log, int[] stateGrid, double[] priorStrengthGrid, int numberOfTries, int maxIter, double convergenceThreshold, int modelSelectionCriterion, double pruningRatio) throws Exception {
        return Petrify.runPetrify(this.mineTransitionSystem(log, stateGrid, priorStrengthGrid, numberOfTries, maxIter, convergenceThreshold, modelSelectionCriterion, pruningRatio));
    }
}
