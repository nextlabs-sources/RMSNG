package com.bluejungle.framework.expressions;

/*
 * All sources, binaries and HTML pages (C) Copyright 2005 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author sergey
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/framework/src/java/main/com/bluejungle/framework/expressions/Predicates.java#1 $
 */

import java.util.LinkedList;
import java.util.Stack;

/**
 * Collection of static utility methods for manipulating predicates.
 */
public class Predicates {

    /**
     * Provides an interface for use in the find() method.
     */
    public interface IDetector {

        /**
         * Checks a composite predicate.
         * @param pred the composite predicate to check.
         * @param preorder when true, indicates that the contained predicates
         * have not been checked at this time; false indicates that all contained
         * predicates have already been checked.
         */
        boolean checkComposite(ICompositePredicate pred, boolean preorder);

        /**
         * Checks a predicate reference.
         * @param pred the predicate reference to check.
         */
        boolean checkReference(IPredicateReference pred);

        /**
         * Checks a relation.
         * @param pred the relation to check.
         */
        boolean checkRelation(IRelation pred);

        /**
         * Checks a leaf or an externally defined predicate.
         * @param pred the leaf predicate to check.
         */
        boolean check(IPredicate pred);
    }

    /**
     * Provides a default implementation of the IDetector interface.
     * All methods of this class return false for all predicates.
     */
    public static class DefaultDetector implements IDetector {

        public boolean checkComposite(ICompositePredicate pred, boolean preorder) {
            return false;
        }

        public boolean checkReference(IPredicateReference pred) {
            return false;
        }

        public boolean checkRelation(IRelation pred) {
            return false;
        }

        public boolean check(IPredicate pred) {
            return false;
        }
    }

    /**
     * Defines an interface for use in transform() method.
     */
    public interface ITransformer {

        /**
         * Prepares to transform a composite predicate.
         * @param pred the composite predicate about to be enumerated.
         */
        void transformCompositeStart(ICompositePredicate pred);

        /**
         * Transforms a composite predicate.
         * @param orig the original composite predicate being transformed.
         * @param res the resulting composite after the trasnformation.
         */
        IPredicate transformCompositeEnd(ICompositePredicate orig, IPredicate res);

        /**
         * Transforms a predicate reference.
         * @param pred the predicate reference to ransform.
         */
        IPredicate transformReference(IPredicateReference pred);

        /**
         * Transforms a relation.
         * @param pred the relation to ransform.
         */
        IPredicate transformRelation(IRelation pred);

        /**
         * Transforms a leaf or an externally defined predicate.
         * @param pred the leaf predicate to ransform.
         */
        IPredicate transform(IPredicate pred);
    }

    /**
     * Provides a NULL-implementation of transformation
     * (i.e. no transformation).
     */
    public static class DefaultTransformer implements ITransformer {

        public void transformCompositeStart(ICompositePredicate pred) {
        }

        public IPredicate transformCompositeEnd(ICompositePredicate orig, IPredicate res) {
            return res;
        }

        public IPredicate transformReference(IPredicateReference pred) {
            return pred;
        }

        public IPredicate transformRelation(IRelation pred) {
            return pred;
        }

        public IPredicate transform(IPredicate pred) {
            return pred;
        }
    }

    /**
     * Searches the given <code>IPredicate</code> with the specified
     * <code>IDetector</code>, and returns the result.
     * @param pred the predicate to be searched.
     * @param detector the detector to be used for searching.
     * @return true if the detector returned "true" for any of the
     * subpredicates, false otherwise.
     */
    public static boolean find(IPredicate pred, final IDetector detector) {
        if (pred == null) {
            return false;
        }
        if (detector == null) {
            throw new NullPointerException("detector");
        }
        class Finder implements IPredicateVisitor {

            boolean found = false;

            public void visit(ICompositePredicate pred, boolean preorder) {
                found |= detector.checkComposite(pred, preorder);
            }

            public void visit(IPredicateReference pred) {
                found |= detector.checkReference(pred);
            }

            public void visit(IRelation pred) {
                found |= detector.checkRelation(pred);
            }

            public void visit(IPredicate pred) {
                found |= detector.check(pred);
            }

            public boolean found() {
                return found;
            }
        }
        Finder finder = new Finder();
        pred.accept(finder, IPredicateVisitor.PREPOSTORDER);
        return finder.found();
    }

