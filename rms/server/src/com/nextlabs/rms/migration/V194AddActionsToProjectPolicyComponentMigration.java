package com.nextlabs.rms.migration;

import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.rms.cc.pojos.ControlCenterComponent;
import com.nextlabs.rms.cc.pojos.ControlCenterComponentRequest;
import com.nextlabs.rms.cc.pojos.ControlCenterIdentifier;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicy;
import com.nextlabs.rms.cc.pojos.ControlCenterPolicyModel;
import com.nextlabs.rms.cc.service.ControlCenterComponentService;
import com.nextlabs.rms.cc.service.ControlCenterModelService;
import com.nextlabs.rms.cc.service.ControlCenterPolicyService;
import com.nextlabs.rms.cc.service.ControlCenterRestClient;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.shared.LogConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/***
 * This migration adds actions to project policy component.
 */
public class V194AddActionsToProjectPolicyComponentMigration implements Migration {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);

    @Override
    public void migrate() {
        String defaultTenant = WebConfig.getInstance().getProperty(WebConfig.PUBLIC_TENANT, com.nextlabs.rms.config.Constants.DEFAULT_TENANT);
        LOGGER.info("Add actions to policy component model");

        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(Tenant.class);
            criteria.add(Restrictions.eq("name", defaultTenant));
            Tenant tenant = (Tenant)criteria.uniqueResult();

            if (tenant == null) {
                return;
            }

            ControlCenterRestClient rsClient = new ControlCenterRestClient(tenant.getName());
            ControlCenterModelService modelService = new ControlCenterModelService(rsClient);
            ControlCenterComponentService componentService = new ControlCenterComponentService(rsClient);
            ControlCenterPolicyService policyService = new ControlCenterPolicyService(rsClient);

            List<String> rights = CentralPoliciesEvaluationHandler.getEvaluatedRights();

            Criteria projectCriteria = session.createCriteria(Project.class);
            List<Project> projects = projectCriteria.list();

            Map<Long, Long> oldActionComponentIdToNew = new HashMap<>();

            for (Project project : projects) {
                String tokenGroupName = project.getKeystore().getTokenGroupName();
                ControlCenterPolicyModel resourceModel = modelService.getResourceModel(tokenGroupName);
                List<ControlCenterIdentifier> actions = resourceModel.getActions();

                // add actions to policy model (the existing action could be VIEW or ACCESS_PROJECT)
                if (actions.size() == 1) {
                    ControlCenterComponent[] existingActionComponents = componentService.getActionComponents(tokenGroupName);
                    for (ControlCenterComponent action : existingActionComponents) {
                        componentService.deleteComponent(action.getId());
                    }
                    componentService.createActionComponents(resourceModel, rights);
                    actions = resourceModel.getActions();
                    resourceModel.setActions(actions);
                    modelService.updateResourcePolicyModel(resourceModel, resourceModel.getAttributes(), Constants.PolicyModelType.RESOURCE, true);
                }

                ControlCenterComponent[] modelActions = componentService.getActionComponents(tokenGroupName);

                // modify actions in policies to policy model actions
                ControlCenterPolicy[] projectPolicies = policyService.list(null, tokenGroupName, null, null, null);
                for (ControlCenterPolicy projectPolicy : projectPolicies) {
                    ControlCenterPolicy policy = policyService.get(projectPolicy.getId());
                    ControlCenterComponentRequest[] actionComponents = policy.getActionComponents();

                    for (ControlCenterComponentRequest actionComponent : actionComponents) {
                        for (ControlCenterComponentRequest.ControlCenterIdWrapper idWrapper : actionComponent.getComponents()) {
                            long id = idWrapper.getId();
                            if (!oldActionComponentIdToNew.containsKey(id)) {
                                ControlCenterComponent oldComponent = componentService.getComponentById(id);
                                Optional<Long> newComponentId = Arrays.stream(modelActions).filter(action -> action.getName().equals(oldComponent.getName())).map(component -> component.getId()).findFirst();
                                if (newComponentId.isPresent()) {
                                    oldActionComponentIdToNew.put(id, newComponentId.get());
                                }
                            }
                            idWrapper.setId((oldActionComponentIdToNew.get(id)));
                        }
                    }

                    policyService.modify(policy);
                }
            }
        } catch (ControlCenterRestClient.ControlCenterServiceException
                | ControlCenterRestClient.ControlCenterRestClientException e) {
            LOGGER.error("Error in add actions to project policy component migration", e);
        }
    }
}
