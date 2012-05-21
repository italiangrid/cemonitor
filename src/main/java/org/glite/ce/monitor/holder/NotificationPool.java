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

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.glite.ce.monitor.registry.SubscriptionRegistry;
import org.glite.ce.monitorapij.resource.types.SubscriptionPersistent;

public class NotificationPool {

    private final static Logger logger = Logger.getLogger(NotificationPool.class.getName());

    private HashMap<String, NotificationHolder> pool;

    private boolean notificationSharedThread;

    public NotificationPool(boolean sharedThread) {
        pool = new HashMap<String, NotificationHolder>();
        notificationSharedThread = sharedThread;
    }

    public NotificationHolder activateHolder(String name, int rate, TopicHolder topicHolder,
            QueryProcessorHolder queryProcessorHolder, SubscriptionRegistry subscriptionRegistry) {

        if (pool.containsKey(name)) {
            return pool.get(name);
        }

        NotificationHolder holder = new NotificationHolder(name, rate, topicHolder, queryProcessorHolder,
                subscriptionRegistry, this);
        pool.put(name, holder);

        return holder;
    }

    public NotificationHolder activateHolder(SubscriptionPersistent subscription, TopicHolder topicHolder,
            ActionHolder actionHolder, QueryProcessorHolder queryProcessorHolder,
            SubscriptionRegistry subscriptionRegistry) {

        String name = this.getNameFromSubscription(subscription);
        int rate = subscription.getPolicy().getRate() * 1000;

        return this.activateHolder(name, rate, topicHolder, queryProcessorHolder, subscriptionRegistry);
    }

    public NotificationHolder getHolder(String name) {
        return pool.get(name);
    }

    public NotificationHolder getHolder(SubscriptionPersistent subscription) {
        if (subscription == null) {
            return null;
        }

        return pool.get(this.getNameFromSubscription(subscription));
    }

    public void check() {
        for (NotificationHolder holder : pool.values()) {
            if (holder.getSubscriptionListSize() == 0) {
                holder.destroy();
                pool.remove(holder.getName());
                logger.info("removed notificationHolder [name = " + holder.getName() + "]");
            }
        }
    }

    public void destroy() {
        for (NotificationHolder holder : pool.values()) {
            holder.destroy();
        }

        pool.clear();
    }

    private String getNameFromSubscription(SubscriptionPersistent subscription) {
        return notificationSharedThread ? String.valueOf(subscription.getPolicy().getRate()) : subscription.getId();
    }
}
