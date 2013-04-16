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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.glite.ce.monitorapij.resource.types.Property;
import org.glite.ce.monitorapij.sensor.AbstractSensor;
import org.glite.ce.monitorapij.sensor.Sensor;
import org.glite.ce.monitorapij.sensor.SensorException;

public class CESensor
    extends AbstractSensor {

    private static final long serialVersionUID = 1321622683;

    private final static Logger logger = Logger.getLogger(CESensor.class.getName());

    public static final String SCRIPT_URL_ATTR = "scriptURI";

    public static final String EXEC_DELAY_ATTR = "executionDelay";

    public static final String MULTI_ATTRS_ATTR = "multiple_attributes";

    private String scriptURI;

    private String multipleAttributes;

    public CESensor() throws SensorException {
        super("CE Sensor", "CE_MONITOR");

        StringBuffer buff = new StringBuffer();
        buff.append("GlueChunkKey,");
        buff.append("GlueForeignKey,");
        buff.append("GlueServiceName,");
        buff.append("GlueServiceVersion,");
        buff.append("GlueServiceEndpoint,");
        buff.append("GlueServiceStatusInfo,");
        buff.append("GlueServiceSemantics,");
        buff.append("GlueServiceOwner,");
        buff.append("GlueServiceDataValue,");
        buff.append("GlueSiteName,");
        buff.append("GlueSiteSponsor,");
        buff.append("GlueSiteOtherInfo,");
        buff.append("GlueCEAccessControlBaseRule,");
        buff.append("GlueClusterService,");
        buff.append("GlueHostApplicationSoftwareRunTimeEnvironment,");
        buff.append("GlueHostLocalFileSystemClient,");
        buff.append("GlueHostRemoteFileSystemServer,");
        buff.append("GlueCESEBindGroupSEUniqueID,");
        buff.append("GlueSEHostingSL,");
        buff.append("GlueSEArchitecture,");
        buff.append("GlueSEType,");
        buff.append("GlueSEAccessProtocolSupportedSecurity,");
        buff.append("GlueSEAccessProtocolCapability,");
        buff.append("GlueSEControlProtocolCapability,");
        buff.append("GlueSLServiceGlueSLLocalFileSystemClient,");
        buff.append("GlueSAAccessControlBaseRule");

        buff.append("GlueServiceAccessPointURL");
        buff.append("GlueServiceAccessControlRule");
        buff.append("GlueInformationServiceURL");
        buff.append("GlueHostService");
        buff.append("GlueCESEBindGroupSEUniqueID");
        buff.append("GlueSEHostingSL");
        buff.append("GlueSLService");
        buff.append("GlueSLLocalFileSystemClient");

        scriptURI = "/usr/libexec/glite-ce-info";
        multipleAttributes = buff.toString();

        Property[] properties = new Property[] { new Property(EXEC_DELAY_ATTR, "60000"),
                new Property(SCRIPT_URL_ATTR, scriptURI), new Property(MULTI_ATTRS_ATTR, multipleAttributes) };

        setProperty(properties);
        setEventOverwriteModeActive(true);
        setScope(Sensor.HIGH);

    }

    public void init()
        throws SensorException {

        super.init();

        Property scriptURIproperty = getProperty(SCRIPT_URL_ATTR);

        scriptURI = scriptURIproperty.getValue();

        ArrayList<String> multiAttributesList = new ArrayList<String>();

        Property property = getProperty(MULTI_ATTRS_ATTR);
        if (property != null) {
            String mAttrStr = property.getValue();

            if (mAttrStr != null) {
                String[] attributes = mAttrStr.split(",");
                StringBuffer buff = new StringBuffer();

                for (int i = 0; i < attributes.length; i++) {
                    buff.append(attributes[i]).append("\n");
                    multiAttributesList.add(attributes[i].toLowerCase());
                }

                multipleAttributes = buff.toString();
            }
        } else {

            throw new SensorException("Cannot retrieve property " + MULTI_ATTRS_ATTR);

        }

        addFormat(new ISMClassAd(multiAttributesList, true));
        addFormat(new ISMClassAd(multiAttributesList, false));
        addFormat(new ISMLDIF(multiAttributesList, true));
        addFormat(new ISMLDIF(multiAttributesList, false));

    }

    public void execute()
        throws SensorException {

        try {

            if (scriptURI == null) {
                logger.error("scriptURI not found: " + scriptURI);
                throw new SensorException(SensorException.ERROR, "scriptURI not found!");
            }

            logger.debug("Executing " + scriptURI);
            Process proc = Runtime.getRuntime().exec(scriptURI, null);

            InternalPipeReader outReader = new InternalPipeReader(proc.getInputStream());
            InternalPipeReader errReader = new InternalPipeReader(proc.getErrorStream());

            outReader.start();
            errReader.start();

            proc.waitFor();
            if (proc.exitValue() != 0) {

                logger.error("Sensor execution failed: " + errReader.getData());

            } else {

                logger.debug("Firing sensor event");
                fireSensorEvent(new CESensorEvent(this, System.currentTimeMillis(), outReader.getData(),
                        multipleAttributes));

            }

        } catch (Exception ex) {

            logger.error(ex.getMessage(), ex);
            throw new SensorException(SensorException.ERROR, ex.getMessage());

        }
    }

    private class InternalPipeReader
        extends Thread {

        private StringBuffer buffer;

        private InputStream inStream;

        public InternalPipeReader(InputStream in) {
            buffer = new StringBuffer();
            inStream = in;
        }

        public void run() {
            BufferedReader reader = null;

            try {
                reader = new BufferedReader(new InputStreamReader(inStream));

                String line = reader.readLine();
                while (line != null) {
                    buffer.append(line).append("\n");
                    line = reader.readLine();
                }

            } catch (Throwable th) {

                logger.error(th.getMessage(), th);

            } finally {

                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }

            }
        }

        public String getData() {
            this.interrupt();
            return buffer.toString();
        }
    }

    public static void main(String[] args) {
        /*
         * TODO moved this code into unit test
         */
        org.apache.log4j.PropertyConfigurator.configure("/tmp/log4j.properties");

        String probeScript = args[0];
        try {
            CESensor sensor = new CESensor();
            Property multiAttrProp = sensor.getProperty(MULTI_ATTRS_ATTR);
            Property[] properties = new Property[] { new Property(EXEC_DELAY_ATTR, "60000"),
                    new Property(SCRIPT_URL_ATTR, probeScript), multiAttrProp };
            sensor.setProperty(properties);
            sensor.init();
            sensor.execute();
            /*
             * TODO retrieve glue from CESensorEvent
             */
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

}
