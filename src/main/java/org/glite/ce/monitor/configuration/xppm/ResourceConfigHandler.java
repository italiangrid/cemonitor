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

package org.glite.ce.monitor.configuration.xppm;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import javax.xml.xpath.XPathExpression;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.configuration.CommonConfigException;
import org.glite.ce.commonj.configuration.xppm.ConfigurationHandler;
import org.glite.ce.commonj.utils.JarClassLoader;
import org.glite.ce.monitorapij.resource.Resource;
import org.glite.ce.monitorapij.resource.types.Property;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public abstract class ResourceConfigHandler
    extends ConfigurationHandler {

    private static Logger logger = Logger.getLogger(ResourceConfigHandler.class.getName());

    protected static final String RES_ID_ATTR = "id";

    protected static final String RES_NAME_ATTR = "name";

    protected static final String RES_JAR_ATTR = "jarpath";

    protected static final String RES_TYPE_ATTR = "type";

    private static final String PARAMETER_TAG = "property";

    private static final String PARA_NAME_ATTR = "name";

    private static final String PARA_VALUE_ATTR = "value";

    protected XPathExpression expr;

    protected ArrayList<Resource> currentResList;

    protected ArrayList<Resource> tmpResList;

    public XPathExpression getXPath() {
        return expr;
    }

    public Object[] getConfigurationElement() {
        if (currentResList != null) {
            Object[] result = new Object[currentResList.size()];
            currentResList.toArray(result);
            return result;
        }
        return null;
    }

    public boolean process(NodeList parsedElements)
        throws CommonConfigException {

        tmpResList = new ArrayList<Resource>(parsedElements.getLength());

        for (int k = 0; k < parsedElements.getLength(); k++) {
            Element resElement = (Element) parsedElements.item(k);

            String id = resElement.getAttribute(RES_ID_ATTR);
            String jar = resElement.getAttribute(RES_JAR_ATTR);
            String name = resElement.getAttribute(RES_NAME_ATTR);
            String type = resElement.getAttribute(RES_TYPE_ATTR);

            Resource resource = createResource(id, name, type, jar);
            if (resource == null) {
                throw new CommonConfigException("Cannot instantiate resource " + id);
            }

            NodeList paraElemList = resElement.getElementsByTagName(PARAMETER_TAG);
            Property[] allParams = new Property[paraElemList.getLength()];

            for (int j = 0; j < paraElemList.getLength(); j++) {
                Element paraElement = (Element) paraElemList.item(j);

                String paraName = paraElement.getAttribute(PARA_NAME_ATTR);
                String paraValue = paraElement.getAttribute(PARA_VALUE_ATTR);
                if (paraName != "") {
                    allParams[j] = new Property(paraName, paraValue);
                    logger.debug("Parameter " + id + "[" + paraName + "] = " + paraValue);
                } else {
                    throw new CommonConfigException("Missing parameter name in " + id);
                }
            }

            resource.setProperty(allParams);
            tmpResList.add(resource);

        }

        return !tmpResList.equals(currentResList);
    }

    public boolean processTriggers()
        throws CommonConfigException {
        return false;
    }

    public void commit() {
        currentResList = tmpResList;
        tmpResList = null;
    }

    public void rollback() {
        tmpResList = null;
    }

    public File[] getTriggers() {
        return null;
    }

    public void clean() {
    }

    protected Resource createResource(String id, String name, String type, String jarFilename) {
        try {
            File jarFile = new File(jarFilename);
            logger.debug("JAR filename: " + jarFilename);
            URL resourceURL = jarFile.toURI().toURL();
            JarClassLoader loader = new JarClassLoader("file://" + jarFile.getPath(), this.getClass().getClassLoader());
            loader.addJarURL(resourceURL);
            String className = loader.getMainClassName(resourceURL);
            if (className == null) {
                throw new IOException("\"Main-Class\" attribute not found into the MANIFEST.MF of "
                        + jarFile.getAbsolutePath());
            }

            Class<?> resourceClass = loader.loadClass(className);
            logger.debug("Loaded class: " + resourceClass.getName());

            Resource result = (Resource) resourceClass.newInstance();
            result.setId(id);
            result.setName(name);
            result.setType(type);
            result.setJarPath(new URI("file://" + jarFile.getAbsolutePath()));
            result.setCreationTime(new GregorianCalendar());

            return result;

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        return null;
    }

}
