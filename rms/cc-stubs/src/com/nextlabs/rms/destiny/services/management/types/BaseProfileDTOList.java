
package com.nextlabs.rms.destiny.services.management.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * 				Base Element for a Profile List.  Specific profile (Agent, User, Communication) will extend
 * 			
 * 
 * <p>Java class for BaseProfileDTOList complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="BaseProfileDTOList"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BaseProfileDTOList")
@XmlSeeAlso({
    CommProfileDTOList.class,
    AgentProfileDTOList.class
})
public class BaseProfileDTOList {

}
