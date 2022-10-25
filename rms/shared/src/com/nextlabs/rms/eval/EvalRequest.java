package com.nextlabs.rms.eval;

public class EvalRequest {

    private Resource[] resources;
    private User user;
    private Application application;
    private Host host;
    private NamedAttributes[] environments;
    private boolean ignoreBuiltInPolicies = true;
    private boolean performObligations = true;
    //TODO: Value is hardcoded here. Check if there is a better way to handle this.
    private int noiseLevel = 3;//PDPSDK.NOISE_LEVEL_USER_ACTION;
    //TODO: Value is hardcoded here. Check if there is a better way to handle this.
    private int timeoutInMins = -1;//PDPSDK.WAIT_FOREVER;
    private String adhocPolicy;
    private String membershipId;
    private String policy;
    private Integer evalType;

    public static final String ATTRIBVAL_RMS_APP_NAME = "RMS";
    public static final String ATTRIBVAL_VIEWER_APP_NAME = "VIEWER";
    public static final String ATTRIBVAL_RES_DIMENSION_FROM = "from";
    public static final String ATTRIBVAL_RES_DIMENSION_TO = "to";
    public static final String ATTRIBVAL_RES_DONT_CARE_ACCEPTABLE = "dont-care-acceptable";
    public static final String ATTRIBVAL_RES_RESOURCE_TYPE_EVAL = "use_resource_type_when_evaluating";
    public static final String ATTRIBVAL_PERFORM_OBLIGATIONS = "nextlabs-perform-obligations";
    public static final String ENVIRONMENT_ATTRIBUTE_NAME = "environment";
    public static final String OBLIGATION_WATERMARK = "OB_OVERLAY";
    public static final String OBLIGATION_COUNT = "CE_ATTR_OBLIGATION_COUNT";
    public static final String OBLIGATION_NAME = "CE_ATTR_OBLIGATION_NAME";
    public static final String OBLIGATION_POLICY = "CE_ATTR_OBLIGATION_POLICY";
    public static final String OBLIGATION_VALUE = "CE_ATTR_OBLIGATION_VALUE";
    public static final String OBLIGATION_VALUE_COUNT = "CE_ATTR_OBLIGATION_NUMVALUES";

    public EvalRequest() {

    }

    public EvalRequest(EvalRequest other) {
        this.resources = other.resources;
        this.user = other.user;
        this.application = other.application;
        this.host = other.host;
        this.environments = other.environments;
        this.ignoreBuiltInPolicies = other.ignoreBuiltInPolicies;
        this.performObligations = other.performObligations;
        this.noiseLevel = other.noiseLevel;
        this.timeoutInMins = other.timeoutInMins;
        this.adhocPolicy = other.adhocPolicy;
        this.membershipId = other.membershipId;
        this.policy = other.policy;
        this.evalType = other.evalType;
    }

    public Resource[] getResources() {
        return resources;
    }

    public void setResources(Resource[] resources) {
        this.resources = resources;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public boolean isPerformObligations() {
        return performObligations;
    }

    public void setPerformObligations(boolean performObligations) {
        this.performObligations = performObligations;
    }

    public int getNoiseLevel() {
        return noiseLevel;
    }

    public void setNoiseLevel(int noiseLevel) {
        this.noiseLevel = noiseLevel;
    }

    public int getTimeoutInMins() {
        return timeoutInMins;
    }

    public void setTimeoutInMins(int timeoutInMins) {
        this.timeoutInMins = timeoutInMins;
    }

    public NamedAttributes[] getEnvironments() {
        return environments;
    }

    public void setEnvironments(NamedAttributes[] environments) {
        this.environments = environments;
    }

    public boolean isIgnoreBuiltInPolicies() {
        return ignoreBuiltInPolicies;
    }

    public void setIgnoreBuiltInPolicies(boolean ignoreBuiltInPolicies) {
        this.ignoreBuiltInPolicies = ignoreBuiltInPolicies;
    }

    public String getAdhocPolicy() {
        return adhocPolicy;
    }

    public void setAdhocPolicy(String adhocPolicy) {
        this.adhocPolicy = adhocPolicy;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(String membershipId) {
        this.membershipId = membershipId;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public Integer getEvalType() {
        return evalType;
    }

    public void setEvalType(Integer evalType) {
        this.evalType = evalType;
    }

    public enum EvalType {
        CENTRAL_ONLY,
        ADHOC_ONLY,
        CENTRAL_PRECEDENCE,
        ADHOC_PRECEDENCE
    }
}
