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

import condor.classad.ClassAd;
import condor.classad.ClassAdParser;
import condor.classad.Expr;
import condor.classad.RecordExpr;

import org.glite.ce.monitorapij.queryprocessor.QueryProcessor;
import org.glite.ce.monitorapij.queryprocessor.QueryResult;
import org.glite.ce.monitorapij.resource.Resource;
import org.glite.ce.monitorapij.sensor.SensorEvent;
import org.glite.ce.monitorapij.sensor.SensorOutputDataFormat;
import org.glite.ce.monitorapij.resource.types.CEMonResource;
import org.glite.ce.monitorapij.resource.types.Query;
import org.glite.ce.monitorapij.types.TopicEvent;

import java.io.StringReader;

import java.util.ArrayList;

/**
 * This class extends <code>CEMonResource</code> and implements the interfaces
 * <code>Resource</code> and <code>QueryProcessor</code>.<br>
 * It is a query processor specialized in classAd queries. It supplies methods
 * to perform a classAd query on an event.
 */
public class ClassAdProcessor extends CEMonResource implements Resource, QueryProcessor {
    private static final long serialVersionUID = 1L;



    /**
     * Creates a new ClassAdProcessor object.
     */
    public ClassAdProcessor() {
        super("ClassAd", "QueryProcessor");
    }



    /**
     * Private method. Check if the query is correctly specified (i.e. it isn't
     * null, it's language is "ClassAd" and expression is specified).
     * 
     * @param query
     *            The checked query.
     * @throws Exception
     *             thrown if the query isn't correctly specified.
     */
    private void checkQuery(Query query) throws Exception {
        if(query == null) {
            throw (new Exception("Invalid argument: the query is null!"));
        }

        if(query.getQueryLanguage() == null) {
            throw (new Exception("the query language is null"));
        }

        if(!query.getQueryLanguage().equalsIgnoreCase(getName())) {
            throw (new Exception("Query Language mismatch: expected \"" + getName() + "\", found \"" + query.getQueryLanguage() + "\""));
        }

        if(query.getExpression() == null) {
            throw (new Exception("the query expression is null"));
        }
    }



    /**
     * Private method.
     * 
     * @param event
     *            DOCUMENT ME!
     * @throws Exception
     *             DOCUMENT ME!
     */
    private void checkEvent(TopicEvent event) throws Exception {
        if(event == null) {
            throw (new Exception("Invalid argument: the event is null!"));
        }

        if((event.getMessage() == null) || (event.getMessage().length == 0)) {
            throw (new Exception("the event is empty"));
        }
    }



    /**
     * Private method.
     * 
     * @param input
     *            DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    private Expr makeClassAdExpression(StringReader input) {
        int format = ClassAdParser.TEXT; // input format
        boolean tracing = false; // -t flag: tell the parser to trace

        ClassAdParser parser = new ClassAdParser(input, format);
        parser.enableTracing(tracing);

        return parser.parse();
    }



    /**
     * Evaluate the query for each single message of the event. Then return the
     * results in an array of <code>QueryResult</code>. If the format of the
     * sensor event is "ISM_LDIF" than the first query result is always true.
     * 
     * @param query
     *            The query to perform.
     * @param event
     *            The event evaluated.
     * @return The results of the query in an array of <code>QueryResult</code>.
     * @throws Exception
     *             Is thrown if the query is not correctly formulated or the
     *             event does not contain messages.
     */
    public QueryResult evaluate(Query query, SensorEvent event) throws Exception {
        checkQuery(query);
//        checkEvent(event);

        if(event == null) {
            throw (new Exception("Invalid argument: the event is null!"));
        }

        ArrayList goodMessages = new ArrayList(0);
        String[] messages = null;
        QueryResult queryResult = null;
        int index = 0;

        SensorEvent se = (SensorEvent) event;
/*
        SensorOutputDataFormat sodf = se.getSensorOutputDataFormatApplied();

        if(sodf.getName().equalsIgnoreCase("ISM_LDIF")) {
            SensorEvent clone = (SensorEvent) se.clone();
            clone.applyFormat("ISM_CLASSAD");
            messages = clone.getMessage();

            queryResult = new QueryResult(messages.length + 1);
            // queryResults[0] = new QueryResult(true, se.getMessage(0), "the
            // first ISM_LDIF message is always true for default");
            queryResult.setResult(0, true);
            index = 1;
        } else if(sodf.getName().equalsIgnoreCase("ISM_LDIF_GLUE_1.2")) {
            SensorEvent clone = (SensorEvent) se.clone();
            clone.applyFormat("ISM_CLASSAD_GLUE_1.2");
            messages = clone.getMessage();

            queryResult = new QueryResult(messages.length + 1);
            // queryResults[0] = new QueryResult(true, se.getMessage(0), "the
            // first ISM_LDIF message is always true for default");
            queryResult.setResult(0, true);
            index = 1;
        } else {        
            messages = event.getMessage();
            queryResult = new QueryResult(messages.length);
        }
*/
        messages = event.getMessage();

        if((messages == null || messages.length == 0)) {
            return new QueryResult(0);
        }

        queryResult = new QueryResult(messages.length);
            
        StringReader input = new StringReader("[test = " + query.getExpression() + " ];");


        ClassAdParser parser = new ClassAdParser(input, ClassAdParser.TEXT);
        parser.enableTracing(false);
        
      //  Expr p = makeClassAdExpression(input);
        Expr p = parser.parse();
        
        
        for (int i = 0; i < messages.length; i++) {
            try {
                StringReader input2 = new StringReader(messages[i]);
                                
                //RecordExpr prova = (RecordExpr) makeClassAdExpression(input2);
                parser.reset(input2);
                RecordExpr prova = (RecordExpr)parser.parse();
                
                prova.insertAttribute("test", p.selectExpr("test"));

                Expr val = ClassAd.eval(prova, new String[] {
                    "test"
                });

                // queryResults[index] = new QueryResult(val.isTrue(),
                // messages[i], "" + val.isTrue());
                queryResult.setResult(index, val.isTrue());
            } catch (Exception ex) {
                // queryResults[index] = new QueryResult(false, messages[i],
                // ex.getMessage());
                queryResult.setResult(index, false);
            } finally {
                index++;
            }
        }

        /*
         * logger.debug( "done msg:" + goodMessages.size( ) ); messages = new
         * String[goodMessages.size( )]; messages =
         * (String[])goodMessages.toArray( messages ); event.setMessage(
         * messages );
         */
        return queryResult;
    }
}
