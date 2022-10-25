/*
 * Created on Feb 8, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2004 by Blue Jungle Inc.,
 * Redwood City CA, Ownership remains with Blue Jungle Inc, All rights reserved
 * worldwide.
 */
package com.bluejungle.pf.domain.epicenter.evaluation;

import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.utils.DynamicAttributes;
import com.bluejungle.pf.domain.destiny.serviceprovider.IServiceProviderManager;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;
import com.bluejungle.pf.domain.destiny.subject.SubjectType;
import com.bluejungle.pf.domain.epicenter.action.IAction;
import com.bluejungle.pf.domain.epicenter.resource.IResource;
import com.bluejungle.pf.engine.destiny.EngineResourceInformation;
import com.bluejungle.pf.engine.destiny.IClientInformationManager;
import com.bluejungle.pf.engine.destiny.IContentAnalysisManager;
import com.bluejungle.pf.engine.destiny.IEngineSubjectResolver;

/**
 * @author sasha
 * @version $Id:
 *          //depot/main/Destiny/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/epicenter/evaluation/IEvaluationRequest.java#5 $:
 */

public interface IEvaluationRequest extends IArguments {

    /**
     * Returns the action.
     *
     * @return the action.
     */
    IAction getAction();

    /**
     * Returns the application.
     *
     * @return the application.
     */
    IDSubject getApplication();

    /**
     * Returns the fromResource.
     *
     * @return the fromResource.
     */
    IResource getFromResource();

    /**
     * Returns the fromResourceInfo
     *
     * @return fromResourceInfo.
     */
    EngineResourceInformation getFromResourceInfo();

    /**
     * Returns the host.
     *
     * @return the host.
     */
    IDSubject getHost();

    /**
     * Returns the host IP Address
     *
     * @return the host IP Address
     */
    String getHostIPAddress();

    /**
     * Returns the toResource.
     *
     * @return the toResource.
     */
    IResource getToResource();

    /**
     * Returns the toResourceInfo
     *
     * @return toResourceInfo.
     */
    EngineResourceInformation getToResourceInfo();

    /**
     * Returns the user.
     *
     * @return the user.
     */
    IDSubject getUser();

    /**
     * Returns the logged in user.
     *
     * @return the user.
     */
    IDSubject getLoggedInUser();

    /**
     * Given a <code>SubjectType</code>, returns a user,
     * a host, or an application.
     * @param type the type of the subject to return.
     * @return a user, a host, or an application from the request.
     */
    IDSubject getSubjectByType(SubjectType type);

    /**
     * Returns the user name
     *
     * @return the user name
     */
    String getUserName();

    /**
     * Returns the requestId, may be null
     *
     * @return the requestId.
     */
    Long getRequestId();

    /**
     * sets the request id
     *
     * @param requestId
     *            request id
     *
     */
    void setRequestId(Long requestId);

    /**
     * Returns the IP address of the remote host if the operation
     * is initiated through a remote access; null otherwise.
     *
     * @return the IP address of the remote host if the operation
     * is initiated through a remote access; null otherwise.
     */
    String getRemoteAddress();

    /**
     * Returns a bag of environment attributes
     */
    DynamicAttributes getEnvironment();

    /**
     * Set the environment attributes
     */
    void setEnvironment(DynamicAttributes env);

    /**
     * Returns the toSubject
     *
     * @return the toSubject
     */
    IDSubject[] getSentTo();

    /**
     * Sets the toSubject
     *
     * @param toSubject
     */
    void setSentTo(IDSubject[] toSubjects);

    /**
     * Returns the time of the request
     */
    long getTimestamp();

    /**
     * Returns the time of the last successful heartbeat
     *
     */
    long getLastSuccessfulHeartbeat();

    /**
     * Sets the last successful heartbeat time
     *
     * @param hb
     */
    void setLastSuccessfulHeartbeat(long hb);

    /**
     * Gets the content analysis manager
     */
    IContentAnalysisManager getContentAnalysisManager();

    /**
     * Returns the logging level
     *
     * @return level
     */
    int getLevel();

    /**
     * Returns the subject resolver
     *
     * @return the subject resolver
     */
    IEngineSubjectResolver getSubjectResolver();

    /**
     * Returns the client information manager
     *
     * @return the client information manager
     */
    IClientInformationManager getClientInformationManager();

    /**
     * Returns the service provider manager
     *
     * @return the service provider manager
     */
    IServiceProviderManager getServiceProviderManager();

    /**
     * Returns whether or not we should execute obligations
     */
    boolean getExecuteObligations();

    /**
     * Returns the (optional) additional policies in this request;
     */
    String getAdditionalPoliciesAsPQL();

    /**
     * Returns whether or not the additional policies override the bundle policies
     */
    boolean getIgnoreBuiltinPolicies();
}
