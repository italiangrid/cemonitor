/*
 * Copyright (c) Members of the EGEE Collaboration. 2004. 
 * See http://www.eu-egee.org/partners/ for details on the copyright
 * holders.  
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
 
/*
 *
 * Authors: Luigi Zangrando <zangrando@pd.infn.it>
 *
 */

package org.glite.ce.monitor.ceplugin;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.glite.ce.monitorapij.sensor.SensorOutputDataFormat;

public class ISMClassAd extends SensorOutputDataFormat {
    
    private final static Logger logger = Logger.getLogger(ISMClassAd.class.getName());
    
    private static HashSet<String> ignoreAttributes = new HashSet<String>();
    
    private static HashSet<String> listAttributes = new HashSet<String>();
    
    static {
        ignoreAttributes.add("createtimestamp");
        ignoreAttributes.add("glueschemaversionminor");
        ignoreAttributes.add("glueschemaversionmajor");
        ignoreAttributes.add("entryttl");
        ignoreAttributes.add("modifytimestamp");
        ignoreAttributes.add("dn");
        ignoreAttributes.add("objectclass");
        
        /*
         * TODO verify list-based attributes
         */
        listAttributes.add("glueforeignkey");
        listAttributes.add("gluesiteotherinfo");
        listAttributes.add("glueclusterservice");
        listAttributes.add("glueceaccesscontrolbaserule");
        listAttributes.add("glueservicecccescontrolbaserule");
        listAttributes.add("glueserviceaccesscontrolrule");
        listAttributes.add("glueserviceowner");
        listAttributes.add("gluehostapplicationsoftwareruntimeenvironment");
    }
    
    private boolean glue1_2 = false;
    private String timestamp = null;
    private String[] msg = null;



    public ISMClassAd(ArrayList<String> multipleAttributes, boolean glue1_2) {
        super("ISM_CLASSAD" + (glue1_2 ? "_GLUE_1.2" : ""));

        this.glue1_2 = glue1_2;

        String[] supportedQueryLang = new String[] {
                "RegEx", "ClassAd"
        };
        setSupportedQueryLang(supportedQueryLang);
    }



    public String[] apply(Hashtable<String, Object> parameters) throws Exception {
        if(parameters == null) {
            throw new Exception(getName() + " apply error: parameter is null");
        }
        
        String time = (String) parameters.get("timestamp");
        
        if(timestamp == null || !timestamp.equals(time)) {
            timestamp = time;
            msg = null;
        }

        if(msg != null) {
            logger.debug("Dialect not found: " + getName());
            return msg;
        } 
        
        String glueSchema = (String) parameters.get("glueSchema");

        if(glueSchema != null) {
            String[] ceinfo = CEGlueSchema.makeCEGlueSchemaInfo(glueSchema, !glue1_2);
            ArrayList<String> result = new ArrayList<String>();

            for (int i = 0; i < ceinfo.length; i++) {
                String[] classad = ldifToClassad(ceinfo[i]);
                for (int j = 0; j < classad.length; j++) {
                    result.add(classad[j]);
                }
            }

            msg = new String[result.size()];
            msg = (String[]) result.toArray(ceinfo);

            return msg;
        } else {
            throw new Exception(getName() +" apply error: glueSchema is null");
        }
    }

    private void parseAttribute(String attr, Hashtable<String, ArrayList<String>> attributeTable, ArrayList<String> glueCESEBindSEList) {
        String[] attribute = attr.split(": ");

        if(attribute != null && attribute.length == 2) {
            String name = attribute[0].trim();
            String value = parseAttributeValue(attribute[1]);

            if (ignoreAttributes.contains(name.toLowerCase())) {
                return;
            }

            if(name.startsWith("GlueCESEBind") && glueCESEBindSEList.size() > 0) {
                String cesebind = (String) glueCESEBindSEList.get(glueCESEBindSEList.size() - 1);
                cesebind += "\n\t\t\t" + name + " = " + value + ";";
                glueCESEBindSEList.set(glueCESEBindSEList.size() - 1, cesebind);

                return;
            }

            ArrayList<String> values = null;

            if(attributeTable.containsKey(name)) {
                values = attributeTable.get(name);
            } else {
                values = new ArrayList<String>();
                attributeTable.put(name, values);
            }

            values.add(value);
        }
    }



