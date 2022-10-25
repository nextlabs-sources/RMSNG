package com.nextlabs.rms.rs;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.Constants.TokenGroupType;
import com.nextlabs.common.shared.JsonClassificationCategory;
import com.nextlabs.common.shared.JsonClassificationCategory.Label;
import com.nextlabs.common.shared.JsonRequest;
import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.RegularExpressions;
import com.nextlabs.common.shared.WebConfig;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.abac.MembershipPoliciesEvaluationHandler;
import com.nextlabs.rms.cc.service.ControlCenterManager;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterRestClientException;
import com.nextlabs.rms.cc.service.ControlCenterRestClient.ControlCenterServiceException;
import com.nextlabs.rms.exception.TokenGroupException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Classification;
import com.nextlabs.rms.hibernate.model.KeyStoreEntry;
import com.nextlabs.rms.hibernate.model.Membership;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.Tenant;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.security.KeyStoreManagerImpl;
import com.nextlabs.rms.service.ProjectService;
import com.nextlabs.rms.service.SystemBucketManagerImpl;
import com.nextlabs.rms.service.TokenGroupManager;
import com.nextlabs.rms.shared.LogConstants;
import com.nextlabs.rms.util.Audit;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

@Path("/classification")
public class ClassificationMgmt {

    private static final Logger LOGGER = LogManager.getLogger(LogConstants.RMS_SERVER_LOG_NAME);
    private static final int MAX_NAME_LENGTH = 60;
    private static final Type LABEL_LIST_TYPE = new TypeToken<List<Label>>() {
    }.getType();
    private static final Type CATEGORY_LIST_TYPE = new TypeToken<List<JsonClassificationCategory>>() {
    }.getType();
    private int maxCategoryNum;
    private int maxLabelNum;

    @PostConstruct
    public void init() {
        maxCategoryNum = Integer.parseInt(WebConfig.getInstance().getProperty(WebConfig.CLASSIFICATION_SELECT_CATEGORY_MAXNUM, "5"));
        maxLabelNum = Integer.parseInt(WebConfig.getInstance().getProperty(WebConfig.CLASSIFICATION_SELECT_LABEL_MAXNUM, "10"));
    }

