/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.impl.dao;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.model.subscription.API;
import org.wso2.carbon.apimgt.api.model.subscription.APIPolicy;
import org.wso2.carbon.apimgt.api.model.subscription.Application;
import org.wso2.carbon.apimgt.api.model.subscription.ApplicationKeyMapping;
import org.wso2.carbon.apimgt.api.model.subscription.ApplicationPolicy;
import org.wso2.carbon.apimgt.api.model.subscription.Subscription;
import org.wso2.carbon.apimgt.api.model.subscription.SubscriptionPolicy;
import org.wso2.carbon.apimgt.api.model.subscription.URLMapping;
import org.wso2.carbon.apimgt.impl.dao.constants.SubscriptionValidationSQLConstants;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionValidationDAO {

    private static Log log = LogFactory.getLog(SubscriptionValidationDAO.class);

    /*
     * This method can be used to retrieve all the APIs in the database
     *
     * @return {@link List<API> List of APIs}
     * */
    public List<API> getAllApis() {

        List<API> apiList = new ArrayList<>();
        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(SubscriptionValidationSQLConstants.GET_ALL_APIS_SQL);
                ResultSet resultSet = ps.executeQuery();
        ) {
            populateAPIList(resultSet, apiList);

        } catch (SQLException e) {
            log.error("Error in loading Apis : ", e);
        }

        return apiList;
    }

    /*
     * This method can be used to retrieve all the Subscriptions in the database
     *
     * @return {@link List<Subscription>}
     * */
    public List<Subscription> getAllSubscriptions() {

        List<Subscription> subscriptions = new ArrayList<>();
        try (Connection conn = APIMgtDBUtil.getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(SubscriptionValidationSQLConstants.GET_ALL_SUBSCRIPTIONS_SQL);
             ResultSet resultSet = ps.executeQuery();) {
            populateSubscriptionsList(subscriptions, resultSet);

        } catch (SQLException e) {
            log.error("Error in loading Subscription : ", e);
        }

        return subscriptions;
    }

    /*
     * This method can be used to retrieve all the Applications in the database
     *
     * @return {@link List<Application>}
     * */
    public List<Application> getAllApplications() {

        List<Application> applications = new ArrayList<>();
        try (Connection conn = APIMgtDBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(SubscriptionValidationSQLConstants.GET_ALL_APPLICATIONS_SQL);
             ResultSet resultSet = ps.executeQuery();
        ) {
            addToApplicationList(applications, resultSet);

        } catch (SQLException e) {
            log.error("Error in loading Applications : ", e);
        }

        return applications;
    }

    private void addToApplicationList(List<Application> list, ResultSet resultSet) throws SQLException {

        if (list == null) {
            list = new ArrayList<>();
        }
        if (resultSet != null) {
            Map<Integer, Application> temp = new Hashtable<>();
            while (resultSet.next()) {
                int appId = resultSet.getInt("APP_ID");
                Application application = temp.get(appId);
                if (application == null) {
                    application = new Application();
                    application.setId(appId);
                    application.setPolicy(resultSet.getString("TIER"));
                    application.setSubName(resultSet.getString("SUB_NAME"));
                    application.setTokenType(resultSet.getString("TOKEN_TYPE"));
                    temp.put(appId, application);
                }
                // todo read from the aplication_group_mapping table and make it a set
//                application.addGroupId(resultSet.getString("GROUP_ID"));
                // todo read attributes from the aplication_attributes table and make it a map
//                String attributeName = resultSet.getString("NAME");
//                String attributeValue = resultSet.getString("VALUE");
//                application.addAttribute(attributeName, attributeValue);

                list.add(application);
            }
        }
    }

    /*
     * This method can be used to retrieve all the ApplicationKeyMappings in the database
     *
     * @return {@link List<ApplicationKeyMapping>}
     * */
    public List<ApplicationKeyMapping> getAllApplicationKeyMappings() {

        List<ApplicationKeyMapping> keyMappings = new ArrayList<>();

        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(SubscriptionValidationSQLConstants.GET_ALL_AM_KEY_MAPPINGS_SQL);
                ResultSet resultSet = ps.executeQuery();
        ) {

            populateApplicationKeyMappingsList(keyMappings, resultSet);

        } catch (SQLException e) {
            log.error("Error in loading Application Key Mappings : ", e);
        }

        return keyMappings;
    }

    /*
     * This method can be used to retrieve all the SubscriptionPolicies in the database
     *
     * @return {@link List<SubscriptionPolicy>}
     * */
    public List<SubscriptionPolicy> getAllSubscriptionPolicies() {

        List<SubscriptionPolicy> subscriptionPolicies = new ArrayList<>();
        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps =
                        conn.prepareStatement(SubscriptionValidationSQLConstants.GET_ALL_SUBSCRIPTION_POLICIES_SQL);
                ResultSet resultSet = ps.executeQuery();
        ) {
            populateSubscriptionPolicyList(subscriptionPolicies, resultSet);

        } catch (SQLException e) {
            log.error("Error in loading Subscription policies : ", e);
        }

        return subscriptionPolicies;
    }

    private void populateSubscriptionPolicyList(List<SubscriptionPolicy> subscriptionPolicies, ResultSet resultSet)
            throws SQLException {

        if (subscriptionPolicies != null && resultSet != null) {
            while (resultSet.next()) {
                SubscriptionPolicy subscriptionPolicyDTO = new SubscriptionPolicy();

                subscriptionPolicyDTO.setId(resultSet.getInt("POLICY_ID"));
                subscriptionPolicyDTO.setName(resultSet.getString("NAME"));
                subscriptionPolicyDTO.setQuotaType(resultSet.getString("QUOTA_TYPE"));
                subscriptionPolicyDTO.setTenantId(resultSet.getInt("TENANT_ID"));

                subscriptionPolicyDTO.setRateLimitCount(resultSet.getInt("RATE_LIMIT_COUNT"));
                subscriptionPolicyDTO.setRateLimitTimeUnit(resultSet.getString("RATE_LIMIT_TIME_UNIT"));
                subscriptionPolicyDTO.setStopOnQuotaReach(resultSet.getBoolean("STOP_ON_QUOTA_REACH"));

                subscriptionPolicies.add(subscriptionPolicyDTO);
            }
        }
    }

    /*
     * This method can be used to retrieve all the ApplicationPolicys in the database
     *
     * @return {@link List<ApplicationPolicy>}
     * */
    public List<ApplicationPolicy> getAllApplicationPolicies() {

        List<ApplicationPolicy> applicationPolicies = new ArrayList<>();
        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps =
                        conn.prepareStatement(SubscriptionValidationSQLConstants.GET_ALL_APPLICATION_POLICIES_SQL);
                ResultSet resultSet = ps.executeQuery();
        ) {
            populateApplicationPolicyList(applicationPolicies, resultSet);

        } catch (SQLException e) {
            log.error("Error in loading application policies : ", e);
        }

        return applicationPolicies;
    }

    /*
     * This method can be used to retrieve all the ApplicationPolicys in the database
     *
     * @return {@link List<ApplicationPolicy>}
     * */
    public List<APIPolicy> getAllApiPolicies() {

        List<APIPolicy> applicationPolicies = new ArrayList<>();
        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps =
                        conn.prepareStatement(SubscriptionValidationSQLConstants.GET_ALL_APPLICATION_POLICIES_SQL);
                ResultSet resultSet = ps.executeQuery();
        ) {
            populateApiPolicyList(applicationPolicies, resultSet);

        } catch (SQLException e) {
            log.error("Error in loading application policies : ", e);
        }

        return applicationPolicies;
    }

    /*
     * This method can be used to retrieve all the APIs of a given tenant in the database
     *
     * @param tenantId : unique identifier of tenant
     * @return {@link List<API>}
     * */
    public List<API> getAllApis(String tenantDomain) {

        List<API> apiList = new ArrayList<>();
        try (
                Connection conn = APIMgtDBUtil.getConnection();) {
            PreparedStatement ps;
            if (tenantDomain.equalsIgnoreCase(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
                ps = conn.prepareStatement(SubscriptionValidationSQLConstants.GET_ST_APIS_SQL);
                ps.setString(1, "/t%");
            } else {
                ps = conn.prepareStatement(SubscriptionValidationSQLConstants.GET_TENANT_APIS_SQL);
                ps.setString(1, "/t/" + tenantDomain + "%");
            }
            ResultSet resultSet = ps.executeQuery();
            populateAPIList(resultSet, apiList);

        } catch (SQLException e) {
            log.error("Error in loading Apis for tenantId : " + tenantDomain, e);
        }

        return apiList;
    }

    /*
     * This method can be used to retrieve an API in the database
     *
     * @param apiId : unique identifier of an API
     * @return {@link API}
     * */
    public API getApi(String version, String context) {

        API api = null;
        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(SubscriptionValidationSQLConstants.GET_API_SQL);) {
            ps.setString(1, version);
            ps.setString(2, context);
            ResultSet resultSet = ps.executeQuery();
            Map<Integer, API> temp = new ConcurrentHashMap<>();
            while (resultSet.next()) {
                int apiId = resultSet.getInt("API_ID");
                api = temp.get(apiId);
                if (api == null) {
                    api = new API();
                    api.setApiId(apiId);
                    api.setProvider(resultSet.getString("API_PROVIDER"));
                    api.setName(resultSet.getString("API_NAME"));
                    api.setPolicy(resultSet.getString("API_TIER"));
                    api.setVersion(resultSet.getString("API_VERSION"));
                    api.setContext(resultSet.getString("CONTEXT"));
                    temp.put(apiId, api);
                }

                URLMapping urlMapping = new URLMapping();
                urlMapping.setThrottlingPolicy(resultSet.getString("RES_TIER"));
                urlMapping.setAuthScheme(resultSet.getString("AUTH_SCHEME"));
                urlMapping.setHttpMethod(resultSet.getString("HTTP_METHOD"));
                api.addResource(urlMapping);

            }

        } catch (SQLException e) {
            log.error("Error in loading API for api : " + context + " : " + version, e);
        }

        return api;
    }

    private void populateAPIList(ResultSet resultSet, List<API> apiList) throws SQLException {

        Map<Integer, API> temp = new ConcurrentHashMap<>();
        while (resultSet.next()) {
            int apiId = resultSet.getInt("API_ID");
            API api = temp.get(apiId);
            if (api == null) {
                api = new API();
                api.setApiId(apiId);
                api.setProvider(resultSet.getString("API_PROVIDER"));
                api.setName(resultSet.getString("API_NAME"));
                api.setPolicy(resultSet.getString("API_TIER"));
                api.setVersion(resultSet.getString("API_VERSION"));
                api.setContext(resultSet.getString("CONTEXT"));
                temp.put(apiId, api);
                apiList.add(api);
            }
            URLMapping urlMapping = new URLMapping();
            urlMapping.setHttpMethod(resultSet.getString("HTTP_METHOD"));
            urlMapping.setAuthScheme(resultSet.getString("AUTH_SCHEME"));
            urlMapping.setThrottlingPolicy(resultSet.getString("THROTTLING_TIER"));
            urlMapping.setUrlPattern(resultSet.getString("URL_PATTERN"));
            api.addResource(urlMapping);
        }
    }

    /*
     * This method can be used to retrieve all the APIs of a given tesanat in the database
     *
     * @param subscriptionId : unique identifier of a subscription
     * @return {@link List<Subscription>}
     * */
    public List<Subscription> getAllSubscriptions(String tenantDomain) {

        List<Subscription> subscriptions = new ArrayList<>();
        try (Connection conn = APIMgtDBUtil.getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(SubscriptionValidationSQLConstants.GET_TENANT_SUBSCRIPTIONS_SQL);
        ) {
            int tenantId = 0;
            try {
                tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(tenantDomain);
            } catch (UserStoreException e) {
                log.error("Error in getting tenant id for loading Subscriptions for tenant : " + tenantDomain, e);
            }
            ps.setInt(1, tenantId);

            try (ResultSet resultSet = ps.executeQuery();) {
                populateSubscriptionsList(subscriptions, resultSet);
            }
        } catch (SQLException e) {
            log.error("Error in loading Subscriptions for tenantId : " + tenantDomain, e);
        }
        return subscriptions;
    }

    private void populateSubscriptionsList(List<Subscription> subscriptions, ResultSet resultSet) throws SQLException {

        if (resultSet != null && subscriptions != null) {
            while (resultSet.next()) {
                Subscription subscription = new Subscription();
                subscription.setSubscriptionId(resultSet.getInt("SUB_ID"));
                subscription.setPolicyId(resultSet.getString("TIER"));
                subscription.setApiId(resultSet.getInt("API_ID"));
                subscription.setAppId(resultSet.getInt("APP_ID"));
                subscription.setSubscriptionState(resultSet.getString("STATUS"));
                subscriptions.add(subscription);
            }
        }
    }

    /*
     * This method can be used to retrieve all the Applications of a given tenant in the database
     * @param tenantId : tenant Id
     * @return {@link Subscription}
     * */
    public List<Application> getAllApplications(String tenantDomain) {

        ArrayList<Application> applications = new ArrayList<>();
        try (Connection conn = APIMgtDBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(SubscriptionValidationSQLConstants.GET_TENANT_APPLICATIONS_SQL);
        ) {
            try {
                int tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(tenantDomain);
                ps.setInt(1, tenantId);
                ResultSet resultSet = ps.executeQuery();
                addToApplicationList(applications, resultSet);
            } catch (UserStoreException e) {
                log.error("Error in getting tenant id for loading Applications for tenant : " + tenantDomain, e);
            }

        } catch (SQLException e) {
            log.error("Error in loading Applications for tenantDomain : " + tenantDomain, e);
        }

        return applications;
    }

    /*
     * @param subscriptionId : unique identifier of a subscription
     * @return {@link Subscription}
     * */
    public List<ApplicationKeyMapping> getAllApplicationKeyMappings(String tenantDomain) {

        List<ApplicationKeyMapping> keyMappings = new ArrayList<>();

        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps =
                        conn.prepareStatement(SubscriptionValidationSQLConstants.GET_TENANT_AM_KEY_MAPPING_SQL);) {
            int tenantId = 0;
            try {
                tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(tenantDomain);
            } catch (UserStoreException e) {
                log.error("Error in loading ApplicationKeyMappings for tenantDomain : " + tenantDomain, e);
            }
            ps.setInt(1, tenantId);
            ResultSet resultSet = ps.executeQuery();
            populateApplicationKeyMappingsList(keyMappings, resultSet);
        } catch (SQLException e) {
            log.error("Error in loading Application key mappings for tenantId : " + tenantDomain, e);
        }

        return keyMappings;
    }

    private void populateApplicationKeyMappingsList(List<ApplicationKeyMapping> keyMappings, ResultSet resultSet)
            throws SQLException {

        if (keyMappings != null && resultSet != null) {

            while (resultSet.next()) {
                ApplicationKeyMapping keyMapping = new ApplicationKeyMapping();
                keyMapping.setApplicationId(resultSet.getInt("APPLICATION_ID"));
                keyMapping.setConsumerKey(resultSet.getString("CONSUMER_KEY"));
                keyMapping.setKeyType(resultSet.getString("KEY_TYPE"));
                keyMappings.add(keyMapping);
            }

        }
    }

    /*
     * @param subscriptionId : unique identifier of a subscription
     * @return {@link Subscription}
     * */
    public List<SubscriptionPolicy> getAllSubscriptionPolicies(String tenantDomain) {

        ArrayList<SubscriptionPolicy> subscriptionPolicies = new ArrayList<>();
        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps =
                        conn.prepareStatement(SubscriptionValidationSQLConstants.GET_TENANT_SUBSCRIPTION_POLICIES_SQL);
        ) {
            int tenantId = 0;
            try {
                tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(tenantDomain);
            } catch (UserStoreException e) {
                log.error("Error in loading SubscriptionPolicies for tenantDomain : " + tenantDomain, e);
            }
            ps.setInt(1, tenantId);
            ResultSet resultSet = ps.executeQuery();
            populateSubscriptionPolicyList(subscriptionPolicies, resultSet);

        } catch (SQLException e) {
            log.error("Error in loading Subscription Policies for tenanatId : " + tenantDomain, e);
        }

        return subscriptionPolicies;
    }

    /*
     * @param tenantDomain : tenant domain name
     * @return {@link List<ApplicationPolicy>}
     * */
    public List<ApplicationPolicy> getAllApplicationPolicies(String tenantDomain) {

        ArrayList<ApplicationPolicy> applicationPolicies = new ArrayList<>();
        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps =
                        conn.prepareStatement(SubscriptionValidationSQLConstants.GET_TENANT_APPLICATION_POLICIES_SQL);) {
            int tenantId = 0;
            try {
                tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(tenantDomain);
            } catch (UserStoreException e) {
                log.error("Error in loading ApplicationPolicies for tenantDomain : " + tenantDomain, e);
            }
            ps.setInt(1, tenantId);
            ResultSet resultSet = ps.executeQuery();

            populateApplicationPolicyList(applicationPolicies, resultSet);

        } catch (SQLException e) {
            log.error("Error in loading application policies for tenantId : " + tenantDomain, e);
        }

        return applicationPolicies;
    }

    private void populateApplicationPolicyList(List<ApplicationPolicy> applicationPolicies, ResultSet resultSet)
            throws SQLException {

        if (applicationPolicies != null && resultSet != null) {
            while (resultSet.next()) {
                ApplicationPolicy applicationPolicyDTO = new ApplicationPolicy();
                applicationPolicyDTO.setId(resultSet.getInt("POLICY_ID"));
                applicationPolicyDTO.setName(resultSet.getString("NAME"));
                applicationPolicyDTO.setQuotaType(resultSet.getString("QUOTA_TYPE"));
                applicationPolicyDTO.setTenantId(resultSet.getInt("TENANT_ID"));
                applicationPolicies.add(applicationPolicyDTO);
            }
        }
    }

    /*
     * @param tenantDomain : tenant domain name
     * @return {@link List<APIPolicy>}
     * */
    public List<APIPolicy> getAllApiPolicies(String tenantDomain) {

        ArrayList<APIPolicy> apiPolicies = new ArrayList<>();
        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps =
                        conn.prepareStatement(SubscriptionValidationSQLConstants.GET_TENANT_API_POLICIES_SQL);) {
            int tenantId = 0;
            try {
                tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(tenantDomain);
            } catch (UserStoreException e) {
                log.error("Error in loading ApplicationPolicies for tenantDomain : " + tenantDomain, e);
            }
            ps.setInt(1, tenantId);
            ResultSet resultSet = ps.executeQuery();

            populateApiPolicyList(apiPolicies, resultSet);

        } catch (SQLException e) {
            log.error("Error in loading application policies for tenantId : " + tenantDomain, e);
        }

        return apiPolicies;
    }

    private void populateApiPolicyList(List<APIPolicy> apiPolicies, ResultSet resultSet)
            throws SQLException {

        if (apiPolicies != null && resultSet != null) {
            while (resultSet.next()) {
                APIPolicy apiPolicyDTO = new APIPolicy();
                apiPolicyDTO.setId(resultSet.getInt("POLICY_ID"));
                apiPolicyDTO.setName(resultSet.getString("NAME"));
                apiPolicyDTO.setQuotaType(resultSet.getString("QUOTA_TYPE"));
                apiPolicyDTO.setTenantId(resultSet.getInt("TENANT_ID"));
                apiPolicies.add(apiPolicyDTO);
            }
        }
    }

    /*
     * @param subscriptionId : unique identifier of a subscription
     * @return {@link Subscription}
     * */
    public Subscription getSubscription(int apiId, int appId) {

        try (Connection conn = APIMgtDBUtil.getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(SubscriptionValidationSQLConstants.GET_SUBSCRIPTION_SQL);
        ) {
            ps.setInt(1, apiId);
            ps.setInt(2, appId);

            try (ResultSet resultSet = ps.executeQuery();) {

                if (resultSet.next()) {
                    Subscription subscription = new Subscription();

                    subscription.setSubscriptionId(resultSet.getInt("SUB_ID"));
                    subscription.setPolicyId(resultSet.getString("TIER"));
                    subscription.setApiId(resultSet.getInt("API_ID"));
                    subscription.setAppId(resultSet.getInt("APP_ID"));
                    subscription.setSubscriptionState(resultSet.getString("STATUS"));
                    return subscription;
                }

            }
        } catch (SQLException e) {
            log.error("Error in loading Subscription by apiId : " + apiId + " appId: " + appId, e);
        }
        return null;
    }

    /*
     * @param applicationId : unique identifier of an application
     * @return {@link List<Application>} a list with one element
     * */
    public List<Application> getApplicationById(int applicationId) {

        List<Application> applicationList = new ArrayList<>();

        try (Connection conn = APIMgtDBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(SubscriptionValidationSQLConstants.GET_APPLICATION_BY_ID_SQL);
        ) {
            ps.setInt(1, applicationId);
            ResultSet resultSet = ps.executeQuery();
            addToApplicationList(applicationList, resultSet);

        } catch (SQLException e) {
            log.error("Error in loading Application by applicationId : " + applicationId, e);
        }
        return applicationList;
    }

    /*
     * @param policyName : name of an application level throttling policy
     * @return {@link ApplicationPolicy}
     * */
    public ApplicationPolicy getApplicationPolicyByNameForTenant(String policyName, String tenantDomain) {

        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps =
                        conn.prepareStatement(SubscriptionValidationSQLConstants.GET_APPLICATION_POLICY_SQL);
        ) {
            int tenantId = 0;
            try {
                tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(tenantDomain);
            } catch (UserStoreException e) {
                log.error("Error in loading ApplicationPolicy for tenantDomain : " + tenantDomain, e);
            }
            ps.setString(1, policyName);
            ps.setInt(2, tenantId);
            ResultSet resultSet = ps.executeQuery();

            if (resultSet.next()) {
                ApplicationPolicy applicationPolicy = new ApplicationPolicy();

                applicationPolicy.setId(resultSet.getInt("POLICY_ID"));
                applicationPolicy.setName(resultSet.getString("NAME"));
                applicationPolicy.setQuotaType(resultSet.getString("QUOTA_TYPE"));
                applicationPolicy.setTenantId(resultSet.getInt("TENANT_ID"));

                return applicationPolicy;
            }

        } catch (SQLException e) {
            log.error("Error in loading application policies by policyId : " + policyName + " of " + policyName, e);
        }

        return null;
    }

    /*
     * @param policyName : name of an application level throttling policy
     * @return {@link ApplicationPolicy}
     * */
    public APIPolicy getApiPolicyByNameForTenant(String policyName, String tenantDomain) {

        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps =
                        conn.prepareStatement(SubscriptionValidationSQLConstants.GET_API_POLICY_SQL);
        ) {
            int tenantId = 0;
            try {
                tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                        .getTenantId(tenantDomain);
            } catch (UserStoreException e) {
                log.error("Error in loading ApplicationPolicy for tenantDomain : " + tenantDomain, e);
            }
        //todo

        } catch (SQLException e) {
            log.error("Error in loading application policies by policyId : " + policyName + " of " + policyName, e);
        }

        return null;
    }

    /*
     * @param policyName : name of the subscription level throttling policy
     * @return {@link SubscriptionPolicy}
     * */
    public SubscriptionPolicy getSubscriptionPolicyByNameForTenant(String policyName, String tenantDomain) {

        if (StringUtils.isNotEmpty(policyName) && StringUtils.isNotEmpty(tenantDomain)) {
            try (
                    Connection conn = APIMgtDBUtil.getConnection();
                    PreparedStatement ps =
                            conn.prepareStatement(SubscriptionValidationSQLConstants.GET_SUBSCRIPTION_POLICY_SQL);
            ) {
                int tenantId = 0;
                try {
                    tenantId = ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                            .getTenantId(tenantDomain);
                } catch (UserStoreException e) {
                    log.error("Error in loading ApplicationPolicy for tenantDomain : " + tenantDomain, e);
                }
                ps.setString(1, policyName);
                ps.setInt(2, tenantId);
                ResultSet resultSet = ps.executeQuery();

                if (resultSet.next()) {
                    SubscriptionPolicy subscriptionPolicy = new SubscriptionPolicy();

                    subscriptionPolicy.setId(resultSet.getInt("POLICY_ID"));
                    subscriptionPolicy.setName(resultSet.getString("NAME"));
                    subscriptionPolicy.setQuotaType(resultSet.getString("QUOTA_TYPE"));
                    subscriptionPolicy.setTenantId(resultSet.getInt("TENANT_ID"));

                    subscriptionPolicy.setRateLimitCount(resultSet.getInt("RATE_LIMIT_COUNT"));
                    subscriptionPolicy.setRateLimitTimeUnit(resultSet.getString("RATE_LIMIT_TIME_UNIT"));
                    subscriptionPolicy.setStopOnQuotaReach(resultSet.getBoolean("STOP_ON_QUOTA_REACH"));
                    return subscriptionPolicy;
                }

            } catch (SQLException e) {
                log.error("Error in retrieving Subscription policy by id : " + policyName + " for " + tenantDomain, e);
            }
        }
        return null;
    }

    /*
     * @param appId : ApplicationId
     * @param keyType : Type of the key ex: PRODUCTION
     * @return {@link ApplicationKeyMapping}
     *
     * */
    public ApplicationKeyMapping getApplicationKeyMapping(int appId, String keyType) {

        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(SubscriptionValidationSQLConstants.GET_AM_KEY_MAPPING_SQL);
        ) {
            ps.setInt(1, appId);
            ps.setString(2, keyType);
            ResultSet resultSet = ps.executeQuery();

            while (resultSet.next()) {
                ApplicationKeyMapping keyMapping = new ApplicationKeyMapping();
                keyMapping.setApplicationId(resultSet.getInt("APPLICATION_ID"));
                keyMapping.setConsumerKey(resultSet.getString("CONSUMER_KEY"));
                keyMapping.setKeyType(resultSet.getString("KEY_TYPE"));
                return keyMapping;
            }

        } catch (SQLException e) {
            log.error("Error in loading  Application Key Mapping for appId : " + appId + " type : " + keyType, e);
        }
        return null;
    }

    /*
     * @param consumerKey : consumer key of an application
     * @return {@link ApplicationKeyMapping}
     *
     * */
    public ApplicationKeyMapping getApplicationKeyMapping(String consumerKey) {

        try (
                Connection conn = APIMgtDBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        SubscriptionValidationSQLConstants.GET_AM_KEY_MAPPING_BY_CONSUMER_KEY_SQL);
        ) {
            ps.setString(1, consumerKey);
            ResultSet resultSet = ps.executeQuery();

            while (resultSet.next()) {
                ApplicationKeyMapping keyMapping = new ApplicationKeyMapping();
                keyMapping.setApplicationId(resultSet.getInt("APPLICATION_ID"));
                keyMapping.setConsumerKey(resultSet.getString("CONSUMER_KEY"));
                keyMapping.setKeyType(resultSet.getString("KEY_TYPE"));
                return keyMapping;
            }

        } catch (SQLException e) {
            log.error("Error in loading Application Key Mappinghsacfrgtghf54trtjkl;{786754w `13457868789[-876re7w4wertyi875 for consumer key : " + consumerKey, e);
        }
        return null;
    }

    /*
     * This method can be used to retrieve all the URLMappings in the database
     *
     * @return {@link List<URLMapping>}
     * */
    public List<URLMapping> getAllURLMappings() {

        List<URLMapping> urlMappings = new ArrayList<>();
        String sql = SubscriptionValidationSQLConstants.GET_ALL_API_URL_MAPPING_SQL;

        try (Connection conn = APIMgtDBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {
            ResultSet resultSet = ps.executeQuery();

            while (resultSet.next()) {
                URLMapping urlMapping = new URLMapping();
                urlMapping.setAuthScheme(resultSet.getString("AUTH_SCHEME"));
                urlMapping.setHttpMethod(resultSet.getString("HTTP_METHOD"));
                urlMapping.setThrottlingPolicy(resultSet.getString("POLICY"));
                urlMappings.add(urlMapping);
            }
        } catch (SQLException e) {
            log.error("Error in loading URLMappings : ", e);
        }

        return urlMappings;
    }

    /*
     * This method can be used to retrieve all the URLMappings of a given tenant in the database
     *
     * @param tenantId : tenant Id
     * @return {@link List<URLMapping>}
     * */
    public List<URLMapping> getAllURLMappings(int tenantId) {

        List<URLMapping> urlMappings = new ArrayList<>();
        String sql = SubscriptionValidationSQLConstants.GET_TENANT_API_URL_MAPPING_SQL;
        String tenantDomain = APIUtil.getTenantDomainFromTenantId(tenantId);
        String contextParam = null;
        if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
            sql = SubscriptionValidationSQLConstants.GET_ST_API_URL_MAPPING_SQL;
            contextParam = "%/t/%";
        } else if (tenantId > 0) {
            contextParam = "%" + tenantDomain + "%";
        } else {
            sql = SubscriptionValidationSQLConstants.GET_ALL_API_URL_MAPPING_SQL;
        }
        try (Connection conn = APIMgtDBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
        ) {
            if (contextParam != null) {
                ps.setString(1, contextParam);
            }
            ResultSet resultSet = ps.executeQuery();

            while (resultSet.next()) {
                URLMapping urlMapping = new URLMapping();
                urlMapping.setAuthScheme(resultSet.getString("AUTH_SCHEME"));
                urlMapping.setHttpMethod(resultSet.getString("HTTP_METHOD"));
                urlMapping.setThrottlingPolicy(resultSet.getString("POLICY"));
                urlMappings.add(urlMapping);
            }
        } catch (SQLException e) {
            log.error("Error in loading URLMappings for tenantId : " + tenantId, e);
        }

        return urlMappings;
    }

}
