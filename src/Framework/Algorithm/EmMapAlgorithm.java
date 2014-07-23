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

package Framework.Algorithm;

import Framework.Models.Log.NonRedundantLog;

public class EmMapAlgorithm implements Algorithm
{
	// number of iterations after which EM aborts, even if convergence has not been achieved
	private int maximumIterations = 100;
	
	// if improvement of target value falls below convergence threshold the algorithm stops
	private double convergenceThreshold = 0.01;
	
	// appends a symbol indicating termination to the end of each sample
	// a corresponding final state will be created and the algorithm is forced to use it
	private boolean useTerminationSymbol = true;
	
	// assumes the process is at the beginning in a unique inital state
	// can be assumed safely...
	// a corresponding inital state will be created and the algorithm is forced to use it
	private boolean useUniqueIntitalState = true;
	
	// number of states used by the algorithm
	private int numberOfStates;
	
	// number of unique symbols
	private int numberOfSymbols;
	
	// maximum length of individual observation sequence
	private int maximumSequenceLength;
	
	// the log containing observations
	private NonRedundantLog log = null;
	
	// the prior strength in terms of the log size (e.g., 0.2 means pseudoobservations with strength 20% of number of cases in log)
	public double priorStrength = 0.0;
	
	
	
	// constructor with default number of states and default prior strength
	public EmMapAlgorithm(NonRedundantLog log)
	{
		this(log, 3);
	}
	
	// constructor with default prior strength
	public EmMapAlgorithm(NonRedundantLog log, int numberOfStates)
	{
		this(log, numberOfStates, 0.2);
	}
	
