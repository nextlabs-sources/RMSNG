
package com.nextlabs.rms.destiny.services.agent.types;

import com.nextlabs.rms.destiny.services.policy.types.DeploymentRequest;
import com.nextlabs.rms.destiny.services.profile.types.AgentProfileStatusData;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for AgentUpdateAcknowledgementData complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="AgentUpdateAcknowledgementData"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="profileStatus" type="{http://bluejungle.com/destiny/services/profile/types}AgentProfileStatusData" minOccurs="0"/&gt;
 *         &lt;element name="policyAssemblyStatus" type="{http://bluejungle.com/destiny/services/policy/types}DeploymentRequest" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AgentUpdateAcknowledgementData", propOrder = {
    "profileStatus",
    "policyAssemblyStatus"
})
public class AgentUpdateAcknowledgementData {

    protected AgentProfileStatusData profileStatus;
    protected DeploymentRequest policyAssemblyStatus;

    /**
     * Gets the value of the profileStatus property.
     *
     * @return
     *     possible object is
     *     {@link AgentProfileStatusData }
     *
     */
    public AgentProfileStatusData getProfileStatus() {
        return profileStatus;
    }

    /**
     * Sets the value of the profileStatus property.
     *
     * @param value
     *     allowed object is
     *     {@link AgentProfileStatusData }
     *
     */
    public void setProfileStatus(AgentProfileStatusData value) {
        this.profileStatus = value;
    }

    /**
     * Gets the value of the policyAssemblyStatus property.
     *
     * @return
     *     possible object is
     *     {@link DeploymentRequest }
     *
     */
    public DeploymentRequest getPolicyAssemblyStatus() {
        return policyAssemblyStatus;
    }

    /**
     * Sets the value of the policyAssemblyStatus property.
     *
     * @param value
     *     allowed object is
     *     {@link DeploymentRequest }
     *
     */
    public void setPolicyAssemblyStatus(DeploymentRequest value) {
        this.policyAssemblyStatus = value;
    }
}
