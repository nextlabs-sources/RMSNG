package com.bluejungle.pf.domain.destiny.subject;

/*
 * All sources, binaries and HTML pages (C) copyright 2004 by NextLabs Inc.,
 * Redwood City CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 *
 * @author sasha
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/subject/SubjectAttribute.java#2 $:
 */

import com.bluejungle.framework.expressions.Constant;
import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IExpression;
import com.bluejungle.framework.expressions.IMultivalue;
import com.bluejungle.framework.expressions.IRelation;
import com.bluejungle.framework.expressions.Multivalue;
import com.bluejungle.framework.expressions.Relation;
import com.bluejungle.framework.expressions.RelationOp;
import com.bluejungle.framework.expressions.ValueType;
import com.bluejungle.framework.utils.MultipartKey;
import com.bluejungle.pf.domain.destiny.common.SpecAttribute;
import com.bluejungle.pf.domain.destiny.serviceprovider.IServiceProviderManager;
import com.bluejungle.pf.domain.destiny.serviceprovider.ISubjectAttributeProvider;
import com.bluejungle.pf.domain.destiny.serviceprovider.ServiceProviderException;
import com.bluejungle.pf.domain.epicenter.common.SpecType;
import com.bluejungle.pf.domain.epicenter.evaluation.IEvaluationRequest;
import com.bluejungle.pf.engine.destiny.EvaluationRequest;
import com.bluejungle.pf.engine.destiny.IClientInformationManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This class contains implementations of subject attributes.
 */

public abstract class SubjectAttribute extends SpecAttribute implements IDSubjectAttribute {

    private static final String NAME_ATTR_NAME = "name";
    private static final String REAL_NAME_ATTR_NAME = "real_name";
    private static final String URL_ATTR_NAME = "url";
    private static final String TOOL_ATTR_NAME = "tool";
    private static final String UID_ATTR_NAME = "uid";
    private static final String DID_ATTR_NAME = "did";
    private static final String CID_ATTR_NAME = "cid";
    private static final String MAIL_ATTR_NAME = "mail";
    private static final String SENT_TO_CLIENT_COUNT_NAME = "sent_to_client_count";
    private static final String CLIENT_ID_NAME = "client_id";
    private static final String MAIL_DOMAIN_ATTR_NAME = "mail_domain";
    private static final String ACCESSGROUP_ID_ATTR_NAME = "accessgroupid";
    private static final String LDAP_ATTR_NAME = "ldapgroup";
    private static final String LDAP_ATTR_ID = "ldapgroupid";
    private static final String HOST_DNS_NAME_ATTR = "dnsname"; // attribute for DNS Host Name
    private static final String INET_ATTR_NAME = "inet_address";
    private static final String LOCATION_ATTR_NAME = "location";
    private static final String LDAP_ATTR_DISPLAY_NAME = "ldapgroupname";

    private static final String ESF_ATTR_NAME = "esfcode";
    private static final String NIPP_ATTR_NAME = "nippcode";
    private static final String JABBERID_ATTR_NAME = "jabberid";

    private static final String EMPTY_LOCATION_PATTERN = "EMPTY";

