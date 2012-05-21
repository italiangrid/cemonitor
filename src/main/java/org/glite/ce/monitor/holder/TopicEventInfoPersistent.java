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
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.log4j.Logger;
import org.glite.ce.monitor.jndi.provider.fscachedprovider.CEGeneralAttributes;
import org.glite.ce.monitorapij.sensor.Sensor;
import org.glite.ce.monitorapij.sensor.SensorEvent;
import org.glite.ce.monitorapij.resource.types.Dialect;
import org.glite.ce.monitorapij.resource.types.Topic;

public final class TopicEventInfoPersistent {
    private static Logger logger = Logger.getLogger(TopicEventInfoPersistent.class.getName());
    private DirContext rootCtx;
    private SensorHolder sensorHolder;

    private static final String TOPIC_ATTR_VISIBILITY = "visibility";
    private static final String TOPIC_ATTR_DIALECT = "dialect";
    private static final String TOPIC_ATTR_IsEventOverwriteModeActive = "isEventOverwriteModeActive";

    public TopicEventInfoPersistent(SensorHolder sensorHolder, String factory, String cemonCacheDir, int cacheSize) throws NamingException {
   //     this.providerFactory = factory;
        this.sensorHolder = sensorHolder;
                        
        Hashtable env = new Hashtable(0);
        env.put(Context.INITIAL_CONTEXT_FACTORY, factory);
        env.put(Context.PROVIDER_URL, cemonCacheDir);
        env.put("cache.size", new Integer(cacheSize));

        rootCtx = (DirContext) (new InitialDirContext(env)).lookup("");
        // logger.debug("Context loaded");
    }

