package com.nextlabs.rms.destiny.services.agent;

import com.nextlabs.rms.destiny.services.ServiceNotReadyFault;
import com.nextlabs.rms.destiny.services.UnauthorizedCallerFault;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * This class was generated by Apache CXF 3.0.4
 * 2015-04-13T13:24:04.141+08:00
 * Generated source version: 3.0.4
 *
 */
@WebService(targetNamespace = "http://bluejungle.com/destiny/services/agent", name = "AgentServiceIF")
@XmlSeeAlso({ com.nextlabs.rms.destiny.types.custom_obligations.ObjectFactory.class,
    com.nextlabs.rms.destiny.services.plugin.types.ObjectFactory.class,
    com.nextlabs.rms.destiny.version.types.ObjectFactory.class,
    com.nextlabs.rms.destiny.types.shared_folder.ObjectFactory.class,
    com.nextlabs.rms.destiny.domain.types.ObjectFactory.class,
    com.nextlabs.rms.destiny.services.management.types.ObjectFactory.class,
    com.nextlabs.rms.destiny.services.policy.types.ObjectFactory.class,
    com.nextlabs.rms.destiny.framework.types.ObjectFactory.class,
    com.nextlabs.rms.destiny.services.profile.types.ObjectFactory.class,
    com.nextlabs.rms.destiny.services.agent.types.ObjectFactory.class })
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface AgentServiceIF {

    @WebResult(name = "startupConfiguration", targetNamespace = "http://bluejungle.com/destiny/services/agent", partName = "startupConfiguration")
    @WebMethod
    public com.nextlabs.rms.destiny.services.agent.types.AgentStartupConfiguration registerAgent(
        @WebParam(partName = "registrationData", name = "registrationData") com.nextlabs.rms.destiny.services.agent.types.AgentRegistrationData registrationData)
            throws ServiceNotReadyFault, UnauthorizedCallerFault;

    @WebMethod
    public void acknowledgeUpdates(
        @WebParam(partName = "id", name = "id") java.math.BigInteger id,
        @WebParam(partName = "acknowledgementData", name = "acknowledgementData") com.nextlabs.rms.destiny.services.agent.types.AgentUpdateAcknowledgementData acknowledgementData)
            throws UnauthorizedCallerFault, ServiceNotReadyFault;

    @WebResult(name = "updates", targetNamespace = "http://bluejungle.com/destiny/services/agent", partName = "updates")
    @WebMethod
    public com.nextlabs.rms.destiny.services.agent.types.AgentUpdates checkUpdates(
        @WebParam(partName = "id", name = "id") java.math.BigInteger id,
        @WebParam(partName = "heartbeat", name = "heartbeat") com.nextlabs.rms.destiny.services.agent.types.AgentHeartbeatData heartbeat)
            throws ServiceNotReadyFault, UnauthorizedCallerFault;
}
