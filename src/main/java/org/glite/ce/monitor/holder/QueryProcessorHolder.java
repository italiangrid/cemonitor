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

import org.glite.ce.monitorapij.queryprocessor.QueryProcessor;
import org.glite.ce.monitorapij.resource.Resource;

/**
 * This class is an extension of <code>ResourceHolder</code> class, characterized for <code>QueryProcessor</code>s.
 * It is used to handle a list of <code>QueryProcessor</code>s placed in a specified directory as plugins.
 * An XML config file is used to store and retreive informations about these processors.
 * The task of checking if processors are added or removed or modified can be scheduled for repeated execution.
 */
public class QueryProcessorHolder extends BasicResourceHolder {
    
    public QueryProcessorHolder() throws InstantiationException {
        super();
    }
    
    /**
     * Get the <code>QueryProcessor</code> Array casted from resources array.
     *
     * @return The <code>QueryProcessor</code> Array casted from resources array.
     * @see org.glite.ce.monitorapij.resource.ResourceHolder.getResources().
     */
    public QueryProcessor[] getQueryProcessors() {
        Resource[] resource = getResources();
        QueryProcessor[] result = new QueryProcessor[resource.length];
        System.arraycopy(resource, 0, result, 0, resource.length);

        return result;
    }
    /**
     * Get an <code>QueryProcessor</code> Array containing the processors named as specified casted from resources array.
     *
     * @param name The name of the searched processors.
     * @return The <code>QueryProcessor</code> Array containing the processors named as specified casted from resources array.
     * @see org.glite.ce.monitorapij.resource.ResourceHolder.getResourceByName(String name).
     */
    public QueryProcessor getQueryProcessor(String name) {
        Resource[] resource = getResourceByName(name);
        
        if(resource != null && resource.length>0) {
            return (QueryProcessor)resource[0];
        }
        
        return null;
    }

    public String getCategory(){
        return "queryprocessor";
    }

}
