package com.bluejungle.pf.destiny.lifecycle;

/* All sources, binaries and HTML pages (C) Copyright 2004 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author Sergey Kalinichenko
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/destiny/lifecycle/EntityType.java#1 $
 */

import com.bluejungle.framework.patterns.EnumBase;
import com.bluejungle.pf.destiny.parser.PQLParser;
import com.bluejungle.pf.domain.epicenter.common.SpecType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This type-safe enumeration represents the type of a development
 * or a deployment entity.
 */
public abstract class EntityType extends EnumBase {

    private final SpecType specType;

    /** Maps SpecType objects to EntityType objects. */
    private static final Map<SpecType, EntityType> forSpecType = new HashMap<SpecType, EntityType>();

    public static final EntityType POLICY = new EntityType("POLICY", SpecType.ILLEGAL) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            return "POLICY " + PQLParser.quoteName(name) + " FOR EMPTY ON EMPTY BY EMPTY DO DENY BY DEFAULT DO ALLOW";
        }
    };

    /**
     * @deprecated
     */
    public static final EntityType HOST = new EntityType("HOST", SpecType.HOST) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            return "COMPONENT " + PQLParser.quoteName(name) + " = EMPTY";
        }
    };

    /**
     * @deprecated
     */
    public static final EntityType USER = new EntityType("USER", SpecType.USER) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            return "COMPONENT " + PQLParser.quoteName(name) + " = EMPTY";
        }
    };

    /**
     * @deprecated
     */
    public static final EntityType APPLICATION = new EntityType("APPLICATION", SpecType.APPLICATION) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            return "COMPONENT " + PQLParser.quoteName(name) + " = EMPTY";
        }
    };

    /**
     * @deprecated
     */
    public static final EntityType RESOURCE = new EntityType("RESOURCE", SpecType.RESOURCE) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            return "COMPONENT " + PQLParser.quoteName(name) + " = EMPTY";
        }
    };

    /**
     * @deprecated
     */
    public static final EntityType PORTAL = new EntityType("PORTAL", SpecType.PORTAL) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            return "COMPONENT " + PQLParser.quoteName(name) + " = EMPTY";
        }
    };

    /**
     * @deprecated
     */
    public static final EntityType ACTION = new EntityType("ACTION", SpecType.ACTION) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            return "COMPONENT " + PQLParser.quoteName(name) + " = EMPTY";
        }
    };

    public static final EntityType COMPONENT = new EntityType("COMPONENT", SpecType.ILLEGAL) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            return "COMPONENT " + PQLParser.quoteName(name) + " = EMPTY";
        }
    };

    public static final EntityType DEVICE = new EntityType("DEVICE", SpecType.ILLEGAL) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            return "COMPONENT " + PQLParser.quoteName(name) + " = EMPTY";
        }
    };

    public static final EntityType SAP = new EntityType("SAP", SpecType.ILLEGAL) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            return "COMPONENT " + PQLParser.quoteName(name) + " = EMPTY";
        }
    };

    public static final EntityType ENOVIA = new EntityType("ENOVIA", SpecType.ILLEGAL) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            return "COMPONENT " + PQLParser.quoteName(name) + " = EMPTY";
        }
    };

    public static final EntityType FOLDER = new EntityType("FOLDER", SpecType.ILLEGAL) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            return "FOLDER " + PQLParser.quoteName(name);
        }
    };

    public static final EntityType LOCATION = new EntityType("LOCATION", SpecType.ILLEGAL) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            return "LOCATION " + PQLParser.quoteName(name) + " = \"EMPTY\"";
        }

    };

    public static final EntityType ILLEGAL = new EntityType("ILLEGAL", SpecType.ILLEGAL) {

        private static final long serialVersionUID = 1L;

        public String emptyPql(String name) {
            throw new UnsupportedOperationException("Instantiating entities of the type <ILLEGAL> is illegal.");
        }
    };

    /**
     * The constructor is private to prevent unwanted instanciations from the outside.
     * @param name is passed through to the constructor of the superclass.
     */
    private EntityType(String name, SpecType specType) {
        super(name, EntityType.class);
        this.specType = specType;
        forSpecType.put(specType, this);
    }

    /**
     * Returns an empty PQL string with the given name.
     * @param name the name of the entity.
     * @return an empty PQL string with the given name.
     */
    public abstract String emptyPql(String name);

    public SpecType getSpecType() {
        return specType;
    }

    /**
     * Returns the number of distinct <code>EntityType</code>s.
     * @return the number of distinct <code>EntityType</code>s.
     */
    public static int getElementCount() {
        return numElements(EntityType.class);
    }

    /**
     * Gets an EntityType by its integer type.
     * @param enumType type of the enumeration element.
     * @return  enumeration element.
     * @throws IllegalArgumentException if there is no
     * enumeration element of type enumType, or there are no
     * enumeration elements added for class clazz.
     */
    public static EntityType forType(int entityType) {
        return getElement(entityType, EntityType.class);
    }

    /**
     * Gets an <code>EntityType</code> by its string name.
     * @param name the name of the desired entity.
     * @return an <code>EntityType</code> by its string name.
     * @throws IllegalArgumentException if there is no
     * enumeration element with name enumName, or there are no
     * enumeration elements added for class clazz
     */
    public static EntityType forName(String name) {
        return getElement(name, EntityType.class);
    }

    /**
     * Checks existence of an <code>EntityType</code> by its string
     * name.
     * @param name the name of the desired entity
     * @return true if the element exists
     */
    public static boolean existsElement(String name) {
        return existsElement(name, EntityType.class);
    }

    /**
     * Given a <code>SpecType</code>, returns the corresponding <code>EntityType</code>.
     * @param specType the <code>SpecType</code> for which you need the <code>EntityType</code>.
     * @return The <code>EntityType</code> corresponding to the given <code>SpecType</code>.
     */
    public static EntityType forSpecType(SpecType specType) {
        EntityType res = forSpecType.get(specType);
        return res != null ? res : ILLEGAL;
    }

    public static Set<EntityType> elements() {
        return elements(EntityType.class);
    }

}
