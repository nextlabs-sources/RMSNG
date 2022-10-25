package com.bluejungle.pf.engine.destiny;

/*
 * Created on Dec 8, 2004
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/engine/destiny/EvaluationRequest.java#2 $:
 */

import com.bluejungle.framework.domain.IHasId;
import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IMultivalue;
import com.bluejungle.framework.utils.DynamicAttributes;
import com.bluejungle.pf.domain.destiny.action.IDAction;
import com.bluejungle.pf.domain.destiny.serviceprovider.IServiceProviderManager;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;
import com.bluejungle.pf.domain.destiny.subject.SubjectType;
import com.bluejungle.pf.domain.epicenter.action.IAction;
import com.bluejungle.pf.domain.epicenter.evaluation.IEvaluationRequest;
import com.bluejungle.pf.domain.epicenter.resource.IResource;
import com.bluejungle.pf.domain.epicenter.subject.ISubjectType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EvaluationRequest is represents a complete request for policy evaluation.
 * Once it is submitted to the evaluation engine, it gets assigned a
 * globally-unique requestId;
 */

public class EvaluationRequest implements Serializable, IEvaluationRequest {

    private static final long serialVersionUID = 1L;

    private long ts;
    private int level;
    private boolean executeObligations = true;
    private EngineResourceInformation fromResourceInfo;
    private EngineResourceInformation toResourceInfo;
    private IResource fromResource;
    private IResource toResource;
    private IContentAnalysisManager contentAnalysisManager = IContentAnalysisManager.DEFAULT;
    private IDAction action;
    private IDSubject user;
    private String userName;
    private IDSubject loggedInUser;
    private IDSubject host;
    private String hostIPAddress;
    private IDSubject application;
    private Long requestId;
    private String remoteAddress;
    private IDSubject[] sentTo;
    private Map<String, BitSet> sentToMatches = new HashMap<String, BitSet>();
    private int sentToPosition = -1;

    private IEngineSubjectResolver subjectResolver;
    private IClientInformationManager clientInfoManager = IClientInformationManager.DEFAULT;
    private Map<String, Object> cache = new HashMap<String, Object>();;
    private long lastSuccessfulHeartbeat;
    private IServiceProviderManager serviceProviderManager = IServiceProviderManager.DEFAULT;
    private DynamicAttributes environment = DynamicAttributes.EMPTY;

    private String additionalPoliciesAsPQL;
    private boolean ignoreBuiltinPolicies; // ignore policies from bundle

    /**
     * Constructor. Leaves all fields as null so they can be set using setters.
     */
    public EvaluationRequest() {
    }

