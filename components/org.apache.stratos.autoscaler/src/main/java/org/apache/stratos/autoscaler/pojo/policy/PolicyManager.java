/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.autoscaler.pojo.policy;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.policy.InvalidPolicyException;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ApplicationPolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.common.clustering.DistributedObjectProvider;

import java.util.Map;

/**
 * Manager class for the purpose of managing Autoscale/Deployment policy definitions.
 */
public class PolicyManager {

    private static final Log log = LogFactory.getLog(PolicyManager.class);
    
    private static final String AS_POLICY_ID_TO_AUTOSCALE_POLICY_MAP = "AS_POLICY_ID_TO_AUTOSCALE_POLICY_MAP";
    private static final String APPLICATION_ID_TO_APPLICATION_POLICY_MAP = "APPLICATION_ID_TO_APPLICATION_POLICY_MAP";
    
    private final transient DistributedObjectProvider distributedObjectProvider;

    private static Map<String, AutoscalePolicy> autoscalePolicyListMap;

    private static Map<String, ApplicationPolicy> applicationPolicyListMap;
    
    /* An instance of a PolicyManager is created when the class is loaded. 
     * Since the class is loaded only once, it is guaranteed that an object of 
     * PolicyManager is created only once. Hence it is singleton.
     */
    
    private static class InstanceHolder {
        private static final PolicyManager INSTANCE = new PolicyManager(); 
    }

    public static PolicyManager getInstance() {
        return InstanceHolder.INSTANCE;
     }
    
    private PolicyManager() {
    	// Initialize distributed object provider
        DistributedObjectProvider distributedObjectProvider = ServiceReferenceHolder.getInstance().getDistributedObjectProvider();
        autoscalePolicyListMap = distributedObjectProvider.getMap(AS_POLICY_ID_TO_AUTOSCALE_POLICY_MAP);
        applicationPolicyListMap = distributedObjectProvider.getMap(APPLICATION_ID_TO_APPLICATION_POLICY_MAP);
    }

    // Add the policy to information model and persist.
    public boolean addAutoscalePolicy(AutoscalePolicy policy) throws InvalidPolicyException {
        if (log.isInfoEnabled()) {
            log.info(String.format("Adding autoscaling policy: [id] %s", policy.getId()));
        }
        if(StringUtils.isEmpty(policy.getId())){
            throw new AutoScalerException("Autoscaling policy id cannot be empty");
        }
        this.addASPolicyToInformationModel(policy);
        RegistryManager.getInstance().persistAutoscalerPolicy(policy);
        if (log.isInfoEnabled()) {
            log.info(String.format("Autoscaling policy is added successfully: [id] %s", policy.getId()));
        }
        return true;
    }

    public boolean updateAutoscalePolicy(AutoscalePolicy policy) throws InvalidPolicyException {
        if(StringUtils.isEmpty(policy.getId())){
            throw new AutoScalerException("Autoscaling policy id cannot be empty");
        }
        this.updateASPolicyInInformationModel(policy);
        RegistryManager.getInstance().persistAutoscalerPolicy(policy);
        if (log.isInfoEnabled()) {
            log.info(String.format("Autoscaling policy is updated successfully: [id] %s", policy.getId()));
        }
        return true;
    }

	public boolean removeAutoscalePolicy(String policyID) throws InvalidPolicyException {
		if (StringUtils.isEmpty(policyID)) {
			throw new AutoScalerException("Autoscaling policy id cannot be empty");
		}
		this.removeASPolicyInInformationModel(policyID);
		RegistryManager.getInstance().removeAutoscalerPolicy(policyID);
		if (log.isInfoEnabled()) {
			log.info(String.format("Autoscaling policy is removed successfully: [id] %s", policyID));
		}
		return true;
	}

    public void addASPolicyToInformationModel(AutoscalePolicy asPolicy) throws InvalidPolicyException {
        if (!autoscalePolicyListMap.containsKey(asPolicy.getId())) {
            if (log.isDebugEnabled()) {
                log.debug("Adding autoscaling policy: " + asPolicy.getId());
            }
            autoscalePolicyListMap.put(asPolicy.getId(), asPolicy);
        } else {
        	String errMsg = "Specified autoscaling policy [" + asPolicy.getId() + "] already exists";
        	log.error(errMsg);
            throw new InvalidPolicyException(errMsg);
        }
    }

