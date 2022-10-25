package com.bluejungle.pf.domain.destiny.resource;

/*
 * Created on Feb 17, 2005
 *
 * All sources, binaries and HTML pages (C) copyright 2007 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 */

import com.bluejungle.framework.expressions.Constant;
import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IArguments;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IRelation;
import com.bluejungle.framework.expressions.Relation;
import com.bluejungle.framework.expressions.RelationOp;
import com.bluejungle.framework.expressions.ValueType;
import com.bluejungle.framework.utils.MultipartKey;
import com.bluejungle.framework.utils.UnmodifiableDate;
import com.bluejungle.pf.domain.destiny.common.SpecAttribute;
import com.bluejungle.pf.domain.destiny.common.SpecReference;
import com.bluejungle.pf.domain.destiny.serviceprovider.IResourceAttributeProvider;
import com.bluejungle.pf.domain.destiny.serviceprovider.IServiceProviderManager;
import com.bluejungle.pf.domain.destiny.serviceprovider.ServiceProviderException;
import com.bluejungle.pf.domain.destiny.subject.SubjectType;
import com.bluejungle.pf.domain.epicenter.common.SpecType;
import com.bluejungle.pf.domain.epicenter.evaluation.IEvaluationRequest;
import com.bluejungle.pf.engine.destiny.EvaluationRequest;
import com.bluejungle.pf.engine.destiny.IEngineSubjectResolver;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/domain/destiny/resource/ResourceAttribute.java#1 $:
 * @author sasha
 * @author sergey
 */

public abstract class ResourceAttribute extends SpecAttribute {

    public static final String FILE_SYSTEM_SUBTYPE = "fso";
    public static final String DESTINY_SUBTYPE = "dso";
    public static final String PORTAL_SUBTYPE = "portal";

    private static final Set<RelationOp> STRING_OPS;

    private static final Set<RelationOp> NUM_DATE_OPS;

    private static final Set<DateFormat> DATE_FORMATS;

    private final String subtype;

    static {
        DATE_FORMATS = new HashSet<DateFormat>(4);
        DATE_FORMATS.add(new SimpleDateFormat("MM/dd/yyyy"));
        // DATE_FORMATS.add(new SimpleDateFormat("S"));
        Set<RelationOp> stringOps = new HashSet<RelationOp>(5);
        stringOps.add(RelationOp.EQUALS);
        stringOps.add(RelationOp.NOT_EQUALS);
        STRING_OPS = Collections.unmodifiableSet(stringOps);

        NUM_DATE_OPS = RelationOp.elements();
    }

    public static final ResourceAttribute NAME = new NameAttributeBase("name", FILE_SYSTEM_SUBTYPE) {

        public Constant build(String str) {
            return super.build(str, str);
        }
    };

    public static final ResourceAttribute TYPE = new NameAttributeBase("type", FILE_SYSTEM_SUBTYPE) {

        public Constant build(String str) {
            return super.build(str, "**." + str);
        }
    };

    public static final ResourceAttribute DIRECTORY = new NameAttributeBase("directory", FILE_SYSTEM_SUBTYPE) {

        public Constant build(String str) {
            return super.build(str, "**/" + str + "/*");
        }
    };

    public static final ResourceAttribute IS_DIRECTORY = new ResourceAttribute("isdirectory", FILE_SYSTEM_SUBTYPE) {

        public IEvalValue evaluate(IArguments args) {
            if (!(args instanceof IEvaluationRequest)) {
                return IEvalValue.NULL;
            }
            return ((IEvaluationRequest)args).getFromResource().getAttribute(getName());
        }

        public Set<RelationOp> validOperators() {
            return RelationOp.elements();
        }

        public ValueType getValueType() {
            return ValueType.LONG;
        }

        public Constant build(String str) {
            try {
                return Constant.build(Long.parseLong(str));
            } catch (NumberFormatException nfe) {
                return Constant.build(0);
            }
        }
    };

    public static final ResourceAttribute ACCESS_DATE = new DateAttribute("access_date", FILE_SYSTEM_SUBTYPE) {
    };
    public static final ResourceAttribute CREATED_DATE = new DateAttribute("created_date", FILE_SYSTEM_SUBTYPE) {
    };
    public static final ResourceAttribute MODIFIED_DATE = new DateAttribute("modified_date", FILE_SYSTEM_SUBTYPE) {
    };

