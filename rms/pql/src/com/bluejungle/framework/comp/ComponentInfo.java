package com.bluejungle.framework.comp;

// Copyright Blue Jungle, Inc.

/*
 * represents information sufficient to construct a component.  Component's name,
 * implementation class name, and lifestyle type are required.  Configuration and
 * interface name are optional
 *
 * @author Sasha Vladimirov
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/comp/ComponentInfo.java#1 $
 */

public class ComponentInfo<T> {

    private String name;
    private String className;
    private String interfaceName;
    private LifestyleType lifestyleType;
    private IConfiguration configuration;

    /**
     * Create a simplest ComponentInfo that the name will be <code>clazz.getName()</code>
     * @requires clazz, lifestyleType are not null
     * @param clazz  implementation class
     * @param lifestyleType lifestyle type
     */
    public ComponentInfo(Class<? extends T> clazz, LifestyleType lifestyleType) {
        this(clazz, lifestyleType, null);
    }

    /**
     * Create a ComponentInfo that the name will be <code>clazz.getName()</code>
     * @requires clazz, lifestyleType are not null
     * @param clazz  implementation class
     * @param lifestyleType lifestyle type
     */
    public ComponentInfo(Class<? extends T> clazz, LifestyleType lifestyleType,
        IConfiguration configuration) {
        this(((clazz != null) ? clazz.getName() : null), clazz, null, lifestyleType, configuration);
    }

    /**
     *
     * Constructor
     * @requires name, className, lifestyleType are not null
     * @param name component name
     * @param className implementation class name
     * @param lifestyleType lifestyle type
     * @deprecated replaced by <code>ComponentInfo(String name, Class<? extends T> clazz,
     *                 LifestyleType lifestyleType)</code>
     */
    @Deprecated
    public ComponentInfo(String name, String className, LifestyleType lifestyleType) {
        this(name, className, null, lifestyleType);
    }

    /**
     *
     * Constructor
     * @requires name, clazz, lifestyleType are not null
     * @param name component name
     * @param clazz implementation class
     * @param lifestyleType lifestyle type
     */
    public ComponentInfo(String name, Class<? extends T> clazz, LifestyleType lifestyleType) {
        this(name, clazz, null, lifestyleType);
    }

    /**
     *
     * Constructor
     * @requires name, className, lifestyleType are not null
     * @param name component name
     * @param className implementation class name
     * @param interfaceName interface name
     * @param lifestyleType lifestyle type
     * @deprecated replaced by <code>String name, Class<? extends T> clazz,
     *               Class<? super T> interfaze,LifestyleType lifestyleType</code>
     */
    @Deprecated
    public ComponentInfo(String name, String className, String interfaceName,
        LifestyleType lifestyleType) {
        this(name, className, interfaceName, lifestyleType, null);
    }

    /**
     *
     * Constructor
     * @requires name, clazz, lifestyleType are not null
     * @param name component name
     * @param clazz implementation class
     * @param interfaze interface
     * @param lifestyleType lifestyle type
     */
    public ComponentInfo(String name, Class<? extends T> clazz, Class<? super T> interfaze,
        LifestyleType lifestyleType) {
        this(name, clazz, interfaze, lifestyleType, null);
    }

    /**
     *
     * Constructor
     * @requires name, className, lifestyleType are not null
     * @param name component name
     * @param className implementation class name
     * @param interfaceName interface name
     * @param lifestyleType lifestyle type
     * @param configuration configuration
     * @deprecated  replaced by <code>ComponentInfo(String name, Class<? extends T> clazz,
     *                 Class<? super T> interfaze, LifestyleType lifestyleType,
     *                 IConfiguration configuration)</code>
     */
    @Deprecated
    public ComponentInfo(String name, String className, String interfaceName,
        LifestyleType lifestyleType, IConfiguration configuration) {
        super();
        this.name = name;
        this.className = className;
        this.interfaceName = interfaceName;
        this.lifestyleType = lifestyleType;
        this.configuration = configuration;
    }

    /**
     *
     * Constructor
     * @requires name, className, lifestyleType are not null
     * @param name component name
     * @param clazz implementation class
     * @param interfaze interface
     * @param lifestyleType lifestyle type
     * @param configuration configuration
     */
    public ComponentInfo(String name, Class<? extends T> clazz, Class<? super T> interfaze,
        LifestyleType lifestyleType, IConfiguration configuration) {
        super();
        this.name = name;
        this.className = clazz != null ? clazz.getName() : null;
        this.interfaceName = interfaze != null ? interfaze.getName() : null;
        this.lifestyleType = lifestyleType;
        this.configuration = configuration;
    }

    /**
     * @return the name of the component's implementation class.
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return the name of the interface this component implements
     */
    public String getInterfaceName() {
        return interfaceName;
    }

    /**
     * @return the lifestyle type of this component
     */
    public LifestyleType getLifestyleType() {
        return lifestyleType;
    }

    /**
     * @return this component's name
     */
    public String getName() {
        return name;
    }

    /**
     * @return this component's configuration
     */
    public IConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * This overrides the existing configuration of this component
     * info with the supplied one.  Properties that exist in the
     * current configuration but do not exist in the overriding one
     * are left untouched.
     *
     * @param config overriding configuration
     */
    public void overrideConfiguration(IConfiguration config) {
        if (configuration == null) {
            configuration = config;
        } else {
            configuration.override(config);
        }
    }
}
