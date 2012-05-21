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

import java.util.ArrayList;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.glite.ce.monitorapij.sensor.SensorEvent;

public final class SensorEventArrayList
    implements Cloneable {
    private ArrayList<String> eventList = null;

    private DirContext rootCtx;

    private SensorHolder sensorHolder;

    public SensorEventArrayList() {
        this.eventList = new ArrayList<String>(0);
    }

    public SensorEventArrayList(DirContext rootCtx) {
        this.eventList = new ArrayList<String>(0);
        this.rootCtx = rootCtx;
    }

    public SensorEventArrayList(final ArrayList<String> eventList, DirContext rootCtx) {
        this.eventList = (eventList == null ? new ArrayList<String>(0) : eventList);
        this.rootCtx = rootCtx;
    }

    public void setSensorHolder(SensorHolder sensorHolder) {
        this.sensorHolder = sensorHolder;
    }

    public ArrayList<String> getEventList() {
        return eventList;
    }

    public DirContext getDirContext() {
        return rootCtx;
    }

    public void setDirContext(DirContext rootCtx) {
        this.rootCtx = rootCtx;
    }

    public void addAll(SensorEventArrayList list) {
        if (list != null) {
            eventList.addAll(list.getEventList());
        }
    }

    public void add(String name) {
        if (name != null) {
            eventList.add(name);
        }
    }

    public void add(String[] name) {
        if (name == null) {
            return;
        }

        for (int i = 0; i < name.length; i++) {
            eventList.add(name[i]);
        }
    }

    public synchronized SensorEventArrayList drain(int index)
        throws IndexOutOfBoundsException {
        if (index < 0) {
            
            throw new IndexOutOfBoundsException("the index is out of range (index < 0)");
            
        } else if (index >= eventList.size()) {
            
            index = eventList.size();
            
        }

        SensorEventArrayList newList = new SensorEventArrayList(new ArrayList<String>(eventList.subList(0, index)),
                rootCtx);
        newList.setSensorHolder(sensorHolder);
        for (int i = 0; i < index; i++) {
            eventList.remove(0);
        }

        return newList;
    }

    public SensorEvent get(int index) {
        if (index < 0 || index > eventList.size() - 1 || rootCtx == null) {
            return null;
        }
        try {
            SensorEvent event = (SensorEvent) rootCtx.lookup((String) eventList.get(index));
            if (event != null && sensorHolder != null) {
                event.setSource(sensorHolder.getSensorByType(event.getProducer()));
            }

            return event;
        } catch (NamingException ex) {
            return null;
        }
    }

    public SensorEvent remove(int index) {
        if (index >= 0 || index < eventList.size()) {
            String name = (String) eventList.remove(index);
            try {
                SensorEvent event = (SensorEvent) rootCtx.lookup(name);
                if (event != null && sensorHolder != null) {
                    event.setSource(sensorHolder.getSensorByName(event.getProducer()));
                }

                return event;
            } catch (NamingException ex) {
                return null;
            }
        }

        return null;
    }

    public int size() {
        return eventList.size();
    }

    public boolean isEmpty() {
        return eventList.isEmpty();
    }

    public void clear() {
        eventList.clear();
    }

    public Object clone() {
        SensorEventArrayList result = new SensorEventArrayList();
        result.eventList = (ArrayList<String>) eventList.clone();
        result.rootCtx = rootCtx;
        return result;
    }
}
