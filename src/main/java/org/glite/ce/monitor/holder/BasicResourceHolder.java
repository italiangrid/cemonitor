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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.glite.ce.monitorapij.resource.Resource;

public abstract class BasicResourceHolder {

    private static Logger logger = Logger.getLogger(BasicResourceHolder.class.getName());

    protected HashMap<String, Resource> resourceList;

    public BasicResourceHolder() {
        resourceList = new HashMap<String, Resource>(0);
    }

    public void addResource(Resource resource) {
        if (resource != null) {
            logger.debug("Inserted new resource " + resource.getId());
            resourceList.put(resource.getId(), resource);
        }
    }

    public void removeResource(Resource resource) {
        if (resource != null) {
            logger.debug("Removed resource " + resource.getId());
            resourceList.remove(resource.getId());
        }
    }

    public Resource getResource(String id) {
        return (id != null) ? resourceList.get(id) : null;
    }

    public int getResourceListSize() {
        return resourceList.size();
    }

    public Resource[] getResources() {
        Resource[] resources = new Resource[resourceList.values().size()];
        resourceList.values().toArray(resources);
        return resources;
    }

    public List<Resource> getResourceList() {
        return new ArrayList<Resource>(resourceList.values());
    }

    public Resource[] getResourceByType(String type) {
        ArrayList<Resource> tmpList = new ArrayList<Resource>(0);

        for (Resource res : resourceList.values()) {
            if (res.getType().equals(type)) {
                tmpList.add(res);
            }
        }

        Resource[] result = new Resource[tmpList.size()];
        tmpList.toArray(result);
        return result;
    }

    public Resource[] getResourceByJARPath(URI path) {
        ArrayList<Resource> tmpList = new ArrayList<Resource>(0);

        for (Resource res : resourceList.values()) {
            if (res.getJarPath().equals(path)) {
                tmpList.add(res);
            }
        }

        Resource[] result = new Resource[tmpList.size()];
        tmpList.toArray(result);
        return result;
    }

    public Resource[] getResourceByName(String name) {
        ArrayList<Resource> tmpList = new ArrayList<Resource>(0);

        for (Resource res : resourceList.values()) {
            if (res.getName().equals(name)) {
                tmpList.add(res);
            }
        }

        Resource[] result = new Resource[tmpList.size()];
        tmpList.toArray(result);
        return result;
    }

    public void setResources(Resource[] resources) {
        clear();

        if (resources == null) {
            return;
        }

        for (int i = 0; i < resources.length; i++) {
            addResource(resources[i]);
        }
    }

    public void clear() {
        resourceList.clear();
    }

}
