package com.bluejungle.pf.destiny.parser;

/*
 * All sources, binaries and HTML pages (C) Copyright 2004 by Blue Jungle Inc, Redwood City, CA. Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author Sergey Kalinichenko
 *
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/destiny/parser/DomainObjectBuilder.java#1 $
 */
import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

import com.bluejungle.framework.expressions.IPredicate;
import com.bluejungle.pf.domain.destiny.common.AccessPolicy;
import com.bluejungle.pf.domain.destiny.common.IAccessPolicy;
import com.bluejungle.pf.domain.destiny.common.IDSpec;
import com.bluejungle.pf.domain.destiny.common.SpecBase;
import com.bluejungle.pf.domain.destiny.policy.IDPolicy;
import com.bluejungle.pf.domain.destiny.policy.Policy;
import com.bluejungle.pf.domain.destiny.subject.Location;
import com.bluejungle.pf.domain.destiny.subject.Subject;
import com.bluejungle.pf.domain.destiny.subject.SubjectType;
import com.bluejungle.pf.domain.epicenter.common.SpecType;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;

/**
 * This class interacts with the ParseTreeWalker and uses the object factories to build the policy objects described by the PQL parse tree.
 *
 * @author sergey
 */
public class DomainObjectBuilder {

    /**
     * Convenience method to process a collection of PQL-containing objects.
     * @param pqls a Collection of objects that implement IHasPQL interface
     * @param visitor the PQL visitor to which the PQL events are reported.
     * @throws PQLException with the embedded cause when the PQL is not valid
     * @throws IllegalArgumentException if one of the members of pqls does not implement IHasPQL
     */
    public static void processInternalPQL(Collection<? extends IHasPQL> pqls, IPQLVisitor visitor) throws PQLException {
        for (IHasPQL pqlElement : pqls) {
            String pql = pqlElement.getPql();
            DomainObjectBuilder dob = new DomainObjectBuilder(pql);
            dob.processInternalPQL(visitor);
        }
    }

    /**
     * A Facade method to process a single PQL <code>String</code>.
     * @param pqlStream a PQL <code>InputStream</code>.
     * @param visitor the PQL visitor to which the PQL events are reported.
     * @throws PQLException with the embedded cause when the PQL is not valid.
     */
    public static void processInternalPQL(InputStream pqlStream, IPQLVisitor visitor) throws PQLException {
        DomainObjectBuilder dob = new DomainObjectBuilder(pqlStream);
        dob.processInternalPQL(visitor);
    }

    /**
     * A Facade method to process a single PQL <code>String</code>.
     * @param pqlReader a PQL <code>Reader</code>.
     * @param visitor the PQL visitor to which the PQL events are reported.
     * @throws PQLException with the embedded cause when the PQL is not valid.
     */
    public static void processInternalPQL(Reader pqlReader, IPQLVisitor visitor) throws PQLException {
        DomainObjectBuilder dob = new DomainObjectBuilder(pqlReader);
        dob.processInternalPQL(visitor);
    }

    /**
     * A Facade method to process a single PQL <code>String</code>.
     * @param pql a PQL <code>String</code>.
     * @param visitor the PQL visitor to which the PQL events are reported.
     * @throws PQLException with the embedded cause when the PQL is not valid.
     */
    public static void processInternalPQL(String pql, IPQLVisitor visitor) throws PQLException {
        DomainObjectBuilder dob = new DomainObjectBuilder(pql);
        dob.processInternalPQL(visitor);
    }

    private PQLLexer lex;

    /**
     * Constructor. Creates an instance using input stream containing pql.
     *
     * @param stream
     */
    public DomainObjectBuilder(InputStream stream) {
        lex = new PQLLexer(stream);
    }

    /**
     *
     * Constructor. Creates an instance using string containing pql.
     *
     * @param pql
     */
    public DomainObjectBuilder(String pql) {
        this(new StringReader(pql));
    }

    /**
     *
     * Constructor. Creates an instance using reader containing pql.
     *
     * @param reader
     */
    public DomainObjectBuilder(Reader reader) {
        lex = new PQLLexer(reader);
    }

    /**
     * Processes all the user-visible pql and calls the visitor as appropriate.
     *
     * @param visitor
     * @throws PQLException
     */
    public void processPQL(IPQLVisitor visitor) throws PQLException {
        try {
            PQLParser parser = new PQLParser(lex);
            parser.program();
            AST parseTree = (AST)parser.getAST();
            PQLTreeWalker walker = new PQLTreeWalker(visitor);
            walker.program_def(parseTree);
        } catch (RecognitionException re) {
            throw new PQLException(re);
        } catch (TokenStreamException te) {
            throw new PQLException(te);
        }
    }

