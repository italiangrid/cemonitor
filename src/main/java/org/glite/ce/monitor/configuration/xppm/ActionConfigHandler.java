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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.configuration.xppm.ConfigurationHandler;
import org.glite.ce.commonj.configuration.xppm.ConfigurationManager;
import org.glite.ce.monitorapij.resource.types.Action;

public class ActionConfigHandler
    extends ResourceConfigHandler {

    private static Logger logger = Logger.getLogger(ActionConfigHandler.class.getName());

    private static final String XPATH_STRING = "/service/action";

    public ActionConfigHandler() throws XPathExpressionException, XPathFactoryConfigurationException {

        XPath xpath = ConfigurationHandler.getXPathFactory().newXPath();
        expr = xpath.compile(XPATH_STRING);

        currentResList = null;
        tmpResList = null;

    }

    public Class<?> getCategory() {
        return Action.class;
    }

    public static void main(String[] args) {
        org.apache.log4j.PropertyConfigurator.configure("/tmp/log4j.properties");

        try {
            ConfigurationManager cMan = new ConfigurationManager(args[0]);
            Object[] tmpArray = cMan.getConfigurationElements(Action.class);

            logger.info("Found actions: " + tmpArray.length);
            for (Object obj : tmpArray) {
                Action action = (Action) obj;
                logger.debug("Found " + action.getName() + ": " + action.getClass().getName());
            }
        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
        }
    }

}
