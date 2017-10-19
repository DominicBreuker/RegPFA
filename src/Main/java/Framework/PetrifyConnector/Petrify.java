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

package Framework.PetrifyConnector;

import java.io.File;
import java.io.IOException;

import Framework.Models.Parsers;
import Framework.Models.PetriNet.PetriNet;
import Framework.Models.TransitionSystem.TransitionSystem;

public class Petrify {

    private static final File petrify = new File("./lib/petrify4.1.exe");
    private static final String tmpFolder = "./tmp/";

    private static final String dead_flag = " -dead";
    private static final String all_flag = "";
    private static final String sat_flag = "";
    private static final String min_flag = "";
    private static final String opt_flag = "";
    private static final String bisim_flag = " -bisim";
    private static final String mints_flag = "";
    private static final String p_flag = "";
    private static final String fc_flag = " -fc";
    private static final String efc_flag = "";
    private static final String uc_flag = "";
    private static final String euc_flag = "";
    private static final String er_flag = "";
    private static final String sm_flag = "";


    // calls petrify --> generate a petri net from a transition system
    public static PetriNet runPetrify(TransitionSystem ts) {
        try {
            // temporary files are required to interact with petrify
            File petriNetFile = new File(tmpFolder + "temp.petrifyOutput");
            File transitionSystemFile = new File(tmpFolder + "temp.ts");

            // write transition system to a file so that petrify can understand it
            ts.initializeNumericalLabels();
            ts.writePetrifyFile(transitionSystemFile);

            // call petrify to create a petri net file
            String cmd = petrify.getAbsolutePath() + " " + dead_flag + all_flag + sat_flag + min_flag + opt_flag + bisim_flag + mints_flag + p_flag + fc_flag + efc_flag + uc_flag + euc_flag + er_flag + sm_flag + " -o " + petriNetFile.getAbsolutePath() + " " + transitionSystemFile.getAbsolutePath();
            //String cmd = "petrify4.1.exe";
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File("./lib/"));
            System.out.println(pb.directory().getAbsolutePath());
            pb.redirectErrorStream(true);
            //pb.start();

            Runtime r = Runtime.getRuntime();
            System.out.println(cmd);
            Process p = r.exec(cmd);
            int return_status = p.waitFor();
            if (return_status != 0) throw new IOException(" !!! ERROR: Petrify encountered an unknown error!");

            // parse the petri net created by petrify (number2symbol mapping from TS is needed)
            PetriNet result = Parsers.readPetriNetFromPetrifyFile(petriNetFile, ts.getNumberToSymbolMapping());

            // delete temporary files!
            File dir = new File(tmpFolder);
            File[] files = dir.listFiles();
            for (File f : files)
                f.delete();

            return result;
        } catch (IOException e) {
            System.out.println("IO Exception when calling petrify for transition system" + ts.name);
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            System.out.println("Interrupt Exception when calling petrify for transition system" + ts.name);
            e.printStackTrace();
            return null;
        }
    }

}
