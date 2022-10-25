package com.nextlabs.rms.cc.service;

import com.google.common.collect.Lists;
import com.google.gson.JsonParseException;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.PolicyModelType;
import com.nextlabs.common.shared.JsonClassificationCategory;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.abac.MembershipPoliciesEvaluationHandler;
import com.nextlabs.rms.cc.exception.ControlCentreNotSupportedException;
import com.nextlabs.rms.cc.exception.DelegationPolicyException;
import com.nextlabs.rms.cc.factory.ControlCenterTagServiceFactory;
import com.nextlabs.rms.cc.pojos.ControlCenterAttribute;
import com.nextlabs.rms.cc.pojos.ControlCenterComponent;
import com.nextlabs.rms.cc.pojos.ControlCenterComponentRequest;
import com.nextlabs.rms.cc.pojos.ControlCenterComponentRequest.ControlCenterIdWrapper;
import com.nextlabs.rms.cc.pojos.ControlCenterCondition;
import com.nextlabs.rms.cc.pojos.ControlCenterObligation;
import com.nextlabs.rms.cc.pojos.ControlCenterOperator;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicy;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicyModel;
import com.nextlabs.rms.cc.pojos.ControlCenterTag;
import com.nextlabs.rms.cc.pojos.ControlCenterVersion;
import com.nextlabs.rms.cc.pojos.JsonPolicy;
import com.nextlabs.rms.cc.pojos.JsonResourceComponent;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterResponse;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterRestClientException;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterServiceException;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.rs.AbstractLogin;
import com.nextlabs.rms.rs.ClassificationMgmt;
import com.nextlabs.rms.shared.LogConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public final class ControlCenterManager {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static ControlCenterVersion ccVersion;
    private static ControlCenterOperator[] ops;

    static {
        try {
            ControlCenterRestClient rsClient = new ControlCenterRestClient();
            ControlCenterInfoService infoService = new ControlCenterInfoService(rsClient);
            setControlCenterVersion(infoService.getControlCenterVersion());
            ControlCenterOperatorService operatorService = new ControlCenterOperatorService(rsClient);
            List<ControlCenterOperator> operatorList = new ArrayList<>();
            HashMap<String, String> hmapOp = new HashMap<>();
            hmapOp.put("=", "STRING");
            hmapOp.put("!=", "STRING");
            hmapOp.put("<", "NUMBER");
            hmapOp.put("<=", "NUMBER");
            hmapOp.put(">", "NUMBER");
            hmapOp.put(">=", "NUMBER");
            ControlCenterOperator[] allOps = operatorService.getAllOperators();
            for (ControlCenterOperator op : allOps) {
                if (hmapOp.containsKey(op.getKey()) && hmapOp.get(op.getKey()).equalsIgnoreCase(op.getDataType())) {
                    ControlCenterOperator operator = new ControlCenterOperator();
                    operator.setDataType("STRING");
                    operator.setKey(op.getKey());
                    operator.setLabel(op.getLabel());
                    operator.setId(op.getId());
                    operatorList.add(operator);
                }
            }
            ops = new ControlCenterOperator[operatorList.size()];
            for (int i = 0; i < operatorList.size(); i++) {
                ops[i] = operatorList.get(i);
            }
        } catch (ControlCenterServiceException | ControlCenterRestClientException e) {
            LOGGER.error("Error occurred while getting equal operator: {}", e.getMessage(), e);
        }
    }

    private ControlCenterManager() {

    }

    public static void createDefaultTenantPolicyModel(
        Constants.TokenGroupType tokenGroupType) {
        String defaultTenant = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", defaultTenant));
            Tenant tenant = (Tenant)criteria.uniqueResult();
            try {
                LOGGER.info("Creating defaultTenant policy model.");
                updateResourceModel(tenant.getName(), ClassificationMgmt.getTenantClassification(session, tenant.getName()), tokenGroupType);
            } catch (Exception e) {
                LOGGER.error("Error occurred while creating defaultTenant policy model", e);
            } finally {
                try {
                    LOGGER.info("Creating defaultTenant abac policy model.");
                    updateABACPolicyModel(tenant);
                } catch (ControlCenterServiceException | ControlCenterRestClientException e) {
                    LOGGER.error("Error occurred while creating defaultTenant abac policy model", e);
                } finally {
                    LOGGER.info("Creation of default tenant policy model completed");
                }
            }
        }
    }

    public static void updateABACPolicyModel(Tenant tenant)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        if (!StringUtils.hasText(WebConfig.getInstance().getProperty(WebConfig.CC_CONSOLE_URL))) {
            return;
        }
        Tenant virtualABACTenant = new Tenant();
        virtualABACTenant.setId(tenant.getId() + com.nextlabs.rms.Constants.MEMBERSHIP_MODEL_SUFFIX);
        virtualABACTenant.setName(tenant.getName() + com.nextlabs.rms.Constants.MEMBERSHIP_MODEL_SUFFIX);

        ControlCenterRestClient rsClient;
        try {
            rsClient = new ControlCenterRestClient(virtualABACTenant.getName());
        } catch (ControlCenterRestClientException e) {
            if (e.getCode() == 400 || e.getCode() == 401) {
                rsClient = create(virtualABACTenant.getName(), PolicyModelType.MEMBERSHIP);
            } else {
                LOGGER.error("Error occurred while initializing ControlCenterRestClient: {}", e.getMessage(), e);
                throw e;
            }
        } catch (ControlCenterServiceException e) {
            LOGGER.error("Error occurred while initializing ControlCenterRestClient: {}", e.getMessage(), e);
            throw e;
        }

        if (rsClient != null) {
            try {
                ControlCenterModelService modelService = new ControlCenterModelService(rsClient);
                ControlCenterPolicyModel abacModel = modelService.getResourceModel(virtualABACTenant.getName());
                List<ControlCenterObligation> obligationList = new ArrayList<>();
                obligationList.add(MembershipPoliciesEvaluationHandler.getABACPolicyModelObligation());
                if (abacModel != null) {
                    abacModel.setObligations(obligationList);
                    // this is to fix upgrade issues with the introduction of ACCESS_PROJECT action
                    abacModel.getActions().removeIf(action -> action.getName().equals(Rights.VIEW.name()));
                }
                String defaultTenantName = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT);
                modelService.updateResourcePolicyModel(abacModel, Lists.newArrayList(toControlCenterAttribute("tenantId")), PolicyModelType.MEMBERSHIP, tenant.getName().equals(defaultTenantName));
            } catch (ControlCenterRestClientException | ControlCenterServiceException e) {
                LOGGER.error("Error occurred while updating model: {}", e.getMessage(), e);
                throw e;
            }
        }
    }

    public static void updateResourceModel(String tokenGroupName, List<JsonClassificationCategory> categories,
        Constants.TokenGroupType tokenGroupType)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        if (!StringUtils.hasText(WebConfig.getInstance().getProperty(WebConfig.CC_CONSOLE_URL))) {
            return;
        }

        ControlCenterRestClient rsClient;
        try {
            rsClient = new ControlCenterRestClient(tokenGroupName);
        } catch (ControlCenterRestClientException e) {
            if (e.getCode() == 400 || e.getCode() == 401) {
                rsClient = create(tokenGroupName, PolicyModelType.RESOURCE);
            } else {
                LOGGER.error("Error occurred while initializing ControlCenterRestClient: {}", e.getMessage(), e);
                throw e;
            }
        } catch (ControlCenterServiceException e) {
            LOGGER.error("Error occurred while initializing ControlCenterRestClient: {}", e.getMessage(), e);
            throw e;
        }

        if (rsClient != null) {
            try {
                ControlCenterModelService modelService = new ControlCenterModelService(rsClient);
                ControlCenterPolicyModel rmsModel = modelService.getResourceModel(tokenGroupName);
                if (!tokenGroupType.equals(Constants.TokenGroupType.TOKENGROUP_SYSTEMBUCKET)) {
                    List<ControlCenterObligation> obligationList = new ArrayList<>();
                    ControlCenterObligationService obligationService = new ControlCenterObligationService();
                    obligationList.add(obligationService.getWatermarkPolicyModelObligation());
                    if (rmsModel != null) {
                        rmsModel.setObligations(obligationList);
                    }
                }
                String defaultTenantName = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT);
                modelService.updateResourcePolicyModel(rmsModel, toControlCenterAttributeList(categories), PolicyModelType.RESOURCE, tokenGroupName.equals(defaultTenantName));
            } catch (ControlCenterRestClientException | ControlCenterServiceException e) {
                LOGGER.error("Error occurred while updating model: {}", e.getMessage(), e);
                throw e;
            }
        }
    }

    public static ControlCenterRestClient create(String tokenGroupName, PolicyModelType policyModelType) {
        ControlCenterRestClient rsClient = null;
        try {
            rsClient = new ControlCenterRestClient();
            ControlCenterUserService userService = new ControlCenterUserService(rsClient);
            String userId = userService.createUser(tokenGroupName);
            if (userId != null) {
                userService.activateUser(tokenGroupName);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created user {} for: {}", userId, tokenGroupName);
                    LOGGER.debug("Activated user {} for: {}", userId, tokenGroupName);
                }
            }

            ControlCenterTagService tagService = ControlCenterTagServiceFactory.createControlCenterTagService(rsClient);
            if (tagService.get(tokenGroupName) == null) {
                tagService.createPolicyTag(tokenGroupName);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created tag for: {}", tokenGroupName);
                }
            }
            rsClient = new ControlCenterRestClient(tokenGroupName);
            ControlCenterModelService modelService = new ControlCenterModelService(rsClient);

            List<ControlCenterAttribute> attributes = new ArrayList<>();
            ControlCenterPolicyModel model = modelService.createResourcePolicyModel(tokenGroupName, attributes, policyModelType);
            ControlCenterComponentService componentService = new ControlCenterComponentService(rsClient);
            if (policyModelType.equals(PolicyModelType.RESOURCE)) {
                String defaultTenantName = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT);
                if (tokenGroupName.equals(defaultTenantName)) {
                    componentService.createActionComponents(model, CentralPoliciesEvaluationHandler.getEvaluatedRights());
                } else {
                    componentService.createActionComponents(model, Lists.newArrayList(Rights.VIEW.name(), Rights.PRINT.name(), Rights.SHARE.name(), Rights.DOWNLOAD.name(), Rights.EDIT.name(), Rights.DECRYPT.name()));
                }
            } else if (policyModelType.equals(PolicyModelType.MEMBERSHIP)) {
                componentService.createActionComponent(model, Rights.ACCESS_PROJECT.name());
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Finished creating tenant.");
            }
        } catch (ControlCenterRestClientException | ControlCenterServiceException e) {
            LOGGER.error("Error occurred while creating tenant: {}", e.getMessage(), e);
        }
        return rsClient;
    }

    private static ControlCenterComponent[] getActionComponents(String tokenGroupName, boolean membershipPolicy)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        Tenant defaultTenant = AbstractLogin.getDefaultTenant();
        ControlCenterRestClient rsClient = new ControlCenterRestClient(membershipPolicy ? defaultTenant.getName() + com.nextlabs.rms.Constants.MEMBERSHIP_MODEL_SUFFIX : defaultTenant.getName());
        ControlCenterComponentService componentService = new ControlCenterComponentService(rsClient);
        String tgName;
        if (membershipPolicy) {
            tgName = defaultTenant.getName() + com.nextlabs.rms.Constants.MEMBERSHIP_MODEL_SUFFIX;
        } else if (tokenGroupName.equals(defaultTenant.getName())) {
            tgName = defaultTenant.getName();
        } else {
            // project tg
            tgName = tokenGroupName;
        }
        return componentService.getActionComponents(tgName);
    }

    public static ControlCenterResponse createPolicy(String tokenGroupName, JsonPolicy jsonPolicy,
        String parsedAdvancedConditions, boolean membershipPolicy)
            throws ControlCenterRestClientException, ControlCenterServiceException, ControlCentreNotSupportedException {
        List<ControlCenterIdWrapper> subjectComponentIds = new ArrayList<>();
        List<ControlCenterIdWrapper> actionComponentIds = new ArrayList<>();

        ControlCenterRestClient rsClient = new ControlCenterRestClient(tokenGroupName);
        ControlCenterComponentService componentService = new ControlCenterComponentService(rsClient);
        ControlCenterTagService tagService = ControlCenterTagServiceFactory.createControlCenterTagService(rsClient);
        if (tagService.getSkyDRMTag() == null) {
            tagService.createSkyDRMTag();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Created SkyDRM tag");
            }
        }
        ControlCenterTag[] tags = new ControlCenterTag[1];
        tags[0] = tagService.getSkyDRMTag();

        ControlCenterPolicyModel membershipModel = null;

        if (membershipPolicy) {
            ControlCenterModelService modelService = new ControlCenterModelService(rsClient);
            membershipModel = modelService.getResourceModel(tokenGroupName);
            updateRequestForMembershipPolicy(tokenGroupName, jsonPolicy, membershipModel);
        }

        for (JsonResourceComponent resource : jsonPolicy.getResources()) {
            for (ControlCenterComponent ccResource : resource.getComponents()) {
                if (ccResource.getId() != null || ccResource.getVersion() != null) {
                    throw new JsonParseException("Resource Component should be updated not created");
                }
                ccResource.setName(jsonPolicy.getName() + "_" + UUID.randomUUID());
                ccResource.setTags(tags);
                ccResource.setId(componentService.createComponent(ccResource));
            }
        }

        tags = new ControlCenterTag[2];
        tags[0] = tagService.get(tokenGroupName);
        tags[1] = tagService.getSkyDRMTag();

        ControlCenterComponent[] actionComponents = getActionComponents(tokenGroupName, membershipPolicy);
        for (String action : jsonPolicy.getActions()) {
            for (ControlCenterComponent component : actionComponents) {
                if (component.getName().equals(action)) {
                    actionComponentIds.add(new ControlCenterIdWrapper(component.getId()));
                    break;
                }
            }
        }

        ControlCenterPolicyService policyService = new ControlCenterPolicyService(rsClient);
        if (jsonPolicy.getToDeploy()) {
            return policyService.saveAndDeploy(tokenGroupName, jsonPolicy, parsedAdvancedConditions, tags, subjectComponentIds, actionComponentIds, membershipModel);
        } else {
            return policyService.save(tokenGroupName, jsonPolicy, parsedAdvancedConditions, tags, subjectComponentIds, actionComponentIds, membershipModel);
        }
    }

    private static void updateRequestForMembershipPolicy(String tokenGroupName, JsonPolicy jsonPolicy,
        ControlCenterPolicyModel membershipModel)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        jsonPolicy.setActions(new String[] { Rights.ACCESS_PROJECT.name() });
        ControlCenterComponent ccResource = new ControlCenterComponent();
        ccResource.setType("RESOURCE");
        ccResource.setPolicyModel(membershipModel);
        ccResource.setName(tokenGroupName);
        ccResource.setConditions(new ControlCenterCondition[] {
            new ControlCenterCondition("tenantId", "=", WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT)) });
        JsonResourceComponent jsonResourceComponent = new JsonResourceComponent();
        jsonResourceComponent.setComponents(new ControlCenterComponent[] { ccResource });
        jsonResourceComponent.setOperator("IN");
        jsonPolicy.setResources(new JsonResourceComponent[] { jsonResourceComponent });
    }

    public static ControlCenterResponse updatePolicy(String tokenGroupName, JsonPolicy jsonPolicy,
        String parsedAdvancedConditions, boolean membershipPolicy)
            throws ControlCenterRestClientException, ControlCenterServiceException, ControlCentreNotSupportedException {
        List<ControlCenterIdWrapper> subjectComponentIds = new ArrayList<>();
        List<ControlCenterIdWrapper> actionComponentIds = new ArrayList<>();

        ControlCenterRestClient rsClient = new ControlCenterRestClient(tokenGroupName);
        ControlCenterComponentService componentService = new ControlCenterComponentService(rsClient);
        ControlCenterPolicyService policyService = new ControlCenterPolicyService(rsClient);
        ControlCenterPolicy currentPolicy = policyService.get(jsonPolicy.getId());

        Set<Long> existingPolicyResourceComponents = new HashSet<>();
        for (ControlCenterComponentRequest controlCenterComponentRequest : currentPolicy.getFromResourceComponents()) {
            List<ControlCenterIdWrapper> ccComponents = controlCenterComponentRequest.getComponents();
            for (ControlCenterIdWrapper controlCenterIdWrapper : ccComponents) {
                existingPolicyResourceComponents.add(controlCenterIdWrapper.getId());
            }
        }

        ControlCenterTagService tagService = ControlCenterTagServiceFactory.createControlCenterTagService(rsClient);
        ControlCenterTag[] tags = new ControlCenterTag[1];
        if (tagService.getSkyDRMTag() == null) {
            tagService.createSkyDRMTag();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Created SkyDRM tag");
            }
        }
        tags[0] = tagService.getSkyDRMTag();

        ControlCenterPolicyModel membershipModel = null;

        if (membershipPolicy) {
            ControlCenterModelService modelService = new ControlCenterModelService(rsClient);
            membershipModel = modelService.getResourceModel(tokenGroupName);
            updateRequestForMembershipPolicy(tokenGroupName, jsonPolicy, membershipModel);
        }

        for (JsonResourceComponent resource : jsonPolicy.getResources()) {
            for (ControlCenterComponent ccResource : resource.getComponents()) {
                ccResource.setTags(tags);
                if (ccResource.getId() == null) {
                    if (ccResource.getVersion() != null) {
                        throw new JsonParseException("Resource Component should be created not updated");
                    }
                    ccResource.setName(jsonPolicy.getName() + "_" + UUID.randomUUID());
                    ccResource.setId(componentService.createComponent(ccResource));

                } else {
                    if (existingPolicyResourceComponents.contains(ccResource.getId())) {
                        int latestComponentVersion = componentService.getComponentById(ccResource.getId()).getVersion();
                        ccResource.setVersion(latestComponentVersion + 1);
                        if (jsonPolicy.getToDeploy()) {
                            ccResource.setDeployed(true);
                        }
                        componentService.modifyComponent(ccResource);
                        existingPolicyResourceComponents.remove(ccResource.getId());
                    } else {
                        throw new JsonParseException("Resource Component should be created not updated");
                    }
                }

            }
        }

        tags = new ControlCenterTag[2];
        tags[0] = tagService.get(tokenGroupName);
        tags[1] = tagService.getSkyDRMTag();

        ControlCenterComponent[] actionComponents = getActionComponents(tokenGroupName, membershipPolicy);
        for (String action : jsonPolicy.getActions()) {
            for (ControlCenterComponent component : actionComponents) {
                if (component.getName().equals(action)) {
                    actionComponentIds.add(new ControlCenterIdWrapper(component.getId()));
                    break;
                }
            }
        }

        currentPolicy = policyService.get(jsonPolicy.getId());
        jsonPolicy.setVersion(currentPolicy.getVersion() + 1);
        ControlCenterResponse response;
        if (jsonPolicy.getToDeploy()) {
            response = policyService.saveAndDeploy(tokenGroupName, jsonPolicy, parsedAdvancedConditions, tags, subjectComponentIds, actionComponentIds, membershipModel);
        } else {
            response = policyService.modify(tokenGroupName, jsonPolicy, parsedAdvancedConditions, tags, subjectComponentIds, actionComponentIds, membershipModel);
        }

        for (Long deleteComponentWithId : existingPolicyResourceComponents) {
            try {
                componentService.deleteComponent(deleteComponentWithId);
            } catch (Throwable e) {
                LOGGER.error("Error occurred while deleting component with id {} {}" + deleteComponentWithId, e.getMessage(), e);
            }
        }
        return response;
    }

    private static void setControlCenterVersion(String version) {
        ccVersion = new ControlCenterVersion(version);
    }

    public static ControlCenterVersion getControlCenterVersion() {
        return ccVersion;
    }

    /**
     * Bootstrap Policy Studio for the first time
     * @throws ControlCentreNotSupportedException 
     * @throws DelegationPolicyException 
     */
    public static void bootstrap()
            throws ControlCenterServiceException, ControlCenterRestClientException, ControlCentreNotSupportedException,
            DelegationPolicyException {
        ControlCenterRestClient rsClient = new ControlCenterRestClient();
        ControlCenterDelegationService delegationService = new ControlCenterDelegationService(rsClient);
        delegationService.createRMSDelegationPolicy();
        LOGGER.info("Created default delegation policy.");
    }

    private static List<ControlCenterAttribute> toControlCenterAttributeList(
        List<JsonClassificationCategory> categories) {
        List<ControlCenterAttribute> rscAttributes = new ArrayList<>();
        boolean hasExt = false;
        for (JsonClassificationCategory cat : categories) {
            ControlCenterAttribute attr = toControlCenterAttribute(cat.getName());
            rscAttributes.add(attr);
            if ("File Extension".equalsIgnoreCase(cat.getName())) {
                hasExt = true;
            }
        }
        if (!hasExt) {
            ControlCenterAttribute fileExt = toControlCenterAttribute("File Extension");
            rscAttributes.add(fileExt);
        }
        return rscAttributes;
    }

    public static ControlCenterAttribute[] getDefaultSubjectAttributes(String tokenGroupName)
            throws ControlCenterServiceException, ControlCenterRestClientException {
        ControlCenterRestClient rsClient = new ControlCenterRestClient(tokenGroupName);
        return new ControlCenterModelService(rsClient).getDefaultSubjectAttributes();
    }

    public static ControlCenterAttribute toControlCenterAttribute(String name) {
        ControlCenterAttribute attr = new ControlCenterAttribute();
        attr.setDataType("STRING");
        attr.setName(name);
        attr.setShortName(name.toLowerCase());
        attr.setOperatorConfigs(ops);
        return attr;
    }

    public static String escapeIllegalChars(String tokenGroupName) {
        return "t_" + tokenGroupName.replace('-', '_').replaceAll("\\s", "");
    }
}