    private String[] ldifToClassad(String ldif) {
        String[] voViews = null;

        if(glue1_2) {
            int index = ldif.indexOf("dn: GlueVOViewLocalID");
            if(index > -1) {
                String voView = ldif.substring(index);
                ldif = ldif.substring(0, index);
                voViews = voView.split("\n\n");
            }
        }

        Hashtable<String, ArrayList<String>> ceAttributeTable = parseLDIF(ldif);
        ArrayList<Hashtable<String, ArrayList<String>>> voViewList = 
                new ArrayList<Hashtable<String, ArrayList<String>>>();
        
        ArrayList<String> ceBaseRuleList = ceAttributeTable.get("GlueCEAccessControlBaseRule");

        HashSet<String> voViewSet = new HashSet<String>();
     
        for (int i = 0; voViews != null && i < voViews.length; i++) {
            if(voViews[i] != null && voViews[i].length() != 0) {
                Hashtable<String, ArrayList<String>> voTable = parseLDIF(voViews[i]);
                ArrayList<String> list = voTable.get("GlueCEAccessControlBaseRule");
                if(list != null) {
                    voViewSet.addAll(list);
                }
                voViewList.add(voTable);
            }
        }

        HashSet<String> ceBaseRuleSet = null;
        if(ceBaseRuleList != null) {
            ceBaseRuleSet = new HashSet<String>(ceBaseRuleList);
        } else {
            ceBaseRuleSet = new HashSet<String>();
        }

        ceBaseRuleSet.removeAll(voViewSet);
        ceBaseRuleList = new ArrayList<String>(ceBaseRuleSet);

        if(!ceBaseRuleList.isEmpty()) {
            ceAttributeTable.put("GlueCEAccessControlBaseRule", ceBaseRuleList);
        }

        int numberOfClassads = voViewList.size() + (ceBaseRuleSet.size() == 0 ? 0 : 1);
        
        String[] classadList = null;
        if(numberOfClassads == 0) {
            classadList = new String[1];
            classadList[0] = makeClassad(ceAttributeTable);
        } else {
            int index = 0;
            classadList = new String[numberOfClassads];

            if(!ceBaseRuleList.isEmpty()) {
                classadList[index] = makeClassad(ceAttributeTable);
                index++;
            }

            for (int i = 0; i < voViewList.size(); i++) {
                Hashtable<String, ArrayList<String>> voTable = voViewList.get(i);
                Hashtable<String, ArrayList<String>> ceAttrClone = (Hashtable<String, ArrayList<String>>) ceAttributeTable.clone();
                ceAttrClone.putAll(voTable);
                classadList[index] = makeClassad(ceAttrClone);
                index++;
            }
        }
        return classadList;
    }



    private String makeClassad(Hashtable<String, ArrayList<String>> ceAttributeTable) {
        ArrayList<String> glueCESEBindSEList = ceAttributeTable.get("GlueCESEBindSEList");
        StringBuffer classad = new StringBuffer("[\n");

        for (Enumeration<String> e = ceAttributeTable.keys(); e.hasMoreElements();) {
            String attrName = e.nextElement();
            if(attrName.equals("GlueCESEBindSEList")) {
                continue;
            }

            ArrayList<String> attrValue = ceAttributeTable.get(attrName);

            classad.append("\t").append(attrName).append(" = ");

            if (attrValue.size()>1 || listAttributes.contains(attrName.toLowerCase())) {
                
                classad.append("{\n\t\t");
    
                for (int i = 0; i < attrValue.size(); i++) {
                    classad.append(attrValue.get(i));
                    classad.append( (i < attrValue.size() - 1) ? ",\n\t\t" : " ");
                }
    
                classad.append("\n\t};\n");
            } else {
                
                classad .append(attrValue.get(0)).append(";\n") ;
                
            }
        }

        if(glueCESEBindSEList != null && !glueCESEBindSEList.isEmpty()) {
            classad.append("\tCloseStorageElements = {\n");

           for (int i = 0; i < glueCESEBindSEList.size(); i++) {
                String cese = (String) glueCESEBindSEList.get(i);
                if(cese != null) {
                    cese += "\n\t\t\tname = GlueCESEBindSEUniqueID;\n\t\t\tmount = GlueCESEBindCEAccesspoint\n\t\t]";
                }

                classad.append(cese).append(((i < glueCESEBindSEList.size() - 1) ? ",\n" : "\n"));
            }
            classad.append("\t};\n");
        }
        
        classad.append("]");
        return classad.toString();
    }



    private Hashtable<String, ArrayList<String>> parseLDIF(String ldif) {
        Hashtable<String, ArrayList<String>> attributeTable = new Hashtable<String, ArrayList<String>>();
        String[] attributes = ldif.split("\n");

        if(attributes == null) {
            return attributeTable;
        }

        ArrayList<String> glueCESEBindSEList = new ArrayList<String>();

        for (int i = 0; i < attributes.length; i++) {
            if(attributes[i].startsWith("dn: GlueCESEBindSEUniqueID=")) {
                glueCESEBindSEList.add("\t\t[");
            }
            parseAttribute(attributes[i], attributeTable, glueCESEBindSEList);
        }

        if(!glueCESEBindSEList.isEmpty()) {
            attributeTable.put("GlueCESEBindSEList", glueCESEBindSEList);
        }
        return attributeTable;
    }



    private String parseAttributeValue(String value) {
        if(value == null) {
            return "undefined";
        }

        String value_lc = value.trim();

        while(value_lc.startsWith(" ")) {
            value_lc = value_lc.substring(1, value_lc.length());
        }

        try {
            Double.parseDouble(value_lc);
            return value_lc;
        } catch (NumberFormatException ex) {}

        if(value_lc.equalsIgnoreCase("true") || value_lc.equalsIgnoreCase("false") || value_lc.equalsIgnoreCase("undefined")) {
            return value_lc.toLowerCase();
        } else {
            value_lc = "\"" + value_lc + "\"";
            return value_lc;
        }
    }
}
