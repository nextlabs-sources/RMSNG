
package com.nextlabs.rms.destiny.services.management.types;

import com.nextlabs.rms.destiny.domain.types.AgentTypeDTO;
import com.nextlabs.rms.destiny.framework.types.TimeIntervalDTO;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 *  Communication Profile Data Transer Object. 
 * 				
 * 
 * <p>Java class for CommProfileDTO complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CommProfileDTO"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://bluejungle.com/destiny/services/management/types}BaseProfileDTO"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="DABSLocation" type="{http://www.w3.org/2001/XMLSchema}anyURI"/&gt;
 *         &lt;element name="agentType" type="{http://bluejungle.com/domain/types}AgentTypeDTO"/&gt;
 *         &lt;element name="heartBeatFrequency" type="{http://bluejungle.com/destiny/framework/types}time-interval-DTO"/&gt;
 *         &lt;element name="logLimit" type="{http://www.w3.org/2001/XMLSchema}unsignedShort" minOccurs="0"/&gt;
 *         &lt;element name="logFrequency" type="{http://bluejungle.com/destiny/framework/types}time-interval-DTO"/&gt;
 *         &lt;element name="pushEnabled" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="defaultPushPort" type="{http://www.w3.org/2001/XMLSchema}unsignedShort"/&gt;
 *         &lt;element name="passwordHash" type="{http://www.w3.org/2001/XMLSchema}base64Binary"/&gt;
 *         &lt;element name="password" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="currentActivityJournalingSettings" type="{http://bluejungle.com/destiny/services/management/types}ActivityJournalingSettingsDTO"/&gt;
 *         &lt;element name="customActivityJournalingSettings" type="{http://bluejungle.com/destiny/services/management/types}ActivityJournalingSettingsDTO"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CommProfileDTO", propOrder = {
    "dabsLocation",
    "agentType",
    "heartBeatFrequency",
    "logLimit",
    "logFrequency",
    "pushEnabled",
    "defaultPushPort",
    "passwordHash",
    "password",
    "currentActivityJournalingSettings",
    "customActivityJournalingSettings"
})
public class CommProfileDTO
        extends BaseProfileDTO {

    @XmlElement(name = "DABSLocation", required = true)
    @XmlSchemaType(name = "anyURI")
    protected String dabsLocation;
    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected AgentTypeDTO agentType;
    @XmlElement(required = true)
    protected TimeIntervalDTO heartBeatFrequency;
    @XmlSchemaType(name = "unsignedShort")
    protected Integer logLimit;
    @XmlElement(required = true)
    protected TimeIntervalDTO logFrequency;
    protected boolean pushEnabled;
    @XmlSchemaType(name = "unsignedShort")
    protected int defaultPushPort;
    @XmlElement(required = true)
    protected byte[] passwordHash;
    @XmlElement(required = true, nillable = true)
    protected String password;
    @XmlElement(required = true)
    protected ActivityJournalingSettingsDTO currentActivityJournalingSettings;
    @XmlElement(required = true)
    protected ActivityJournalingSettingsDTO customActivityJournalingSettings;

    /**
     * Gets the value of the dabsLocation property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDABSLocation() {
        return dabsLocation;
    }

    /**
     * Sets the value of the dabsLocation property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDABSLocation(String value) {
        this.dabsLocation = value;
    }

    /**
     * Gets the value of the agentType property.
     * 
     * @return
     *     possible object is
     *     {@link AgentTypeDTO }
     *     
     */
    public AgentTypeDTO getAgentType() {
        return agentType;
    }

    /**
     * Sets the value of the agentType property.
     * 
     * @param value
     *     allowed object is
     *     {@link AgentTypeDTO }
     *     
     */
    public void setAgentType(AgentTypeDTO value) {
        this.agentType = value;
    }

    /**
     * Gets the value of the heartBeatFrequency property.
     * 
     * @return
     *     possible object is
     *     {@link TimeIntervalDTO }
     *     
     */
    public TimeIntervalDTO getHeartBeatFrequency() {
        return heartBeatFrequency;
    }

    /**
     * Sets the value of the heartBeatFrequency property.
     * 
     * @param value
     *     allowed object is
     *     {@link TimeIntervalDTO }
     *     
     */
    public void setHeartBeatFrequency(TimeIntervalDTO value) {
        this.heartBeatFrequency = value;
    }

    /**
     * Gets the value of the logLimit property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getLogLimit() {
        return logLimit;
    }

    /**
     * Sets the value of the logLimit property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setLogLimit(Integer value) {
        this.logLimit = value;
    }

    /**
     * Gets the value of the logFrequency property.
     * 
     * @return
     *     possible object is
     *     {@link TimeIntervalDTO }
     *     
     */
    public TimeIntervalDTO getLogFrequency() {
        return logFrequency;
    }

    /**
     * Sets the value of the logFrequency property.
     * 
     * @param value
     *     allowed object is
     *     {@link TimeIntervalDTO }
     *     
     */
    public void setLogFrequency(TimeIntervalDTO value) {
        this.logFrequency = value;
    }

    /**
     * Gets the value of the pushEnabled property.
     * 
     */
    public boolean isPushEnabled() {
        return pushEnabled;
    }

    /**
     * Sets the value of the pushEnabled property.
     * 
     */
    public void setPushEnabled(boolean value) {
        this.pushEnabled = value;
    }

    /**
     * Gets the value of the defaultPushPort property.
     * 
     */
    public int getDefaultPushPort() {
        return defaultPushPort;
    }

    /**
     * Sets the value of the defaultPushPort property.
     * 
     */
    public void setDefaultPushPort(int value) {
        this.defaultPushPort = value;
    }

    /**
     * Gets the value of the passwordHash property.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getPasswordHash() {
        return passwordHash;
    }

    /**
     * Sets the value of the passwordHash property.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setPasswordHash(byte[] value) {
        this.passwordHash = value;
    }

    /**
     * Gets the value of the password property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the value of the password property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassword(String value) {
        this.password = value;
    }

    /**
     * Gets the value of the currentActivityJournalingSettings property.
     * 
     * @return
     *     possible object is
     *     {@link ActivityJournalingSettingsDTO }
     *     
     */
    public ActivityJournalingSettingsDTO getCurrentActivityJournalingSettings() {
        return currentActivityJournalingSettings;
    }

    /**
     * Sets the value of the currentActivityJournalingSettings property.
     * 
     * @param value
     *     allowed object is
     *     {@link ActivityJournalingSettingsDTO }
     *     
     */
    public void setCurrentActivityJournalingSettings(ActivityJournalingSettingsDTO value) {
        this.currentActivityJournalingSettings = value;
    }

    /**
     * Gets the value of the customActivityJournalingSettings property.
     * 
     * @return
     *     possible object is
     *     {@link ActivityJournalingSettingsDTO }
     *     
     */
    public ActivityJournalingSettingsDTO getCustomActivityJournalingSettings() {
        return customActivityJournalingSettings;
    }

    /**
     * Sets the value of the customActivityJournalingSettings property.
     * 
     * @param value
     *     allowed object is
     *     {@link ActivityJournalingSettingsDTO }
     *     
     */
    public void setCustomActivityJournalingSettings(ActivityJournalingSettingsDTO value) {
        this.customActivityJournalingSettings = value;
    }

}
