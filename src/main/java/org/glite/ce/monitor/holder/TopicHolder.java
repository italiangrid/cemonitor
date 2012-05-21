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

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.glite.ce.commonj.utils.Timer;
import org.glite.ce.commonj.utils.TimerTask;
import org.glite.ce.monitor.configuration.CEMonServiceConfig;
import org.glite.ce.monitorapij.resource.types.Topic;
import org.glite.ce.monitorapij.sensor.Sensor;
import org.glite.ce.monitorapij.sensor.SensorEvent;

/**
 * This class manages a list of all <code>Topic</code>s supported by the
 * CEMonitor and a list of related events.
 */
public final class TopicHolder {
    
    
    private final class EventRemover extends TimerTask {
        public EventRemover(String topicName) {
            super(topicName);
        }

        public void run() {
            if (backend == null) {
                logger.warn("EventRemover: backend is null!");
                return;
            }

            String[] eventGroupArray = backend.getTopicEventGroups(getName());
            SensorEventEnumeration eventEnum = null;

            for (int i = 0; i < eventGroupArray.length; i++) {
                logger.info("EventRemover: removing expired events for the topic: " + getName() + " [event group: " + eventGroupArray[i] + "] ... ");
                eventEnum = backend.getTopicEventsEnumeration(getName(), eventGroupArray[i]);
                SensorEvent event = null;

                while (eventEnum.hasMoreElements()) {
                    event = eventEnum.nextSensorEvent();

                    if (event != null && event.isExpired()) {
                        try {
                            logger.debug("EventRemover: removing event from topic: " + getName() + " [event group: " + eventGroupArray[i] + "] event id = " + event.getName());

                            backend.removeTopicEvent(event);
                        } catch (EventNotFoundException e) {
                            logger.error(e.getMessage(), e);
                        } catch (IllegalArgumentException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                    event = null;
                }

                logger.info("EventRemover: removing expired events for the topic: " + getName() + " [event group: " + eventGroupArray[i] + "] ... DONE!!!!!");
            }
        }
    }
    
    private final static Logger logger = Logger.getLogger(TopicHolder.class.getName());
    private int eventId = 0;
    private Timer timer = null;
    private TopicEventInfoPersistent backend = null;
    private Hashtable<String, EventRemover> eventRemoverTable;
    private SensorHolder sensorHolder;

    /**
     * Creates a new TopicHolder object.
     */
    public TopicHolder(SensorHolder sensorHolder) {
        timer = new Timer("TopicHolder_TIMER", true);
        eventRemoverTable = new Hashtable<String, EventRemover>(0);
        this.sensorHolder = sensorHolder;
        
        String cachePath = null;
        int cacheSize = 100;

        CEMonServiceConfig sConfiguration = CEMonServiceConfig.getConfiguration();
        if (sConfiguration!=null) {
            cachePath = sConfiguration.getGlobalAttributeAsString("backendLocation");
            cacheSize = sConfiguration.getGlobalAttributeAsInt("backendCacheSize", 100);
        }

        if (cachePath==null) cachePath = "/var/cache/cemonitor";
        File dir = new File(cachePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            backend = new TopicEventInfoPersistent(sensorHolder, "org.glite.ce.commonj.jndi.provider.fscachedprovider.CEGeneralDirContextFactory", cachePath, cacheSize);
        } catch (NamingException e) {
            logger.error("CTOR() - NamingException catched: [" + e.getMessage() + "]. Follow the Stack trace:", e);
        }
    }

    public void destroy() {
        Iterator<EventRemover> iterator = eventRemoverTable.values().iterator();
        
        while(iterator.hasNext()) {        
            ((EventRemover)iterator.next()).cancel();            
        }
        
        timer.cancel();
        timer.purge();
        
        eventRemoverTable.clear();
    }

    /**
     * Add a <code>Topic</code> to the list of supported topics.
     * 
     * @param topic
     *            The <code>Topic</code> to be supported.
     * @throws NamingException
     * @throws NameAlreadyBoundException
     * @throws IllegalArgumentException
     */
    public final void addTopic(Topic topic) throws IllegalArgumentException, NameAlreadyBoundException, NamingException {
        if (topic == null) {
            throw new IllegalArgumentException("topic not specified!");
        }

        if (topic.isPurgeAllEventsOnStartup()) {
            logger.info("addTopic() - purging all events belonging to " + topic.getName() + " topic");
            try {
                backend.removeTopic(topic);
            } catch (TopicNotFoundException e) {
                logger.error("addTopic() - TopicNotFoundException catched: [" + e.getMessage() + "]. Follow the Stack trace:", e);
            }
        }

        backend.bindTopic(topic, true);

        if (!eventRemoverTable.containsKey(topic.getName())) {
            EventRemover eventRemover = new EventRemover(topic.getName());
            eventRemoverTable.put(topic.getName(), eventRemover);
            timer.schedule(eventRemover, 0, 600000, TimerTask.EXECUTION_TYPE.FIXED_DELAY_POST_EXECUTION); // 10 minutes
        }

    }

    public final boolean checkSupportedTopic(String topicName) {
        return backend.checkSupportedTopic(topicName);
    }

    /**
     * Check if the specified <code>Topic</code> is supported.
     * 
     * @param topic
     *            The <code>Topic</code> to be checked.
     * @return True if the topic is supported, false otherwise.
     */
    public final boolean checkSupportedTopic(Topic topic) {
        return backend.checkSupportedTopic(topic);
    }

    public final synchronized int getEventId() {
        return eventId++;
    }

    public final SensorEventArrayList getEvents(String topicName, String receiverId, String receiverGroup) throws IllegalArgumentException, TopicNotFoundException {
        Topic topic = backend.getTopic(topicName);
        return getEvents(topic, receiverId, receiverGroup);
    }

    public final SensorEventArrayList getEvents(Topic topic, String receiverId, String receiverGroup) throws IllegalArgumentException, TopicNotFoundException {
        if (topic == null) {
            throw (new IllegalArgumentException("The topic argument is null"));
        }
        if (!backend.checkSupportedTopic(topic)) {
            throw (new TopicNotFoundException("Topic \"" + topic.getName() + "\" not found"));
        }

        String group = null;

        if (topic.getVisibility().equalsIgnoreCase(Topic.ALL)) {
            group = Topic.ALL;
        } else if (topic.getVisibility().equalsIgnoreCase(Topic.GROUP)) {
            if (receiverGroup == null) {
                throw (new IllegalArgumentException("the receiverGroup cannot be null when the topic visibility is \"GROUP\""));
            }
            group = receiverGroup;
        } else if (topic.getVisibility().equalsIgnoreCase(Topic.USER)) {
            if (receiverId == null) {
                throw (new IllegalArgumentException("the receiverId cannot be null when the topic visibility is \"USER\""));
            }
            group = receiverId;
        }

        logger.debug("TopicHolder getEvents: retrieving events from topic " + topic.getName() + "...");

        SensorEventArrayList eventList = backend.getTopicEvents(topic.getName(), group);

        logger.debug("TopicHolder getEvents: retrieving " + eventList.size() + " events from topic " + topic.getName() + " DONE!!!");

        eventList.setSensorHolder(sensorHolder);

        return eventList;
    }

    /**
     * Get the <code>Topic</code> specified by its name, if it is supported.
     * 
     * @param name
     *            The name of the searched topic.
     * @return The searched <code>Topic</code> is supported, null otherwise.
     * @throws TopicNotFoundException
     * @throws IllegalArgumentException
     */
    public final Topic getTopic(String name) throws IllegalArgumentException, TopicNotFoundException {
        return backend.getTopic(name);
    }

    /**
     * Get the list of all supported topics.
     * 
     * @return An array containing the supported <code>Topic</code>s.
     */
    public final Topic[] getTopics() {
        Sensor[] sensorArray = sensorHolder.getSensors();
        
        ArrayList<Topic> topicList = new ArrayList<Topic>(0);
        
        for(int i=0; i<sensorArray.length; i++) {
            try {
                topicList.add(backend.getTopic(sensorArray[i].getType()));
            } catch (IllegalArgumentException e) {
                logger.error(e.getMessage());
            } catch (TopicNotFoundException e) {
                logger.error(e.getMessage());
            }
        }

        Topic[] topicArray = new Topic[topicList.size()];
        topicArray = (Topic[]) topicList.toArray(topicArray);
        return topicArray;
    }

    /**
     * Remove a <code>Topic</code> from the list of supported topics specifying
     * its name.
     * 
     * @param topic
     *            The <code>Topic</code> name to be removed.
     * @throws TopicNotFoundException
     * @throws IllegalArgumentException
     */
    public final void removeTopic(String topicName) throws IllegalArgumentException, TopicNotFoundException {
        backend.removeTopic(topicName);
        EventRemover eventRemover = (EventRemover) eventRemoverTable.remove(topicName);
        if (eventRemover != null) {
            eventRemover.cancel();
            timer.purge();
        }
    }

    /**
     * Remove a <code>Topic</code> from the list of supported topics.
     * 
     * @param topic
     *            The <code>Topic</code> to be removed.
     * @throws TopicNotFoundException
     * @throws IllegalArgumentException
     */
    public final void removeTopic(Topic topic) throws IllegalArgumentException, TopicNotFoundException {
        backend.removeTopic(topic);

        EventRemover eventRemover = (EventRemover) eventRemoverTable.remove(topic.getName());
        if (eventRemover != null) {
            eventRemover.cancel();
            timer.purge();
        }
    }


    public final void removeEvent(SensorEvent event) throws IllegalArgumentException, EventNotFoundException {
        if (event == null) {
            throw (new IllegalArgumentException("Event argument not specified!"));
        }

        backend.removeTopicEvent(event);
        logger.debug("removed event [id=" + event.getID() + "] [name=" + event.getName() + "]");
    }
    
    /**
     * Set the current event related to the specified <code>Topic</code>.
     * 
     * @param topic
     *            The <code>Topic</code> the <code>Event</code> is related to.
     * @param event
     *            The occurred <code>Event</code>
     * @throws TopicNotFoundException
     * @throws IllegalArgumentException
     */
    public final void setCurrentEvent(String topicName, SensorEvent event) throws IllegalArgumentException, TopicNotFoundException {
        if (topicName != null) {
            setCurrentEvent(getTopic(topicName), event);
        }
    }

    /**
     * Check if the <code>Topic</code> specified by its name is supported.
     * 
     * @param topic
     *            The <code>Topic</code> name to be checked.
     * @return True if the topic is supported, false otherwise.
     * @throws TopicNotFoundException
     * @throws IllegalArgumentException
     */
    public final void setCurrentEvent(Topic topic, SensorEvent event) throws IllegalArgumentException, TopicNotFoundException {
        if (topic == null) {
            throw (new IllegalArgumentException("The topic argument is null"));
        }
        if (event == null) {
            throw (new IllegalArgumentException("The event argument is null"));
        }
        if (!backend.checkSupportedTopic(topic)) {
            throw (new TopicNotFoundException("Topic \"" + topic.getName() + "\" not found"));
        }

        SensorEvent eventClone = (SensorEvent) event.clone();
        String eventId;

        if (topic.isEventOverwriteModeActive() && event.getName() != null) {
            eventId = eventClone.getName();
        } else {
            eventClone.setID(getEventId());
            eventId = "" + eventClone.getID();
            eventClone.setName(eventId);
        }

        if (event.getExpirationTime() == null) {
            Calendar time = (Calendar) eventClone.getTimestamp().clone();
            time.add(Calendar.DAY_OF_YEAR, 1);
            eventClone.setExpirationTime(time);
        }

        try {
            backend.bindTopicEvent(topic, eventClone);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
        } catch (NameAlreadyBoundException e) {
            logger.error(e.getMessage(), e);
        } catch (NamingException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
