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
 * Author Luigi Zangrando <zangrando@pd.infn.it>
 *
 */

package org.glite.ce.monitor.action;

import java.util.Calendar;

import org.glite.ce.monitorapij.queryprocessor.QueryResult;
import org.glite.ce.monitorapij.resource.types.Action;
import org.glite.ce.monitorapij.resource.types.Parameter;
import org.glite.ce.monitorapij.ws.CEMonitorConsumerStub;

/**
 * This class is an extension of <code>Action</code> class.<br>
 * It is used to mark an ISM_LDIF formatted message as valid, not expired,
 * message. This is done by adding a TTLCEinfo=300 field to the LDIF message, so
 * as the purchaser will interpret this message as valid.
 */
public class SendNotificationAction
    extends Action {

    private static final long serialVersionUID = 1318509356;

    /**
     * Creates a new SendNotificationAction object.
     */
    public SendNotificationAction() {

        super("SendNotification");

    }

    /**
     * This method is called by <code>NotificationHolder</code> specifying a
     * <code>Notification</code> and a <code>QueryResult</code> array.<br>
     * It performs the specific operations of this action:<br>
     * The dialect is read from the topic of the specified notification, and
     * then it is checked using checkDialect() method to verify if it is a
     * ISM_LDIF dialect.<br>
     * Then, for each successful QueryResult, the relative sensor event is
     * called, and then its message is changed adding the TTLCEInfo information.
     * 
     * @param The
     *            specified notification.
     * @param result
     *            The specified <code>QueryResult</code> array.
     * 
     * @throws Exception
     *             DOCUMENT ME!
     */

    public synchronized void execute()
        throws Exception {
        Parameter parameter = getParameter("notification");

        if (parameter == null) {
            return;
        }

        CEMonitorConsumerStub.Notification notification = (CEMonitorConsumerStub.Notification) parameter.getValue();

        parameter = getParameter("queryResult");

        if (parameter == null) {
            return;
        }

        QueryResult[] result = (QueryResult[]) parameter.getValue();
        CEMonitorConsumerStub.Event[] eventArray = notification.getEvent();

        if ((result == null || result.length == 0) && (eventArray == null || eventArray.length == 0)) {
            parameter = getParameter("subscriptionId");
            if (parameter == null) {
                return;
            }

            if (notification.getTopic().getName().equalsIgnoreCase("CREAM_JOBS")) {
                String classad = "[ SUBSCRIPTION_ID = \"" + parameter.getValue() + "\";\nKEEP_ALIVE = true; ]";
                eventArray = new CEMonitorConsumerStub.Event[1];
                eventArray[0] = new CEMonitorConsumerStub.Event();
                eventArray[0].setID(-1);
                eventArray[0].setTimestamp(Calendar.getInstance());
                eventArray[0].setMessage(new String[] { classad });
                eventArray[0].setProducer("CREAM Job Sensor");

                notification.setEvent(eventArray);
            }
        }

    }
}
