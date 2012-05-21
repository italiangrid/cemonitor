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

import java.util.ArrayList;

import org.glite.ce.monitorapij.queryprocessor.QueryResult;
import org.glite.ce.monitorapij.resource.types.Action;
import org.glite.ce.monitorapij.resource.types.Parameter;
import org.glite.ce.monitorapij.ws.CEMonitorConsumerStub;

/**
 * This class is an extension of <code>Action</code> class.<br>
 * It is used to mark an ISM_LDIF formatted message as not valid. This is done
 * by filtering the messages which received a successful
 * <code>QueryResult</code> and eliminating the others. The TTLCEinfo field will
 * not be added to the LDIF message, as done by
 * <code>SendNotificationAction</code>, so as the purchaser receiving this
 * notification will not interpret this message as valid.
 */
public class DoNotSendNotificationAction
    extends Action {

    private static final long serialVersionUID = 1318509641;

    /**
     * Creates a new DoNotSendNotificationAction object.
     */
    public DoNotSendNotificationAction() {

        super("DoNotSendNotification");

    }

    /**
     * This method is called by <code>NotificationHolder</code> specifying a
     * <code>Notification</code> and a <code>QueryResult</code> array.<br>
     * It performs the specific operations of this action:<br>
     * The dialect is read from the topic of the specified notification, and
     * then it is checked using checkDialect() method to verify if it is a
     * ISM_LDIF dialect.<br>
     * Then, for each QueryResult, the relative sensor event is called and its
     * message is changed removing unsuccessful messages.<br>
     * Note that the TTLCEinfo information will not be added to the message, in
     * this way the purchaser receiving the notification will interpret it as
     * not valid.
     * 
     * @param The
     *            specified notification.
     * @param result
     *            The specified <code>QueryResult</code> array.
     * @throws Exception
     *             thrown if the dialect is null or different from either
     *             ISM_LDIF or ISM_CLASSAD.
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
        if (result.length == 0) {
            return;
        }

        CEMonitorConsumerStub.Event[] eventArray = notification.getEvent();

        ArrayList<CEMonitorConsumerStub.Event> goodEventList = new ArrayList<CEMonitorConsumerStub.Event>(0);

        for (int i = 0; i < eventArray.length; i++) {
            ArrayList<String> goodMessages = new ArrayList<String>(0);
            CEMonitorConsumerStub.Event event = eventArray[i];
            QueryResult eventResult = result[i];

            for (int j = 0; j < eventResult.size(); j++) {
                if (eventResult.isSuccessfully(j) != this.isDoActionWhenQueryIs()) {
                    goodMessages.add(event.getMessage()[j]);
                }
            }

            if (goodMessages.size() > 0) {
                String[] msg = new String[goodMessages.size()];
                goodMessages.toArray(msg);
                event.setMessage(msg);
                goodEventList.add(event);
            }
        }
        eventArray = null;
        eventArray = new CEMonitorConsumerStub.Event[goodEventList.size()];
        goodEventList.toArray(eventArray);
        notification.setEvent(eventArray);

        goodEventList.clear();
        goodEventList = null;
    }
}