    /**
     * Transforms the predicate using the provided transformer.
     * @param pred the predicate to be transformed.
     * @param transformer the transformer that performs the transformation.
     * @return the transformed predicate, or null if the transformer has removed
     * everything from the original predicate.
     */
    public static IPredicate transform(IPredicate pred, final ITransformer transformer) {
        if (pred == null) {
            return null;
        }
        if (transformer == null) {
            throw new NullPointerException("transformer");
        }
        class TransformingVisitor implements IPredicateVisitor {

            private final Stack<IPredicate> predicateStack = new Stack<IPredicate>();

            public void visit(ICompositePredicate pred, boolean preorder) {
                if (preorder) {
                    transformer.transformCompositeStart(pred);
                } else {
                    IPredicate tmp;
                    if (pred.getOp() == BooleanOp.AND) {
                        tmp = visitAnd(pred.predicateCount());
                    } else if (pred.getOp() == BooleanOp.OR) {
                        tmp = visitOr(pred.predicateCount());
                    } else if (pred.getOp() == BooleanOp.NOT) {
                        if (pred.predicateCount() == 1) {
                            tmp = visitNot();
                        } else {
                            throw new IllegalArgumentException(
                                    "NOT must have exactly one argument (got " + pred.predicateCount() + ")");
                        }
                    } else {
                        // Unknown operations are processed generically
                        LinkedList<IPredicate> children = new LinkedList<IPredicate>();
                        for (int i = 0; i != pred.predicateCount(); i++) {
                            IPredicate top = predicateStack.pop();
                            if (top != null) {
                                children.addFirst(top);
                            }
                        }
                        switch (children.size()) {
                            case 0:
                                tmp = null;
                                break;
                            case 1:
                                tmp = children.getFirst();
                                break;
                            default:
                                tmp = new CompositePredicate(pred.getOp(), children);
                                break;
                        }
                    }
                    predicateStack.push(transformer.transformCompositeEnd(pred, tmp));
                }
            }

            private IPredicate visitAnd(int count) {
                LinkedList<IPredicate> children = new LinkedList<IPredicate>();
                boolean seenNull = false;
                boolean seenTrue = false;
                boolean seenFalse = false;
                for (int i = 0; i != count; i++) {
                    IPredicate top = predicateStack.pop();
                    if (top == null) {
                        seenNull = true;
                        continue;
                    }
                    if (top == PredicateConstants.TRUE) {
                        seenTrue = true;
                        continue;
                    }
                    if (top == PredicateConstants.FALSE) {
                        seenFalse = true;
                        continue;
                    }
                    children.addFirst(top);
                }
                if (seenFalse) {
                    return PredicateConstants.FALSE;
                }
                switch (children.size()) {
                    case 0:
                        if (seenTrue && !seenNull) {
                            return PredicateConstants.TRUE;
                        } else {
                            return null;
                        }
                    case 1:
                        return children.getFirst();
                    default:
                        return new CompositePredicate(BooleanOp.AND, children);
                }
            }

            private IPredicate visitOr(int count) {
                LinkedList<IPredicate> children = new LinkedList<IPredicate>();
                boolean seenNull = false;
                boolean seenTrue = false;
                boolean seenFalse = false;
                for (int i = 0; i != count; i++) {
                    IPredicate top = predicateStack.pop();
                    if (top == null) {
                        seenNull = true;
                        continue;
                    }
                    if (top == PredicateConstants.TRUE) {
                        seenTrue = true;
                        continue;
                    }
                    if (top == PredicateConstants.FALSE) {
                        seenFalse = true;
                        continue;
                    }
                    children.addFirst(top);
                }
                if (seenTrue) {
                    return PredicateConstants.TRUE;
                }
                switch (children.size()) {
                    case 0:
                        if (seenFalse && !seenNull) {
                            return PredicateConstants.FALSE;
                        } else {
                            return null;
                        }
                    case 1:
                        return children.getFirst();
                    default:
                        return new CompositePredicate(BooleanOp.OR, children);
                }
            }

            private IPredicate visitNot() {
                IPredicate top = predicateStack.pop();
                if (top == PredicateConstants.TRUE) {
                    return PredicateConstants.FALSE;
                } else if (top == PredicateConstants.FALSE) {
                    return PredicateConstants.TRUE;
                } else if (top == null) {
                    return null;
                } else {
                    return new CompositePredicate(BooleanOp.NOT, top);
                }
            }

            public void visit(IPredicateReference pred) {
                predicateStack.push(transformer.transformReference(pred));
            }

            public void visit(IRelation pred) {
                predicateStack.push(transformer.transformRelation(pred));
            }

            public void visit(IPredicate pred) {
                predicateStack.push(transformer.transform(pred));
            }

            public IPredicate transformed() {
                if (predicateStack.size() != 1) {
                    return PredicateConstants.FALSE;
                }
                return predicateStack.pop();
            }
        }
        TransformingVisitor transformingVisitor = new TransformingVisitor();
        pred.accept(transformingVisitor, IPredicateVisitor.PREPOSTORDER);
        return transformingVisitor.transformed();
    }
}