    @Secured
    @GET
    @Path("/{tokenGroupName}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getClassificationProfile(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tokenGroupName") String tokenGroupName) {
        boolean error = true;
        try {
            tokenGroupName = URLDecoder.decode(tokenGroupName, StandardCharsets.UTF_8.name());
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            List<JsonClassificationCategory> categories;
            try (DbSession session = DbSession.newSession()) {
                KeyStoreEntry keyStore = new KeyStoreManagerImpl().getKeyStore(tokenGroupName);
                boolean isProject = false;
                if (keyStore == null) {
                    return new JsonResponse(400, "Invalid request.").toJson();
                }
                SystemBucketManagerImpl sbm = new SystemBucketManagerImpl();
                if (sbm.isSystemBucket(tokenGroupName)) {
                    return new JsonResponse(4006, "Classification APIs for system bucket should be called using its tenant only").toJson();
                }
                Tenant tenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);
                Membership membership = null;
                if (tenant.getName().equals(tokenGroupName)) {
                    membership = TenantMgmt.getMembership(session, userId, tenant.getId());
                }
                if (membership == null) {
                    membership = ProjectService.getActiveMembership(session, userId, tokenGroupName);
                    isProject = true;
                }
                if (membership == null && !MembershipPoliciesEvaluationHandler.isDynamicallyAccessible(session, us, tokenGroupName)) {
                    return new JsonResponse(403, "User not part of token group").toJson();
                }
                categories = isProject ? getProjectClassification(session, tokenGroupName) : getTenantClassification(session, tokenGroupName);
            }

            JsonResponse response = new JsonResponse("OK");
            response.putResult("categories", categories);
            response.putResult("maxCategoryNum", maxCategoryNum);
            response.putResult("maxLabelNum", maxLabelNum);
            error = false;
            return response.toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ClassificationMgmt", "getClassificationProfile", !error ? 1 : 0, userId, tokenGroupName);
        }
    }

    public static List<JsonClassificationCategory> getProjectClassification(DbSession session, String tokenGroupName) {
        List<JsonClassificationCategory> categories = new ArrayList<>();
        Criteria criteria = session.createCriteria(Classification.class);
        criteria.addOrder(Order.asc("orderId"));
        criteria.createCriteria("project", "p");
        criteria.createCriteria("p.keystore", "k");
        criteria.add(Restrictions.eq("k.tokenGroupName", tokenGroupName));
        @SuppressWarnings("unchecked")
        List<Classification> classifications = criteria.list();
        for (int i = 0; i < classifications.size(); i++) {
            categories.add(toJson(classifications.get(i)));
        }
        return categories;
    }

    public static List<JsonClassificationCategory> getTenantClassification(DbSession session, String tokenGroupName) {
        List<JsonClassificationCategory> categories = new ArrayList<>();
        Criteria criteria = session.createCriteria(Classification.class);
        criteria.createCriteria("tenant", "t");
        criteria.add(Restrictions.eq("t.name", tokenGroupName));
        criteria.addOrder(Order.asc("orderId"));
        @SuppressWarnings("unchecked")
        List<Classification> classifications = criteria.list();
        for (int i = 0; i < classifications.size(); i++) {
            categories.add(toJson(classifications.get(i)));
        }
        return categories;
    }

    @Secured
    @POST
    @Path("/{tokenGroupName}")
    @Produces(MediaType.APPLICATION_JSON)
    public String updateClassificationProfile(@Context HttpServletRequest request, @HeaderParam("userId") int userId,
        @HeaderParam("ticket") String ticket, @HeaderParam("clientId") String clientId,
        @HeaderParam("platformId") Integer platformId, @PathParam("tokenGroupName") String tokenGroupName,
        String json) {
        boolean error = true;
        try {
            tokenGroupName = URLDecoder.decode(tokenGroupName, StandardCharsets.UTF_8.name());
            UserSession us = (UserSession)request.getAttribute(RESTAPIAuthenticationFilter.USERSESSION);
            JsonRequest req = JsonRequest.fromJson(json);
            if (req == null) {
                return new JsonResponse(400, "Missing request").toJson();
            }
            List<JsonClassificationCategory> categories = req.getParameter("categories", CATEGORY_LIST_TYPE);
            if (categories == null) {
                return new JsonResponse(400, "Missing required parameters").toJson();
            }
            if (categories.size() > maxCategoryNum) {
                return new JsonResponse(4005, "Category Limit exceeded").toJson();
            }
            Set<String> categorySet = new HashSet<>();
            Gson gson = new Gson();
            for (JsonClassificationCategory category : categories) {
                String categoryName = category.getName();
                if (!StringUtils.hasText(categoryName)) {
                    return new JsonResponse(400, "Missing required parameters").toJson();
                } else if (categoryName.length() > MAX_NAME_LENGTH) {
                    return new JsonResponse(4001, "Category name is too long.").toJson();
                } else if (!validateName(categoryName)) {
                    return new JsonResponse(4002, "Category name contains illegal characters.").toJson();
                } else if (category.getLabels().size() > maxLabelNum || gson.toJson(category.getLabels()).length() > 1200) {
                    return new JsonResponse(4005, "Label Limit exceeded").toJson();
                } else if (category.getLabels() == null || category.getLabels().size() == 0) {
                    return new JsonResponse(4007, "Atleast one label has to be added per category").toJson();
                }
                boolean foundDefault = false;
                Set<String> labelSet = new HashSet<>();

                for (Label label : category.getLabels()) {
                    String labelName = label.getName();
                    if (!StringUtils.hasText(labelName)) {
                        return new JsonResponse(400, "Missing required parameters").toJson();
                    } else if (labelName.length() > MAX_NAME_LENGTH) {
                        return new JsonResponse(4001, "Label name is too long.").toJson();
                    } else if (!validateName(labelName)) {
                        return new JsonResponse(4002, "Label name contains illegal characters.").toJson();
                    }
                    if (!category.isMultiSelect() && label.isDefault()) {
                        if (!foundDefault) {
                            foundDefault = true;
                        } else {
                            return new JsonResponse(4004, "A non multi-select category cannot have multiple default labels").toJson();
                        }
                    }
                    labelSet.add(labelName.toLowerCase());
                }
                if (labelSet.size() != category.getLabels().size()) {
                    return new JsonResponse(4003, "Duplicate label names.").toJson();
                }
                categorySet.add(categoryName.toLowerCase());
            }
            if (categorySet.size() != categories.size()) {
                return new JsonResponse(4003, "Duplicate category names.").toJson();
            }

            try (DbSession session = DbSession.newSession()) {
                User user = us.getUser();
                KeyStoreEntry keyStore = new KeyStoreManagerImpl().getKeyStore(tokenGroupName);
                if (keyStore == null) {
                    return new JsonResponse(400, "Invalid request.").toJson();
                }
                Tenant tenant = TenantMgmt.getTenantByTokenGroupName(session, tokenGroupName);

                TokenGroupManager tokenGroupMgr;
                try {
                    tokenGroupMgr = TokenGroupManager.newInstance(session, tokenGroupName, us.getLoginTenant());
                } catch (TokenGroupException e) {
                    return new JsonResponse(400, e.getMessage()).toJson();
                }
                Membership membership = tokenGroupMgr.getStaticMembership(session, userId);
                if (membership == null && !MembershipPoliciesEvaluationHandler.isDynamicallyAccessible(session, us, tokenGroupName)) {
                    return new JsonResponse(4006, "User not part of token group").toJson();
                }
                if (tokenGroupMgr.getGroupType().equals(TokenGroupType.TOKENGROUP_SYSTEMBUCKET)) {
                    return new JsonResponse(4006, "Classification APIs for system bucket should be called using its tenant only").toJson();
                }
                if (tokenGroupMgr.getGroupType().equals(TokenGroupType.TOKENGROUP_TENANT) && !tenant.isAdmin(user.getEmail())) {
                    return new JsonResponse(403, "Only tenant admin can modify the classification profile.").toJson();
                }
                if (tokenGroupMgr.getGroupType().equals(TokenGroupType.TOKENGROUP_PROJECT) && !ProjectService.isMemberProjectOwner(membership)) {
                    return new JsonResponse(403, "Only project owner can modify the classification profile.").toJson();
                }

                session.beginTransaction();
                Query query = null;
                Project project = null;
                if (tokenGroupName.equalsIgnoreCase(tenant.getName())) {
                    query = session.createNamedQuery("deleteClassificationsByTenant");
                    query.setParameter("tenantId", tenant.getId());
                } else {
                    query = session.createNamedQuery("deleteClassificationsByProject");
                    project = ProjectService.getProjectByTokenGroupName(session, tokenGroupName);
                    query.setParameter("projectId", project.getId());
                }
                query.executeUpdate();
                for (int i = 0; i < categories.size(); i++) {
                    Classification classification = fromJson(categories.get(i), i);
                    if (project != null) {
                        classification.setProject(project);
                        classification.setType(Constants.TokenGroupType.TOKENGROUP_PROJECT);
                    } else {
                        classification.setTenant(tenant);
                        classification.setType(Constants.TokenGroupType.TOKENGROUP_TENANT);
                    }
                    session.save(classification);
                }
                if (project != null) {
                    project.setConfigurationModified(new Date());
                    session.save(project);
                } else {
                    tenant.setConfigurationModified(new Date());
                    session.save(tenant);
                }

                ControlCenterManager.updateResourceModel(tokenGroupName, categories, tokenGroupMgr.getGroupType());
                session.commit();
            }
            JsonResponse resp = new JsonResponse(201, "Classification profile successfully updated");
            error = false;
            return resp.toJson();
        } catch (ControlCenterServiceException | ControlCenterRestClientException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to propagate to control center", e);
            }
            return new JsonResponse(5003, "Unable to propagate to control center").toJson();
        } catch (IllegalArgumentException | JsonParseException | ClassCastException | IllegalStateException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to parse JSON (value: {}): {}", json, e.getMessage(), e);
            }
            return new JsonResponse(400, "Malformed request.").toJson();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            return new JsonResponse(500, "Internal Server Error.").toJson();
        } finally {
            Audit.audit(request, "API", "ClassificationMgmt", "updateClassificationProfile", !error ? 1 : 0, userId, tokenGroupName);
        }
    }

    static JsonClassificationCategory toJson(Classification classification) {
        JsonClassificationCategory json = new JsonClassificationCategory();
        json.setName(classification.getName());
        json.setMandatory(classification.isMandatory());
        json.setMultiSelect(classification.isMultiSel());
        json.setParentId(classification.getParentId());
        List<Label> labels = new Gson().fromJson(classification.getLabels(), LABEL_LIST_TYPE);
        json.setLabels(labels);
        return json;
    }

    static Classification fromJson(JsonClassificationCategory json, int orderId) {
        Classification classification = new Classification();
        classification.setName(json.getName());
        classification.setMandatory(json.isMandatory());
        classification.setMultiSel(json.isMultiSelect());
        classification.setLabels(new Gson().toJson(json.getLabels()));
        classification.setParentId(json.getParentId());
        classification.setOrderId(orderId);
        return classification;
    }

    private boolean validateName(String name) throws UnsupportedEncodingException {
        if (name.length() == name.getBytes(StandardCharsets.UTF_8.name()).length) {
            Matcher matcher = RegularExpressions.CLASSIFICATION_NAME_PATTERN.matcher(name);
            return matcher.matches();
        }
        return false;
    }
}
