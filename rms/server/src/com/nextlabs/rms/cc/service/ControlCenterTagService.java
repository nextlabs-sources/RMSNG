package com.nextlabs.rms.cc.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.rms.cc.pojos.ControlCenterTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ControlCenterTagService extends ControlCenterRestClient {

    private String createEndPoint;

    public ControlCenterTagService(ControlCenterRestClient rs, String createEndPoint) {
        super(rs);
        setCreateEndpoint(createEndPoint);
    }

    public List<ControlCenterTag> getAll(String key)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + "/console/api/v1/config/tags/list";
        ControlCenterTag tagFilter = new ControlCenterTag();
        tagFilter.setType("POLICY_TAG");
        HashMap<String, Object> filter = new HashMap<>();
        filter.put("tag", tagFilter);
        filter.put("pageSize", "65535");
        ControlCenterResponse psTagResponse = doPost(serviceUrl, filter, ControlCenterResponse.class);
        List<ControlCenterTag> tenantTags = new ArrayList<>();
        if (CODE_5000.equals(psTagResponse.getStatusCode())) {
            return tenantTags;
        } else if (!CODE_1003.equals(psTagResponse.getStatusCode())) {
            StringBuilder sb = new StringBuilder("Error occurred while fetching all tags: ").append(psTagResponse.getStatusCode()).append(" - ").append(psTagResponse.getMessage());
            throw new ControlCenterServiceException(sb.toString());
        } else {
            JsonElement data = psTagResponse.getData();
            ControlCenterTag[] tags = new Gson().fromJson(data, ControlCenterTag[].class);
            for (ControlCenterTag tag : tags) {
                if (StringUtils.hasText(key)) {
                    if (StringUtils.equalsIgnoreCase(tag.getKey(), key)) {
                        tenantTags.add(tag);
                    }
                } else {
                    tenantTags.add(tag);
                }
            }
            return tenantTags;
        }
    }

    public String createPolicyTagBase(String key, String value)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        String serviceUrl = getConsoleUrl() + getCreateEndpoint();
        ControlCenterTag tag = new ControlCenterTag();
        tag.setKey(key);
        tag.setLabel(value);
        tag.setType("POLICY_TAG");
        ControlCenterResponse psResponse = doPost(serviceUrl, tag, ControlCenterResponse.class);
        if (!CODE_1000.equals(psResponse.getStatusCode())) {
            StringBuilder sb = new StringBuilder("Error occurred while creating policy tag: ").append(psResponse.getStatusCode()).append(" - ").append(psResponse.getMessage());
            throw new ControlCenterServiceException(sb.toString());
        }
        JsonElement data = psResponse.getData();
        return data.getAsString();
    }

    public String createSkyDRMTag()
            throws ControlCenterRestClientException, ControlCenterServiceException {
        return createPolicyTagBase(ControlCenterConstants.SKYDRM_TAG_KEY, ControlCenterConstants.SKYDRM_TAG_LABEL);
    }

    public String createPolicyTag(String value)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        return createPolicyTagBase(ControlCenterConstants.TENANT_TAG_KEY, value);
    }

    public void createPolicyTags(List<String> values)
            throws ControlCenterRestClientException, ControlCenterServiceException, ExecutionException,
            InterruptedException {
        List<ControlCenterTag> tags = new ArrayList<>();
        List<ControlCenterTag> existingTags = getAll(ControlCenterConstants.TENANT_TAG_KEY);
        for (String value : values) {
            if (lookup(value, existingTags) == null) {
                ControlCenterTag tag = new ControlCenterTag();
                tag.setKey(ControlCenterConstants.TENANT_TAG_KEY);
                tag.setLabel(value);
                tag.setType("POLICY_TAG");
                tags.add(tag);
            }
        }
        String serviceUrl = getConsoleUrl() + getCreateEndpoint();
        List<ControlCenterAsyncResponse<ControlCenterResponse>> psResponses = doPostAsync(serviceUrl, tags);
        for (ControlCenterAsyncResponse<ControlCenterResponse> async : psResponses) {
            ControlCenterTag tag = (ControlCenterTag)async.getRequest();
            if (async.getResponse() == null) {
                LOGGER.error("Error occurred while creating tag {}: {}", tag.getLabel(), async.getException().getMessage(), async.getException());
            } else {
                ControlCenterResponse psResponse = async.getResponse();
                if (!CODE_1000.equals(psResponse.getStatusCode())) {
                    StringBuilder sb = new StringBuilder("Error occurred while creating tag: ").append(psResponse.getStatusCode()).append(" - ").append(psResponse.getMessage());
                    LOGGER.error(sb.toString());
                } else if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created tag: {}", tag.getLabel());

                }
            }
        }
    }

    public ControlCenterTag get(String tokenGroupName)
            throws ControlCenterRestClientException, ControlCenterServiceException {
        List<ControlCenterTag> existingTags = getAll(ControlCenterConstants.TENANT_TAG_KEY);
        return lookup(tokenGroupName, existingTags);
    }

    public ControlCenterTag getSkyDRMTag()
            throws ControlCenterRestClientException, ControlCenterServiceException {
        List<ControlCenterTag> existingTags = getAll(ControlCenterConstants.SKYDRM_TAG_KEY);
        return lookup(ControlCenterConstants.SKYDRM_TAG_LABEL, existingTags);
    }

    protected ControlCenterTag lookup(String label, List<ControlCenterTag> existingTags) {
        for (ControlCenterTag tenantTag : existingTags) {
            if (StringUtils.equalsIgnoreCase(label, tenantTag.getLabel())) {
                return tenantTag;
            }
        }
        return null;
    }

    private String getCreateEndpoint() {
        return createEndPoint;
    }

    private void setCreateEndpoint(String createEndPoint) {
        this.createEndPoint = createEndPoint;
    }

}