    public static final ResourceAttribute OWNER = new ResourceAttribute("owner", FILE_SYSTEM_SUBTYPE) {

        /**
         * @see SpecAttribute#evaluate(IArguments)
         */
        @Override
        public IEvalValue evaluate(IArguments args) {
            if (args instanceof IEvaluationRequest) {
                return ((IEvaluationRequest)args).getFromResource().getAttribute(getName());
            } else if (args instanceof IPResource) {
                return EvalValue.build(((IPResource)args).getOwner().getUid());
            } else {
                return IEvalValue.NULL;
            }
        }

        public Constant build(String str) {
            try {
                return Constant.build(Long.parseLong(str));
            } catch (NumberFormatException nfe) {
                // Not a number - make a string
                return Constant.build(str);
            }
        }

        public Set<RelationOp> validOperators() {
            return STRING_OPS;
        }

        public ValueType getValueType() {
            return ValueType.STRING;
        }
    };

    public static final ResourceAttribute OWNER_GROUP = new AbstractDestinyUserGroup("owner_group", FILE_SYSTEM_SUBTYPE) {
    };

    public static final ResourceAttribute OWNER_LDAP_GROUP = new AbstractLDAPUserGroup("owner_ldap_group", FILE_SYSTEM_SUBTYPE, OWNER) {
    };

    public static final ResourceAttribute SIZE = new ResourceAttribute("size", FILE_SYSTEM_SUBTYPE) {

        /**
         * @see SpecAttribute#evaluate(IArguments)
         */
        @Override
        public IEvalValue evaluate(IArguments args) {
            if (!(args instanceof IEvaluationRequest)) {
                return IEvalValue.NULL;
            }
            return ((IEvaluationRequest)args).getFromResource().getAttribute(getName());
        }

        public Constant build(String str) {
            try {
                long val = Long.parseLong(str);
                return Constant.build(val);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid operand: " + str + " number expected");
            }
        }

        public Set<RelationOp> validOperators() {
            return NUM_DATE_OPS;
        }

        public ValueType getValueType() {
            return ValueType.LONG;
        }
    };

    private static abstract class AbstractUserGroup extends ResourceAttribute {

        public AbstractUserGroup(String name, String type) {
            super(name, type);
        }

        public Set<RelationOp> validOperators() {
            return STRING_OPS;
        }

        public ValueType getValueType() {
            return ValueType.STRING;
        }
    }

    private static abstract class AbstractDestinyUserGroup extends AbstractUserGroup {

        public AbstractDestinyUserGroup(String name, String type) {
            super(name, type);
        }

        /**
         * @see SpecAttribute#buildRelation(RelationOp, String)
         */
        @Override
        public IRelation buildRelation(RelationOp op, String value) {
            return new Relation(op, this, new SpecReference(value));
        }

        /**
         * @see SpecAttribute#evaluate(IArguments)
         */
        @Override
        public IEvalValue evaluate(IArguments args) {
            throw new UnsupportedOperationException(getName() + ".evaluate");
        }
    }

    public static abstract class AbstractLDAPUserGroup extends AbstractUserGroup {

        private final ResourceAttribute baseAttribute;

        public AbstractLDAPUserGroup(String name, String type, ResourceAttribute baseAttribute) {
            super(name, type);
            this.baseAttribute = baseAttribute;
        }

        /**
         * @see SpecAttribute#evaluate(IArguments)
         */
        @Override
        public IEvalValue evaluate(IArguments args) {
            if (!(args instanceof EvaluationRequest)) {
                return IEvalValue.NULL;
            }
            EvaluationRequest req = (EvaluationRequest)args;
            IEngineSubjectResolver subjResolver = req.getSubjectResolver();
            if (subjResolver == null) {
                return IEvalValue.NULL;
            }
            IEvalValue baseValue = req.getFromResource().getAttribute(baseAttribute.getName());
            ;
            if (baseValue != null && baseValue.getValue() != null) {
                return subjResolver.getGroupsForSubject(baseValue.getValue().toString(), SubjectType.USER);
            } else {
                return IEvalValue.EMPTY;
            }
        }