    public void updateASPolicyInInformationModel(AutoscalePolicy asPolicy) throws InvalidPolicyException {
        if (autoscalePolicyListMap.containsKey(asPolicy.getId())) {
            if (log.isDebugEnabled()) {
                log.debug("Updating autoscaling policy: " + asPolicy.getId());
            }
            autoscalePolicyListMap.put(asPolicy.getId(), asPolicy);
        }
    }

	public void removeASPolicyInInformationModel(String policyID) throws InvalidPolicyException {
		if (autoscalePolicyListMap.containsKey(policyID)) {
			if (log.isDebugEnabled()) {
				log.debug("Updating autoscaling policy: " + policyID);
			}
			autoscalePolicyListMap.remove(policyID);
		}
		else{
			throw new InvalidPolicyException("No such policy ID [" + policyID + "] exists");
		}
	}
	
	public void removeApplicationPolicyInInformationModel(String applicationId) throws InvalidPolicyException {
		if (applicationPolicyListMap.containsKey(applicationId)) {
			if (log.isDebugEnabled()) {
				log.debug("Removing application policy [application-id] " + applicationId);
			}
			applicationPolicyListMap.remove(applicationId);
		}
		else{
			throw new InvalidPolicyException("No such application id [" + applicationId + "] exists");
		}
	}
	
    /**
     * Removes the specified policy
     *
     * @param policyId
     * @throws InvalidPolicyException
     */
    public void undeployAutoscalePolicy(String policyId) throws InvalidPolicyException {
        if (autoscalePolicyListMap.containsKey(policyId)) {
            if (log.isDebugEnabled()) {
                log.debug("Removing policy :" + policyId);
            }
            autoscalePolicyListMap.remove(policyId);
            RegistryManager.getInstance().removeAutoscalerPolicy(policyId);
        } else {
            throw new InvalidPolicyException("No such policy ID [" + policyId + "] exists");
        }
    }

    /**
     * Returns an array of the Autoscale policies contained in this manager.
     *
     * @return
     */
    public AutoscalePolicy[] getAutoscalePolicyList() {        
        return autoscalePolicyListMap.values().toArray(new AutoscalePolicy[0]);
    }

    /**
     * Returns the autoscale policy to which the specified id is mapped or null
     *
     * @param id
     * @return
     */
    public AutoscalePolicy getAutoscalePolicy(String id) {
        return autoscalePolicyListMap.get(id);
    }

	public void addApplicationPolicy(ApplicationPolicy applicationPolicy) throws InvalidPolicyException {
		String applicationId = applicationPolicy.getApplicationId();
		if (log.isInfoEnabled()) {
			log.info(String.format("Adding application policy for application: [id] %s", applicationId));
		}
		this.addApplicationPolicyToInformationModel(applicationPolicy);
		RegistryManager.getInstance().persistApplicationPolicy(applicationPolicy);

		if (log.isInfoEnabled()) {
			log.info(String
			        .format("Application policy is added successfully: [application-id] %s",
			                applicationId));
		}

	}
	
	public boolean removeApplicationPolicy(String applicationId) throws InvalidPolicyException {
		if (StringUtils.isEmpty(applicationId)) {
			throw new AutoScalerException("Application policy id cannot be empty");
		}
		this.removeApplicationPolicyInInformationModel(applicationId);
		RegistryManager.getInstance().removeApplicationPolicy(applicationId);
		if (log.isInfoEnabled()) {
			log.info(String.format("Application policy is removed successfully: [id] %s", applicationId));
		}
		return true;
	}

	public void addApplicationPolicyToInformationModel(ApplicationPolicy applicationPolicy) throws InvalidPolicyException {
		String applicationId = applicationPolicy.getApplicationId();
        if (!applicationPolicyListMap.containsKey(applicationId)) {
            if (log.isDebugEnabled()) {
                log.debug("Adding application policy for application Id: " + applicationId);
            }
            applicationPolicyListMap.put(applicationId, applicationPolicy);
        } else {
        	String errMsg = "Application policy is already exists for appplication [" + applicationId + "] ";
        	log.error(errMsg);
            throw new InvalidPolicyException(errMsg);
        }
	    
    }
	
	/**
	 * Retruns an ApplicationPolicy of a given application
	 * 
	 * @param applicationId
	 * @return
	 */
    public ApplicationPolicy getApplicationPolicy(String applicationId) {
        return applicationPolicyListMap.get(applicationId);
    }

}