	// constructor with specified number of states
	public EmMapAlgorithm(NonRedundantLog log, int numberOfStates, double priorStrength)
	{
		this.log = log;
		this.numberOfSymbols = log.getNumberOfUniqueSymbols();
		this.numberOfStates = numberOfStates;
		this.maximumSequenceLength = log.getLengthOfLongestCase();
		this.priorStrength = priorStrength;
	}
	
	
	public EmMapResult runAlgorithm()
	{
		// ----- data structure definition ----- // 
				
		// arrays to store parameters 
		double[] prior = new double[numberOfStates];
		double[][] obsmat = new double[numberOfStates][numberOfSymbols];
		double[][][] transcube = new double[numberOfStates][numberOfSymbols][numberOfStates];
		
		// helper arrays for message passing
		double forward_messages[][] = new double[maximumSequenceLength-1][numberOfStates];
		double backward_messages[][] = new double[maximumSequenceLength-1][numberOfStates];
		double constants[] = new double[maximumSequenceLength-1];
		
		// target values are stored in this variables
		double loglik = 0.0; // loglik (conditioned on parameters) 
		double conv_loglik = 0.0; // loglik variable for convergence checking
		double old_conv_loglik = 0.0;
		
		// helper arrays tom compute parameter updates (it is sufficient to store numerators)
		double prior_numerator[] = new double[numberOfStates];
		double obsmat_numerator[][] = new double[numberOfStates][numberOfSymbols];
		double transcube_numerator[][][] = new double[numberOfStates][numberOfSymbols][numberOfStates];
		
		// compute number of parameters and adjust psuedo-observation number accordingly
		double numberOfParameters = numberOfStates + numberOfStates * numberOfSymbols + numberOfStates * numberOfSymbols * numberOfStates;
		double pseudoObservations = this.priorStrength / numberOfParameters;
		
		// temporary variables
		double tmp_prior;
		double[] tmp_obsmat = new double[numberOfStates];
		double[][] tmp_transcube = new double[numberOfStates][numberOfSymbols];
		
		double sum1 = 0;
		double sum2 = 0;
		double sum3 = 0;
		
		int iterations = 0;
		
		
		// ----- generate random initial parameters ----- //
		
		long startingTime = System.currentTimeMillis();
		long endingTime;
		
		// specify range of random parameters
		double high = 0.75;
		double low = 0.25;
		
		for (int i = 0 ; i < numberOfStates ; i++)
		{
			// random parameters for prior
			prior[i] = Math.random() * (high - low) + low;
			sum1 += prior[i];
			
			for (int j = 0 ; j < numberOfSymbols ; j++)
			{
				// random parameters for obsmat
				 obsmat[i][j] = Math.random() * (high - low) + low;
				 sum2 += obsmat[i][j];
				 
				 for (int k = 0 ; k < numberOfStates ; k++)
				 {
					 // random parameters for transcube
					 transcube[i][j][k] = Math.random() * (high - low) + low;
					 sum3 = sum3 + transcube[i][j][k];
				 }
				 
				 for (int k = 0 ; k < numberOfStates ; k++)
				 {
					// normalize transcube
					 transcube[i][j][k] = transcube[i][j][k] / sum3;
				 }
				 sum3 = 0;
			}
			
			for (int j = 0 ; j < numberOfSymbols ; j++)
			{
				// normalize obsmat
				obsmat[i][j] = obsmat[i][j] / sum2;
			}
			sum2 = 0;
		}
		for (int i = 0 ; i < numberOfStates ; i++)
		{
			// normalize prior
			prior[i] = prior[i] / sum1;
		}
		sum1 = 0;
		
		// if unique initial state is to be used, make state 1 this initial state!
		if (this.useUniqueIntitalState == true)
		{
			prior[0] = 1.0;
			if (numberOfStates > 1)
			{
				for (int i = 1 ; i < numberOfStates ; i++ )
				{
					prior[i] = 0.0;
				}
			}
		}
		
		// if a termination symbol is used, make the last state the termination state!
		if (this.useTerminationSymbol == true)
		{
			
			// 1. states other than the last state cannot emit the termination symbol
			for (int i = 0 ; i < numberOfStates-1 ; i++)
			{
				obsmat[i][numberOfSymbols-1] = 0;
			}
			
			// 2. the last state emits the termination symbol (which is the one with the highest number)
			obsmat[numberOfStates-1][numberOfSymbols-1] = 1;
			
			// 3. the last state shall emit the termination symbol only
			for (int i = 0 ; i < (numberOfSymbols-1) ; i++)
			{
				obsmat[numberOfStates-1][i] = 0;
			}
			obsmat[numberOfStates-1][numberOfSymbols-1] = 1;
			
			// 4. once in the last state it can never be left
			for (int i = 0 ; i < numberOfStates-1 ; i++)
			{
				transcube[numberOfStates-1][numberOfSymbols-1][i] = 0;
			}
			transcube[numberOfStates-1][numberOfSymbols-1][numberOfStates-1] = 1;
		}
		
		
		// ----- parameter estimation ----- //
		
		int cur_obs;
		int next_obs;
		
		// iteratively optimize parameters
		for (int i = 0 ; i < this.maximumIterations ; i++)
		{
			// save current log-likelihood for later
			old_conv_loglik = conv_loglik;
			loglik = 0.0;
			conv_loglik = 0.0;
			
			// initialize all temporary values to zero
			for (int j = 0 ; j < numberOfStates ; j++)
			{
				prior_numerator[j] = 0.0;
				for (int t = 0 ; t < numberOfSymbols ; t++)
				{
					obsmat_numerator[j][t] = 0.0;
					for (int s = 0 ; s < numberOfStates ; s++)
					{
						transcube_numerator[j][t][s] = 0.0;
					}
				}
			}
			
			// start calculating messages (for each case independently)
			for (int c = 0; c < this.log.getNumberOfUniqueCases() ; c++)
			{
				// determine number of symbols in this case
				int N = this.log.getNumericalLog().get(c).size();
				
				// determine multiplicity of this case
				double caseMultiplicity = (double)this.log.getCaseMultiplicity(c);
				
				// --- compute forward messages --- ///
				
				// first factor
				cur_obs = this.log.getNumericalLogEntry(c, 0);
				constants[0] = 0;
				for (int j = 0 ; j < numberOfStates ; j++)
				{
					forward_messages[0][j] = 0;
					for (int k = 0 ; k < numberOfStates ; k++)
					{
						forward_messages[0][j] = forward_messages[0][j] + (prior[k] * obsmat[k][cur_obs] * transcube[k][cur_obs][j]);
					}
					constants[0] = constants[0] + forward_messages[0][j];
				}
				loglik = loglik + (Math.log(constants[0]) * caseMultiplicity);
				constants[0] = 1 / constants[0];
				for (int j = 0 ; j < numberOfStates ; j++)
				{
					forward_messages[0][j] = forward_messages[0][j] * constants[0];
				}
				
				// middle factors
				for (int n = 1 ; n < N - 2 ; n++)
				{
					cur_obs = this.log.getNumericalLogEntry(c, n);
					constants[n] = 0;
					for (int j = 0 ; j < numberOfStates ; j++)
					{
						forward_messages[n][j] = 0;
						for (int k = 0 ; k < numberOfStates ; k++)
						{
							forward_messages[n][j] = forward_messages[n][j] + (forward_messages[n-1][k] * obsmat[k][cur_obs] * transcube[k][cur_obs][j]);
						}
						constants[n] = constants[n] + forward_messages[n][j];
					}
					loglik = loglik + (Math.log(constants[n]) * caseMultiplicity);
					constants[n] = 1 / constants[n];
					for (int j = 0 ; j < numberOfStates ; j++)
					{
						forward_messages[n][j] = forward_messages[n][j] * constants[n];
					}
				}
				
				// last factor
				constants[N-2] = 0;
				cur_obs = this.log.getNumericalLogEntry(c, N-2);
				next_obs = this.log.getNumericalLogEntry(c, N-1);
				for (int j = 0 ; j < numberOfStates ; j++)
				{
					forward_messages[N-2][j] = 0;
					for (int k = 0 ; k < numberOfStates ; k++)
					{
						forward_messages[N-2][j] = forward_messages[N-2][j] + (forward_messages[N-2-1][k] * obsmat[k][cur_obs] * obsmat[j][next_obs] * transcube[k][cur_obs][j]);			
					}
					constants[N-2] = constants[N-2] + forward_messages[N-2][j];
				}
				loglik = loglik + (Math.log(constants[N-2]) * caseMultiplicity);
				constants[N-2] = 1 / constants[N-2];
				for (int j = 0 ; j < numberOfStates ; j++)
				{
					forward_messages[N-2][j] = forward_messages[N-2][j] * constants[N-2];
					obsmat_numerator[j][next_obs] = obsmat_numerator[j][next_obs] + (forward_messages[N-2][j] * caseMultiplicity);
				}
				
				
				// --- compute backward messages --- //
				
				// first factor (direct update of numerators of update equations)
				for (int j = 0 ; j < numberOfStates ; j++)
				{
					backward_messages[N-2][j] = 0;
					for (int k = 0 ; k < numberOfStates ; k++)
					{
						backward_messages[N-2][j] = backward_messages[N-2][j] + (obsmat[k][next_obs]  * transcube[j][cur_obs][k]);
						transcube_numerator[k][cur_obs][j] = transcube_numerator[k][cur_obs][j] + ((forward_messages[N-2-1][k] * obsmat[k][cur_obs] * transcube[k][cur_obs][j] * obsmat[j][next_obs] * constants[N-2]) * caseMultiplicity);
					}
					backward_messages[N-2][j] = backward_messages[N-2][j] * obsmat[j][cur_obs] * constants[N-2];
					obsmat_numerator[j][cur_obs] = obsmat_numerator[j][cur_obs] + ((forward_messages[N-2-1][j] * backward_messages[N-2][j]) * caseMultiplicity);
				}
				
				// middle factors  (direct update of numerators of update equations)
				for (int n = N-2-1 ; n > 0 ; n--)
				{
					cur_obs = this.log.getNumericalLogEntry(c, n);
					
					for (int j = 0 ; j < numberOfStates ; j++)
					{
						backward_messages[n][j] = 0;						
						for (int k = 0 ; k < numberOfStates ; k++)
						{
							backward_messages[n][j] = backward_messages[n][j] + (backward_messages[n+1][k] * transcube[j][cur_obs][k]);
							transcube_numerator[k][cur_obs][j] = transcube_numerator[k][cur_obs][j] + ((forward_messages[n-1][k] * obsmat[k][cur_obs] * transcube[k][cur_obs][j] * backward_messages[n+1][j] * constants[n]) * caseMultiplicity);
						}
						backward_messages[n][j] = backward_messages[n][j] * obsmat[j][cur_obs] * constants[n];
						obsmat_numerator[j][cur_obs] = obsmat_numerator[j][cur_obs] + ((forward_messages[n-1][j] * backward_messages[n][j]) * caseMultiplicity);
					}
				}
				
				// last factor (direct update of numerators of update equations)
				cur_obs = this.log.getNumericalLogEntry(c, 0);
				for (int j = 0 ; j < numberOfStates ; j++)
				{
					backward_messages[0][j] = 0;
					for (int k = 0 ; k < numberOfStates ; k++)
					{
						backward_messages[0][j] = backward_messages[0][j] + (backward_messages[0+1][k]  * transcube[j][cur_obs][k]);
						transcube_numerator[k][cur_obs][j] = transcube_numerator[k][cur_obs][j] + ((prior[k] * obsmat[k][cur_obs] * transcube[k][cur_obs][j] * backward_messages[0+1][j] * constants[0]) * caseMultiplicity);
					}
					backward_messages[0][j] = backward_messages[0][j] * obsmat[j][cur_obs] * prior[j] * constants[0];
					prior_numerator[j] = prior_numerator[j] + (backward_messages[0][j] * caseMultiplicity);
					obsmat_numerator[j][cur_obs] = obsmat_numerator[j][cur_obs] + (backward_messages[0][j] * caseMultiplicity);
				}
				
			}
			// all cases are processed
			
			
			// ----- check for convergence ----- //
			
			conv_loglik = loglik;
			if (pseudoObservations > 0)
			{
				for (int j = 0 ; j < numberOfStates -1 ; j++)
				{
					for (int t = 0 ; t < numberOfSymbols -1 ; t++)
					{
						conv_loglik += pseudoObservations * Math.log(obsmat[j][t]);
						for (int s = 0 ; s < numberOfStates -1 ; s++)
						{
							conv_loglik += pseudoObservations * Math.log(transcube[j][t][s]);
						}
					}
				}
			}
			
			
			// check for convergence, but not in the first step
			if (i > 0)
			{
				// stop if difference is too small
				if (Math.abs(conv_loglik - old_conv_loglik) < this.convergenceThreshold)
				{
					endingTime = System.currentTimeMillis();
					iterations = i;
					System.out.println("... EM finished after " + iterations + " iterations in " + (new Double(endingTime - startingTime)/1000) + " seconds! (" + numberOfStates + " states) | loglik: " + loglik + " | pseudo-obs = " + pseudoObservations + " | priorStrength = " + this.priorStrength);
					break;
				}
				else if (i == (this.maximumIterations -1)) {
					// if this is the last iteration, algorithm did not converge. Output stats anyways
					endingTime = System.currentTimeMillis();
					iterations = i;
					System.out.println("... EM finished without converging after " + iterations + " iterations  in " + (new Double(endingTime - startingTime)/1000) + " seconds! (" + numberOfStates + " states) | loglik: " + loglik + " | pseudo-obs = " + pseudoObservations + " | priorStrength = " + this.priorStrength);
				}
					
			}
			
			
			// ----- update parameters now ----- //
			
			// determine normalization factors
			tmp_prior = 0.0;
			
			for (int j = 0 ; j < numberOfStates ; j++)
			{
				tmp_prior = tmp_prior + prior_numerator[j] + pseudoObservations;
				tmp_obsmat[j] = 0.0;
				for (int t = 0 ; t < numberOfSymbols ; t++)
				{
					tmp_obsmat[j] = tmp_obsmat[j] + obsmat_numerator[j][t] + pseudoObservations;
					tmp_transcube[j][t] = 0.0;
					for (int s = 0 ; s < numberOfStates ; s++)
					{
						tmp_transcube[j][t] = tmp_transcube[j][t] + transcube_numerator[j][t][s] + pseudoObservations;
					}
				}
			}
			
			// update parameters
			for (int j = 0 ; j < numberOfStates ; j++)
			{
				// Update prior only if the initial state is not set a priori
				if (this.useUniqueIntitalState == false)
					prior[j] = (prior_numerator[j] + pseudoObservations) / tmp_prior;
				for (int t = 0 ; t < numberOfSymbols ; t++)
				{
					// Update obsmat for last state only if it is not the explicit termination state
					// also proceed with updates only if we are not at the termination state!
					if ((this.useTerminationSymbol == false) || (j < (numberOfStates-1)))
					{
						obsmat[j][t] = (obsmat_numerator[j][t] + pseudoObservations) / tmp_obsmat[j];
						for (int s = 0 ; s < numberOfStates ; s++)
						{
							if (tmp_transcube[j][t] > 0.0000001)
							{
								// Update partial transcube for leaving last state only if it is not the explicit termination state
								if ((this.useTerminationSymbol == false) || (j < (numberOfStates-1)))
									transcube[j][t][s] = (transcube_numerator[j][t][s] + pseudoObservations) / tmp_transcube[j][t];
							}
							else
							{
								// if denominator for transcube updates becomes too small, set arbitrary values to avoid division by zero
								if (j < (numberOfStates-1))
									transcube[j][t][s] = 1.0 / numberOfStates;
							}
						}
					}
				}
			}
		}
		
		// ----------------------------------------- //
		// --------- model selection statistics----- //
		// ----------------------------------------- //
		
		// number of model parameters for AIC / BIC computation
		int numberOfParams = numberOfStates + numberOfStates * numberOfSymbols + numberOfStates * numberOfSymbols * numberOfStates;
		if (this.useUniqueIntitalState == true)
			numberOfParams -= numberOfStates;
		if (this.useTerminationSymbol == true)
			numberOfParams -= (numberOfStates * numberOfSymbols);
		
		// number of observations in the data
		int numberOfObservations = log.getNumberOfSymbols();
		
		// number of model parameters for HEU computation (only non-zero parameters of the optimal solution are counted)
		int numberOfNonZeroParameters = 0;
		for (int i = 0 ; i < numberOfStates ; i++)
			for (int j = 0 ; j < numberOfSymbols ; j++)
				for (int k = 0 ; k < numberOfStates ; k++)
					if ((transcube[i][j][k] != (1.0/numberOfStates)) & ((transcube[i][j][k] * obsmat[i][j]) > 0.05))
						numberOfNonZeroParameters++;
		
		double AIC = ((-2)*loglik) + (2*numberOfParams);
		double BIC = ((-2)*loglik) + (numberOfParams * Math.log(numberOfObservations));
		// correct AIC in case of small sample sizes, but only if no numerical problems occur!
		if ((2.0 * (numberOfParams*(numberOfParams+1.0) / new Double((numberOfObservations - numberOfParams - 1)))) > 0)
			AIC = AIC + (2.0 * (numberOfParams*(numberOfParams+1.0) / new Double((numberOfObservations - numberOfParams - 1))));
		double HEU = ((-2)*loglik) + (2.0 * numberOfNonZeroParameters);
		
		//System.out.println("AIC: " + AIC);
		//System.out.println("BIC: " + BIC);
		//System.out.println("HEU: " + HEU);
		
		EmMapResult result = new EmMapResult();
		result.setPrior(prior);
		result.setObsmat(obsmat);
		result.setTranscube(transcube);
		result.setAIC(AIC);
		result.setBIC(BIC);
		result.setHEU(HEU);
		result.setLog(log);
		result.setIterations(iterations);
		result.setNumberOfObservations(numberOfObservations);
		result.setNumberOfStates(numberOfStates);
		result.setNumberOfSymbols(numberOfSymbols);
		result.setPriorStrength(priorStrength);
		result.setLoglik(loglik);
		
		
		return result;
	}
	