        /**
         * @see SpecAttribute#build(String)
         */
        @Override
        public Constant build(String str) {
            try {
                return Constant.build(Long.parseLong(str));
            } catch (NumberFormatException nfe) {
                // Not a number - make a string
                return Constant.build(str);
            }
        }

    }

    static abstract class AbstractMatchAttribute extends ResourceAttribute {

        /**
         * The name of the actual attribute accessed to get the value.
         */
        private final String alias;

        /**
         * Creates an <code>AbstractMatchAttribute</code>.
         * @param name the name of the attribute in the language.
         * @param type the type of the attribute.
         */
        protected AbstractMatchAttribute(String name, String subtype) {
            this(name, subtype, name);
        }

        /**
         * Creates an <code>AbstractMatchAttribute</code>.
         * @param name the name of the attribute in the language.
         * @param type the type of the attribute.
         * @param alias the actual attribute accessed to get the value.
         */
        protected AbstractMatchAttribute(String name, String subtype, String alias) {
            super(name, subtype);
            this.alias = alias;
        }

        /**
         * @see SpecAttribute#evaluate(IArguments)
         */
        @Override
        public IEvalValue evaluate(IArguments args) {
            if (!(args instanceof IEvaluationRequest)) {
                return IEvalValue.NULL;
            }
            return ((IEvaluationRequest)args).getFromResource().getAttribute(alias);
        }

        /**
         * @see SpecAttribute#validOperators()
         */
        @Override
        public Set<RelationOp> validOperators() {
            return STRING_OPS;
        }

        /**
         * @see SpecAttribute#validTypes()
         */
        @Override
        public ValueType getValueType() {
            return ValueType.STRING;
        }

        /**
         * Makes a <code>Constant</code> for use with this attribute.
         *
         * @param value the value <code>String</code>.
         * @return the <code>Constant</code> for use with this attribute.
         */
        public Constant build(String value) {
            return build(value, value);
        }

        /**
         * Makes a <code>Constant</code> for use with this attribute.
         *
         * @param orig the original <code>String</code>.
         * @param preprocessed the <code>String</code> preprocessed by the
         * build method of the corresponding subclass
         * (adding slashes, stars, etc.)
         * @return the <code>Constant</code> for use with this attribute.
         */
        protected Constant build(String orig, String preprocessed) {
            StringBuffer buf = new StringBuffer(preprocessed.length());
            // Trim the initial and trailing blanks
            int maxLen = preprocessed.length();
            while (maxLen != 0 && Character.isWhitespace(preprocessed.charAt(maxLen - 1))) {
                maxLen--;
            }
            int i = 0;
            while (i != maxLen && Character.isWhitespace(preprocessed.charAt(i))) {
                i++;
            }
            // Convert preprocessed to lower case paying attention to
            // the special characters '?' and '!'
            while (i != maxLen) {
                char ch = preprocessed.charAt(i++);
                switch (ch) {
                    case '?':
                    case '!':
                        buf.append(ch);
                        if (i != maxLen) {
                            buf.append(preprocessed.charAt(i++));
                        }
                        break;
                    case '\\':
                        buf.append('/');
                        break;
                    default:
                        buf.append(Character.toLowerCase(ch));
                }
            }
            return Constant.build(postprocessConstantValue(buf.toString()), orig);
        }

        /**
         * Subclasses override this method to decorate the string
         * on which the matching is done.
         *
         * @param constValue The preprocessed string.
         * @return the constant string after post-processing.
         */
        protected String postprocessConstantValue(String constValue) {
            return constValue;
        }

    }

    /**
     * This class is package-private to enable unit testing
     * of some of its methods.
     */
    static abstract class NameAttributeBase extends AbstractMatchAttribute {

        protected NameAttributeBase(String name, String subtype) {
            super(name, subtype, "name");
        }

        private static boolean isLinux() {
            String os = System.getProperty("os.name");
            return os.equals("Linux");
        }

        private static final Pattern VAR_PATTERN = Pattern.compile("\\[[a-z_]+\\].*");

        // Schemes are the part of the URL before the ':' (e.g. file, http, ftp, etc.)
        // Normally these can contain both upper and lower case letters (as well as the given
        // special characters), but we've converted this string to lower case.
        private static final Pattern SCHEME_NAME_PATTERN = Pattern.compile("[a-z0-9_+\\.\\-]*:.*");