    /**
     * Processes all the user-visible pql and calls the visitor as appropriate.
     *
     * @param visitor
     * @throws PQLException
     */
    public void processInternalPQL(IPQLVisitor visitor) throws PQLException {
        try {
            PQLParser parser = new PQLParser(lex);
            parser.program();
            AST parseTree = (AST)parser.getAST();
            PQLTreeWalker walker = new PQLTreeWalker(visitor);
            walker.internal_program_def(parseTree);
        } catch (RecognitionException re) {
            throw new PQLException(re);
        } catch (TokenStreamException te) {
            throw new PQLException(te);
        }
    }

    /**
     * returns a policy contained in the PQL.  If the pql contains
     * more than one policy, the last one is returned.
     *
     * @return parsed policy
     * @throws PQLException with the embedded cause when the PQL does not represent a valid policy
     */
    public IDPolicy processPolicy() throws PQLException {
        OneObjectVisitor v = new OneObjectVisitor();
        processInternalPQL(v);
        return v.getPolicy();
    }

    /**
     * returns a SpecBase object contained in the PQL.  If the pql contains
     * more than one subject spec, the last one is returned.
     *
     * @return parsed subject spec
     * @throws PQLException with the embedded cause when the PQL does not represent a valid subject spec
     */
    public IDSpec processSpec() throws PQLException {
        OneObjectVisitor v = new OneObjectVisitor();
        processInternalPQL(v);
        return v.getSpec();
    }

    /**
     * return a location contained in the PQL.  If pql contains more than
     * one location, the last one is returned.
     *
     * @return parsed location
     * @throws PQLException
     */
    public Location processLocation() throws PQLException {
        OneObjectVisitor v = new OneObjectVisitor();
        processInternalPQL(v);
        return v.getLocation();
    }

    /**
     * return access policy represented by PQL.  If pql contains more than
     * one access policy, the last one is returned.
     *
     * @return parsed access policy
     * @throws PQLException
     */
    public IAccessPolicy processAccessPolicy() throws PQLException {
        OneObjectVisitor v = new OneObjectVisitor();
        PQLParser parser = new PQLParser(lex);
        try {
            parser.standalone_access_policy();
            AST parseTree = (AST)parser.getAST();
            PQLTreeWalker walker = new PQLTreeWalker(v);
            walker.program_def(parseTree);
        } catch (RecognitionException re) {
            throw new PQLException(re);
        } catch (TokenStreamException te) {
            throw new PQLException(te);
        }
        return v.getAccessPolicy();
    }

    public static class OneObjectVisitor implements IPQLVisitor {

        private IDPolicy policy;
        private Location location;
        private IDSpec spec;
        private IAccessPolicy accessPolicy;

        /**
         * @see IPQLVisitor#visitPolicy(DomainObjectDescriptor, IDPolicy)
         */
        public void visitPolicy(DomainObjectDescriptor descr, IDPolicy policy) {
            this.policy = policy;
            if (descr.getOwner() != null) {
                policy.setOwner(new Subject(descr.getOwner().toString(), descr.getOwner().toString(), descr.getOwner().toString(), descr.getOwner(), SubjectType.USER));
            }
            if (descr.getAccessPolicy() != null) {
                ((Policy)policy).setAccessPolicy((AccessPolicy)descr.getAccessPolicy());
            }
        }

        /**
         * @see IPQLVisitor#visitFolder(DomainObjectDescriptor)
         */
        public void visitFolder(DomainObjectDescriptor descriptor) {
            // Policy Folders do not produce objects
        }

        /**
         * @see IPQLVisitor#visitResource(DomainObjectDescriptor descr, IDResourceSpec)
         */
        public void visitComponent(DomainObjectDescriptor descr, IPredicate pred) {
            spec = new SpecBase(null, SpecType.RESOURCE, descr.getId(), descr.getName(), descr.getDescription(), descr.getStatus(), pred, descr.isHidden());
            if (descr.getOwner() != null) {
                spec.setOwner(new Subject(descr.getOwner().toString(), descr.getOwner().toString(), descr.getOwner().toString(), descr.getOwner(), SubjectType.USER));
            }
            if (descr.getAccessPolicy() != null) {
                ((SpecBase)spec).setAccessPolicy((AccessPolicy)descr.getAccessPolicy());
            }
        }

        /**
         * @see IPQLVisitor#visitAccessPolicy(DomainObjectDescriptor descr, IAccessPolicy)
         */
        public void visitAccessPolicy(DomainObjectDescriptor descr, IAccessPolicy accessPolicy) {
            this.accessPolicy = accessPolicy;
        }

        /**
         * Returns the policy.
         *
         * @return the policy.
         */
        public IDPolicy getPolicy() {
            return this.policy;
        }

        /**
         * @see IPQLVisitor#visitLocation(DomainObjectDescriptor, Location)
         */
        public void visitLocation(DomainObjectDescriptor descriptor, Location location) {
            this.location = location;
        }

        public Location getLocation() {
            return location;
        }

        public IDSpec getSpec() {
            return spec;
        }

        public IAccessPolicy getAccessPolicy() {
            return accessPolicy;
        }
    }
}
