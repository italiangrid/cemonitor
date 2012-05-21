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

package org.glite.ce.monitor.holder;

import java.io.IOException;
import java.util.Properties;

import org.apache.axis2.databinding.types.URI;
import org.apache.log4j.Logger;
import org.glite.ce.monitorapij.resource.types.Dialect;
import org.glite.ce.monitorapij.resource.types.SubscriptionPersistent;
import org.glite.ce.monitorapij.resource.types.Topic;
import org.glite.ce.monitorapij.ws.CEMonitorConsumerStub;
import org.glite.security.trustmanager.axis2.AXIS2SocketFactory;

public class CEMonNotificationClient 
    implements NotificationClient {
    
    private final static Logger logger = Logger.getLogger(CEMonNotificationClient.class.getName());
    
    private CEMonitorConsumerStub.Notification notification;
    
    public CEMonNotificationClient(SubscriptionPersistent subscription) {
        
        URI consumerURI = null;
        try {
            consumerURI = new URI(subscription.getMonitorConsumerURL().toString());
        }catch(Exception ex){
            /*
             * Already checked
             */
            logger.error(ex.getMessage(), ex);
        }
        
        Topic topic = subscription.getTopic();
        
        notification = new CEMonitorConsumerStub.Notification();
        notification.setExpirationTime(subscription.getExpirationTime());
        CEMonitorConsumerStub.Topic newTopic = new CEMonitorConsumerStub.Topic();
        newTopic.setName(topic.getName());
        if (topic.getDialect() != null && topic.getDialect().length > 0) {
            Dialect[] dialect = topic.getDialect();
            CEMonitorConsumerStub.Dialect[] newDialect = new CEMonitorConsumerStub.Dialect[dialect.length];

            for (int x = 0; x < dialect.length; x++) {
                newDialect[x] = new CEMonitorConsumerStub.Dialect();
                newDialect[x].setName(dialect[x].getName());
                newDialect[x].setQueryLanguage(dialect[x].getQueryLanguage());
            }
            newTopic.setDialect(newDialect);
        }
        notification.setTopic(newTopic);
        notification.setEvent(null);
        notification.setConsumerURL(consumerURI);
        
    }

    public void processEvents(SensorEventArrayList evnList){
    }

    public SensorEventArrayList getProcessedEvents(){
        return null;
    }
    
    public int size() {
        return 0;
    }

    public void sendEvents(Properties sslConfig) throws IOException {
        URI consumerURI = notification.getConsumerURL();
        AXIS2SocketFactory.setCurrentProperties(sslConfig);
        
        try{
            CEMonitorConsumerStub consumer = new CEMonitorConsumerStub(consumerURI.toString());
            CEMonitorConsumerStub.Notify msg = new CEMonitorConsumerStub.Notify();
            msg.setNotification(notification);
            consumer.notify(msg);
        }catch(Throwable th){
            logger.error(th.getMessage());
            throw new IOException("Cannot send notification to " + consumerURI.toString());
        }
    }

}
