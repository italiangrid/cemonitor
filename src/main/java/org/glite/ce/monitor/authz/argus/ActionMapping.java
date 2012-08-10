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

package org.glite.ce.monitor.authz.argus;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.glite.authz.pep.profile.AbstractAuthorizationProfile;
import org.glite.authz.pep.profile.GridCEAuthorizationProfile;
import org.glite.ce.commonj.authz.AuthorizationException;
import org.glite.ce.commonj.authz.argus.ActionMappingInterface;

public class ActionMapping
    implements ActionMappingInterface {

    public static final String[] mandProperties = new String[0];

    public String getXACMLAction(QName operation) {

        String opName = operation.getLocalPart();

        if (opName.equals("GetInfo") || opName.equals("GetTopics") || opName.equals("GetEvent")
                || opName.equals("GetTopicEvent")) {
            return GridCEAuthorizationProfile.ACTION_GET_INFO;
        }

        if (opName.equals("Subscribe") || opName.equals("Update") || opName.equals("Unsubscribe")
                || opName.equals("PauseSubscription") || opName.equals("ResumeSubscription")) {
            return GridCEAuthorizationProfile.ACTION_SUBSCRIPTION_MANAGE;
        }

        if (opName.equals("GetSubscription") || opName.equals("GetSubscriptionRef")) {
            return GridCEAuthorizationProfile.ACTION_SUBSCRIPTION_GET_INFO;
        }

        return null;
    }

    public String getAttributeMapping(String obId, String attrId) {
        return null;
    }

    public String[] getMandatoryProperties() {
        return mandProperties;
    }

    public void checkMandatoryProperties(Iterator<String> props)
        throws AuthorizationException {
    }

    public AbstractAuthorizationProfile getProfile() {
        return GridCEAuthorizationProfile.getInstance();
    }
}