    public void bindTopic(Topic topic, boolean useRebind) throws IllegalArgumentException,
            NamingException, NameAlreadyBoundException {
        if (topic == null) {
            throw (new IllegalArgumentException("The topic argument is null"));
        }

        String objName = topic.getName();
        DirContext topicContext = null;
        boolean nameAlreadyBound = false;

        try {
            topicContext = (DirContext) rootCtx.lookup(objName);
            nameAlreadyBound = true;
        } catch (NameNotFoundException nfEx) {
            nameAlreadyBound = false;
        }

        if (nameAlreadyBound && !useRebind) {
            throw (new NameAlreadyBoundException("Topic \"" + objName + "\" already bound"));
        }

        CEGeneralAttributes attributes = new CEGeneralAttributes();
        attributes.put(TOPIC_ATTR_VISIBILITY, topic.getVisibility());
        attributes.put(TOPIC_ATTR_IsEventOverwriteModeActive, new Boolean(topic.isEventOverwriteModeActive()));

        Dialect[] dialect = topic.getDialect();
        if (dialect != null) {
            BasicAttribute ba = new BasicAttribute(TOPIC_ATTR_DIALECT, true);
            for (int i = 0; i < dialect.length; i++) {
                if(dialect[i] != null) {
                    ba.add(dialect[i].getName());
                    ba.add(dialect[i].getQueryLanguage());
                }
            }

            attributes.put(ba);
        }

        if (nameAlreadyBound) {
            topicContext.modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, attributes);
        } else {
            topicContext = (DirContext) rootCtx.createSubcontext(objName, attributes);
        }
    }

    public void removeTopic(Topic topic) throws IllegalArgumentException, TopicNotFoundException {
        if (topic == null) {
            throw (new IllegalArgumentException("The topic argument is null"));
        }

        removeTopic(topic.getName());
    }

    public void removeTopic(String topicName) throws IllegalArgumentException, TopicNotFoundException {
        if (topicName == null) {
            throw (new IllegalArgumentException("The topicName argument is null"));
        }

        try {
            rootCtx.destroySubcontext(topicName);
        } catch (NamingException e) {
            throw(new TopicNotFoundException("Topic \"" + topicName + "\" not found"));
        }
    }

    public boolean checkSupportedTopic(Topic topic) {
        if (topic == null) {
            return false;
        }

        return checkSupportedTopic(topic.getName());
    }

    public boolean checkSupportedTopic(String topicName) {
        if (topicName == null) {
            return false;
        }

        try {
            Enumeration allNames = rootCtx.list("");
            while (allNames.hasMoreElements()) {
                NameClassPair item = (NameClassPair) allNames.nextElement();
                if (topicName.equals(item.getName())) {
                    return true;
                }
            }
        } catch (NamingException nEx) {
            // logger.error(nEx.getMessage());
        }

        return false;
    }

    public Topic[] getTopics() throws IllegalArgumentException {
        Topic[] topicArray = null;

        try {
            Enumeration allNames = rootCtx.list("");
            ArrayList list = new ArrayList(0);

            while (allNames.hasMoreElements()) {
                NameClassPair item = (NameClassPair) allNames.nextElement();
                try {
                    list.add(getTopic(item.getName()));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (TopicNotFoundException e) {
                    e.printStackTrace();
                }
            }

            topicArray = new Topic[list.size()];
            topicArray = (Topic[]) list.toArray(topicArray);
        } catch (NamingException nEx) {
            topicArray = new Topic[0];
        }

        return topicArray;
    }

    public Topic getTopic(String topicName) throws IllegalArgumentException, TopicNotFoundException {
        if (topicName == null) {
            throw (new IllegalArgumentException("The topicName argument is null"));
        }

        try {
            DirContext topicContext = (DirContext) rootCtx.lookup(topicName);
            Attributes attr = topicContext.getAttributes("", new String[] { TOPIC_ATTR_VISIBILITY,
                    TOPIC_ATTR_DIALECT, TOPIC_ATTR_IsEventOverwriteModeActive });

            Topic topic = new Topic();
            topic.setName(topicName);

            Attribute attribute = attr.get(TOPIC_ATTR_VISIBILITY);

            if (attribute != null) {
                topic.setVisibility((String) attribute.get());
            } 
            
            
            attribute = attr.get(TOPIC_ATTR_IsEventOverwriteModeActive);

            if (attribute != null) {
                topic.setEventOverwriteModeActive(((Boolean) attribute.get()).booleanValue());
            }

            attribute = attr.get(TOPIC_ATTR_DIALECT);
            if (attribute != null) {
                ArrayList list = new ArrayList(0);                
                int index = 0;
                
                while(index < attribute.size()) {
                    Dialect dialect = new Dialect((String)attribute.get(index));
                    dialect.setQueryLanguage((String[])attribute.get(index+1));
                    
                    list.add(dialect);
                    index +=2;
                }
                
                Dialect[] dialectArray = new Dialect[list.size()];
                dialectArray = (Dialect[])list.toArray(dialectArray);
                
                topic.setDialect(dialectArray);
            }

            return topic;
        } catch (NamingException nEx) {
            throw(new TopicNotFoundException("Topic \"" + topicName + "\" not supported"));
        }
    }

    public SensorEventArrayList getTopicEvents(Topic topic, String eventGroup)
            throws IllegalArgumentException {
        if (topic == null) {
            throw (new IllegalArgumentException("The topic argument is null"));
        }
        return getTopicEvents(topic.getName(), eventGroup);
    }


    public SensorEventArrayList getTopicEvents(Topic topic) throws IllegalArgumentException {
        if (topic == null) {
            throw (new IllegalArgumentException("The topic argument is null"));
        }
        
        return getTopicEvents(topic.getName());
    }
   
    public String[] getTopicEventGroups(String topicName) throws IllegalArgumentException {
        if (topicName == null) {
            throw (new IllegalArgumentException("The topicName argument is null"));
        }

        String[] groupArray = null;

        ArrayList list = new ArrayList(0);
            
        try {
            Enumeration allNames = rootCtx.list(topicName);

            while (allNames.hasMoreElements()) {
                NameClassPair item = (NameClassPair) allNames.nextElement();
                list.add(item.getName());
            }
            groupArray = new String[list.size()];
            groupArray = (String[]) list.toArray(groupArray);
        } catch (NamingException nEx) {
            groupArray = new String[0];
        }

        return groupArray;
    
    }
    
    public SensorEventArrayList getTopicEvents(String topicName) throws IllegalArgumentException {
        if (topicName == null) {
            throw (new IllegalArgumentException("The topicName argument is null"));
        }

        //SensorEvent[] eventArray = null;

        SensorEventArrayList list = new SensorEventArrayList(rootCtx);
            
        try {
            Enumeration allNames = rootCtx.list(topicName);

            while (allNames.hasMoreElements()) {
                NameClassPair item = (NameClassPair) allNames.nextElement();
                list.addAll(getTopicEvents(topicName, item.getName()));
            }

          //  eventArray = new SensorEvent[list.size()];
          //  eventArray = (SensorEvent[]) list.toArray(eventArray);
        } catch (NamingException nEx) {
           // eventArray = new SensorEvent[0];
        }

        return list;
    }
    

    public SensorEventArrayList getTopicEvents(String topicName, String eventGroup)
            throws IllegalArgumentException {
        if (topicName == null) {
            throw (new IllegalArgumentException("The topicName argument is null"));
        }
        if (eventGroup == null) {
            throw (new IllegalArgumentException("The eventGroup argument is null"));
        }

        SensorEventArrayList list = new SensorEventArrayList(rootCtx);
        
        try {   
            Enumeration allNames = rootCtx.list(topicName + "/" + eventGroup);
            
            while (allNames.hasMoreElements()) {
                NameClassPair item = (NameClassPair) allNames.nextElement();    
                list.add(topicName + "/" + eventGroup + "/" + item.getName());
                
             //   list.add(item.getName());
              /*      
                SensorEvent event = getTopicEvent(topicName, eventGroup, item.getName());
                if(event != null) {
                    list.add(event);
                }*/
            }

        } catch (NamingException nEx) {        
            logger.error(nEx);
        }

        return list;
    }


    public SensorEventEnumeration getTopicEventsEnumeration(String topicName, String eventGroup) throws IllegalArgumentException {
        if (topicName == null) {
            throw (new IllegalArgumentException("The topicName argument is null"));
        }
        if (eventGroup == null) {
            throw (new IllegalArgumentException("The eventGroup argument is null"));
        }
        
        return new SensorEventEnumeration(topicName, eventGroup, rootCtx);
    }
    
    public SensorEvent getTopicEvent(Topic topic, String eventGroup, String eventName)
            throws IllegalArgumentException {
        if (topic == null) {
            throw (new IllegalArgumentException("The topic argument is null"));
        }
        return getTopicEvent(topic.getName(), eventGroup, eventName);
    }
    
    
    public SensorEvent getTopicEvent(String topicName, String eventGroup, String eventName)
            throws IllegalArgumentException {
        if (topicName == null) {
            throw (new IllegalArgumentException("The topicName argument is null"));
        }
        if (eventGroup == null) {
            throw (new IllegalArgumentException("The eventGroup argument is null"));
        }
        if (eventName == null) {
            throw (new IllegalArgumentException("The eventName argument is null"));
        }

        try {
            SensorEvent event = (SensorEvent) rootCtx.lookup(topicName + "/" + eventGroup + "/" + eventName);
            if(event != null) {   
                event.setSource(sensorHolder.getSensorByName(event.getProducer()));         
            }
  
            return event;
        } catch (NamingException nEx) {
          //  System.out.println("TopicEventInfoP:getTopicEvent error " + nEx.toString());
        }

        return null;
    }


//    public void removeAllTopicEvents(Topic topic) throws IllegalArgumentException {
//        if (topic == null) {
//            throw (new IllegalArgumentException("topic not specified!"));
//        }
//        
//        removeAllTopicEvents(topic.getName());
//    }
//    
//    public void removeAllTopicEvents(String topicName) throws IllegalArgumentException {
//        if (topicName == null) {
//            throw (new IllegalArgumentException("topic's name not spicified!"));
//        }
//        
//        try {
//            rootCtx.destroySubcontext(topicName);
//        } catch (NamingException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }
    
    public void removeTopicEvent(SensorEvent event) throws IllegalArgumentException, EventNotFoundException {
        if (event == null) {
            throw (new IllegalArgumentException("The event argument is null"));
        }
        
        Topic topic = null;
        try {
            Sensor sensor = sensorHolder.getSensorByType(event.getProducer());
            if(sensor != null) {
                topic = getTopic(sensor.getType());
            }
             //   topic = getTopic(event.getProducer());
        } catch (Exception e) {
            throw (new EventNotFoundException(e));
        } 
       
        if (topic == null) {
            throw (new IllegalArgumentException("Topic \"" + event.getProducer() + "\" not found"));
        }
        
        String eventCtxName = null;

        try {
            if (topic.getVisibility().equalsIgnoreCase(Topic.ALL)) {
                eventCtxName = rootCtx.composeName(Topic.ALL, topic.getName());
                
            } else if (topic.getVisibility().equalsIgnoreCase(Topic.GROUP)) {
                if (event.getReceiverGroup() != null) {
                    eventCtxName = rootCtx.composeName(event.getReceiverGroup(), topic.getName());
                } else {
                    throw (new EventNotFoundException("SensorEvent receiverGroup not defined!"));
                }
            } else if (topic.getVisibility().equalsIgnoreCase(Topic.USER)) {
                if (event.getReceiverId() != null) {
                    eventCtxName = rootCtx.composeName(event.getReceiverId(), topic.getName());
                } else {
                    throw (new EventNotFoundException("SensorEvent receiverId not defined!"));
                }
            }
        } catch (NamingException e) {
            throw (new EventNotFoundException(e));
        }
        
        try {
            DirContext eventContext = (DirContext) rootCtx.lookup(eventCtxName);
            if(eventContext != null && event.getName() != null) {
                eventContext.unbind(event.getName());  
            }
 

//            Object obj = eventContext.lookup(event.getName());

/*
    System.out.println("cemon eventCtxName = " + eventCtxName); 

            Enumeration allNames = eventContext.list("");
            while (allNames.hasMoreElements()) {
                NameClassPair item = (NameClassPair) allNames.nextElement();

    System.out.println("cemon item name = " + item.getName());

                if(event.getName().equals(item.getName())) {
                   try {
                       Object obj = eventContext.lookup(item.getName());
                   } catch(Exception e) { 
                   }
                }
            }
            eventContext.unbind(event.getName());
*/
        } catch (NameNotFoundException e) {
            logger.error(e.getMessage());
            throw (new EventNotFoundException(e));
        } catch (NamingException e) {
            logger.error(e.getMessage());
            throw (new EventNotFoundException(e));
        }
    }

    
    public void bindTopicEvent(Topic topic, SensorEvent event) throws IllegalArgumentException,
            NamingException, NameAlreadyBoundException {
        if (topic == null || topic.getName() == null) {
            throw (new IllegalArgumentException("The topic argument is null"));
        }
        if (event == null) {
            throw (new IllegalArgumentException("The event argument is null"));
        }
        
        event.setProducer(topic.getName());
        
        String eventCtxName = null;

        if (topic.getVisibility().equalsIgnoreCase(Topic.ALL)) {
            eventCtxName = rootCtx.composeName(Topic.ALL, topic.getName());
        } else if (topic.getVisibility().equalsIgnoreCase(Topic.GROUP)) {
            if (event.getReceiverGroup() != null) {
                eventCtxName = rootCtx.composeName(event.getReceiverGroup(), topic.getName());
            } else {
                throw (new NamingException("SensorEvent receiverGroup not defined!"));
            }
        } else if (topic.getVisibility().equalsIgnoreCase(Topic.USER)) {
            if (event.getReceiverId() != null) {
                eventCtxName = rootCtx.composeName(event.getReceiverId(), topic.getName());
            } else {
                throw (new NamingException("SensorEvent receiverId not defined!"));
            }
        }

        DirContext topicContext = (DirContext) rootCtx.createSubcontext(eventCtxName);
        
        if (topic.isEventOverwriteModeActive()) {
//            Object obj = null;
//            try {
//                obj = topicContext.lookup(event.getName());
//            } catch(Exception e) {}        
//            if(obj != null) {
//                topicContext.rebind(event.getName(), event);             
//            } else {
//                topicContext.bind(event.getName(), event);
//            }
                
            try {
                topicContext.rebind(event.getName(), event);
            } catch(Exception e) {
                topicContext.bind(event.getName(), event);
            }
        } else {
            topicContext.bind(event.getName(), event);
        }
    }

}
