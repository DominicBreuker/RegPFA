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
import java.io.IOException;

import Framework.Models.PetriNet.PetriNet;
import Framework.Models.TransitionSystem.TransitionSystem;

public class Visualizer {
	
	public static final String dotPath = "./lib/dot/bin/dot.exe";
	public static final String pdfPath = "./lib/SumatraPDF.exe";
	public static final String tmpPath = "./tmp/";
	
	public static void showPetriNet(PetriNet pNet)
	{
		try
		{
			// create temporary files
			File dotFile = new File(tmpPath + pNet.name);
			File pdfFile = new File(tmpPath + pNet.name + ".pdf");
			pNet.writeDotFile(dotFile);
			
			// run dot
			Runtime r = Runtime.getRuntime();
			String[] argsDot = {dotPath, "-Tpdf", dotFile.getAbsolutePath(), "-o", pdfFile.getAbsolutePath()};
			Process pDot = r.exec(argsDot);
			pDot.waitFor();
			
			// run pdf viewer
			String[] argsPDF = {pdfPath, pdfFile.getAbsolutePath()};
			Process pPDF = r.exec(argsPDF);
			pPDF.waitFor();
			
			// delete temporary files
			dotFile.delete();
			pdfFile.delete();
		}
		catch (IOException e)
		{
			System.out.println("IO Exception when visualizing petri net " + pNet.name);
			e.printStackTrace();
		}
		catch (InterruptedException e)
		{
			System.out.println("Interrupt Exception when visualizing petri net " + pNet.name);
			e.printStackTrace();
		}
	}
	
	public static void showTransitionSystem(TransitionSystem ts)
	{
		try
		{
			// create temporary files
			File dotFile = new File(tmpPath + ts.name);
			File pdfFile = new File(tmpPath + ts.name + ".pdf");
			ts.writeDotFile(dotFile);
			
			// run dot
			Runtime r = Runtime.getRuntime();
			String[] argsDot = {dotPath, "-Tpdf", dotFile.getAbsolutePath(), "-o", pdfFile.getAbsolutePath()};
			Process pDot = r.exec(argsDot);
			pDot.waitFor();
			
			// run pdf viewer
			String[] argsPDF = {pdfPath, pdfFile.getAbsolutePath()};
			Process pPDF = r.exec(argsPDF);
			pPDF.waitFor();
			
			// delete temporary files
			dotFile.delete();
			pdfFile.delete();
		}
		catch (IOException e)
		{
			System.out.println("IO Exception when visualizing transition system " + ts.name);
			e.printStackTrace();
		}
		catch (InterruptedException e)
		{
			System.out.println("Interrupt Exception when visualizing transition system " + ts.name);
			e.printStackTrace();
		}
	}
	
}