    /**
     *
     * Main constructor. This constructor should be used for
     * evaluation requests on operations that have a source and a
     * destination, such as copy.
     *
     * @param requestId
     *            the id of the request
     * @param action
     *            action being done
     * @param fromResource
     *            the source resource of the action, such as the file being
     *            copied
     * @param toResource
     *            the destination resource of the action, such as the directory
     *            a file is being copied to (null is acceptable)
     * @param fromResourceInfo
     *            engine associated resource data for the fromResource (mostly policy
     *            associations). Null is acceptable
     * @param toResourceInfo
     *            engine associated resource data for the toResource (mostly policy
     *            associations). Null is acceptable
     * @param user
     *            user performing the action
     * @param userName
     *            principal name of the user performing the action
     * @param loggedInUser
     *            logged in user (usually, but not always, the user performing the action). null means assume same as user
     * @param application
     *            application performing the action, null means application is unknown
     * @param sentTo
     *            the target subjects of the action
     * @param host
     *            host performing the action, null means localhost
     * @param hostIPAddress
     *            IP address of the host performing the action
     * @param remoteAddress
     *            if null, request is made from the host. Otherwise (e.g. rdp), the ip address (as a binary string) of the actual source of the query
     * @param environment
     *            the environment context (null means no environment context)
     * @param executeObligations
     *            should obligations be executed for this request
     * @param ts
     *            time stamp of the request
     * @param level
     *            logging level
     * @param additionalPoliciesAsPQL
     *            additional policies to be used during policy evaluation
     * @param ignoreBuiltinPolicies
     *            ignore built-in (i.e. bundle) policies and only use those s[ecified by additionalPoliciesAsPQL
     * @param contentAnalysisManager
     *            the content analysis manager for the resources
     * @param subjectResolver
     *            the subject resolver
     * @param clientInfoManager
     *            the client information manager (null = no manager)
     * @param serviceProviderManager
     *            the service provider manager (null = no manager)
     */
    public EvaluationRequest(
        Long requestId
        , IDAction action
        , IResource fromResource
        , EngineResourceInformation fromResourceInfo
        , IResource toResource
        , EngineResourceInformation toResourceInfo
        , IDSubject user
        , String userName
        , IDSubject loggedInUser
        , IDSubject application
        , IDSubject[] sentTo
        , IDSubject host
        , String hostIPAddress
        , String remoteAddress
        , DynamicAttributes environment
        , long lastSuccessfulHeartbeat
        , boolean executeObligations
        , long ts
        , int level
        , String additionalPoliciesAsPQL
        , boolean ignoreBuiltinPolicies
        , IContentAnalysisManager contentAnalysisManager
        , IEngineSubjectResolver subjectResolver
        , IClientInformationManager clientInfoManager
        , IServiceProviderManager serviceProviderManager) {
        super();
        this.requestId = requestId;
        this.action = action;
        this.fromResource = fromResource;
        this.fromResourceInfo = fromResourceInfo;
        this.toResource = toResource;
        this.toResourceInfo = toResourceInfo;
        this.user = user;
        this.userName = userName;
        if (loggedInUser == null) {
            this.loggedInUser = user;
        } else {
            this.loggedInUser = loggedInUser;
        }
        this.application = application;
        this.sentTo = sentTo;
        this.host = host;
        this.hostIPAddress = hostIPAddress;
        this.remoteAddress = remoteAddress;
        if (environment != null) {
            this.environment = environment;
        }
        this.lastSuccessfulHeartbeat = lastSuccessfulHeartbeat;
        this.executeObligations = executeObligations;
        this.ts = ts;
        this.level = level;
        this.additionalPoliciesAsPQL = additionalPoliciesAsPQL;
        this.ignoreBuiltinPolicies = (additionalPoliciesAsPQL != null) ? ignoreBuiltinPolicies : false;
        if (contentAnalysisManager != null) {
            this.contentAnalysisManager = contentAnalysisManager;
        }
        this.subjectResolver = subjectResolver;
        if (clientInfoManager != null) {
            this.clientInfoManager = clientInfoManager;
        }
        if (serviceProviderManager != null) {
            this.serviceProviderManager = serviceProviderManager;
        }
    }

    /** Hack.  The resource predicates don't specify internally if they are for
     *  the 'to' or 'from' resource - they just assume 'from'.  In order to evaluate
     *  the 'to' resource correctly we need a new request with the 'to' resource
     *  in the place of the 'from' resource.
     */
    static public EvaluationRequest createToRequest(IEvaluationRequest req) {
        return createToRequest(req, req.getToResource());
    }

    static public EvaluationRequest createToRequest(IEvaluationRequest req, IResource toResource) {
        return new EvaluationRequest(
                (long)-1
                , (IDAction)req.getAction()
                , toResource
                , req.getToResourceInfo()
                , null
                , null
                , req.getUser()
                , req.getUserName()
                , req.getLoggedInUser()
                , req.getApplication()
                , null
                , req.getHost()
                , req.getHostIPAddress()
                , req.getRemoteAddress()
                , req.getEnvironment()
                , req.getLastSuccessfulHeartbeat()
                , req.getExecuteObligations()
                , req.getTimestamp()
                , req.getLevel()
                , req.getAdditionalPoliciesAsPQL()
                , req.getIgnoreBuiltinPolicies()
                , IContentAnalysisManager.DEFAULT
                , req.getSubjectResolver()
                , req.getClientInformationManager()
                , req.getServiceProviderManager());
    }

    /**
     * Returns the action.
     *
     * @return the action.
     */
    public IAction getAction() {
        return action;
    }

    /**
     * Sets the action
     *
     * @param action
     *            The action to set.
     */
    public void setAction(IDAction action) {
        this.action = action;
    }

    /**
     * Returns the application.
     *
     * @return the application.
     */
    public IDSubject getApplication() {
        return application;
    }

    /**
     * Sets the application
     *
     * @param application
     *            The application to set.
     */
    public void setApplication(IDSubject application) {
        this.application = application;
    }

    /**
     * Returns the fromResource.
     *
     * @return the fromResource.
     */
    public IResource getFromResource() {
        return fromResource;
    }

