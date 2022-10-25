
package com.nextlabs.rms.destiny.services.management.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * 				Agent Profile Info.  A Profile Info contains all elements of a Profile that are modifiable (e.g not ID, created date, modified date).
 * 			
 * 
 * <p>Java class for AgentProfileInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AgentProfileInfo"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://bluejungle.com/destiny/services/management/types}BaseProfileInfo"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="hookAllProc" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="hookSystemProc" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="logViewingEnabled" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="trayIconEnabled" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="hookedApplications" type="{http://bluejungle.com/destiny/services/management/types}ApplicationList" minOccurs="0"/&gt;
 *         &lt;element name="protectedApplications" type="{http://bluejungle.com/destiny/services/management/types}ApplicationList" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AgentProfileInfo", propOrder = {
    "hookAllProc",
    "hookSystemProc",
    "logViewingEnabled",
    "trayIconEnabled",
    "hookedApplications",
    "protectedApplications"
})
public class AgentProfileInfo
        extends BaseProfileInfo {

    protected boolean hookAllProc;
    protected boolean hookSystemProc;
    protected boolean logViewingEnabled;
    protected boolean trayIconEnabled;
    protected ApplicationList hookedApplications;
    protected ApplicationList protectedApplications;

    /**
     * Gets the value of the hookAllProc property.
     * 
     */
    public boolean isHookAllProc() {
        return hookAllProc;
    }

    /**
     * Sets the value of the hookAllProc property.
     * 
     */
    public void setHookAllProc(boolean value) {
        this.hookAllProc = value;
    }

    /**
     * Gets the value of the hookSystemProc property.
     * 
     */
    public boolean isHookSystemProc() {
        return hookSystemProc;
    }

    /**
     * Sets the value of the hookSystemProc property.
     * 
     */
    public void setHookSystemProc(boolean value) {
        this.hookSystemProc = value;
    }

    /**
     * Gets the value of the logViewingEnabled property.
     * 
     */
    public boolean isLogViewingEnabled() {
        return logViewingEnabled;
    }

    /**
     * Sets the value of the logViewingEnabled property.
     * 
     */
    public void setLogViewingEnabled(boolean value) {
        this.logViewingEnabled = value;
    }

    /**
     * Gets the value of the trayIconEnabled property.
     * 
     */
    public boolean isTrayIconEnabled() {
        return trayIconEnabled;
    }

    /**
     * Sets the value of the trayIconEnabled property.
     * 
     */
    public void setTrayIconEnabled(boolean value) {
        this.trayIconEnabled = value;
    }

    /**
     * Gets the value of the hookedApplications property.
     * 
     * @return
     *     possible object is
     *     {@link ApplicationList }
     *     
     */
    public ApplicationList getHookedApplications() {
        return hookedApplications;
    }

    /**
     * Sets the value of the hookedApplications property.
     * 
     * @param value
     *     allowed object is
     *     {@link ApplicationList }
     *     
     */
    public void setHookedApplications(ApplicationList value) {
        this.hookedApplications = value;
    }

    /**
     * Gets the value of the protectedApplications property.
     * 
     * @return
     *     possible object is
     *     {@link ApplicationList }
     *     
     */
    public ApplicationList getProtectedApplications() {
        return protectedApplications;
    }

    /**
     * Sets the value of the protectedApplications property.
     * 
     * @param value
     *     allowed object is
     *     {@link ApplicationList }
     *     
     */
    public void setProtectedApplications(ApplicationList value) {
        this.protectedApplications = value;
    }

}
