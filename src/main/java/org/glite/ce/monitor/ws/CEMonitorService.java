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

package org.glite.ce.monitor.ws;

import java.io.FileInputStream;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;

import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.service.Lifecycle;
import org.apache.log4j.Logger;
import org.glite.ce.commonj.configuration.xppm.ConfigurationEvent;
import org.glite.ce.commonj.configuration.xppm.ConfigurationListener;
import org.glite.ce.commonj.utils.CEUtils;
import org.glite.ce.faults.AuthorizationFault;
import org.glite.ce.faults.GenericFault;
import org.glite.ce.monitor.configuration.CEMonServiceConfig;
import org.glite.ce.monitor.holder.ActionHolder;
import org.glite.ce.monitor.holder.NotificationHolder;
import org.glite.ce.monitor.holder.NotificationPool;
import org.glite.ce.monitor.holder.PredefinedSubscriptionHolder;
import org.glite.ce.monitor.holder.QueryProcessorHolder;
import org.glite.ce.monitor.holder.SensorEventArrayList;
import org.glite.ce.monitor.holder.SensorHolder;
import org.glite.ce.monitor.holder.TopicHolder;
import org.glite.ce.monitor.holder.TopicNotFoundException;
import org.glite.ce.monitor.registry.SubscriptionRegistry;
import org.glite.ce.monitorapij.faults.DialectNotSupportedFault;
import org.glite.ce.monitorapij.faults.SubscriptionFault;
import org.glite.ce.monitorapij.faults.TopicNotSupportedFault;
import org.glite.ce.monitorapij.resource.Resource;
import org.glite.ce.monitorapij.resource.types.SubscriptionPersistent;
import org.glite.ce.monitorapij.sensor.Sensor;
import org.glite.ce.monitorapij.sensor.SensorEvent;
import org.glite.ce.monitorapij.sensor.SensorException;
import org.glite.ce.monitorapij.sensor.SensorListener;
import org.glite.ce.monitorapij.sensor.SensorOutputDataFormat;
import org.glite.ce.monitorapij.types.Action;
import org.glite.ce.monitorapij.types.Dialect;
import org.glite.ce.monitorapij.types.Event;
import org.glite.ce.monitorapij.types.EventArray;
import org.glite.ce.monitorapij.types.Info;
import org.glite.ce.monitorapij.types.Property;
import org.glite.ce.monitorapij.types.Subscription;
import org.glite.ce.monitorapij.types.SubscriptionList;
import org.glite.ce.monitorapij.types.SubscriptionRef;
import org.glite.ce.monitorapij.types.SubscriptionRefList;
import org.glite.ce.monitorapij.types.Topic;
import org.glite.ce.monitorapij.types.TopicArray;
import org.glite.ce.monitorapij.ws.Authentication_Fault;
import org.glite.ce.monitorapij.ws.Authorization_Fault;
import org.glite.ce.monitorapij.ws.CEMonitorSkeleton;
import org.glite.ce.monitorapij.ws.DialectNotSupported_Fault;
import org.glite.ce.monitorapij.ws.Generic_Fault;
import org.glite.ce.monitorapij.ws.GetEvent;
import org.glite.ce.monitorapij.ws.GetEventResponse;
import org.glite.ce.monitorapij.ws.GetInfo;
import org.glite.ce.monitorapij.ws.GetInfoResponse;
import org.glite.ce.monitorapij.ws.GetSubscription;
import org.glite.ce.monitorapij.ws.GetSubscriptionRef;
import org.glite.ce.monitorapij.ws.GetSubscriptionRefResponse;
import org.glite.ce.monitorapij.ws.GetSubscriptionResponse;
import org.glite.ce.monitorapij.ws.GetTopicEvent;
import org.glite.ce.monitorapij.ws.GetTopicEventResponse;
import org.glite.ce.monitorapij.ws.GetTopics;
import org.glite.ce.monitorapij.ws.GetTopicsResponse;
import org.glite.ce.monitorapij.ws.Notify;
import org.glite.ce.monitorapij.ws.NotifyResponse;
import org.glite.ce.monitorapij.ws.PauseSubscription;
import org.glite.ce.monitorapij.ws.PauseSubscriptionResponse;
import org.glite.ce.monitorapij.ws.Ping;
import org.glite.ce.monitorapij.ws.PingResponse;
import org.glite.ce.monitorapij.ws.ResumeSubscription;
import org.glite.ce.monitorapij.ws.ResumeSubscriptionResponse;
import org.glite.ce.monitorapij.ws.Subscribe;
import org.glite.ce.monitorapij.ws.SubscribeResponse;
import org.glite.ce.monitorapij.ws.SubscriptionNotFound_Fault;
import org.glite.ce.monitorapij.ws.Subscription_Fault;
import org.glite.ce.monitorapij.ws.TopicNotSupported_Fault;
import org.glite.ce.monitorapij.ws.Unsubscribe;
import org.glite.ce.monitorapij.ws.UnsubscribeResponse;
import org.glite.ce.monitorapij.ws.Update;
import org.glite.ce.monitorapij.ws.UpdateResponse;

import eu.emi.security.authn.x509.impl.CertificateUtils;

