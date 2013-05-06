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

import org.apache.log4j.Logger;

public class CEGlueSchema {
    
    private final static Logger logger = Logger.getLogger(CEGlueSchema.class.getName());
    
    public static final int GlueCE = 0;

    public static final int GlueCluster = 1;

    public static final int GlueSubCluster = 2;

    public static final int GlueCESEBindSE = 3;

    public static final int GlueCESEBindGroupCE = 4;

    public static final int GlueVOView = 5;

    public static final String[] keys = new String[] { "GlueCE", "GlueCluster", "GlueSubCluster", "GlueCESEBindSE",
            "GlueCESEBindGroupCE", "GlueVOView" };

    public static String[] makeCEGlueSchemaInfo(String glueSchema, boolean ignoreGlueVOView) {
        if (glueSchema == null) {
            return null;
        }

        String[] msg = null;
        String[] dn = glueSchema.split("dn");

        ArrayList<String>[] elements = new ArrayList[6];
        elements[GlueCE] = new ArrayList<String>();
        elements[GlueCluster] = new ArrayList<String>();
        elements[GlueSubCluster] = new ArrayList<String>();
        elements[GlueCESEBindSE] = new ArrayList<String>();
        elements[GlueCESEBindGroupCE] = new ArrayList<String>();
        elements[GlueVOView] = new ArrayList<String>();

        for (int i = 0; i < dn.length; i++) {
            dn[i] = "dn" + dn[i];
            int type = checkElementType(dn[i]);
            if (type != -1) {
                elements[type].add(dn[i]);
            }
        }

        msg = new String[elements[0].size()];

        for (int x = 0; x < elements[GlueCE].size(); x++) {
            String ceInfo = (String) elements[GlueCE].get(x);
            String ceUniqueID, clusterUniqueID;

            try {
                ceUniqueID = getID(ceInfo, "dn: GlueCEUniqueID", ",").trim();
                clusterUniqueID = getID(ceInfo, "GlueForeignKey", "\n").trim();

                ArrayList<String> clusterInfo = getElements(elements[GlueCluster], clusterUniqueID,
                        "GlueClusterUniqueID=", "dn: GlueClusterUniqueID", ",");
                for (int i = 0; i < clusterInfo.size(); i++) {
                    ceInfo += "\n\n" + clusterInfo.get(i);

                    ArrayList<String> subclusterInfo = getElements(elements[GlueSubCluster], clusterUniqueID, "",
                            "GlueChunkKey", "\n");
                    ceInfo += "\n\n" + elementsToString(subclusterInfo);
                }

                ArrayList<String> bindGroupInfo = getElements(elements[GlueCESEBindGroupCE], ceUniqueID, "",
                        "GlueCESEBindGroupCEUniqueID:", "\n");
                ceInfo += "\n\n" + elementsToString(bindGroupInfo);

                ArrayList<String> bindInfo = getElements(elements[GlueCESEBindSE], ceUniqueID, "",
                        "GlueCESEBindCEUniqueID:", "\n");
                ceInfo += "\n\n" + elementsToString(bindInfo);

                if (!ignoreGlueVOView) {
                    ArrayList<String> voView = getElements(elements[GlueVOView], ceUniqueID, "", "GlueCEUniqueID", ",");
                    ceInfo += "\n\n" + elementsToString(voView);
                }
            } catch (Throwable th) {
                
                logger.error(th.getMessage(), th);
                
            }

            msg[x] = ceInfo;
        }

        return msg;
    }

    private static String getID(String dn, String key, String delim)
        throws Exception {
        if (dn == null || key == null) {
            throw (new Exception("dn or key is null!"));
        }

        int index = dn.indexOf(key);

        if (index > -1) {
            index += key.length() + 1;
            String id = dn.substring(index, dn.indexOf(delim, index + 1));
            return id;
        } else {
            throw (new Exception("key " + key + " not found!"));
        }
    }

    private static int checkElementType(String dn) {
        for (int i = 0; i < keys.length; i++) {
            if (dn.startsWith("dn: " + keys[i] + "UniqueID") || dn.startsWith("dn: " + keys[i] + "LocalID")) {
                return i;
            }
        }

        return -1;
    }

    private static ArrayList<String> getElements(ArrayList<String> elements, String uniqueID, String prefix,
            String key, String delim)
        throws Exception {
        ArrayList<String> list = new ArrayList<String>();

        for (int y = 0; y < elements.size(); y++) {
            String info = (String) elements.get(y);
            String id = getID(info, key, delim).trim();

            if (uniqueID.equals(prefix + id)) {
                list.add(info);
            }
        }

        return list;
    }

    private static String elementsToString(ArrayList<String> elements) {
        String result = "";

        if (elements != null) {
            for (int i = 0; i < elements.size(); i++) {
                String info = (String) elements.get(i);

                if (info != null) {
                    result += "\n\n" + elements.get(i);
                }
            }
        }

        return result;
    }

}
