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

/**
 * This class is an extension of <code>ResourceHolder</code> class, characterized for <code>PredefinedSubscription</code>s.
 * It is used to handle a list of <code>PredefinedSubscription</code>s placed in a specified path.
 * The task of checking if predefined subscriptions are added or removed or modified 
 * can be scheduled for repeated execution.
 */
public class PredefinedSubscriptionHolder extends BasicResourceHolder {

    public PredefinedSubscriptionHolder() throws InstantiationException {
        super();
    }
    
    public String getCategory(){
        return "subscription";
    }
    
}