    /**
     * Sets the fromResource
     *
     * @param fromResource
     *            The fromResource to set.
     */
    public void setFromResource(IResource fromResource) {
        this.fromResource = fromResource;
    }

    /**
     * Returns the sentTo subjects
     *
     * @return the toSubjects
     */
    public IDSubject[] getSentTo() {
        return sentTo;
    }

    /**
     * Sets the toSubjects
     *
     * @param toSubjects
     */
    public void setSentTo(IDSubject[] sentTo) {
        this.sentTo = sentTo;
    }

    /**
     * Returns the host.
     *
     * @return the host.
     */
    public IDSubject getHost() {
        return host;
    }

    /**
     * Returns the host IP address
     *
     * @return the host IP address
     */
    public String getHostIPAddress() {
        return hostIPAddress;
    }

    /**
     * Sets the host
     *
     * @param host
     *            The host to set.
     */
    public void setHost(IDSubject host) {
        this.host = host;
    }

    /**
     * Sets the host IP Address
     *
     * @param hostIPAddress
     *            The host IP address to set.
     */
    public void setHostIPAddress(String hostIPAddress) {
        this.hostIPAddress = hostIPAddress;
    }

    /**
     * Returns the toResource.
     *
     * @return the toResource.
     */
    public IResource getToResource() {
        return toResource;
    }

    /**
     * Sets the toResource
     *
     * @param toResource
     *            The toResource to set.
     */
    public void setToResource(IResource toResource) {
        this.toResource = toResource;
    }

    /**
     * Returns the user.
     *
     * @return the user.
     */
    public IDSubject getUser() {
        return user;
    }

    /**
     * Returns the logged in user
     *
     * @return logged in user
     */
    public IDSubject getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * Returns the user name
     *
     * @return the user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the user
     *
     * @param user
     *            The user to set.
     */
    public void setUser(IDSubject user) {
        this.user = user;
    }

    public IDSubject getSubjectByType(SubjectType type) {
        if (type == SubjectType.USER) {
            if (sentTo == null || sentToPosition < 0 || sentToPosition > sentTo.length) {
                return getUser();
            } else {
                return sentTo[sentToPosition];
            }
        }
        if (type == SubjectType.HOST) {
            return getHost();
        }
        if (type == SubjectType.APP) {
            return getApplication();
        }
        if (type == SubjectType.RECIPIENT) {
            if (sentTo == null || sentTo.length == 0) {
                return new IDSubject() {

                    @Override
                    public String getUid() {
                        return "";
                    }

                    @Override
                    public String getUniqueName() {
                        return "";
                    }

                    @Override
                    public IEvalValue getGroups() {
                        return EvalValue.build(IMultivalue.EMPTY);
                    }

                    @Override
                    public IEvalValue getAttribute(String name) {
                        return IEvalValue.NULL;
                    }

                    @Override
                    public Set<Map.Entry<String, IEvalValue>> getEntrySet() {
                        return Collections.<Map.Entry<String, IEvalValue>> emptySet();
                    }

                    @Override
                    public boolean isCacheable() {
                        return false;
                    }

                    @Override
                    public String getName() {
                        return "";
                    }

                    @Override
                    public ISubjectType getSubjectType() {
                        return SubjectType.RECIPIENT;
                    }

                    @Override
                    public Long getId() {
                        return IHasId.UNKNOWN_ID;
                    }
                };
            } else if (sentToPosition < 0 || sentToPosition > sentTo.length) {
                // A position is specified if we are iterating through the recipients. If we aren't, then this
                // is a single recipient match
                return sentTo[0];
            } else {
                return sentTo[sentToPosition];
            }
        }
        return null;
    }

    public String toString() {
        StringBuffer rv = new StringBuffer("Evaluation Request[");
        rv.append("fromResource : ").append(fromResource);
        if (toResource != null) {
            rv.append(", toResource : ").append(toResource);
        }
        rv.append(", action : ").append(action);
        rv.append(", user : ").append(user);
        rv.append(", host : ").append(host);
        rv.append(", app : ").append(application);
        rv.append("]");

        return rv.toString();
    }

    /**
     * Returns the requestId, may be null
     *
     * @return the requestId.
     */
    public Long getRequestId() {
        return requestId;
    }

    /**
     * Sets the requestId.
     *
     * @param requestId
     *            The requestId to set.
     */
    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public EvaluationRequest(IResource fromResource) {
        super();
        this.fromResource = fromResource;
        this.subjectResolver = null;
    }

