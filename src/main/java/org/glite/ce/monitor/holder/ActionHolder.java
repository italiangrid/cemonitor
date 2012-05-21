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

import org.glite.ce.monitorapij.resource.Resource;
import org.glite.ce.monitorapij.resource.types.Action;



/**
 * This class is an extension of <code>ResourceHolder</code> class, characterized for <code>Action</code>s.
 * It is used to handle a list of <code>Action</code>s placed in a specified directory as plugins.
 * An XML config file is used to store and retreive informations about these actions.
 * The task of checking if actions are added or removed or modified can be scheduled for repeated execution.
 */
public class ActionHolder extends BasicResourceHolder {
    
    /**
     * Creates a new ActionHolder object.
     * @throws InstantiationException 
     */
    public ActionHolder() throws InstantiationException {
        super();
    }

    /**
     * Get the <code>Action</code> saved in the specified position.
     *
     * @param id The position of the searched action in the resource Array.
     *
     * @return The casted <code>Action</code> positioned as specified int the resource array. 
     * @see org.glite.ce.monitorapij.resource.ResourceHolder.getResource(int arg0).
     */
    public Action getActionById(String id) {
        return (Action) getResource(id);
    }

    /**
     * Get the <code>Action</code> Array casted from resources array.
     *
     * @return The <code>Action</code> Array casted from resources array.
     * @see org.glite.ce.monitorapij.resource.ResourceHolder.getResources().
     */
    public Action[] getActions() {
        Resource[] resource = getResources();
        Action[] result = new Action[resource.length];
        System.arraycopy(resource, 0, result, 0, resource.length);

        return result;
    }
    
    /**
     * Get an <code>Action</code> Array containing the actions named as specified casted from resources array.
     *
     * @param name The name of the searched actions.
     * @return The <code>Action</code> Array containing the actions named as specified casted from resources array.
     * @see org.glite.ce.monitorapij.resource.ResourceHolder.getResourceByName(String name).
     */
    public Action getAction(String name) {
        Resource[] resource = getResourceByName(name);
        
        if(resource != null && resource.length>0) {
            return (Action)resource[0];
        }
        
        return null;
    }

    public String getCategory() {
        return "action";
    }
    
}
