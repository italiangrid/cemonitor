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
import java.net.URI;
import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.configuration.CommonConfigException;
import org.glite.ce.commonj.configuration.xppm.ConfigurationHandler;
import org.glite.ce.commonj.configuration.xppm.ConfigurationManager;
import org.glite.ce.monitorapij.resource.types.Action;
import org.glite.ce.monitorapij.resource.types.Dialect;
import org.glite.ce.monitorapij.resource.types.Policy;
import org.glite.ce.monitorapij.resource.types.Query;
import org.glite.ce.monitorapij.resource.types.SubscriptionPersistent;
import org.glite.ce.monitorapij.resource.types.Topic;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SubscriptionConfigHandler
    extends ConfigurationHandler {

    private static Logger logger = Logger.getLogger(SubscriptionConfigHandler.class.getName());

    private static final String XPATH_STRING = "/service/subscription";

    protected static final String SUBS_ID_ATTR = "id";

    protected static final String SUBS_URL_ATTR = "monitorconsumerurl";

    protected static final String SUBS_PROTO_ATTR = "sslprotocol";

    protected static final String SUBS_CRED_ATTR = "credentialfile";

    protected static final String SUBS_PWD_ATTR = "sslkeypasswd";

    protected static final String SUBS_SUBID_ATTR = "subscriberid";

    protected static final String SUBS_SUBGRP_ATTR = "subscribergroup";

    protected static final String SUBS_RETRY_ATTR = "retrycount";

    protected static final String TOPIC_TAG = "topic";

    protected static final String TOPIC_NAME_ATTR = "name";

    protected static final String DIALECT_TAG = "dialect";

    protected static final String DIALECT_NAME_ATTR = "name";

    protected static final String POLICY_TAG = "policy";

    protected static final String POLICY_RATE_ATTR = "rate";

    protected static final String QUERY_TAG = "query";

    protected static final String QUERY_LANG_ATTR = "querylanguage";

    protected static final String ACTION_TAG = "action";

    protected static final String ACTION_NAME_ATTR = "name";

    protected static final String ACTION_DO_ATTR = "doactionwhenqueryis";

    protected XPathExpression expr;

    protected ArrayList<SubscriptionPersistent> currentResList;

    protected ArrayList<SubscriptionPersistent> tmpResList;

    public SubscriptionConfigHandler() throws XPathExpressionException, XPathFactoryConfigurationException {

        XPath xpath = ConfigurationHandler.getXPathFactory().newXPath();
        expr = xpath.compile(XPATH_STRING);

        currentResList = null;
        tmpResList = null;

    }

    public Class<?> getCategory() {
        return SubscriptionPersistent.class;
    }

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

        tmpResList = new ArrayList<SubscriptionPersistent>(parsedElements.getLength());

        for (int k = 0; k < parsedElements.getLength(); k++) {
            Element resElement = (Element) parsedElements.item(k);

            String id = resElement.getAttribute(SUBS_ID_ATTR);
            if (id == "") {
                throw new CommonConfigException("Missing id in subscription tag");
            }

            URI consumerURI = null;
            try {
                consumerURI = new URI(resElement.getAttribute(SUBS_URL_ATTR));
            } catch (Exception ex) {
                throw new CommonConfigException(ex.getMessage(), ex);
            }

            int retryCount = -1;
            try {
                resElement.getAttribute(SUBS_RETRY_ATTR);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }

            String credFilename = resElement.getAttribute(SUBS_CRED_ATTR);
            String keyPwd = resElement.getAttribute(SUBS_PWD_ATTR);
            String sslProto = resElement.getAttribute(SUBS_PROTO_ATTR);
            String subscriberId = resElement.getAttribute(SUBS_SUBID_ATTR);
            String subscriberGroup = resElement.getAttribute(SUBS_SUBGRP_ATTR);

            SubscriptionPersistent currSubscr = new SubscriptionPersistent();
            currSubscr.setId(id);
            currSubscr.setMonitorConsumerURL(consumerURI);
            currSubscr.setMaxRetryCount(retryCount);
            if (credFilename != "") {
                currSubscr.setCredentialFile(credFilename);
            }
            if (keyPwd != "") {
                currSubscr.setPassphrase(keyPwd);
            }
            if (sslProto != "") {
                currSubscr.setSSLProtocol(sslProto);
            }
            if (subscriberId != "") {
                currSubscr.setSubscriberId(subscriberId);
            }
            if (subscriberGroup != "") {
                currSubscr.setSubscriberGroup(subscriberGroup);
            }

            NodeList topicList = resElement.getElementsByTagName(TOPIC_TAG);
            if (topicList.getLength() == 0) {
                throw new CommonConfigException("Missing topic tag in" + id);
            }
            Element topicElem = (Element) topicList.item(0);
            Topic currTopic = new Topic();
            currTopic.setName(topicElem.getAttribute(TOPIC_NAME_ATTR));
            currSubscr.setTopic(currTopic);

            NodeList dialectList = topicElem.getElementsByTagName(DIALECT_TAG);
            if (dialectList.getLength() > 0) {

                Dialect[] dialects = new Dialect[dialectList.getLength()];
                for (int j = 0; j < dialectList.getLength(); j++) {
                    Element diaElem = (Element) dialectList.item(j);
                    String tmps = diaElem.getAttribute(DIALECT_NAME_ATTR);
                    if (tmps == "") {
                        throw new CommonConfigException("Missing dialect name in " + id);
                    }
                    dialects[j] = new Dialect(tmps);
                }

                currTopic.setDialect(dialects);
            }

            NodeList policyList = resElement.getElementsByTagName(POLICY_TAG);
            if (policyList.getLength() == 0) {
                throw new CommonConfigException("Missing policy in " + id);
            }
            Element policyElem = (Element) policyList.item(0);
            Policy currPolicy = new Policy();
            currSubscr.setPolicy(currPolicy);
            try {
                currPolicy.setRate(Integer.parseInt(policyElem.getAttribute(POLICY_RATE_ATTR)));
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }

            NodeList queryList = policyElem.getElementsByTagName(QUERY_TAG);
            if (queryList.getLength() == 0) {
                throw new CommonConfigException("Missing query in " + id);
            }
            Element queryElem = (Element) queryList.item(0);
            String qLang = queryElem.getAttribute(QUERY_LANG_ATTR);
            String qText = queryElem.getTextContent();
            Query currQuery = new Query();
            currPolicy.setQuery(currQuery);
            currQuery.setQueryLanguage(qLang);
            currQuery.setExpression(qText);

            NodeList actionList = policyElem.getElementsByTagName(ACTION_TAG);
            if (actionList.getLength() > 0) {

                Action[] actions = new Action[actionList.getLength()];

                for (int j = 0; j < actionList.getLength(); j++) {
                    Element actElem = (Element) actionList.item(j);
                    Action action = new Action();
                    action.setName(actElem.getAttribute(ACTION_NAME_ATTR));
                    action.setDoActionWhenQueryIs(actElem.getAttribute(ACTION_DO_ATTR).equalsIgnoreCase("true"));
                    actions[j] = action;
                }

                currPolicy.setAction(actions);

            }

            tmpResList.add(currSubscr);

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

    public static void main(String[] args) {
        org.apache.log4j.PropertyConfigurator.configure("/tmp/log4j.properties");

        try {
            ConfigurationManager cMan = new ConfigurationManager(args[0]);
            Object[] tmpArray = cMan.getConfigurationElements(SubscriptionPersistent.class);

            logger.info("Found subscriptions: " + tmpArray.length);
            for (Object obj : tmpArray) {
                SubscriptionPersistent subscription = (SubscriptionPersistent) obj;
                logger.debug("Found " + subscription.getId() + ": " + subscription.getMonitorConsumerURL());
            }
        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
        }
    }
}
