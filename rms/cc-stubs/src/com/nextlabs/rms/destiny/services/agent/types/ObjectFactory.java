
package com.nextlabs.rms.destiny.services.agent.types;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.bluejungle.destiny.services.agent.types package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.bluejungle.destiny.services.agent.types
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link AgentHeartbeatData }
     * 
     */
    public AgentHeartbeatData createAgentHeartbeatData() {
        return new AgentHeartbeatData();
    }

    /**
     * Create an instance of {@link AgentStartupConfiguration }
     * 
     */
    public AgentStartupConfiguration createAgentStartupConfiguration() {
        return new AgentStartupConfiguration();
    }

    /**
     * Create an instance of {@link AgentRegistrationData }
     * 
     */
    public AgentRegistrationData createAgentRegistrationData() {
        return new AgentRegistrationData();
    }

    /**
     * Create an instance of {@link AgentUpdates }
     * 
     */
    public AgentUpdates createAgentUpdates() {
        return new AgentUpdates();
    }

}
