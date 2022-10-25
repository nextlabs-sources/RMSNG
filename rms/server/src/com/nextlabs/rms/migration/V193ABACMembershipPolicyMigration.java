package com.nextlabs.rms.migration;

import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.cc.exception.ControlCentreNotSupportedException;
import com.nextlabs.rms.cc.pojos.ControlCenterComponent;
import com.nextlabs.rms.cc.pojos.ControlCenterComponentRequest;
import com.nextlabs.rms.cc.pojos.ControlCenterCondition;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicy;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicyModel;
import com.nextlabs.rms.cc.pojos.JsonPolicy;
import com.nextlabs.rms.cc.pojos.JsonResourceComponent;
import com.nextlabs.rms.cc.service.ControlCenterComponentService;
import com.nextlabs.rms.cc.service.ControlCenterManager;
import com.nextlabs.rms.cc.service.ControlCenterModelService;
import com.nextlabs.rms.cc.service.ControlCenterPolicyService;
import com.nextlabs.rms.cc.service.ControlCenterRestClient;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.shared.LogConstants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/***
 * This migration modifies the existing ABAC policies with ACCESS_PROJECT action and resource component with tenant id.
 */
public class V193ABACMembershipPolicyMigration implements Migration {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void migrate() {
        String defaultTenant = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
        LOGGER.info("ABAC membership policy migration");

        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", defaultTenant));
            Tenant tenant = (Tenant)criteria.uniqueResult();

            if (tenant == null) {
                return;
            }

            LOGGER.info("Creating ACCESS_PROJECT action if not exist");
            String membershipTokenGroupName = tenant.getName() + com.nextlabs.rms.Constants.MEMBERSHIP_MODEL_SUFFIX;
            ControlCenterRestClient rsClient = new ControlCenterRestClient(membershipTokenGroupName);
            ControlCenterComponentService componentService = new ControlCenterComponentService(rsClient);
            ControlCenterComponent[] actionComponents = componentService.getActionComponents(membershipTokenGroupName);
            boolean isAccessProjectActionExists = false;
            ControlCenterComponent viewComponent = null;
            for (ControlCenterComponent actionComponent : actionComponents) {
                if (Rights.VIEW.name().equals(actionComponent.getName())) {
                    viewComponent = actionComponent;
                    continue;
                }
                if (Rights.ACCESS_PROJECT.name().equals(actionComponent.getName())) {
                    isAccessProjectActionExists = true;
                }
            }

            ControlCenterModelService modelService = new ControlCenterModelService(rsClient);
            ControlCenterPolicyModel membershipModel = modelService.getResourceModel(membershipTokenGroupName);
            if (!isAccessProjectActionExists) {
                componentService.createActionComponent(membershipModel, Rights.ACCESS_PROJECT.name());
            }

            LOGGER.info("Migrating ABAC membership policies with ACCESS_PROJECT action and tenant id resource component");
            ControlCenterPolicyService policyService = new ControlCenterPolicyService(rsClient);
            ControlCenterPolicy[] membershipPolicies = policyService.list(null, membershipTokenGroupName, null, null, null);
            for (ControlCenterPolicy membershipPolicy : membershipPolicies) {
                ControlCenterPolicy policy = policyService.get(membershipPolicy.getId());
                ControlCenterComponentRequest[] resourceComponents = policy.getFromResourceComponents();
                if (resourceComponents != null && resourceComponents.length > 0) {
                    continue;
                }

                LOGGER.debug("Migrating policy: {}", policy.getName());

                JsonPolicy jsonPolicy = new JsonPolicy();
                jsonPolicy.setId(policy.getId());
                jsonPolicy.setName(policy.getName().replace(membershipTokenGroupName, "").trim());
                jsonPolicy.setDescription(policy.getDescription());
                jsonPolicy.setEffectType(policy.getEffectType());
                jsonPolicy.setStatus(policy.getStatus());
                jsonPolicy.setVersion(policy.getVersion());

                jsonPolicy.setActions(new String[] { Rights.ACCESS_PROJECT.name() });

                ControlCenterComponent ccResource = new ControlCenterComponent();
                ccResource.setType("RESOURCE");
                ccResource.setPolicyModel(membershipModel);
                ccResource.setName(membershipTokenGroupName);
                ccResource.setConditions(new ControlCenterCondition[] {
                    new ControlCenterCondition("tenantId", "=", WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT)) });
                JsonResourceComponent jsonResourceComponent = new JsonResourceComponent();
                jsonResourceComponent.setComponents(new ControlCenterComponent[] { ccResource });
                jsonResourceComponent.setOperator("IN");
                jsonPolicy.setResources(new JsonResourceComponent[] { jsonResourceComponent });

                if (policy.getScheduleConfig() != null) {
                    jsonPolicy.setScheduleConfig(policy.getScheduleConfig());
                }

                ControlCenterManager.updatePolicy(membershipTokenGroupName, jsonPolicy, policy.getExpression(), true);
            }

            if (viewComponent != null) {
                componentService.deleteComponent(viewComponent.getId());
            }
        } catch (ControlCenterRestClient.ControlCenterServiceException
                | ControlCenterRestClient.ControlCenterRestClientException | ControlCentreNotSupportedException e) {
            LOGGER.error("Error in ABAC membership policy migration", e);
        }
    }
}
