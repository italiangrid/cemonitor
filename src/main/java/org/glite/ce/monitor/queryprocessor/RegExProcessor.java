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

package org.glite.ce.monitor.queryprocessor;

import org.apache.log4j.Logger;

import org.glite.ce.monitorapij.queryprocessor.QueryProcessor;
import org.glite.ce.monitorapij.queryprocessor.QueryResult;
import org.glite.ce.monitorapij.resource.Resource;
import org.glite.ce.monitorapij.sensor.SensorEvent;
import org.glite.ce.monitorapij.sensor.SensorOutputDataFormat;
import org.glite.ce.monitorapij.resource.types.CEMonResource;
import org.glite.ce.monitorapij.resource.types.Query;
import org.glite.ce.monitorapij.types.TopicEvent;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class extends <code>CEMonResource</code> 
 * and implements the interfaces <code>Resource</code> and <code>QueryProcessor</code>.<br>
 * It is a query processor specialized in RegEx queries. 
 * It supplies methods to perform a RegEx query on an event.
 */
public class RegExProcessor extends CEMonResource implements Resource, QueryProcessor {
    private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(RegExProcessor.class.getName());

    /**
     * Creates a new RegExProcessor object.
     * Its name is "RegEx" and its type is "QueryProcessor".
     */
    public RegExProcessor() {
        super("RegEx", "QueryProcessor");
    }

    /**
     * Private method.
     *
     * @param query DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    private void checkQuery(Query query) throws Exception {
        if (query == null) {
            throw (new Exception("Invalid argument: the query is null!"));
        }

        if (query.getQueryLanguage() == null) {
            throw (new Exception("the query language is null"));
        }

        if (!query.getQueryLanguage().equalsIgnoreCase(getName())) {
            throw (new Exception("Query Language mismatch: expected \"" + getName() + "\", found \"" + query.getQueryLanguage() + "\""));
        }

        if (query.getExpression() == null) {
            throw (new Exception("the query expression is null"));
        }
    }

    /**
     * Private Method.
     *
     * @param event DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    private void checkEvent(TopicEvent event) throws Exception {
        if (event == null) {
            throw (new Exception("Invalid argument: the event is null!"));
        }

        if ((event.getMessage() == null) || (event.getMessage().length == 0)) {
            throw (new Exception("the event is empty"));
        }
    }

    /**
     * Evaluate the query for each single message of the event. 
     * Then return the results in an array of <code>QueryResult</code>.
     * If the format of the sensor event is "ISM_LDIF" than the first query result is always true.
     *
     * @param query The query to perform.
     * @param event The event evaluated.
     *
     * @return The results of the query in an array of <code>QueryResult</code>.
     * 
     *
     * @throws Exception thrown if the query is not correctly formulated or the event does not contain 
     * messages.
     */
    public QueryResult evaluate(Query query, SensorEvent event) throws Exception {    
        checkQuery(query);
        checkEvent(event);

        ArrayList goodMessages = new ArrayList();
        String[] messages = event.getMessage();
        QueryResult queryResult = new QueryResult(messages.length);

        String regex = query.getExpression();

    //    logger.debug("REGEX!!!!!   " + regex);
    
        SensorEvent se = (SensorEvent) event;
        SensorOutputDataFormat sodf = se.getSensorOutputDataFormatApplied();
        int index = 0;

        if (sodf.getName().equalsIgnoreCase("ISM_LDIF")) {
            queryResult.setResult(0, true);
            index = 1;
        }
        

        Pattern pattern = (regex != null) ? Pattern.compile(regex) : null;

        for (int i = index; i < messages.length; i++) {
            try {
                Matcher m = pattern.matcher(messages[i]);
                boolean good = m.find();

               // queryResults[index] = new QueryResult(good, messages[i], "" + good);
                queryResult.setResult(index, good);                
            } catch (Exception e) {
              //  queryResults[index] = new QueryResult(false, messages[i], e.getMessage());
                queryResult.setResult(index, false);                
            } finally {
                index++;
            }
        }

        return queryResult;
    }
}
