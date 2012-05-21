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

package org.glite.ce.monitor.registry;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.log4j.Logger;
import org.glite.ce.monitorapij.resource.types.SubscriptionPersistent;
import org.glite.ce.monitorapij.resource.types.Topic;

/**
 * 
 * This class is an extension of <code>ResourceRegistry</code> characterized to
 * manage a list of subscriptions.<br>
 * This list is initially specified in a configuration file and then can be
 * modified adding or removing subscriptions.<br>
 * If the subscription is persistent, each modification on it will be saved in
 * the configuration file.
 */
public class SubscriptionRegistry {

    private final static Logger logger = Logger.getLogger(SubscriptionRegistry.class.getName());

    private DirContext rootCtx;

    /**
     * Creates a new SubscriptionRegistry object and initializes it. The
     * configuration file name is set to the specified string.
     * 
     * @param filename
     *            The name of the configuration file.
     * 
     * @throws FileNotFoundException
     *             thrown if the configuration file named as specified has not
     *             been found.
     * @throws Exception
     *             thrown if there is any problem writing or reading the
     *             configuration file.
     */
    public SubscriptionRegistry(String cacheDir, int cacheSize, String factory)
        throws FileNotFoundException, NamingException {

        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, factory);
        env.put(Context.PROVIDER_URL, cacheDir);
        env.put("cache.size", new Integer(cacheSize));

        logger.debug("factory=[" + factory + "] - cacheDir=[" + cacheDir + "] - cacheSize=[" + cacheSize + "]");

        DirContext topCtx = new InitialDirContext(env);

        try {

            rootCtx = (DirContext) topCtx.lookup("subscription");
        } catch (NameNotFoundException nnfEx) {
            logger.warn("Context subscription not found, trying to create it");
            rootCtx = topCtx.createSubcontext("subscription", null);
        }

        /*
         * Remove subscriptions at startup
         */
        try {
            Enumeration<NameClassPair> allNames = rootCtx.list("");

            while (allNames.hasMoreElements()) {

                NameClassPair item = (NameClassPair) allNames.nextElement();

                Object obj = rootCtx.lookup(item.getName());
                if ((obj != null) && obj instanceof SubscriptionPersistent) {
                    SubscriptionPersistent subscription = (SubscriptionPersistent) obj;

                    if (subscription.removeOnStartup() || subscription.isExpired()) {
                        logger.debug("unregistering subscription: id = " + subscription.getId());

                        try {
                            unregister(subscription);
                        } catch (Throwable th) {
                            logger.error(th.getMessage(), th);
                        }

                    }
                }
            }
        } catch (Throwable th) {
            logger.error(th.getMessage(), th);
        }

    }

    /**
     * Get a <code>SubscriptionPersistent</code> object created using the
     * specified subscription.<br>
     * This will be added in the registry. as this is considered persistent by
     * default.
     * 
     * @param subscription
     *            The subscription to add.
     * 
     * @return A <code>SubscriptionPersistent</code> generated using the
     *         specified subscription.
     * 
     * @throws Exception
     *             thrown if the subscription is null or expired or if some
     *             problem occurs reading or writing the configuration file.
     */
    public void register(SubscriptionPersistent subscription)
        throws IllegalArgumentException, NamingException {
        if (subscription == null) {
            throw (new IllegalArgumentException("the subscription is null"));
        }

        if (subscription.getId() == null) {
            throw (new IllegalArgumentException("The key argument is null"));
        }

        rootCtx.bind(subscription.getId(), subscription);
    }

    public void unregister(SubscriptionPersistent subscription)
        throws IllegalArgumentException, NamingException {
        if (subscription == null) {
            throw (new IllegalArgumentException("the subscription is null"));
        }

        unregister(subscription.getId());
    }

    public void unregister(String key)
        throws NamingException {
        if (key == null) {
            throw (new IllegalArgumentException("The key argument is null"));
        }

        rootCtx.unbind(key);
    }

    public void update(SubscriptionPersistent subscription)
        throws IllegalArgumentException, NamingException {
        if (subscription == null) {
            throw (new IllegalArgumentException("the subscription is null"));
        }

        if (subscription.getId() == null) {
            throw (new IllegalArgumentException("The key argument is null"));
        }

        rootCtx.rebind(subscription.getId(), subscription);
    }

    /**
     * Get the registered <code>SubscriptionPersistent</code> specifying its ID.
     * 
     * @param id
     *            The identifier of the subscription.
     * 
     * @return The required <code>SubscriptionPersistent</code> if present in
     *         the registry, null otherwise.
     * @throws NamingException
     */
    public SubscriptionPersistent getSubscription(String id)
        throws NamingException {
        SubscriptionPersistent subscription = null;

        if (id != null) {
            try {

                logger.debug("Looking for " + id);
                subscription = (SubscriptionPersistent) rootCtx.lookup(id);

            } catch (ClassCastException ccEx) {
                logger.error(ccEx);
            }
        }

        return subscription;
    }

    public ArrayList<SubscriptionPersistent> getAllSubscriptions() {

        return getSubscriptionByObject(null);

    }

    /**
     * Get the registered <code>SubscriptionPersistent</code> having the
     * specified <code>Topic</code>.<br>
     * Expired subscriptions are filtered and removed from the registry.
     * 
     * @param topic
     *            The <code>Topic</code> characterizing the required
     *            subscriptions.
     * 
     * @return An array of <code>SubscriptionPersistent</code> containing the
     *         required subscriptions, if existing.
     */
    public ArrayList<SubscriptionPersistent> getSubscriptionByTopic(Topic topic) {
        if (topic == null) {
            return null;
        }

        return getSubscriptionByObject(topic);

    }

    public ArrayList<SubscriptionPersistent> getSubscriptionBySubscriberId(String subscriberId) {
        if (subscriberId == null) {
            return null;
        }

        return getSubscriptionByObject(subscriberId);

    }

    private ArrayList<SubscriptionPersistent> getSubscriptionByObject(Object criterion) {
        ArrayList<SubscriptionPersistent> goodSubscriptions = new ArrayList<SubscriptionPersistent>();

        try {
            Enumeration<NameClassPair> allNames = rootCtx.list("");

            while (allNames.hasMoreElements()) {

                NameClassPair item = allNames.nextElement();

                Object obj = rootCtx.lookup(item.getName());

                if ((obj != null) && obj instanceof SubscriptionPersistent) {
                    SubscriptionPersistent subscription = (SubscriptionPersistent) obj;

                    if (subscription.isExpired()) {
                        try {
                            unregister(subscription.getId());
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                        }

                        continue;
                    }

                    if (criterion==null) {
                        
                        goodSubscriptions.add(subscription);
                        
                    }else if (criterion instanceof Topic) {
                        Topic topic = (Topic) criterion;
                        if ((subscription.getTopic() != null)
                                && subscription.getTopic().getName().equals(topic.getName())) {
                            goodSubscriptions.add(subscription);
                        }
                    } else {
                        String subscriberId = criterion.toString();
                        String sub_Id = subscription.getSubscriberId();

                        if ((sub_Id != null) && sub_Id.equals(subscriberId)) {
                            goodSubscriptions.add(subscription);
                        }
                    }

                }

            }
        } catch (NamingException nEx) {
            logger.error(nEx);
        }

        return goodSubscriptions;
    }

}