        /**
         * @see ResourceAttribute.AbstractMatchAttribute#postprocessConstantValue(String)
         */
        @Override
        protected String postprocessConstantValue(String str) {
            final String prefix;
            if (str.startsWith("/")) { // an absolute path, remote or local
                if (!str.startsWith("//")) { //local
                    if (isLinux()) {
                        prefix = "///";
                    } else {
                        prefix = "///?c:";
                    }
                } else { //   remote/share
                    if (!str.startsWith("//**")) {
                        prefix = "";
                    } else {
                        prefix = "//?c*/"; // If //** add a ?c so local files are not matched
                        str = str.substring(2);
                    }
                }
            } else if (str.startsWith("**")) {
                prefix = "/";
            } else if (VAR_PATTERN.matcher(str).matches() ||
                    SCHEME_NAME_PATTERN.matcher(str).matches()) {
                prefix = "///";
            } else if (str.startsWith("?") || str.startsWith("!")) {
                if (str.length() < 3 || str.charAt(2) != ':') {
                    prefix = "//**/";
                } else {
                    prefix = "///";
                }
            } else {
                if (str.length() < 2 || str.charAt(1) != ':') {
                    prefix = "//**/";
                } else {
                    prefix = "///";
                }
            }
            return "file:" + prefix + str;
        }
    }

    private static abstract class DateAttribute extends ResourceAttribute {

        public DateAttribute(String name, String subtype) {
            super(name, subtype);
        }

        /**
         * @see SpecAttribute#build(String)
         */
        public Constant build(String str) {
            for (DateFormat format : DATE_FORMATS) {
                try {
                    return Constant.build(format.parse(str), '"' + str + '"');
                } catch (ParseException e) {
                    // Ignore and try the next format
                }
            }
            // The string was not formatted as a DATE - let's check if it's a LONG representing the time
            try {
                return Constant.build(UnmodifiableDate.forTime(Long.parseLong(str)), str);
            } catch (NumberFormatException nfe) {
                // We are out of ideas as to what the format might be - use START_OF_TIME for the value.
                return Constant.build(UnmodifiableDate.START_OF_TIME, "" + UnmodifiableDate.START_OF_TIME.getTime());
            }
        }

        /**
         * @see SpecAttribute#evaluate(IArguments)
         */
        @Override
        public IEvalValue evaluate(IArguments args) {
            if (!(args instanceof IEvaluationRequest)) {
                return IEvalValue.NULL;
            }
            return ((IEvaluationRequest)args).getFromResource().getAttribute(getName());
        }

        /**
         * @see SpecAttribute#validOperators()
         */
        public Set<RelationOp> validOperators() {
            return NUM_DATE_OPS;
        }

        /**
         * @see SpecAttribute#getValueType()
         */
        public ValueType getValueType() {
            return ValueType.LONG;
        }
    }

    // portal support
    public static final ResourceAttribute PORTAL_NAME = new AbstractMatchAttribute("name", PORTAL_SUBTYPE) {
    };

    public static final ResourceAttribute PORTAL_CREATED = new DateAttribute("created", PORTAL_SUBTYPE) {
    };

    public static final ResourceAttribute PORTAL_MODIFIED = new DateAttribute("modified", PORTAL_SUBTYPE) {
    };

    public static final ResourceAttribute PORTAL_CREATED_BY = new AbstractMatchAttribute("created_by", PORTAL_SUBTYPE) {
    };

    public static final ResourceAttribute PORTAL_CREATED_BY_LDAP_GROUP = new AbstractLDAPUserGroup("created_by_ldap_group", PORTAL_SUBTYPE, PORTAL_CREATED_BY) {
    };

    public static final ResourceAttribute PORTAL_CREATED_BY_GROUP = new AbstractDestinyUserGroup("created_by_group", PORTAL_SUBTYPE) {
    };

    public static final ResourceAttribute PORTAL_MODIFIED_BY = new AbstractMatchAttribute("modified_by", PORTAL_SUBTYPE) {
    };

    public static final ResourceAttribute PORTAL_MODIFIED_BY_LDAP_GROUP = new AbstractLDAPUserGroup("modified_by_ldap_group", PORTAL_SUBTYPE, PORTAL_MODIFIED_BY) {
    };

