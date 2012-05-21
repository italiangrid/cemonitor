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

import java.util.Enumeration;        
import javax.naming.NameClassPair;
import javax.naming.directory.DirContext; 
import org.glite.ce.monitorapij.sensor.SensorEvent;  

 
            
public class SensorEventEnumeration implements Enumeration {
    private Enumeration eventEnum = null;
    private DirContext rootCtx;
    private String topicName, eventGroup;
    
    protected SensorEventEnumeration(String topicName, String eventGroup, DirContext ctx) {
        this.rootCtx = ctx;
        this.topicName = topicName;
        this.eventGroup = eventGroup;
        try {
            eventEnum = rootCtx.list(topicName + "/" + eventGroup);
        } catch(Exception e) {}        
    }
    
    public boolean hasMoreElements() {
        if(eventEnum != null) {
            return eventEnum.hasMoreElements();
        }
        
        return false;        
    }
        
    public Object nextElement() {
        if(eventEnum == null || rootCtx == null) {
            return null;
        }
                
        NameClassPair item = (NameClassPair) eventEnum.nextElement();
        if(item != null) {   
            try {         
                return (SensorEvent) rootCtx.lookup(topicName + "/" + eventGroup + "/" + item.getName());    
            } catch(Exception e) {}                                   
        }
        
        return null;
    }
    
    
    public SensorEvent nextSensorEvent() {
        return (SensorEvent)nextElement();
    }
    
}