    public static final SubjectAttribute USER_NAME = new SubjectAttribute(NAME_ATTR_NAME, SubjectType.USER) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return EvalValue.build(subj.getUniqueName());
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final SubjectAttribute RECIPIENT_USER_NAME = new SubjectAttribute(NAME_ATTR_NAME, SubjectType.RECIPIENT) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return EvalValue.build(subj.getUniqueName());
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final SubjectAttribute USER_REAL_NAME = new SubjectAttribute(REAL_NAME_ATTR_NAME, SubjectType.USER) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return EvalValue.build(subj.getName());
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final SubjectAttribute RECIPIENT_REAL_NAME = new SubjectAttribute(REAL_NAME_ATTR_NAME, SubjectType.RECIPIENT) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return EvalValue.build(subj.getName());
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final IDSubjectAttribute USER_UID = new SubjectAttribute(UID_ATTR_NAME, SubjectType.USER) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return EvalValue.build(subj.getUid());
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final IDSubjectAttribute RECIPIENT_UID = new SubjectAttribute(UID_ATTR_NAME, SubjectType.RECIPIENT) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return EvalValue.build(subj.getUid());
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final SubjectAttribute USER_ID = new IdBasedSubjectAttribute(DID_ATTR_NAME, SubjectType.USER) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return EvalValue.build(subj.getId().longValue());
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final SubjectAttribute CONTACT_ID = new IdBasedSubjectAttribute(CID_ATTR_NAME, SubjectType.USER) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return EvalValue.build(subj.getId().longValue());
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final SubjectAttribute APPUSER_ID = new IdBasedSubjectAttribute(DID_ATTR_NAME, SubjectType.APPUSER) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return EvalValue.build(subj.getId().longValue());
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final SubjectAttribute APPUSER_ACCESSGROUP_ID = new IdBasedSubjectAttribute(ACCESSGROUP_ID_ATTR_NAME, SubjectType.APPUSER) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj instanceof IAccessibleSubject) {
                return ((IAccessibleSubject)subj).getAccessGroups();
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final SubjectAttribute USER_ESF = new AbstractDynamicAttribute(ESF_ATTR_NAME, SubjectType.USER) {
    };

    public static final SubjectAttribute USER_NIPP = new AbstractDynamicAttribute(NIPP_ATTR_NAME, SubjectType.USER) {
    };

    public static final SubjectAttribute USER_JABBERID = new AbstractDynamicAttribute(JABBERID_ATTR_NAME, SubjectType.USER) {

        @Override
        public IEvalValue evaluate(IArguments args) {
            IEvalValue res = IEvalValue.NULL;
            IDSubject subj = getSubject(args);
            if (subj != null) {
                res = subj.getAttribute(JABBERID_ATTR_NAME);

                // If the user doesn't have a jabber id then we can
                // use the email address.  This will happen with
                // 'sendto' users, because they are given to us as an
                // email address only.
                if (res == null || res == IEvalValue.NULL) {
                    res = USER_EMAIL.evaluate(args);
                }
            }
            return res;
        }
    };

    public static final SubjectAttribute USER_LDAP_GROUP = new SubjectAttribute(LDAP_ATTR_NAME, SubjectType.USER) {
    };

    public static final SubjectAttribute USER_LDAP_GROUP_ID = new IdBasedSubjectAttribute(LDAP_ATTR_ID, SubjectType.USER) {
    };

    public static final SubjectAttribute USER_LDAP_GROUP_DISPLAY_NAME = new SubjectAttribute(LDAP_ATTR_DISPLAY_NAME, SubjectType.USER) {
    };

    public static final SubjectAttribute USER_EMAIL = new AbstractDynamicAttribute(MAIL_ATTR_NAME, SubjectType.USER) {
    };

    public static final SubjectAttribute USER_EMAIL_DOMAIN = new AbstractDynamicAttribute(MAIL_DOMAIN_ATTR_NAME, SubjectType.USER) {

        /**
         * @see AbstractDynamicAttribute#evaluate(IArguments)
         */
        @Override
        public IEvalValue evaluate(IArguments args) {
            IEvalValue baseVal = USER_EMAIL.evaluate(args);
            if (baseVal == null || baseVal == IEvalValue.NULL) {
                return IEvalValue.NULL;
            }
            Object v = baseVal.getValue();
            if (v == null) {
                return IEvalValue.NULL;
            }
            if (v instanceof IMultivalue) {
                return EvalValue.build(
                        new DomainMultival((IMultivalue)v)
                        );
            }
            return EvalValue.build(getDomainString(v.toString()));
        }

        /**
         * @see SubjectAttribute#build(String)
         */
        @Override
        public Constant build(String pattern) {
            if (pattern == null) {
                return Constant.EMPTY;
            }
            return Constant.build(
                    new DomainMultival(pattern)
                    , pattern
                    );
        }

    };

    public static final SubjectAttribute USER_CLIENT_ID = new AbstractDynamicAttribute(CLIENT_ID_NAME, SubjectType.USER) {

        @Override
        public IEvalValue evaluate(IArguments args) {
            if (args instanceof EvaluationRequest) {
                EvaluationRequest req = (EvaluationRequest)args;
                IClientInformationManager clientInfoManager = req.getClientInformationManager();
                IEvalValue baseVal = USER_EMAIL.evaluate(args);
                if (baseVal == null || baseVal == IEvalValue.NULL) {
                    return IEvalValue.EMPTY;
                }
                Object v = baseVal.getValue();
                if (v == null) {
                    return IEvalValue.EMPTY;
                }
                Set<String> clientIds = new HashSet<String>();
                if (v instanceof IMultivalue) {
                    for (IEvalValue str : (IMultivalue)v) {
                        Object e = str.getValue();
                        if (e instanceof String) {
                            addAll(clientIds, clientInfoManager.getClientIdsForEmail((String)e));
                        }
                    }
                } else if (v instanceof String) {
                    addAll(clientIds, clientInfoManager.getClientIdsForEmail((String)v));
                }
                return EvalValue.build(Multivalue.create(clientIds));
            } else {
                return IEvalValue.EMPTY;
            }
        }

        private void addAll(Set<String> set, String[] ids) {
            for (String id : ids) {
                set.add(id);
            }
        }

    };

    public static final SubjectAttribute SENT_TO_CLIENT_COUNT = new AbstractDynamicAttribute(SENT_TO_CLIENT_COUNT_NAME, SubjectType.USER) {

        @Override
        public IEvalValue evaluate(IArguments args) {
            if (args instanceof EvaluationRequest) {
                EvaluationRequest req = (EvaluationRequest)args;
                IClientInformationManager clientInfoManager = req.getClientInformationManager();
                IEvalValue res = (IEvalValue)req.getCached(SENT_TO_CLIENT_COUNT_NAME);
                if (res != null) {
                    return res;
                }
                IDSubject[] toUsers = req.getSentTo();
                Set<String> clientIds = new HashSet<String>();
                for (IDSubject toUser : toUsers) {
                    IEvalValue baseVal = toUser.getAttribute(USER_EMAIL.getName());
                    if (baseVal == null || baseVal == IEvalValue.NULL) {
                        continue;
                    }
                    Object v = baseVal.getValue();
                    if (v == null) {
                        continue;
                    }
                    if (v instanceof IMultivalue) {
                        for (IEvalValue str : (IMultivalue)v) {
                            Object e = str.getValue();
                            if (e instanceof String) {
                                addAll(clientIds, clientInfoManager.getClientIdsForEmail((String)e));
                            }
                        }
                    } else if (v instanceof String) {
                        addAll(clientIds, clientInfoManager.getClientIdsForEmail((String)v));
                    }
                }
                res = EvalValue.build(clientIds.size());
                req.setCached(SENT_TO_CLIENT_COUNT_NAME, res);
                return res;
            } else {
                return EvalValue.build(0);
            }
        }

        private void addAll(Set<String> set, String[] vals) {
            for (String v : vals) {
                set.add(v);
            }
        }

        @Override
        public ValueType getValueType() {
            return ValueType.LONG;
        }

        @Override
        public Constant build(String val) {
            return buildLongConstant(val);
        }

    };

    public static final SubjectAttribute APP_NAME = new AbstractDynamicAttribute(NAME_ATTR_NAME, SubjectType.APP) {
    };

    public static final SubjectAttribute APP_URL = new AbstractDynamicAttribute(URL_ATTR_NAME, SubjectType.APP) {
    };

    public static final SubjectAttribute APP_TOOL = new AbstractDynamicAttribute(TOOL_ATTR_NAME, SubjectType.APP) {
    };

    public static final IDSubjectAttribute APP_UID = new SubjectAttribute(UID_ATTR_NAME, SubjectType.APP) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return EvalValue.build(subj.getUid());
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final SubjectAttribute APP_ID = new IdBasedSubjectAttribute(DID_ATTR_NAME, SubjectType.APP) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return EvalValue.build(subj.getId().longValue());
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final SubjectAttribute HOST_NAME = new SubjectAttribute(NAME_ATTR_NAME, SubjectType.HOST) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return EvalValue.build(subj.getUniqueName());
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final IDSubjectAttribute HOST_UID = new SubjectAttribute(UID_ATTR_NAME, SubjectType.HOST) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return EvalValue.build(subj.getUid());
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final SubjectAttribute HOST_ID = new IdBasedSubjectAttribute(DID_ATTR_NAME, SubjectType.HOST) {

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                Long id = subj.getId();
                return id != null ? EvalValue.build(id.longValue()) : IEvalValue.NULL;
            } else {
                return IEvalValue.NULL;
            }
        }
    };

    public static final SubjectAttribute HOST_LDAP_GROUP = new SubjectAttribute(LDAP_ATTR_NAME, SubjectType.HOST) {
    };

    public static final SubjectAttribute HOST_LDAP_GROUP_ID = new IdBasedSubjectAttribute(LDAP_ATTR_ID, SubjectType.HOST) {
    };

    public static final SubjectAttribute HOST_LDAP_GROUP_DISPLAY_NAME = new SubjectAttribute(LDAP_ATTR_DISPLAY_NAME, SubjectType.HOST) {
    };

    // attribute for DNS host name
    // declaring this as AbstractDynamicAttribute makes it a dynamic attribute (i.e. evaluated in Policy Controller)
    public static final SubjectAttribute HOST_DNS_NAME = new AbstractDynamicAttribute(HOST_DNS_NAME_ATTR, SubjectType.HOST) {

        // evaluate method is invoked by Policy Controller
        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj == null)
                return IEvalValue.NULL;
            String fqdn = subj.getName(); // subject name should be FQDN (Fully Qualified Domain Name) at this point
            return EvalValue.build(fqdn);
        }
    };

    public static final SubjectAttribute INET_ADDRESS = new InetAddressAttribute(INET_ATTR_NAME, SubjectType.HOST);

    public static final SubjectAttribute LOCATION = new InetAddressAttribute(LOCATION_ATTR_NAME, SubjectType.HOST) {

        public IRelation buildRelation(RelationOp op, String val) {
            return new Relation(op, this, new LocationReference(val));
        }

        public IRelation buildRelation(RelationOp op, IExpression expr) {
            if (expr instanceof Constant) {
                return buildRelation(op, ((Constant)expr).getRepresentation());
            } else {
                return new Relation(op, this, expr);
            }
        }
    };

    private static abstract class AbstractDynamicAttribute extends SubjectAttribute {

        protected AbstractDynamicAttribute(String name, SubjectType subjType) {
            super(name, subjType);
        }

        public boolean isDynamic() {
            return true;
        }

        /**
         * @see SubjectAttribute#evaluate(IArguments)
         */
        @Override
        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                IEvalValue res = subj.getAttribute(name);
                return (res != null) ? res : IEvalValue.NULL;
            } else {
                return IEvalValue.NULL;
            }
        }

    }

    private static abstract class AbstractGroupAttribute extends SubjectAttribute {

        protected AbstractGroupAttribute(SubjectType subjType) {
            super("GROUP", subjType);
        }

        public Constant build(String val) {
            return buildLongConstant(val);
        }

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                return subj.getGroups();
            } else {
                return IEvalValue.EMPTY;
            }
        }
    }

    public static final SubjectAttribute USER_GROUP = new AbstractGroupAttribute(SubjectType.USER) {
    };

    public static final SubjectAttribute HOST_GROUP = new AbstractGroupAttribute(SubjectType.HOST) {
    };

    public static final SubjectAttribute APP_GROUP = new AbstractGroupAttribute(SubjectType.APP) {
    };

    public static SubjectAttribute getGroupAttribute(SubjectType subjType) {
        if (subjType == null) {
            throw new NullPointerException("subjType");
        }
        if (subjType == SubjectType.USER) {
            return USER_GROUP;
        } else if (subjType == SubjectType.HOST) {
            return HOST_GROUP;
        } else if (subjType == SubjectType.APP) {
            return APP_GROUP;
        } else {
            throw new IllegalArgumentException("subjType");
        }
    }

    private final SubjectType subjType;

    protected SubjectAttribute(String name, SubjectType subjType) {
        super(name, new MultipartKey(name.toLowerCase(), subjType));
        this.subjType = subjType;
    }

    /**
     * Factory method for making subject attributes.
     * @param name the name of the subject attribute.
     * @return a new or an existing subject attribute.
     */
    public static SubjectAttribute forNameAndType(String name, SubjectType subjType) {
        MultipartKey key = new MultipartKey(name.toLowerCase(), subjType);
        if (isRegistered(key)) {
            return (SubjectAttribute)getRegistered(key);
        } else {
            SubjectAttribute res = new ExternalSubjectAttribute(name, subjType);
            register(key, res);
            return res;
        }
    }

    // by default an attribute is static
    @SuppressWarnings("deprecation")
    public boolean isDynamic() {
        return false;
    }

    public IEvalValue evaluate(IArguments args) {
        IDSubject subj = getSubject(args);
        if (subj != null) {
            IEvalValue res = subj.getAttribute(name);

            return (res == null ? evaluateWithProvider(args) : res);
        } else {
            return IEvalValue.NULL;
        }
    }

    private IEvalValue evaluateWithProvider(IArguments args) {
        if (args instanceof IEvaluationRequest) {
            IEvaluationRequest req = (IEvaluationRequest)args;
            IServiceProviderManager serviceProviderManager = req.getServiceProviderManager();

            for (ISubjectAttributeProvider sp : serviceProviderManager.getAllServiceProvidersByType(ISubjectAttributeProvider.class)) {
                try {
                    IDSubject subj = getSubject(args);
                    IEvalValue val = sp.getAttribute(subj, name);

                    if (val != null) {
                        if (subj instanceof IMSubject) {
                            ((IMSubject)subj).setAttribute(name, val);
                        }

                        return val;
                    }
                } catch (ServiceProviderException spe) {
                    // Do nothing, try the next service provider
                }
            }
        }

        return IEvalValue.NULL;
    }

    public static final class ExternalSubjectAttribute extends SubjectAttribute {

        private ExternalSubjectAttribute(String name, SubjectType subjType) {
            super(name, subjType);
        }
    }

    private static abstract class IdBasedSubjectAttribute extends SubjectAttribute {

        protected IdBasedSubjectAttribute(String name, SubjectType subjType) {
            super(name, subjType);
        }

        public Constant build(String val) {
            return buildLongConstant(val);
        }
    }

    private static class InetAddressAttribute extends SubjectAttribute {

        private static final Pattern dottedIPAddressPattern = Pattern.compile("^[\\d]{1,3}\\.[\\d]{1,3}\\.[\\d]{1,3}\\.[\\d]{1,3}$");

        public InetAddressAttribute(String name, SubjectType subjType) {
            super(name, subjType);
        }

        public Constant build(String pattern) {
            if (pattern == null) {
                String addr = buildSingleAddress(pattern);
                return Constant.build(addr, addr);
            }

            // We may have multiple addresses separated by ,
            String[] addresses = pattern.split(",");

            if (addresses == null || addresses.length == 1) {
                String addr = buildSingleAddress(pattern);
                return Constant.build(addr, pattern);
            }

            List<String> res = new ArrayList<String>(addresses.length);

            for (String address : addresses) {
                res.add(buildSingleAddress(address));
            }

            return Constant.build(Multivalue.create(res), pattern);
        }

        // the address is stored as a string of 32 char with a 0 or 1 and ? representing
        // bits that we don't care about
        private String buildSingleAddress(String pattern) {
            if (pattern == null || pattern.length() == 0) {
                return "";
            }
            if (EMPTY_LOCATION_PATTERN.equalsIgnoreCase(pattern)) {
                return EMPTY_LOCATION_PATTERN;
            }
            int slashIndex = pattern.indexOf('/');
            InetAddress address;
            int patternBits = 0;
            try {
                if (slashIndex >= 0) {
                    String ipPortion = pattern.substring(0, slashIndex);
                    address = InetAddress.getByName(ipPortion);
                    String pbPortion = pattern.substring(slashIndex + 1);
                    patternBits = address.getAddress().length * 8 - Integer.parseInt(pbPortion);
                    if (patternBits < 0) {
                        throw new RuntimeException("invalid inet_address value: " + pattern);
                    }
                } else {
                    address = InetAddress.getByName(pattern);
                }
            } catch (NumberFormatException nfe) {
                throw new RuntimeException("invalid inet_address value: " + pattern, nfe);
            } catch (UnknownHostException uhe) {
                throw new RuntimeException("invalid inet_address value: " + pattern, uhe);
            }
            return toBinaryString(address, patternBits);
        }

        public IEvalValue evaluate(IArguments args) {
            IDSubject subj = getSubject(args);
            if (subj != null) {
                String hostName = subj.getName();

                List<String> addrStrings = new ArrayList<String>();

                if (isIPAddress(hostName)) {
                    String[] strOctets = hostName.split("\\.");
                    short[] octets = new short[4];
                    for (int i = 0; i < 4; i++) {
                        try {
                            short octet = Short.parseShort(strOctets[i]);
                            if (octet < 0 || octet > 255) {
                                throw new RuntimeException("Invalid octet in " + hostName);
                            }
                            octets[i] = octet;
                        } catch (NumberFormatException e) {
                            octets[i] = 0;
                        }
                    }
                    addrStrings.add(toBinaryString(octets, 0));
                } else {
                    InetAddress[] addresses;
                    try {
                        addresses = InetAddress.getAllByName(hostName);
                    } catch (UnknownHostException e) {
                        addresses = new InetAddress[0];
                    }
                    for (int i = 0; i < addresses.length; i++) {
                        addrStrings.add(toBinaryString(addresses[i], 0));
                    }
                }
                return EvalValue.build(Multivalue.create(addrStrings));
            } else {
                return IEvalValue.EMPTY;
            }
        }

        public boolean isDynamic() {
            return true;
        }

        private String toBinaryString(InetAddress address, int ignoredBits) {
            byte[] boctets = address.getAddress();
            short[] octets = new short[boctets.length];

            for (int i = 0; i < boctets.length; i++) {
                octets[i] = boctets[i];
            }
            return toBinaryString(octets, ignoredBits);
        }

        private String toBinaryString(short[] octets, int ignoredBits) {
            int addrLenInBytes = octets.length;
            int bitsRemaining = addrLenInBytes * 8 - ignoredBits;
            StringBuffer rv = new StringBuffer(addrLenInBytes * 8);
            for (int i = 0; bitsRemaining > 0; i++) {
                short s = octets[i];
                for (int j = 0; j != 8 && bitsRemaining > 0; j++, s <<= 1, bitsRemaining--) {
                    rv.append(((s & 0x80) == 0) ? '0' : '1');
                }
            }
            if (ignoredBits != 0) {
                rv.append("?D");
            }
            return rv.toString();
        }

        private static boolean isIPAddress(String host) {
            return dottedIPAddressPattern.matcher(host).matches();
        }
    }

    public Constant build(String pattern) {
        return Constant.build(pattern);
    }

    /**
     * @see SpecAttribute#validOperators()
     */
    public Set<RelationOp> validOperators() {
        return STRING_OPS;
    }

    public SpecType getSpecType() {
        return subjType.getSpecType();
    }

    /**
     * @see IAttribute#getObjectSubTypeName()
     */
    public String getObjectSubTypeName() {
        return subjType.getName();
    }

    /**
     * @see IDSubjectAttribute#getSubjectType()
     */
    public final SubjectType getSubjectType() {
        return subjType;
    }

    /**
     * @see SpecAttribute#validTypes()
     */
    public ValueType getValueType() {
        return ValueType.STRING;
    }

    /**
     * Returns the subject of the specific type
     * expected by this attribute.
     *
     * @return he subject of the specific type
     * expected by this attribute.
     */
    protected final IDSubject getSubject(IArguments args) {
        if (args instanceof IEvaluationRequest) {
            return ((IEvaluationRequest)args).getSubjectByType(getSubjectType());
        } else if (args instanceof IDSubject) {
            return (IDSubject)args;
        } else {
            return null;
        }
    }

    private static final Set<RelationOp> STRING_OPS;

    static {
        Set<RelationOp> stringOps = new HashSet<RelationOp>(5);
        stringOps.add(RelationOp.EQUALS);
        stringOps.add(RelationOp.NOT_EQUALS);
        STRING_OPS = Collections.unmodifiableSet(stringOps);
    }

    private static class DomainMultival implements IMultivalue {

        private final Set<String> domains = new HashSet<String>();

        public DomainMultival(String pattern) {
            String[] elements = pattern.split("\\s|[:;,]");
            for (int i = 0; i != elements.length; i++) {
                domains.add(elements[i].toLowerCase());
            }

        }

        public DomainMultival(IMultivalue emails) {
            if (emails == null) {
                throw new NullPointerException("emails");
            }
            if (emails.getType() != ValueType.STRING) {
                throw new IllegalArgumentException("emails");
            }
            for (IEvalValue s : emails) {
                domains.add(getDomainString(s.getValue().toString()));
            }
        }

        /**
         * @see IMultivalue#get(int)
         */
        public Object get(int index) {
            return domains.toArray()[index];
        }

        /**
         * @see IMultivalue#getType()
         */
        public ValueType getType() {
            return ValueType.STRING;
        }

        /**
         * @see IMultivalue#includes(IEvalValue)
         */
        public boolean includes(IEvalValue val) {
            if (val == null) {
                return false;
            }
            Object v = val.getValue();
            if (v instanceof String) {
                return domains.contains(v);
            }
            if (v instanceof DomainMultival) {
                DomainMultival dmv = (DomainMultival)v;
                for (String s : dmv.domains) {
                    if (domains.contains(s)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * @see IMultivalue#includes(IMultivalue, IMultivalueEqual)
         */
        public boolean includes(IMultivalue mval, IMultivalueEqual eq) {
            return includes(mval);
        }

        /**
         * @see IMultivalue#includes(IEvalValue, IMultivalueEqual)
         */
        public boolean includes(IEvalValue val, IMultivalueEqual eq) {
            return includes(val);
        }

        /**
         * @see IMultivalue#includes(IMultivalue)
         */
        public boolean includes(IMultivalue multival) {
            if (multival == null) {
                return false;
            }
            if (multival instanceof DomainMultival) {
                DomainMultival dmv = (DomainMultival)multival;
                for (String s : dmv.domains) {
                    if (!domains.contains(s)) {
                        return false;
                    }
                }
                return true;
            } else {
                return multival.includes(this);
            }
        }

        /**
         * @see IMultivalue#includes(IEvalValue, IMultivalueEqual)
         */
        public boolean intersects(IMultivalue val, IMultivalueEqual eq) {
            return intersects(val);
        }

        /**
         * @see IMultivalue#intersects(IMultivalue)
         */
        public boolean intersects(IMultivalue multival) {
            if (multival == null) {
                return false;
            }
            if (multival instanceof DomainMultival) {
                DomainMultival dmv = (DomainMultival)multival;
                for (String s : dmv.domains) {
                    if (domains.contains(s)) {
                        return true;
                    }
                }
                return false;
            } else {
                return multival.intersects(this);
            }
        }

        /**
         * @see IMultivalue#isEmpty()
         */
        public boolean isEmpty() {
            return domains.isEmpty();
        }

        /**
         * @see IMultivalue#iterator()
         */
        public Iterator<IEvalValue> iterator() {
            return new Iterator<IEvalValue>() {

                Iterator<String> iter = domains.iterator();

                public boolean hasNext() {
                    return iter.hasNext();
                }

                public IEvalValue next() {
                    return EvalValue.build(iter.next());
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /**
         * @see IMultivalue#size()
         */
        public int size() {
            return domains.size();
        }

        /**
         * @see IMultivalue#toArray(T[])
         */
        public <T> T[] toArray(T[] array) {
            return domains.toArray(array);
        }

    }

    private static Constant buildLongConstant(String val) {
        try {
            return Constant.build(Long.parseLong(val));
        } catch (NumberFormatException e) {
            return Constant.build(val);
        }
    }

    private static String getDomainString(String email) {
        return email.substring(email.indexOf('@') + 1).toLowerCase();
    }

}