    public static final ResourceAttribute PORTAL_MODIFIED_BY_GROUP = new AbstractDestinyUserGroup("modified_by_group", PORTAL_SUBTYPE) {
    };

    public static final ResourceAttribute PORTAL_FILESIZE = new AbstractMatchAttribute("file_size", PORTAL_SUBTYPE) {
    };

    public static final ResourceAttribute PORTAL_TITLE = new AbstractMatchAttribute("title", PORTAL_SUBTYPE) {
    };

    public static final ResourceAttribute PORTAL_DESC = new AbstractMatchAttribute("desc", PORTAL_SUBTYPE) {
    };

    public static final ResourceAttribute PORTAL_SUB_TYPE = new AbstractMatchAttribute("sub_type", PORTAL_SUBTYPE) {
    };

    public static final ResourceAttribute PORTAL_TYPE = new AbstractMatchAttribute("type", PORTAL_SUBTYPE) {
    };

    public static final ResourceAttribute PORTAL_URL = new AbstractMatchAttribute("url", PORTAL_SUBTYPE) {
    };

    /**
     * Constructor
     * @param name
     */
    protected ResourceAttribute(String name, String resSubtype) {
        super(name, new MultipartKey(name, resSubtype));
        this.subtype = resSubtype;
    }

    public SpecType getSpecType() {
        return SpecType.RESOURCE;
    }

    public String getObjectSubTypeName() {
        return subtype;
    }

    public static ResourceAttribute getElement(String name, String subtype) {
        return (ResourceAttribute)getRegistered(key(name, subtype));
    }

    private static MultipartKey key(String name, String subtype) {
        return new MultipartKey(name, subtype);
    }

    /**
     * Factory method for making resource attributes.
     * @param name the name of the resource attribute.
     * @return a new or an existing resource attribute.
     */
    public static ResourceAttribute forNameAndType(String name, String subtype) {
        return forNameAndType(name, subtype, false);
    }

    /**
     * Factory method for making resource attributes.
     * @param name the name of the resource attribute.
     * @return a new or an existing resource attribute.
     */
    public static ResourceAttribute forNameAndType(String name, String subtype, boolean onlyBuiltin) {

        MultipartKey key = key(name, subtype);
        if (isRegistered(key)) {
            return (ResourceAttribute)getRegistered(key);
        } else if (!onlyBuiltin) {
            if (DESTINY_SUBTYPE.equals(subtype)) {
                return new ResourceAttribute("owner", DESTINY_SUBTYPE) {

                    public IEvalValue evaluate(IArguments args) {
                        IPResource resource = (IPResource)args;
                        return EvalValue.build(resource.getOwner().getId().longValue());
                    }

                    public Set<RelationOp> validOperators() {
                        return Collections.emptySet();
                    }

                    public ValueType getValueType() {
                        return null;
                    }
                };
            } else {
                return new ResourceAttribute(name, subtype) {

                    public IEvalValue evaluate(IArguments args) {
                        if (!(args instanceof IEvaluationRequest)) {
                            return IEvalValue.NULL;
                        }

                        IEvaluationRequest req = (IEvaluationRequest)args;
                        IEvalValue res = req.getFromResource().getAttribute(getName());

                        if (res == IEvalValue.NULL) {
                            IServiceProviderManager serviceProviderManager = req.getServiceProviderManager();

                            for (IResourceAttributeProvider rp : serviceProviderManager.getAllServiceProvidersByType(IResourceAttributeProvider.class)) {
                                try {
                                    IEvalValue val = rp.getAttribute(req.getFromResource(), getName());

                                    if (val != null) {
                                        return val;
                                    }
                                } catch (ServiceProviderException spe) {
                                    // Continue with next service provider
                                }
                            }
                        }

                        return res;
                    }

                    public ValueType getValueType() {
                        return ValueType.STRING;
                    }

                    public Set<RelationOp> validOperators() {
                        return STRING_OPS;
                    }
                };
            }
        } else {
            return null;
        }
    }

    public boolean isLDAPGroupReference() {
        return false;
    }

    public boolean isDestinyGroupReference() {
        return false;
    }

}