	public int getMaximumIterations() {
		return maximumIterations;
	}

	public void setMaximumIterations(int maximumIterations) {
		this.maximumIterations = maximumIterations;
	}

	public double getConvergenceThreshold() {
		return convergenceThreshold;
	}

	public void setConvergenceThreshold(double convergenceThreshold) {
		this.convergenceThreshold = convergenceThreshold;
	}

	public boolean isUseTerminationSymbol() {
		return useTerminationSymbol;
	}

	public void setUseTerminationSymbol(boolean useTerminationSymbol) {
		this.useTerminationSymbol = useTerminationSymbol;
	}

	public boolean isUseUniqueIntitalState() {
		return useUniqueIntitalState;
	}

	public void setUseUniqueIntitalState(boolean useUniqueIntitalState) {
		this.useUniqueIntitalState = useUniqueIntitalState;
	}

	public int getNumberOfStates() {
		return numberOfStates;
	}

	public void setNumberOfStates(int numberOfStates) {
		this.numberOfStates = numberOfStates;
	}

	public int getNumberOfSymbols() {
		return numberOfSymbols;
	}

	public void setNumberOfSymbols(int numberOfSymbols) {
		this.numberOfSymbols = numberOfSymbols;
	}

	public int getMaximumSequenceLength() {
		return maximumSequenceLength;
	}

	public void setMaximumSequenceLength(int maximumSequenceLength) {
		this.maximumSequenceLength = maximumSequenceLength;
	}

	public NonRedundantLog getLog() {
		return log;
	}

	public void setLog(NonRedundantLog log) {
		this.log = log;
	}


}