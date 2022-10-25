/*
 * Created on Dec 16, 2014
 *
 * All sources, binaries and HTML pages (C) copyright 2014 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author ichiang
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/domain/src/java/main/com/nextlabs/domain/log/PolicyActivityInfoV5.java#2 $:
 */
package com.bluejungle.domain.log;

import com.bluejungle.domain.policydecision.PolicyDecisionEnumType;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.utils.DynamicAttributes;
import com.bluejungle.framework.utils.IPair;
import com.bluejungle.framework.utils.Pair;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class PolicyActivityInfoV5 implements Externalizable {

    public static final String FROM_RESOURCE_ATTRIBUTES_TAG = "RF";
    public static final String RECIPIENTS_ATTRIBUTES_TAG = "RC";
    public static final String USER_ATTRIBUTES_TAG = "SU";

    FromResourceInformation fromResourceInfo;
    ToResourceInformation toResourceInfo;
    long hostId;
    String hostIP;
    String hostName;
    long userId;
    String userName;
    long applicationId;
    String applicationName;
    String action;
    PolicyDecisionEnumType policyDecision;
    long decisionRequestId;
    long ts;
    int level;
    Map<String, DynamicAttributes> attributesMap;
    List<IPair<String, String>> evaluationAnnotations;

    PolicyActivityInfoV5() {
    }

    public PolicyActivityInfoV5(PolicyActivityInfoV5 orig) {
        this(orig.fromResourceInfo,
                orig.toResourceInfo,
                orig.userName,
                orig.userId,
                orig.hostName,
                orig.hostIP,
                orig.hostId,
                orig.applicationName,
                orig.applicationId,
                orig.action,
                orig.policyDecision,
                orig.decisionRequestId,
                orig.ts,
                orig.level,
                new HashMap<String, DynamicAttributes>(),
                new ArrayList<IPair<String, String>>());

        // sharing mutable classes is risky. Get our own copies.
        attributesMap.putAll(orig.attributesMap);
        evaluationAnnotations.addAll(orig.evaluationAnnotations);
    }

    public PolicyActivityInfoV5(FromResourceInformation fromResourceInfo,
        ToResourceInformation toResourceInfo,
        String userName,
        long userId,
        String hostName,
        String hostIP,
        long hostId,
        String applicationName,
        long applicationId,
        String action,
        PolicyDecisionEnumType policyDecision,
        long decisionRequestId,
        long ts,
        int level,
        Map<String, DynamicAttributes> attributesMap,
        List<IPair<String, String>> evaluationAnnotations) {
        super();

        this.fromResourceInfo = fromResourceInfo;
        checkResourceInfoFormat(this.fromResourceInfo); // Fix to Bug 9258
        this.toResourceInfo = toResourceInfo;
        checkResourceInfoFormat(this.toResourceInfo); // Fix to Bug 9258
        this.userName = userName;
        this.userId = userId;
        this.hostName = hostName;
        this.hostIP = hostIP;
        this.hostId = hostId;
        this.applicationName = applicationName;
        this.applicationId = applicationId;
        this.action = action;
        this.policyDecision = policyDecision;
        this.decisionRequestId = decisionRequestId;
        this.ts = ts;
        this.level = level;

        if (attributesMap == null) {
            this.attributesMap = new HashMap<String, DynamicAttributes>();
        } else {
            this.attributesMap = attributesMap;
        }

        if (evaluationAnnotations == null) {
            this.evaluationAnnotations = new ArrayList<IPair<String, String>>();
        } else {
            this.evaluationAnnotations = evaluationAnnotations;
        }
    }

    public String getAction() {
        return action;
    }

    public long getApplicationId() {
        return applicationId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public long getDecisionRequestId() {
        return decisionRequestId;
    }

    public ResourceInformation getFromResourceInfo() {
        return fromResourceInfo;
    }

    public long getHostId() {
        return hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public String getHostIP() {
        return hostIP;
    }

    public PolicyDecisionEnumType getPolicyDecision() {
        return policyDecision;
    }

    public List<IPair<String, String>> getEvaluationAnnotations() {
        return evaluationAnnotations;
    }

    public ResourceInformation getToResourceInfo() {
        return toResourceInfo;
    }

    public long getTs() {
        return ts;
    }

    public long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public int getLevel() {
        return level;
    }

    public Map<String, DynamicAttributes> getAttributesMap() {
        return this.attributesMap;
    }

    /**
     * Sets the customAttr
     * @param customAttr The customAttr to set.
     */
    public void setAttributesMap(Map<String, DynamicAttributes> attributesMap) {
        if (attributesMap == null) {
            throw new NullPointerException("attributesMap");
        }
        this.attributesMap = attributesMap;
    }

    public void addAttributes(String attrTpye, DynamicAttributes attribute) {
        if (attrTpye == null) {
            throw new NullPointerException("attrTpye");
        }
        if (attribute == null) {
            throw new NullPointerException("attribute");
        }
        if (attributesMap.containsKey(attrTpye)) {
            DynamicAttributes tempAttr = attributesMap.get(attrTpye);
            tempAttr.putAll(attribute);
            this.attributesMap.put(attrTpye, tempAttr);
        } else {
            this.attributesMap.put(attrTpye, attribute);
        }
    }

    public void addAttribute(String attrTpye, String key, String value) {
        if (attrTpye == null) {
            throw new NullPointerException("attrTpye");
        }
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        if (attributesMap.containsKey(attrTpye)) {
            DynamicAttributes tempAttr = attributesMap.get(attrTpye);
            tempAttr.add(key, value);
            this.attributesMap.put(attrTpye, tempAttr);
        } else {
            DynamicAttributes newAttribute = new DynamicAttributes();
            newAttribute.add(key, value);
            this.attributesMap.put(attrTpye, newAttribute);
        }
    }

    public void addAttribute(String attrTpye, String key, IEvalValue value) {
        if (attrTpye == null) {
            throw new NullPointerException("attrTpye");
        }
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        if (attributesMap.containsKey(attrTpye)) {
            DynamicAttributes tempAttr = attributesMap.get(attrTpye);
            tempAttr.put(key, value);
            this.attributesMap.put(attrTpye, tempAttr);
        } else {
            DynamicAttributes newAttribute = new DynamicAttributes();
            newAttribute.put(key, value);
            this.attributesMap.put(attrTpye, newAttribute);
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        fromResourceInfo.writeExternal(out);
        if (toResourceInfo != null) {
            out.writeBoolean(true);
            toResourceInfo.writeExternal(out);
        } else {
            out.writeBoolean(false);
        }
        out.writeLong(hostId);
        out.writeUTF(hostIP);
        out.writeUTF(hostName);
        out.writeLong(userId);
        out.writeUTF(userName);
        out.writeLong(applicationId);
        out.writeUTF(applicationName);
        out.writeUTF(action);
        out.writeInt(policyDecision.getType());
        out.writeLong(decisionRequestId);
        out.writeLong(ts);
        out.writeInt(level);

        out.writeLong(attributesMap.size());
        for (String attrType : attributesMap.keySet()) {
            GenericAttributeExternalizer.writeAttributes(out, attrType, attributesMap.get(attrType));
        }

        out.writeLong(evaluationAnnotations.size());
        for (IPair<String, String> annotation : evaluationAnnotations) {
            out.writeUTF(annotation.first());
            out.writeUTF(annotation.second());
        }
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        fromResourceInfo = new FromResourceInformation();
        fromResourceInfo.readExternal(in);
        checkResourceInfoFormat(fromResourceInfo); // Fix to Bug 9258
        boolean existsToInfo = in.readBoolean();
        if (existsToInfo) {
            toResourceInfo = new ToResourceInformation();
            toResourceInfo.readExternal(in);
            checkResourceInfoFormat(toResourceInfo); // Fix to Bug 9258
        } else {
            toResourceInfo = null;
        }
        hostId = in.readLong();
        hostIP = in.readUTF();
        hostName = in.readUTF();
        userId = in.readLong();
        userName = in.readUTF();
        applicationId = in.readLong();
        applicationName = in.readUTF();
        action = in.readUTF();
        policyDecision = PolicyDecisionEnumType.getPolicyDecisionEnum(in.readInt());
        decisionRequestId = in.readLong();
        ts = in.readLong();
        level = in.readInt();

        long attributes = in.readLong();
        attributesMap = new HashMap<String, DynamicAttributes>();
        for (int i = 0; i < attributes; i++) {
            attributesMap.putAll(GenericAttributeExternalizer.readAttributes(in));
        }

        long annotations = in.readLong();
        evaluationAnnotations = new ArrayList<IPair<String, String>>();
        for (long i = 0; i < annotations; i++) {
            String f = in.readUTF();
            String s = in.readUTF();
            evaluationAnnotations.add(new Pair<String, String>(f, s));
        }
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof PolicyActivityInfoV5)) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        PolicyActivityInfoV5 info = (PolicyActivityInfoV5)obj;
        boolean toResourceEquals;
        if (toResourceInfo == null) {
            toResourceEquals = (info.toResourceInfo == null);
        } else {
            toResourceEquals = toResourceInfo.equals(info.toResourceInfo);
        }

        return (fromResourceInfo.equals(info.fromResourceInfo) &&
                toResourceEquals &&
                hostId == info.hostId &&
                hostIP.equals(info.hostIP) &&
                hostName.equals(info.hostName) &&
                userId == info.userId &&
                userName.equals(info.userName) &&
                applicationId == info.applicationId &&
                applicationName.equals(info.applicationName) &&
                action.equals(info.action) &&
                policyDecision.equals(info.policyDecision) &&
                decisionRequestId == info.decisionRequestId &&
                ts == info.ts &&
                level == info.level &&
        //                attributesEqual(info.getCustomAttr(), getCustomAttr()) &&
        getEvaluationAnnotations().equals(info.getEvaluationAnnotations()));
    }

    private static boolean attributesEqual(DynamicAttributes attrs, DynamicAttributes otherAttrs) {
        if (attrs == null || otherAttrs == null) {
            return (attrs == otherAttrs);
        } else {
            if (attrs.size() == otherAttrs.size()) {
                for (Map.Entry<String, IEvalValue> ca : otherAttrs.entrySet()) {
                    if (!attrs.containsKey(ca.getKey())) {
                        return false;
                    }
                    if (ca.getValue() != null) {
                        if (!ca.getValue().equals(attrs.get(ca.getKey()))) {
                            return false;
                        }
                    } else {
                        if (attrs.get(ca.getKey()) != null) {
                            return false;
                        }
                    }
                }
            } else {
                return false;
            }
        }

        return true;
    }

    public String toString() {
        StringBuffer rv = new StringBuffer("PolicyActivityInfoV5[");
        rv.append("fromResource: " + fromResourceInfo);
        rv.append(", toResource: " + toResourceInfo);
        rv.append(", hostId: " + hostId);
        rv.append(", hostIP: " + hostIP);
        rv.append(", hostName: " + hostName);
        rv.append(", userId: " + userId);
        rv.append(", userName: " + userName);
        rv.append(", applicationId: " + applicationId);
        rv.append(", applicationName: " + applicationName);
        rv.append(", action: " + action);
        rv.append(", policyDecision: " + policyDecision);
        rv.append(", decisionRequestId: " + decisionRequestId);
        rv.append(", ts: " + ts);
        rv.append(", level: " + level);
        rv.append(", custom attributes: ");
        if (getAttributesMap() == null) {
            rv.append(" <EMPTY> ");
        } else {
            for (Map.Entry<String, DynamicAttributes> entry : getAttributesMap().entrySet()) {
                String attributeType = entry.getKey();
                for (Map.Entry<String, IEvalValue> attributes : entry.getValue().entrySet()) {
                    rv.append(attributeType + ": " + "{" + attributes.getKey() + ", " + attributes.getValue().getValue() + "}");
                }
            }

            rv.append("]");
        }
        rv.append(", annotations: ");
        for (IPair<String, String> annotation : evaluationAnnotations) {
            rv.append(annotation.first() + "=" + annotation.second());
        }
        return rv.toString();
    }

    /* A private function created for fix to Bug 9258 - ftpe: the 'file' and 'resource' in report are not resonable
     * If the resource name starts with file:// (more than 2 slashes) and continues with some_url://, cut the first file://
     * For example, "file:///ftp://gnu.org" becomes "ftp://gnu.org"
     */
    static final String partialPatternString = "file:/{2,}"; // e.g. "file:///"
    static final String wholePatternString = partialPatternString + "\\S+://.*"; // e.g. "file:///ftp://..."
    static final Pattern partialPattern = Pattern.compile(partialPatternString);
    static final Pattern wholePattern = Pattern.compile(wholePatternString);

    private void checkResourceInfoFormat(ResourceInformation resInfo) {
        if (resInfo == null)
            return;

        String name = resInfo.getName();
        if (name != null && wholePattern.matcher(name).matches() == true) {
            resInfo.setName(partialPattern.matcher(name).replaceFirst(""));
        }
    }
}