public class CEMonitorService
    extends CEMonitorSkeleton
    implements Lifecycle, SensorListener, ConfigurationListener {

    private final static Logger logger = Logger.getLogger(CEMonitorService.class.getName());

    private final static String DEFAULT_PROVIDER = "org.glite.ce.commonj.jndi.provider.fscachedprovider.CEGeneralDirContextFactory";

    protected static int numberOfInstances = 0;

    protected static Hashtable<String, String> visibilityTable = new Hashtable<String, String>(3);

    private static int maxExpirationTime_Hours = 8760;

    protected static QueryProcessorHolder queryProcessorHolder = null;

    protected static ActionHolder actionHolder = null;

    protected static SensorHolder sensorHolder = null;

    protected static TopicHolder topicHolder = null;

    protected static PredefinedSubscriptionHolder predefinedSubscriptionHolder = null;

    protected static SubscriptionRegistry subscriptionRegistry = null;

    protected static NotificationPool notificationPool = null;

    public void init(ServiceContext context) {

        synchronized (CEMonitorService.class) {
            if (numberOfInstances > 0) {
                return;
            }

            logger.info("Initializing CEMonitor");

            numberOfInstances++;

            visibilityTable.put("HIGH", "ALL");
            visibilityTable.put("MEDIUM", "GROUP");
            visibilityTable.put("LOW", "USER");

            /*
             * CEMon configuration system initialization
             */
            CEMonServiceConfig sConfiguration = CEMonServiceConfig.getConfiguration();
            if (sConfiguration == null) {
                throw new RuntimeException("Cannot configure the service");
            }

            /*
             * Internal parameters initialization
             */
            maxExpirationTime_Hours = sConfiguration.getGlobalAttributeAsInt("maxSubscriptionExpirationTime_hours",
                    8760);
            String tmps = sConfiguration.getGlobalAttributeAsString("notificationSharedThread");
            boolean sharedThread = tmps != null ? Boolean.parseBoolean(tmps) : true;

            if (notificationPool == null) {
                notificationPool = new NotificationPool(sharedThread);
            }

            /*
             * Initialization of the query processor holder
             */
            if (queryProcessorHolder == null) {
                try {
                    queryProcessorHolder = new QueryProcessorHolder();
                    ArrayList<Resource> allResList = sConfiguration.getResources("queryprocessor");
                    for (Resource resource : allResList) {
                        queryProcessorHolder.addResource(resource);
                    }
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }

            /*
             * Initialization of the action holder
             */
            if (actionHolder == null) {
                try {
                    actionHolder = new ActionHolder();
                    ArrayList<Resource> allResList = sConfiguration.getResources("action");
                    for (Resource resource : allResList) {
                        actionHolder.addResource(resource);
                    }
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }

            /*
             * Initialization of the sensorHolder and topicHolder
             */
            if (sensorHolder == null) {
                try {
                    sensorHolder = new SensorHolder();
                    topicHolder = new TopicHolder(sensorHolder);

                    ArrayList<Resource> allResList = sConfiguration.getResources("sensor");
                    for (Resource resource : allResList) {
                        this.addSensor((Sensor) resource);
                        sensorHolder.addResource(resource);
                    }

                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }

            /*
             * Initialization of the subscription registry
             */
            if (subscriptionRegistry == null) {
                String cachePath = sConfiguration.getGlobalAttributeAsString("backendLocation");
                if (cachePath == null) {
                    cachePath = "/var/cache/cemonitor";
                }

                int cacheSize = sConfiguration.getGlobalAttributeAsInt("backendCacheSize", 100);

                String subscriptionRegistryConfProvider = sConfiguration
                        .getGlobalAttributeAsString("subscriptionRegistryConfProvider");
                if (subscriptionRegistryConfProvider == null) {
                    subscriptionRegistryConfProvider = DEFAULT_PROVIDER;
                }

                try {
                    subscriptionRegistry = new SubscriptionRegistry(cachePath, cacheSize,
                            subscriptionRegistryConfProvider);
                    for (SubscriptionPersistent subscription : subscriptionRegistry.getAllSubscriptions()) {
                        this.addToNotificationHolder(subscription);
                    }
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }

            /*
             * Initialization of the static subscription holder
             */
            if (predefinedSubscriptionHolder == null) {
                try {

                    predefinedSubscriptionHolder = new PredefinedSubscriptionHolder();
                    ArrayList<Resource> resList = sConfiguration.getResources("subscription");
                    fillInSubscriptionPersistentList(resList);

                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }

            sConfiguration.registerListener(this);

        }
    }

    public void destroy(ServiceContext context) {

        synchronized (CEMonitorService.class) {
            if (numberOfInstances > 1) {
                return;
            }

            logger.info("Destroying CEMonitor");
            numberOfInstances--;

            notificationPool.destroy();

            if (sensorHolder != null) {
                Sensor[] sensors = sensorHolder.getSensors();
                for (int i = 0; i < sensors.length; i++) {
                    sensors[i].destroySensor();
                }
            }

            if (topicHolder != null) {
                topicHolder.destroy();
            }

        }
    }

    public ResumeSubscriptionResponse resumeSubscription(ResumeSubscription resumeSubscription)
        throws Authorization_Fault, Authentication_Fault, SubscriptionNotFound_Fault, Generic_Fault {

        if (resumeSubscription == null || resumeSubscription.getSubscriptionRef() == null) {
            throw makeGenericFault("resumeSubscription", "0", "Missing subscription reference");
        }

        String subscriptionID = resumeSubscription.getSubscriptionRef().getSubscriptionID();
        if ((subscriptionID == null) || subscriptionID.equals("")) {
            throw makeGenericFault("resumeSubscription", "0", "Subscription ID undefined");
        }

        try {
            SubscriptionPersistent subscription = subscriptionRegistry.getSubscription(subscriptionID);
            subscription.resume();
            subscriptionRegistry.update(subscription);
        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
            throw makeGenericFault("resumeSubscription", "0", "Cannot resume subscription " + subscriptionID);
        }

        ResumeSubscriptionResponse response = new ResumeSubscriptionResponse();
        return response;
    }

    public GetSubscriptionRefResponse getSubscriptionRef(GetSubscriptionRef getSubscriptionRef)
        throws Authorization_Fault, Authentication_Fault, Generic_Fault {

        GetSubscriptionRefResponse response = new GetSubscriptionRefResponse();
        SubscriptionRefList refList = new SubscriptionRefList();

        ArrayList<SubscriptionPersistent> subscriptions = subscriptionRegistry
                .getSubscriptionBySubscriberId(getUserDN_FQAN(null));
        if (subscriptions != null) {
            SubscriptionRef[] allReferences = new SubscriptionRef[subscriptions.size()];
            for (int i = 0; i < allReferences.length; i++) {
                SubscriptionPersistent subscription = (SubscriptionPersistent) subscriptions.get(i);
                allReferences[i] = new SubscriptionRef();
                allReferences[i].setSubscriptionID(subscription.getId());
                allReferences[i].setExpirationTime(subscription.getExpirationTime());
            }
        } else {
            refList.setSubscriptionRef(new SubscriptionRef[0]);
        }

        response.setSubscriptionRefList(refList);
        return response;
    }

    public GetInfoResponse getInfo(GetInfo getInfo)
        throws Authorization_Fault, Authentication_Fault, Generic_Fault {
        GetInfoResponse response = new GetInfoResponse();
        Info info = new Info();

        CEMonServiceConfig sConfiguration = CEMonServiceConfig.getConfiguration();

        info.setDescription("CEMon service");
        info.setInterfaceVersion("N/A");
        info.setServiceVersion("N/A");
        info.setStartupTime(Calendar.getInstance());
        Property[] property = new Property[1];
        property[0] = new Property();
        property[0].setName("hostDN");
        String sslCertFilename = sConfiguration.getGlobalAttributeAsString("sslcertfile");

        if (sslCertFilename != null) {
            try {

                FileInputStream fIn = new FileInputStream(sslCertFilename);
                X509Certificate hostCert = CertificateUtils.loadCertificate(fIn, CertificateUtils.Encoding.PEM);
                String hostDN = hostCert.getSubjectX500Principal().getName();
                property[0].setValue(hostDN);
                logger.debug("Host DN: " + hostDN);
            } catch (Exception ex) {
                logger.warn("Cannot retrieve server certificate", ex);
                property[0].setValue("N/A");
            }
        }

        info.setProperty(property);

        String tmps = sConfiguration.getGlobalAttributeAsString("ServiceDescription");
        if (tmps != null) {
            info.setDescription(tmps);
        }

        tmps = sConfiguration.getGlobalAttributeAsString("ServiceVersion");
        if (tmps != null) {
            info.setServiceVersion(tmps);
        }

        tmps = sConfiguration.getGlobalAttributeAsString("InterfaceVersion");
        if (tmps != null) {
            info.setInterfaceVersion(tmps);
        }

        org.glite.ce.monitorapij.resource.types.Action[] action = actionHolder.getActions();

        Action[] newAction = new Action[action.length];
        for (int i = 0; i < action.length; i++) {
            newAction[i] = new Action();
            newAction[i].setName(action[i].getName());
        }

        info.setAction(newAction);

        /*
         * TODO don't use WS call, extract common code
         */
        try {
            GetTopicsResponse tmpresp = this.getTopics(new GetTopics());
            info.setTopic(tmpresp.getTopicArray().getTopic());
        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
        }

        response.setInfo(info);
        return response;
    }

    public UnsubscribeResponse unsubscribe(Unsubscribe unsubscribe)
        throws Authorization_Fault, Authentication_Fault, SubscriptionNotFound_Fault, Generic_Fault {

        if (unsubscribe == null || unsubscribe.getSubscriptionRef() == null) {
            throw makeGenericFault("unsubscribe", "0", "Missing subscription reference");
        }

        String subscriptionID = unsubscribe.getSubscriptionRef().getSubscriptionID();
        if ((subscriptionID == null) || subscriptionID.equals("")) {
            throw makeGenericFault("unsubscribe", "0", "Subscription ID undefined");
        }

        try {
            SubscriptionPersistent subscription = subscriptionRegistry.getSubscription(subscriptionID);
            NotificationHolder notificationHolder = notificationPool.activateHolder(subscription, topicHolder,
                    actionHolder, queryProcessorHolder, subscriptionRegistry);
            notificationHolder.removeSubscription(subscription);

            notificationPool.check();

            subscriptionRegistry.unregister(subscriptionID);

            logger.debug("Unregistering Subscription [" + subscriptionID + "]");
        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
            throw makeGenericFault("unsubscribe", "0", "Cannot unsubscribe subscription " + subscriptionID);
        }

        UnsubscribeResponse response = new UnsubscribeResponse();
        return response;
    }

    public UpdateResponse update(Update update)
        throws Authorization_Fault, Authentication_Fault, Subscription_Fault, Generic_Fault {

        Subscription subscription = update.getSubscription();
        SubscriptionPersistent subscriptionPersistent;

        try {
            subscriptionPersistent = subscriptionRegistry.getSubscription(subscription.getId());
            if (subscriptionPersistent == null) {
                throw new Exception("subscriptionPersistent is null");
            }
        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
            throw makeGenericFault("update", "0", "Subscription [" + subscription.getId() + "] not found!");
        }

        if (!subscriptionPersistent.getSubscriberId().equals(getUserDN_FQAN(null))) {
            String msg = "subscription update not allowed: the subscription [" + subscription.getId()
                    + "] belongs to another client!";
            logger.error(msg);
            throw makeAuthorizationFault("update", "1", msg);
        }

        NotificationHolder notificationHolder = notificationPool.activateHolder(subscriptionPersistent, topicHolder,
                actionHolder, queryProcessorHolder, subscriptionRegistry);
        notificationHolder.removeSubscription(subscriptionPersistent);

        try {

            SubscriptionPersistent newSubscriptionPersistent = new SubscriptionPersistent(subscription);

            checkSubscription(newSubscriptionPersistent);

            subscriptionPersistent.setTopic(newSubscriptionPersistent.getTopic());
            subscriptionPersistent.setPolicy(newSubscriptionPersistent.getPolicy());
            subscriptionPersistent.setMonitorConsumerURL(newSubscriptionPersistent.getMonitorConsumerURL());
            subscriptionPersistent.setExpirationTime(subscription.getExpirationTime());
            subscriptionPersistent.setType(subscription.getType());

            if ((subscriptionPersistent.getPolicy().getAction() == null)
                    || (subscriptionPersistent.getPolicy().getAction().length == 0)) {
                logger.warn("update() - subscriptionPersistent.getPolicy().getAction() is NULL. Creating it...");

                org.glite.ce.monitorapij.resource.types.Action[] acts = new org.glite.ce.monitorapij.resource.types.Action[1];
                acts[0] = new org.glite.ce.monitorapij.resource.types.Action("SendNotification");
                acts[0].setDoActionWhenQueryIs(true);
                subscriptionPersistent.getPolicy().setAction(acts);
            }

            if (subscription.getJarPath() != null) {
                subscriptionPersistent.setJarPath(new URI(subscription.getJarPath().toString()));
            }

            Property[] property = subscription.getProperty();
            if (property != null) {
                org.glite.ce.monitorapij.resource.types.Property[] newProperty = new org.glite.ce.monitorapij.resource.types.Property[property.length];

                for (int i = 0; i < property.length; i++) {
                    newProperty[i] = new org.glite.ce.monitorapij.resource.types.Property(property[i].getName(),
                            property[i].getValue());
                }
                subscriptionPersistent.setProperty(newProperty);
            }

            notificationHolder.addSubscription(subscriptionPersistent);

            subscriptionRegistry.update(subscriptionPersistent);

            SubscriptionRef subscriptionRef = new SubscriptionRef();
            subscriptionRef.setSubscriptionID(subscriptionPersistent.getId());
            subscriptionRef.setExpirationTime(subscriptionPersistent.getExpirationTime());

            logger.info("update() - Subscription [" + subscription.getId() + "] updated!");

            UpdateResponse response = new UpdateResponse();
            response.setSubscriptionRef(subscriptionRef);
            return response;

        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
            throw makeGenericFault("update", "0", "Error updating subscription");
        }
    }

    public SubscribeResponse subscribe(Subscribe subscribe)
        throws Authorization_Fault, Authentication_Fault, Subscription_Fault, Generic_Fault {

        Subscription subscription = subscribe.getSubscription();
        try {

            SubscriptionPersistent subscriptionPersistent = new SubscriptionPersistent(subscription);
            if ((subscriptionPersistent.getPolicy().getAction() == null)
                    || (subscriptionPersistent.getPolicy().getAction().length == 0)) {
                logger.warn("subscribe() - subscriptionPersistent.getPolicy().getAction() is NULL. Creating it...");

                org.glite.ce.monitorapij.resource.types.Action[] acts = new org.glite.ce.monitorapij.resource.types.Action[1];
                acts[0] = new org.glite.ce.monitorapij.resource.types.Action("SendNotification");
                acts[0].setDoActionWhenQueryIs(true);
                subscriptionPersistent.getPolicy().setAction(acts);
            }

            String voName = CEUtils.getUserDefaultVO();
            if (voName != null) {
                subscriptionPersistent.setSubscriberId(getUserDN_FQAN(voName));
                subscriptionPersistent.setSubscriberGroup(voName);
            } else {
                subscriptionPersistent.setSubscriberId(getUserDN_FQAN(null));
            }

            checkSubscription(subscriptionPersistent);

            addToNotificationHolder(subscriptionPersistent);
            subscriptionRegistry.register(subscriptionPersistent);

            SubscriptionRef subscriptionRef = new SubscriptionRef();
            subscriptionRef.setSubscriptionID(subscriptionPersistent.getId());
            subscriptionRef.setExpirationTime(subscriptionPersistent.getExpirationTime());

            logger.info("New subscription registered: subscription id = " + subscriptionPersistent.getId()
                    + " - subscriber id = " + subscriptionPersistent.getSubscriberId());

            SubscribeResponse response = new SubscribeResponse();
            response.setSubscriptionRef(subscriptionRef);
            return response;
        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
            throw makeGenericFault("update", "0", "Error creating subscription");
        }
    }

    public GetEventResponse getEvent(GetEvent getEvent)
        throws Authorization_Fault, Authentication_Fault, DialectNotSupported_Fault, Generic_Fault,
        TopicNotSupported_Fault {

        Event[] evnArray = this.getTopicEvent(null).getEventArray().getEvent();
        GetEventResponse response = new GetEventResponse();
        if (evnArray != null && evnArray.length > 0) {
            response.setEvent(evnArray[0]);
        }
        return response;
    }

    public GetTopicsResponse getTopics(GetTopics getTopics)
        throws Authorization_Fault, Authentication_Fault, Generic_Fault {

        GetTopicsResponse response = new GetTopicsResponse();

        if (topicHolder != null) {
            org.glite.ce.monitorapij.resource.types.Topic[] topicArray = topicHolder.getTopics();
            Topic[] newTopic = new Topic[topicArray.length];

            for (int i = 0; i < topicArray.length; i++) {
                org.glite.ce.monitorapij.resource.types.Topic topic = topicArray[i];
                org.glite.ce.monitorapij.resource.types.Dialect[] dialect = topic.getDialect();

                Dialect[] newDialect = null;
                if (dialect != null) {
                    newDialect = new Dialect[dialect.length];

                    for (int j = 0; j < dialect.length; j++) {
                        newDialect[j] = new Dialect();
                        newDialect[j].setName(dialect[j].getName());
                        newDialect[j].setQueryLanguage(dialect[j].getQueryLanguage());
                    }
                }

                newTopic[i] = new Topic();
                newTopic[i].setName(topic.getName());
                newTopic[i].setDialect(newDialect);
                newTopic[i].setVisibility(topic.getVisibility());
            }

            TopicArray resTopicArray = new TopicArray();
            resTopicArray.setTopic(newTopic);
            response.setTopicArray(resTopicArray);
        }

        return response;
    }

    public GetSubscriptionResponse getSubscription(GetSubscription getSubscription)
        throws Authorization_Fault, Authentication_Fault, Generic_Fault {

        ArrayList<SubscriptionPersistent> subscriptions = null;
        String subscriberId = getUserDN_FQAN(null);

        SubscriptionRef[] refList = getSubscription.getSubscriptionRefList().getSubscriptionRef();
        if (refList == null || refList.length == 0) {

            subscriptions = subscriptionRegistry.getSubscriptionBySubscriberId(subscriberId);

        } else {
            subscriptions = new ArrayList<SubscriptionPersistent>(0);
            for (int i = 0; i < refList.length; i++) {
                try {

                    SubscriptionPersistent subscription = subscriptionRegistry.getSubscription(refList[i]
                            .getSubscriptionID());

                    if (subscription.getSubscriberId().equals(subscriberId)) {

                        logger.debug("getSubscription() - Adding subscription [" + subscription.getCompleteInfo() + "]");

                        subscriptions.add(subscription);
                    }

                } catch (Throwable th) {
                    logger.error(th.getMessage(), th);
                    throw makeGenericFault("getSubscription", "0",
                            "Cannot retrieve subscription " + refList[i].getSubscriptionID());
                }
            }
        }

        Subscription[] subscriptionArray = null;
        if (subscriptions == null || subscriptions.size() == 0) {
            subscriptionArray = new Subscription[0];
        } else {
            subscriptionArray = new Subscription[subscriptions.size()];
            for (int i = 0; i < subscriptions.size(); i++) {
                try {
                    subscriptionArray[i] = subscriptions.get(i).cloneSubscription();
                } catch (Throwable th) {
                    logger.error(th.getMessage(), th);
                    throw makeGenericFault("getSubscription", "0", "Cannot register subscription");
                }
            }
        }

        GetSubscriptionResponse response = new GetSubscriptionResponse();
        SubscriptionList subscrList = new SubscriptionList();
        subscrList.setSubscription(subscriptionArray);
        response.setSubscriptionList(subscrList);

        return response;
    }

    public PingResponse ping(Ping ping)
        throws Authorization_Fault, Authentication_Fault {
        return new PingResponse();
    }

    public GetTopicEventResponse getTopicEvent(GetTopicEvent getTopicEvent)
        throws Authorization_Fault, Authentication_Fault, DialectNotSupported_Fault, Generic_Fault,
        TopicNotSupported_Fault {

        Topic topic = getTopicEvent.getTopic();
        if (topic == null) {
            throw makeGenericFault("getTopicEvent", "0", "Topic is undefined");
        }

        org.glite.ce.monitorapij.resource.types.Topic systemTopic;
        try {
            systemTopic = topicHolder.getTopic(topic.getName());
        } catch (TopicNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            throw makeTopicNotSupportedFault("getTopicEvent", "2", "Topic not specified");
        } catch (IllegalArgumentException ex) {
            logger.error(ex.getMessage(), ex);
            throw makeTopicNotSupportedFault("getTopicEvent", "3", "Topic [" + topic.getName() + "] not supported");
        }

        SensorEventArrayList eventList;
        try {
            String userVO = CEUtils.getUserDefaultVO();
            String subscriberId = getUserDN_FQAN(userVO);
            eventList = topicHolder.getEvents(topic.getName(), subscriberId, userVO);
        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
            throw makeTopicNotSupportedFault("getTopicEvent", "4", "No event registered for topic" + topic.getName());
        }

        Dialect[] dialect = topic.getDialect();
        if ((dialect != null) && (dialect.length > 0)) {
            if (!systemTopic.checkSupportedDialect(dialect[0].getName())) {
                throw makeDialectNotSupportedFault("getTopicEvent", "5", "Dialect [" + dialect[0].getName()
                        + "] not supported!");
            }

            String dataFormat = dialect[0].getName();

            for (int i = 0; i < eventList.size(); i++) {
                SensorEvent event = (SensorEvent) eventList.get(i);

                SensorOutputDataFormat format = event.getSensorOutputDataFormatApplied();
                if (format == null || !format.getName().equals(dataFormat)) {
                    try {
                        event.applyFormat(dataFormat);
                    } catch (SensorException ex) {
                        logger.error(ex.getMessage(), ex);
                        throw makeGenericFault("getTopicEvent", "6", "Cannot apply format");
                    }
                }
            }

        }

        Event[] topicEvent = new Event[eventList.size()];
        for (int i = 0; i < eventList.size(); i++) {
            SensorEvent event = (SensorEvent) eventList.get(i);
            topicEvent[i] = new Event();
            topicEvent[i].setID(event.getID());
            topicEvent[i].setTimestamp(event.getTimestamp());
            topicEvent[i].setMessage(event.getMessage());
            topicEvent[i].setProducer(event.getProducer());
        }

        GetTopicEventResponse response = new GetTopicEventResponse();
        EventArray tmpArray = new EventArray();
        tmpArray.setEvent(topicEvent);
        response.setEventArray(tmpArray);
        return response;
    }

    public NotifyResponse notify(Notify notify) {
        return new NotifyResponse();
    }

    public PauseSubscriptionResponse pauseSubscription(PauseSubscription pauseSubscription)
        throws Authorization_Fault, Authentication_Fault, SubscriptionNotFound_Fault, Generic_Fault {
        if (pauseSubscription == null || pauseSubscription.getSubscriptionRef() == null) {
            throw makeGenericFault("resumeSubscription", "0", "Missing subscription reference");
        }

        String subscriptionID = pauseSubscription.getSubscriptionRef().getSubscriptionID();
        if ((subscriptionID == null) || subscriptionID.equals("")) {
            throw makeGenericFault("pauseSubscription", "0", "Subscription ID undefined");
        }

        try {
            SubscriptionPersistent subscription = subscriptionRegistry.getSubscription(subscriptionID);
            subscription.pause();
            subscriptionRegistry.update(subscription);
        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
            throw makeGenericFault("pauseSubscription", "0", "Cannot pause subscription " + subscriptionID);
        }

        PauseSubscriptionResponse response = new PauseSubscriptionResponse();
        return response;
    }

    public synchronized void doOnSensorEvent(SensorEvent event) {
        if (event == null) {
            return;
        }

        Sensor sensor = event.getSource();

        if (sensor != null) {
            try {
                if (topicHolder == null) {
                    logger.error("TopicHolder not initialized");
                } else {
                    topicHolder.setCurrentEvent(sensor.getType(), event);
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    public synchronized void notify(ConfigurationEvent event) {

        CEMonServiceConfig sConfiguration = CEMonServiceConfig.getConfiguration();

        Class<?> category = event.getCategory();

        if (category == CEMonServiceConfig.getClassForCategory("sensor")) {

            synchronized (sensorHolder) {
                List<Resource> sensors = sensorHolder.getResourceList();
                for (Resource resource : sensors) {
                    try {
                        removeSensor((Sensor) resource);
                        sensorHolder.removeResource(resource);
                        logger.info("Removed sensor " + resource.getName());
                    } catch (Throwable th) {
                        logger.error("Cannot remove sensor " + resource.getName(), th);
                    }
                }

                ArrayList<Resource> allResList = sConfiguration.getResources("sensor");

                for (Resource resource : allResList) {
                    try {
                        addSensor((Sensor) resource);
                        sensorHolder.addResource(resource);
                        logger.info("Inserted sensor " + resource.getName());
                    } catch (Throwable th) {
                        logger.error("Cannot insert sensor " + resource.getName(), th);
                    }
                }
            }

        } else if (category == CEMonServiceConfig.getClassForCategory("subscription")) {

            synchronized (subscriptionRegistry) {

                List<Resource> pSubscriptions = predefinedSubscriptionHolder.getResourceList();
                for (Resource resource : pSubscriptions) {
                    try {
                        SubscriptionPersistent pSubscr = (SubscriptionPersistent) resource;
                        NotificationHolder notificationHolder = notificationPool.activateHolder(pSubscr, topicHolder,
                                actionHolder, queryProcessorHolder, subscriptionRegistry);
                        notificationHolder.removeSubscription(pSubscr);
                        subscriptionRegistry.unregister(pSubscr);
                        logger.info("removed subscription [id = " + pSubscr.getId() + "]");

                        notificationPool.check();

                    } catch (Throwable th) {
                        logger.error("Cannot remove sensor " + resource.getName(), th);
                    }
                }

                ArrayList<Resource> resList = sConfiguration.getResources("subscription");
                fillInSubscriptionPersistentList(resList);
            }

        }
    }

    private void fillInSubscriptionPersistentList(ArrayList<Resource> resList) {
        for (Resource resource : resList) {
            predefinedSubscriptionHolder.addResource(resource);
            try {
                SubscriptionPersistent subscription = (SubscriptionPersistent) resource;
                logger.debug("Registering subscription: " + subscription.getId() + " "
                        + subscription.getMonitorConsumerURL());
                checkSubscription(subscription);
                subscription.setRemoveOnStartup(true);
                subscriptionRegistry.register(subscription);
                addToNotificationHolder(subscription);
                logger.info("added new subscription [id = " + subscription.getId() + "]");
            } catch (Throwable th) {
                logger.error(th.getMessage(), th);
            }
        }

    }

    private void addSensor(Sensor sensor) {

        String sensorType = sensor.getType();
        if (sensorType == null) {
        }

        try {
            sensor.init();
            sensor.addSensorListener(this);

            SensorOutputDataFormat[] formats = sensor.getFormats();
            org.glite.ce.monitorapij.resource.types.Dialect[] dialects = new org.glite.ce.monitorapij.resource.types.Dialect[formats.length];

            for (int i = 0; i < formats.length; i++) {
                dialects[i] = new org.glite.ce.monitorapij.resource.types.Dialect();
                dialects[i].setName(formats[i].getName());
                dialects[i].setQueryLanguage(formats[i].getSupportedQueryLang());
            }

            org.glite.ce.monitorapij.resource.types.Topic topic = new org.glite.ce.monitorapij.resource.types.Topic(
                    sensorType);
            topic.setPurgeAllEventsOnStartup(sensor.isPurgeAllEventsOnStartup());
            topic.setDialect(dialects);
            topic.setEventOverwriteModeActive(sensor.isEventOverwriteModeActive());
            topic.setVisibility((String) visibilityTable.get(sensor.getScope()));

            try {
                /*
                 * TODO verify deadlock
                 */
                topicHolder.addTopic(topic);

            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }

            try {
                sensor.startSensor();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                removeSensor(sensor);
            }

        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
        }
    }

    private void removeSensor(Sensor sensor) {
        sensor.removeSensorListener(this);

        try {
            topicHolder.removeTopic(sensor.getType());
        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
        }

        sensor.destroySensor();

    }

    private void checkSubscription(SubscriptionPersistent subscription)
        throws Subscription_Fault {
        try {
            if (subscription == null) {
                throw new Exception("Subscription undefined");
            }

            Calendar time = Calendar.getInstance();

            if (subscription.getExpirationTime() == null) {

                time.add(Calendar.HOUR_OF_DAY, maxExpirationTime_Hours);
                subscription.setExpirationTime(time);

            } else if (subscription.getExpirationTime().before(time)) {
                throw new Exception("The subscription is expired");
            } else {
                time.add(Calendar.HOUR_OF_DAY, maxExpirationTime_Hours);

                if (time.before(subscription.getExpirationTime())) {
                    subscription.setExpirationTime(time);
                }
            }

            if (subscription.getMonitorConsumerURL() == null) {
                throw new Exception("MonitorConsumerURL not specified");
            }

            org.glite.ce.monitorapij.resource.types.Policy policy = subscription.getPolicy();
            if (policy == null) {

                policy = new org.glite.ce.monitorapij.resource.types.Policy();
                policy.setRate(5);
                subscription.setPolicy(policy);

            } else if (policy.getRate() < 5) {
                policy.setRate(5);
            }

            org.glite.ce.monitorapij.resource.types.Topic topic = subscription.getTopic();

            if (topic == null) {
                throw new Exception("Topic not specified");
            }

            if (!topicHolder.checkSupportedTopic(topic.getName())) {
                throw new Exception("Topic not supported");
            }

            org.glite.ce.monitorapij.resource.types.Dialect[] dialects = topic.getDialect();
            org.glite.ce.monitorapij.resource.types.Topic systemTopic = topicHolder.getTopic(topic.getName());
            topic.setVisibility(systemTopic.getVisibility());

            if ((dialects != null) && (dialects.length > 0)) {

                org.glite.ce.monitorapij.resource.types.Dialect dialect = systemTopic.getDialect(dialects[0].getName());

                if (dialect == null) {
                    throw new Exception("Dialect \"" + dialects[0].getName() + "\" not supported");
                }

                org.glite.ce.monitorapij.resource.types.Query query = policy.getQuery();
                if ((query != null) && (query.getQueryLanguage() != null)) {
                    if (!dialect.checkSupportedQueryLanguage(query.getQueryLanguage())) {
                        throw new Exception("QueryLanguage [" + query.getQueryLanguage() + "] not supported ");
                    }
                }

                org.glite.ce.monitorapij.resource.types.Action[] actionArray = policy.getAction();
                if (actionArray != null) {
                    for (int i = 0; i < actionArray.length; i++) {
                        org.glite.ce.monitorapij.resource.types.Action action = actionHolder.getAction(actionArray[i]
                                .getName());
                        if (action == null) {
                            throw new Exception("Action [" + actionArray[i].getName() + "] not supported");
                        }
                    }
                }

            } else {
                org.glite.ce.monitorapij.resource.types.Dialect[] dialectArray = systemTopic.getDialect();
                if (dialectArray != null && (dialectArray.length > 0)) {
                    org.glite.ce.monitorapij.resource.types.Dialect[] newDialArray = new org.glite.ce.monitorapij.resource.types.Dialect[1];
                    newDialArray[0] = new org.glite.ce.monitorapij.resource.types.Dialect();
                    newDialArray[0].setName(dialectArray[0].getName());
                    newDialArray[0].setQueryLanguage(dialectArray[0].getQueryLanguage());
                    topic.setDialect(newDialArray);
                }
            }

        } catch (Throwable th) {
            Subscription_Fault fault = new Subscription_Fault();
            SubscriptionFault msg = new SubscriptionFault();
            msg.setMethodName("checkSubscription");
            msg.setErrorCode("0");
            msg.setDescription(th.getMessage());
            msg.setFaultCause(th.getMessage());
            msg.setTimestamp(new GregorianCalendar());
            fault.setFaultMessage(msg);
            throw fault;
        }

    }

    protected void addToNotificationHolder(SubscriptionPersistent subscription) {
        org.glite.ce.monitorapij.resource.types.Policy policy = subscription.getPolicy();
        int rate = 60;

        if (policy != null) {
            rate = policy.getRate();
        } else {
            subscription.setPolicy(new org.glite.ce.monitorapij.resource.types.Policy());

            synchronized (sensorHolder) {
                try {
                    Sensor sensor = sensorHolder.getSensorByType(subscription.getTopic().getName());
                    rate = Integer.parseInt(((Resource) sensor).getProperty("executionDelay").getValue());
                } catch (Throwable th) {
                    logger.error(th.getMessage(), th);
                }
            }
        }

        if (rate < 5) {
            rate = 5;
        }

        subscription.getPolicy().setRate(rate);

        NotificationHolder notificationHolder = notificationPool.activateHolder(subscription, topicHolder,
                actionHolder, queryProcessorHolder, subscriptionRegistry);

        logger.debug("Adding SUBSCRIPTION to NotificationHolder: " + subscription.getCompleteInfo());

        notificationHolder.addSubscription(subscription);

    }

    private Generic_Fault makeGenericFault(String methodName, String errorCode, String description) {
        Generic_Fault fault = new Generic_Fault();
        GenericFault msg = new GenericFault();
        msg.setMethodName(methodName);
        msg.setErrorCode(errorCode);
        msg.setDescription(description);
        msg.setFaultCause(description);
        msg.setTimestamp(new GregorianCalendar());
        fault.setFaultMessage(msg);
        return fault;
    }

    private Authorization_Fault makeAuthorizationFault(String methodName, String errorCode, String description) {
        Authorization_Fault fault = new Authorization_Fault();
        AuthorizationFault msg = new AuthorizationFault();
        msg.setMethodName(methodName);
        msg.setErrorCode(errorCode);
        msg.setDescription(description);
        msg.setFaultCause(description);
        msg.setTimestamp(new GregorianCalendar());
        fault.setFaultMessage(msg);
        return fault;
    }

    private TopicNotSupported_Fault makeTopicNotSupportedFault(String methodName, String errorCode, String description) {
        TopicNotSupported_Fault fault = new TopicNotSupported_Fault();
        TopicNotSupportedFault msg = new TopicNotSupportedFault();
        msg.setMethodName(methodName);
        msg.setErrorCode(errorCode);
        msg.setDescription(description);
        msg.setFaultCause(description);
        msg.setTimestamp(new GregorianCalendar());
        fault.setFaultMessage(msg);
        return fault;
    }

    private DialectNotSupported_Fault makeDialectNotSupportedFault(String methodName, String errorCode,
            String description) {
        DialectNotSupported_Fault fault = new DialectNotSupported_Fault();
        DialectNotSupportedFault msg = new DialectNotSupportedFault();
        msg.setErrorCode(errorCode);
        msg.setDescription(description);
        msg.setFaultCause(description);
        msg.setTimestamp(new GregorianCalendar());
        fault.setFaultMessage(msg);
        return fault;
    }

    private String getUserDN_FQAN(String vo) {
        String fqan = "";
        List<String> fqanlist = null;

        if (vo != null) {
            fqanlist = CEUtils.getFQAN(vo);
        } else {
            String userVO = CEUtils.getUserDefaultVO();

            if (userVO != null) {
                fqanlist = CEUtils.getFQAN(userVO);
            }
        }

        if (fqanlist != null && fqanlist.size() > 0) {
            fqan = fqanlist.get(0).toString().replaceAll("\\W", "_");
        }

        return CEUtils.getUserDN_X500().replaceAll("\\W", "_") + fqan;
    }

}