    public IEngineSubjectResolver getSubjectResolver() {
        return subjectResolver;
    }

    /**
     * @see IEvaluationRequest#setEnvironment()
     */
    public void setEnvironment(DynamicAttributes environment) {
        this.environment = environment;
    }

    /**
     * @see IEvaluationRequest#getEnvironment()
     */
    public DynamicAttributes getEnvironment() {
        return environment;
    }

    /**
     * @see IEvaluationRequest#getRemoteAddress()
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Sets the IP address of the remote access computer.
     * @param value the IP address of the remote access computer.
     */
    public void setRemoteAddress(String value) {
        remoteAddress = value;
    }

    /**
     * @see IEvaluationRequest#getLastSuccessfulHeartbeat()
     */
    public long getLastSuccessfulHeartbeat() {
        return lastSuccessfulHeartbeat;
    }

    public void setLastSuccessfulHeartbeat(long hb) {
        lastSuccessfulHeartbeat = hb;
    }

    public EngineResourceInformation getFromResourceInfo() {
        return fromResourceInfo;
    }

    public EngineResourceInformation getToResourceInfo() {
        return toResourceInfo;
    }

    public long getTimestamp() {
        return ts;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Returns the executeObligations.
     *
     * @return the executeObligations.
     */
    public boolean getExecuteObligations() {
        return executeObligations;
    }

    /**
     * Sets the executeObligations
     *
     * @param executeObligations The executeObligations to set.
     */
    public void setExecuteObligations(boolean executeObligations) {
        this.executeObligations = executeObligations;
    }

    /**
     * Obtain the client information manager
     * @return the client information manager.
     */
    public IClientInformationManager getClientInformationManager() {
        return clientInfoManager;
    }

    /**
     * Get the additional policies that should be evaluated along with this request
     */
    public String getAdditionalPoliciesAsPQL() {
        return additionalPoliciesAsPQL;
    }

    /**
     * Get whether or not we should ignore the builtin policies
     */
    public boolean getIgnoreBuiltinPolicies() {
        return ignoreBuiltinPolicies;
    }

    /**
     * Obtain the resource content manager
     */
    public IContentAnalysisManager getContentAnalysisManager() {
        return contentAnalysisManager;
    }

    /**
     * Sets the sent-to position.
     * @param n the new sent-to position.
     */
    public void setSentToPosition(int n) {
        sentToPosition = n;
    }

    /**
     * Gets the current sent-to position.
     * @return the current sent-to position.
     */
    public int getSentToPosition() {
        return sentToPosition;
    }

    /**
     * Resets the sent-to position back to its original value of -1.
     */
    public void resetSentToPosition() {
        sentToPosition = -1;
    }

    /**
     * Marks the current sent-to position applicable to the policy with the given name.
     * @param policyName the name of the policy.
     */
    public void addSentToMatchForCurrentPosition(String policyName) {
        if (sentToPosition >= 0 && sentToPosition < sentTo.length) {
            if (sentToMatches.containsKey(policyName)) {
                sentToMatches.get(policyName).set(sentToPosition);
            } else {
                BitSet bs = new BitSet(sentTo.length);
                bs.set(sentToPosition);
                sentToMatches.put(policyName, bs);
            }
        }
    }

    /**
     * Gets the subject IDs that match the specific policy.
     *
     * @param policyName the name of the policy the matching sent-to IDs for which
     * need to be retrieved.
     * @return A list of IDSubject matched for the specified policy.
     */
    public List<IDSubject> getSentToMatchesForPolicy(String policyName) {
        List<IDSubject> res = new ArrayList<IDSubject>();
        BitSet bs = sentToMatches.get(policyName);
        if (bs != null) {
            for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
                res.add(sentTo[i]);
            }
        }
        return res;
    }

    public void setServiceProviderManager(IServiceProviderManager serviceProviderManager) {
        this.serviceProviderManager = serviceProviderManager;
    }

    public IServiceProviderManager getServiceProviderManager() {
        return serviceProviderManager;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EvaluationRequest)) {
            return false;
        }
        EvaluationRequest er = (EvaluationRequest)other;
        return requestId.equals(er.requestId);
    }

    @Override
    public int hashCode() {
        return requestId.hashCode();
    }

    public void setCached(String key, Object value) {
        cache.put(key, value);
    }

    public Object getCached(String key) {
        return cache.get(key);
    }

}
