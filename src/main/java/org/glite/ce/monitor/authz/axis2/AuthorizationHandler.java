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

package org.glite.ce.monitor.authz.axis2;

import java.util.Calendar;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.log4j.Logger;
import org.glite.ce.faults.AuthorizationFault;
import org.glite.ce.monitor.configuration.CEMonServiceConfig;

import eu.emi.security.canl.axis2.CANLAXIS2SocketFactory;

public class AuthorizationHandler
    extends org.glite.ce.commonj.authz.axis2.AuthorizationHandler {

    private static final Logger logger = Logger.getLogger(AuthorizationHandler.class.getName());

    private final static String CREDENTIALS_PROXY_FILE = "proxy";

    private final static String CREDENTIALS_CERT_FILE = "cert";

    private final static String CREDENTIALS_KEY_FILE = "key";

    private final static String CREDENTIALS_KEY_PASSWD = "password";

    private final static String CA_LOCATION = "truststore";

    private final static String CRL_MODE = "crlcheckingmode";

    private final static String CACRL_REFRESH = "updateinterval";

    public AuthorizationHandler() {
        super();
    }

    public Handler.InvocationResponse invoke(MessageContext msgContext)
        throws AxisFault {

        /*
         * TODO verify conflict. This workaround resolves the conflict between
         * Argus and consumer clients. Argus client uses the trustmanager for
         * axis2, replacing the previous factory
         */

        /*
         * TODO use credentials defined into argus-pep tag, not the global ones
         */

        Properties sslConfig = new Properties();

        CEMonServiceConfig sConfiguration = CEMonServiceConfig.getConfiguration();
        if (sConfiguration == null) {
            throw getAuthorizationFault("Service is not configured", msgContext);
        }

        String tmps = sConfiguration.getGlobalAttributeAsString("gridproxyfile");
        if (tmps != "") {

            sslConfig.put(CREDENTIALS_PROXY_FILE, tmps);

        } else {

            String certFilename = sConfiguration.getGlobalAttributeAsString("sslcertfile");
            String keyFilename = sConfiguration.getGlobalAttributeAsString("sslkeyfile");
            String passwd = sConfiguration.getGlobalAttributeAsString("sslkeypasswd");
            if (passwd == null)
                passwd = "";

            if (certFilename == "" || keyFilename == "") {
                throw new RuntimeException("Missing user credentials");
            } else {
                sslConfig.put(CREDENTIALS_CERT_FILE, certFilename);
                sslConfig.put(CREDENTIALS_KEY_FILE, keyFilename);
            }

            sslConfig.put(CREDENTIALS_KEY_PASSWD, passwd);

        }

        /*
         * TODO changes in configurations: replaced sslCAfiles with
         * CApath removed; sslCRLfiles add disableCRL and sslRefreshTime;
         */

        String CALocation = sConfiguration.getGlobalAttributeAsString("CApath");
        if (CALocation != "") {
            sslConfig.put(CA_LOCATION, CALocation);
        }

        String disableCRL = sConfiguration.getGlobalAttributeAsString("disableCRL");
        if (disableCRL.equalsIgnoreCase("true")) {
            sslConfig.put(CRL_MODE, "ignore");
        } else {
            sslConfig.put(CRL_MODE, "ifvalid");
        }

        sConfiguration.getGlobalAttributeAsInt("sslRefreshTime", 3600000);
        sslConfig.put(CACRL_REFRESH, "updateinterval");

        CANLAXIS2SocketFactory.setCurrentProperties(sslConfig);

        return super.invoke(msgContext);
    }

    protected AxisFault getAuthorizationFault(String message, MessageContext msgContext) {

        SOAPFactory soapFactory;
        if (msgContext.isSOAP11()) {
            soapFactory = OMAbstractFactory.getSOAP11Factory();
        } else {
            soapFactory = OMAbstractFactory.getSOAP12Factory();
        }

        QName faultCode = new QName("http://www.w3.org/2003/05/soap-envelope", "Sender", "env");
        QName operation = this.getOperation(msgContext);
        String opName = operation == null ? "invoke" : operation.getLocalPart();
        String faultReason = "Authorization error";

        AuthorizationFault authzFault = new AuthorizationFault();
        authzFault.setDescription(message);
        authzFault.setErrorCode("0");
        authzFault.setFaultCause(faultReason);
        authzFault.setMethodName(opName);
        authzFault.setTimestamp(Calendar.getInstance());

        try {
            OMElement faultDetail = authzFault.getOMElement(null, soapFactory);
            return new AxisFault(faultCode, faultReason, null, null, faultDetail);
        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
        }

        return new AxisFault(faultReason);
    }

}
