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
import java.util.Hashtable;

import org.glite.ce.monitor.ceplugin.CEGlueSchema;
import org.glite.ce.monitorapij.sensor.SensorOutputDataFormat;

public class ISMLDIF extends SensorOutputDataFormat {
    private String multipleAttributes = "";
    private boolean glue1_2 = false;
    private String timestamp = null;
    private String[] msg = null;



    public ISMLDIF(ArrayList<String> multipleAttributes, boolean glue1_2) {
        super("ISM_LDIF" + (glue1_2 ? "_GLUE_1.2": ""));
        this.glue1_2 = glue1_2;        

        for (int i = 0; i < multipleAttributes.size(); i++) {
            this.multipleAttributes += multipleAttributes.get(i) + "\n";
        }

        String[] supportedQueryLang = new String[] {
                "RegEx", "ClassAd"
        };
        setSupportedQueryLang(supportedQueryLang);        
    }



    public String[] apply(Hashtable<String, Object> parameters) throws Exception {
        if(parameters == null) {
            throw (new Exception(getName() + " apply error: parameter is null"));
        }
        
        String time = (String) parameters.get("timestamp");
        
        if(timestamp == null || !timestamp.equals(time)) {
            timestamp = time;
            msg = null;
        }
        
        if(msg != null) {
            return msg;
        } 
        
        String glueSchema = (String) parameters.get("glueSchema");

        if(glueSchema != null) {
            String[] ceinfo = CEGlueSchema.makeCEGlueSchemaInfo(glueSchema, !glue1_2);
            msg = new String[ceinfo.length + 1];
            msg[0] = multipleAttributes;

            System.arraycopy(ceinfo, 0, msg, 1, ceinfo.length);

            return msg;
        } else {
            throw new Exception(getName() + " apply error: glueSchema is null");
        }
    }

}
