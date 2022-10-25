
package com.nextlabs.rms.destiny.framework.types;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.bluejungle.destiny.framework.types package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.bluejungle.destiny.framework.types
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link BadArgumentFault }
     *
     */
    public BadArgumentFault createBadArgumentFault() {
        return new BadArgumentFault();
    }

    /**
     * Create an instance of {@link CommitFault }
     *
     */
    public CommitFault createCommitFault() {
        return new CommitFault();
    }

    /**
     * Create an instance of {@link ServiceNotReadyFault }
     *
     */
    public ServiceNotReadyFault createServiceNotReadyFault() {
        return new ServiceNotReadyFault();
    }

    /**
     * Create an instance of {@link UnauthorizedCallerFault }
     *
     */
    public UnauthorizedCallerFault createUnauthorizedCallerFault() {
        return new UnauthorizedCallerFault();
    }

    /**
     * Create an instance of {@link UnknownEntryFault }
     *
     */
    public UnknownEntryFault createUnknownEntryFault() {
        return new UnknownEntryFault();
    }

    /**
     * Create an instance of {@link UniqueConstraintViolationFault }
     *
     */
    public UniqueConstraintViolationFault createUniqueConstraintViolationFault() {
        return new UniqueConstraintViolationFault();
    }

}
