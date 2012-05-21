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

package org.glite.ce.monitor.holder;

import org.glite.ce.monitorapij.resource.Resource;
import org.glite.ce.monitorapij.sensor.Sensor;


/**
 * This class is an extension of <code>ResourceHolder</code> class, characterized for <code>Sensor</code>s.
 * It is used to handle a list of <code>Sensor</code>s placed in a specified directory as plugins.
 * An XML config file is used to store and retreive informations about these Sensors.
 * The task of checking if Sensors are added or removed or modified can be scheduled for repeated execution.
 */

public class SensorHolder extends BasicResourceHolder {
    
    /**
     * Creates a new SensorHolder object.
     *
     * @param context The axis <code>MessageContext</code>.
     * @param actionPath The path to the plugin jar file relative to this sensor. 
     * @param actionConfig The path of the configuration file for the plugin.
     * @throws InstantiationException 
     */
    public SensorHolder() throws InstantiationException {
        super();
    }

    /**
     * Get the <code>Sensor</code> saved in the specified position.
     *
     * @param id The position of the searched Sensor in the resource Array.
     *
     * @return The casted <code>Sensor</code> positioned as specified int the resource array. 
     * @see org.glite.ce.monitorapij.resource.ResourceHolder.getResource(int arg0).
     */
    public Sensor getSensorById(String id) {
        return (Sensor) getResource(id);
    }
    

    public Sensor getSensorByName(String id) {
        Sensor[] sensors = getSensors();

        for (int i = 0; i < sensors.length; i++) {
            if (sensors[i].getName().equalsIgnoreCase(id)) {
                return sensors[i];
            }
        }

        return null;
    }

    public Sensor getSensorByType(String type) {
        Sensor[] sensors = getSensors();

        for (int i = 0; i < sensors.length; i++) {
            if (sensors[i].getType().equalsIgnoreCase(type)) {
                return sensors[i];
            }
        }

        return null;
    }

    /**
     * Get the <code>Sensor</code> Array casted from resources array.
     *
     * @return The <code>Sensor</code> Array casted from resources array.
     * @see org.glite.ce.monitorapij.resource.ResourceHolder.getResources().
     */
    public Sensor[] getSensors() {
        Resource[] resource = getResources();
        Sensor[] result = new Sensor[resource.length];
        System.arraycopy(resource, 0, result, 0, resource.length);

        return result;
    }

    public String getCategory(){
        return "sensor";
    }
    
}
