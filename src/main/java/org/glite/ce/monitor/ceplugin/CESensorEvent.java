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

import org.apache.log4j.Logger;
import org.glite.ce.monitorapij.sensor.Sensor;
import org.glite.ce.monitorapij.sensor.SensorEvent;
import org.glite.ce.monitorapij.sensor.SensorException;

public class CESensorEvent
    extends SensorEvent {

    private static final long serialVersionUID = 1321627928;

    private final static Logger logger = Logger.getLogger(CESensorEvent.class.getName());

    public CESensorEvent(Sensor source, long when, String glueSchema) {
        this(source, when, glueSchema, null);
    }

    public CESensorEvent(Sensor source, long when, String glueSchema, String multiple_attributes) {
        super(source, -1);
        this.setName("CESensorEvent");

        if (glueSchema != null) {
            while (glueSchema.startsWith("\n")) {
                glueSchema = glueSchema.substring(1, glueSchema.length() - 1);
            }
        }

        addParameter("glueSchema", glueSchema);
        addParameter("multipleAttributes", multiple_attributes);
        addParameter("timestamp", "" + when);

        try {

            applyFormat(source.getDefaultFormat());

        } catch (SensorException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

}
