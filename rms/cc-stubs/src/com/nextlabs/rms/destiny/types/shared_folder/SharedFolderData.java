
package com.nextlabs.rms.destiny.types.shared_folder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for SharedFolderData complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SharedFolderData"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="aliasList" type="{http://bluejungle.com/destiny/types/shared_folder}SharedFolderAliasList"/&gt;
 *         &lt;element name="cookie" type="{http://bluejungle.com/destiny/types/shared_folder}SharedFolderDataCookie"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SharedFolderData", propOrder = {
    "aliasList",
    "cookie"
})
public class SharedFolderData {

    @XmlElement(required = true)
    protected SharedFolderAliasList aliasList;
    @XmlElement(required = true)
    protected SharedFolderDataCookie cookie;

    /**
     * Gets the value of the aliasList property.
     * 
     * @return
     *     possible object is
     *     {@link SharedFolderAliasList }
     *     
     */
    public SharedFolderAliasList getAliasList() {
        return aliasList;
    }

    /**
     * Sets the value of the aliasList property.
     * 
     * @param value
     *     allowed object is
     *     {@link SharedFolderAliasList }
     *     
     */
    public void setAliasList(SharedFolderAliasList value) {
        this.aliasList = value;
    }

    /**
     * Gets the value of the cookie property.
     * 
     * @return
     *     possible object is
     *     {@link SharedFolderDataCookie }
     *     
     */
    public SharedFolderDataCookie getCookie() {
        return cookie;
    }

    /**
     * Sets the value of the cookie property.
     * 
     * @param value
     *     allowed object is
     *     {@link SharedFolderDataCookie }
     *     
     */
    public void setCookie(SharedFolderDataCookie value) {
        this.cookie = value;
    }

}
