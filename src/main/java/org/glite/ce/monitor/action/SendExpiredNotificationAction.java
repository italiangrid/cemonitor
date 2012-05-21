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

import org.glite.ce.monitorapij.queryprocessor.QueryResult;
import org.glite.ce.monitorapij.resource.types.Action;
import org.glite.ce.monitorapij.resource.types.Parameter;
import org.glite.ce.monitorapij.resource.types.Property;
import org.glite.ce.monitorapij.ws.CEMonitorConsumerStub;

/**
 * This class is an extension of <code>Action</code> class.<br>
 * It is used to mark an ISM_LDIF formatted message as expired message. This is
 * done by adding a TTLCEinfo=0 field to the LDIF message, so as the purchaser
 * will interpret this message as expired.
 */
public class SendExpiredNotificationAction
    extends Action {

    private static final long serialVersionUID = 1318511605;

    /**
     * Creates a new SendExpiredNotificationAction object.
     */
    public SendExpiredNotificationAction() {
        super("SendExpiredNotification");

        Property property = new Property("supportedDialectsList", "LFID,ISM_LDIF,OLD_CLASSAD,NEW_CLASSAD,ISM_CLASSAD");
        setProperty(new Property[] { property });
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

        String dialect = notification.getTopic().getDialect()[0].getName();

        // checkDialect(dialect);

        boolean isISM_LDIF = dialect.toLowerCase().indexOf("ldif") == -1 ? false : true;

        CEMonitorConsumerStub.Event[] eventArray = notification.getEvent();

        for (int i = 0; i < eventArray.length; i++) {
            CEMonitorConsumerStub.Event event = eventArray[i];
            QueryResult eventResult = result[i];

            for (int j = 0; j < eventResult.size(); j++) {
                if (eventResult.isSuccessfully(j) == this.isDoActionWhenQueryIs()) {
                    String[] msgList = event.getMessage();
                    String msg = msgList[j];

                    int index = msg.indexOf("TTLCEinfo");
                    if (index > -1) {
                        msg = msg.substring(0, index);
                    }

                    if (isISM_LDIF) {
                        if (j > 0) {
                            msg += "\nTTLCEinfo: 0";
                        }
                    } else {
                        msg = msg.substring(0, msg.length() - 2);
                        msg += "\n\tTTLCEinfo = 0;\n]";
                    }

                    msgList[j] = msg;
                    event.setMessage(msgList);
                }
            }
        }
    }
}
