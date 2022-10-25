package com.nextlabs.rms.eval.pojos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.nextlabs.rms.eval.EvalRequest;
import com.nextlabs.rms.eval.PolicyEvalException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName(value = "Request")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
public class XacmlEvalRequest {

    @JsonProperty("ReturnPolicyIdList")
    private boolean returnPolicyIdList;

    @JsonProperty("Category")
    private final List<XacmlCategory> categories = new ArrayList<>();

    @JsonProperty("MultiRequests")
    private XacmlMultiRequests xacmlMultiRequests;

    public boolean isReturnPolicyIdList() {
        return returnPolicyIdList;
    }

    public void setReturnPolicyIdList(boolean returnPolicyIdList) {
        this.returnPolicyIdList = returnPolicyIdList;
    }

    public List<XacmlCategory> getCategories() {
        return categories;
    }

    public void addCategory(XacmlCategory category) {
        this.categories.add(category);
    }

    public void addCategory(List<XacmlCategory> categories) {
        this.categories.addAll(categories);
    }

    public XacmlMultiRequests getXacmlMultiRequests() {
        return xacmlMultiRequests;
    }

    public void setXacmlMultiRequests(XacmlMultiRequests xacmlMultiRequests) {
        this.xacmlMultiRequests = xacmlMultiRequests;
    }

    public static XacmlEvalRequest getXacmlRequest(EvalRequest evalRequest) throws PolicyEvalException {
        XacmlEvalRequest xacmlEvalRequest = new XacmlEvalRequest();
        List<XacmlCategory> categories = getCategories(evalRequest);
        xacmlEvalRequest.addCategory(categories);
        return xacmlEvalRequest;
    }

    public static XacmlEvalRequest getXacmlRequest(List<EvalRequest> evalRequests) throws PolicyEvalException {
        XacmlEvalRequest xacmlEvalRequest = new XacmlEvalRequest();
        List<XacmlRequestReference> xacmlRequestReferences = new ArrayList<>();
        for (EvalRequest evalRequest : evalRequests) {
            List<XacmlCategory> categories = getCategories(evalRequest);
            xacmlEvalRequest.addCategory(categories);
            List<String> referenceIds = new ArrayList<>();
            categories.forEach(cat -> referenceIds.add(cat.getCategoryId()));
            xacmlRequestReferences.add(new XacmlRequestReference(referenceIds));
        }
        xacmlEvalRequest.setXacmlMultiRequests(new XacmlMultiRequests(xacmlRequestReferences));
        return xacmlEvalRequest;
    }

    private static List<XacmlCategory> getCategories(EvalRequest evalRequest) throws PolicyEvalException {
        List<XacmlCategory> categories = new ArrayList<>();
        categories.add(XacmlHost.getXacmlHost(evalRequest.getHost()));
        categories.add(XacmlApplication.getXacmlApplication(evalRequest.getApplication()));
        Arrays.asList(evalRequest.getResources()).forEach(resource -> categories.add(XacmlResource.getXacmlResource(resource)));
        categories.add(XacmlSubject.getXacmlSubject(evalRequest.getUser()));
        XacmlEnvironment environment;
        if (evalRequest.getEnvironments() != null) {
            environment = XacmlEnvironment.getXacmlEnvironment(Arrays.asList(evalRequest.getEnvironments()));
        } else {
            environment = XacmlEnvironment.getDefaultXacmlEnvironment();
        }
        environment.addAttribute(new XacmlAttribute(EvalRequest.ATTRIBVAL_PERFORM_OBLIGATIONS, evalRequest.isPerformObligations() ? "all" : "pep"));
        categories.add(environment);
        return categories;
    }
}
