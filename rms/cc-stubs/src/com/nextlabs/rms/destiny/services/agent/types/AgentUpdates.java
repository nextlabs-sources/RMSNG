
package com.nextlabs.rms.destiny.services.agent.types;

import com.nextlabs.rms.destiny.services.management.types.AgentProfileDTO;
import com.nextlabs.rms.destiny.services.management.types.CommProfileDTO;
import com.nextlabs.rms.destiny.services.plugin.types.AgentPluginData;
import com.nextlabs.rms.destiny.types.custom_obligations.CustomObligationsData;
import com.nextlabs.rms.destiny.types.shared_folder.SharedFolderData;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for AgentUpdates complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AgentUpdates"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="commProfile" type="{http://bluejungle.com/destiny/services/management/types}CommProfileDTO" minOccurs="0"/&gt;
 *         &lt;element name="agentProfile" type="{http://bluejungle.com/destiny/services/management/types}AgentProfileDTO" minOccurs="0"/&gt;
 *         &lt;element name="policyDeploymentBundle" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="serverBusy" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&gt;
 *         &lt;element name="sharedFolderData" type="{http://bluejungle.com/destiny/types/shared_folder}SharedFolderData" minOccurs="0"/&gt;
 *         &lt;element name="customObligationsData" type="{http://bluejungle.com/destiny/types/custom_obligations}CustomObligationsData" minOccurs="0"/&gt;
 *         &lt;element name="pluginData" type="{http://bluejungle.com/destiny/services/plugin/types}AgentPluginData" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AgentUpdates", propOrder = {
    "commProfile",
    "agentProfile",
    "policyDeploymentBundle",
    "serverBusy",
    "sharedFolderData",
    "customObligationsData",
    "pluginData"
})
public class AgentUpdates {

    protected CommProfileDTO commProfile;
    protected AgentProfileDTO agentProfile;
    protected String policyDeploymentBundle;
    @XmlElement(defaultValue = "false")
    protected Boolean serverBusy;
    protected SharedFolderData sharedFolderData;
    protected CustomObligationsData customObligationsData;
    protected AgentPluginData pluginData;

    /**
     * Gets the value of the commProfile property.
     * 
     * @return
     *     possible object is
     *     {@link CommProfileDTO }
     *     
     */
    public CommProfileDTO getCommProfile() {
        return commProfile;
    }

    /**
     * Sets the value of the commProfile property.
     * 
     * @param value
     *     allowed object is
     *     {@link CommProfileDTO }
     *     
     */
    public void setCommProfile(CommProfileDTO value) {
        this.commProfile = value;
    }

    /**
     * Gets the value of the agentProfile property.
     * 
     * @return
     *     possible object is
     *     {@link AgentProfileDTO }
     *     
     */
    public AgentProfileDTO getAgentProfile() {
        return agentProfile;
    }

    /**
     * Sets the value of the agentProfile property.
     * 
     * @param value
     *     allowed object is
     *     {@link AgentProfileDTO }
     *     
     */
    public void setAgentProfile(AgentProfileDTO value) {
        this.agentProfile = value;
    }

    /**
     * Gets the value of the policyDeploymentBundle property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPolicyDeploymentBundle() {
        return policyDeploymentBundle;
    }

    /**
     * Sets the value of the policyDeploymentBundle property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPolicyDeploymentBundle(String value) {
        this.policyDeploymentBundle = value;
    }

    /**
     * Gets the value of the serverBusy property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isServerBusy() {
        return serverBusy;
    }

    /**
     * Sets the value of the serverBusy property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setServerBusy(Boolean value) {
        this.serverBusy = value;
    }

    /**
     * Gets the value of the sharedFolderData property.
     * 
     * @return
     *     possible object is
     *     {@link SharedFolderData }
     *     
     */
    public SharedFolderData getSharedFolderData() {
        return sharedFolderData;
    }

    /**
     * Sets the value of the sharedFolderData property.
     * 
     * @param value
     *     allowed object is
     *     {@link SharedFolderData }
     *     
     */
    public void setSharedFolderData(SharedFolderData value) {
        this.sharedFolderData = value;
    }

    /**
     * Gets the value of the customObligationsData property.
     * 
     * @return
     *     possible object is
     *     {@link CustomObligationsData }
     *     
     */
    public CustomObligationsData getCustomObligationsData() {
        return customObligationsData;
    }

    /**
     * Sets the value of the customObligationsData property.
     * 
     * @param value
     *     allowed object is
     *     {@link CustomObligationsData }
     *     
     */
    public void setCustomObligationsData(CustomObligationsData value) {
        this.customObligationsData = value;
    }

    /**
     * Gets the value of the pluginData property.
     * 
     * @return
     *     possible object is
     *     {@link AgentPluginData }
     *     
     */
    public AgentPluginData getPluginData() {
        return pluginData;
    }

    /**
     * Sets the value of the pluginData property.
     * 
     * @param value
     *     allowed object is
     *     {@link AgentPluginData }
     *     
     */
    public void setPluginData(AgentPluginData value) {
        this.pluginData = value;
    }

}
