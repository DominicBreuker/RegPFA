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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class LogParsers {

    public static ArrayList<ArrayList<String>> parseXes(File f) throws IOException {

        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();

        LineIterator iter = FileUtils.lineIterator(f);

        ArrayList<String> currentLine = new ArrayList<String>();
        String[] tmpEvent = new String[2];

        int status = 0; // 0 = outside trace // 1 = inside trace // 2 = inside event
        while (iter.hasNext()) {
            String line = iter.nextLine();

            if (line.trim().startsWith("</trace>")) {
                // trace left
                result.add(currentLine);
                status = 0;
            } else if (line.trim().startsWith("<trace>")) {
                // new trace entered
                status = 1;
                currentLine = new ArrayList<String>();
            } else if (line.trim().startsWith("<event>") && (status == 1)) {
                // new event entered
                tmpEvent = new String[2];
                status = 2;
            } else if ((line.trim().startsWith("<string key=\"concept:name\" value=\"")) && (status == 2)) {
                // name of event found
                String tmpString = line.trim().substring("<string key=\"concept:name\" value=\"".length());
                tmpEvent[0] = tmpString.substring(0, tmpString.length() - 3);
                //System.out.println(tmpString);
            } else if ((line.trim().startsWith("<string key=\"lifecycle:transition\" value=\"")) && (status == 2)) {
                // lifecycle type of event found
                String tmpString = line.trim().substring("<string key=\"lifecycle:transition\" value=\"".length());
                tmpEvent[1] = tmpString.substring(0, tmpString.length() - 3);
                //System.out.println(tmpString);
            } else if (line.trim().startsWith("</event>") && (status == 2)) {
                // event left
                currentLine.add(tmpEvent[0] + "(" + tmpEvent[1] + ")");
                status = 1;
            } else {
                // do nothing
            }
        }
        return result;
    }


    public static ArrayList<ArrayList<String>> parseMxml(File f) throws IOException, ParserConfigurationException, SAXException {

        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document rawLog = dBuilder.parse(f);

        rawLog.getDocumentElement().normalize();

        System.out.println("Root element :" + rawLog.getDocumentElement().getNodeName());

        NodeList processList = rawLog.getElementsByTagName("Process");
        Node firstProcess = processList.item(0);

        NodeList processInstances = firstProcess.getChildNodes();

        for (int i = 0; i < processInstances.getLength(); i++) {
            Node processInstance = processInstances.item(i);
            ArrayList<String> currentInstance = new ArrayList<String>();
            NodeList auditTrailEntries = processInstance.getChildNodes();
            for (int j = 0; j < auditTrailEntries.getLength(); j++) {
                Node currentAuditTrailEntry = auditTrailEntries.item(j);
                if (currentAuditTrailEntry.getNodeType() == Node.ELEMENT_NODE) {
                    Element currentAuditTrailEntryElement = (Element) currentAuditTrailEntry;
                    String eventType = currentAuditTrailEntryElement.getElementsByTagName("WorkflowModelElement").item(0).getTextContent();
                    String eventLifeCycleType = currentAuditTrailEntryElement.getElementsByTagName("EventType").item(0).getTextContent();
                    currentInstance.add(eventType + "(" + eventLifeCycleType + ")");
                }
            }
            if (currentInstance.size() > 0)
                result.add(currentInstance);
        }

        return result;
    }
}