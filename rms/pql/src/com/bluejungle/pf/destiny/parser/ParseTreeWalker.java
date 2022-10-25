package com.bluejungle.pf.destiny.parser;

/* All sources, binaries and HTML pages (C) Copyright 2004 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author Sergey Kalinichenko
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/destiny/parser/ParseTreeWalker.java#1 $
 */

import antlr.collections.AST;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Walks the AST produced by ANTLR emitting SAX2-like "events."
 * @author sergey
 */
public class ParseTreeWalker {

    /**
     * Walks the AST produced by the PQL parser, reporting
     * SAX-like events back to the handler.
     * @param tree the AST tree to be walked.
     * @param h the handler to which to report the events.
     * @throws SAXException when the underlying handler reports an exception.
     */
    public static void walk(AST tree, ContentHandler h) throws SAXException {
        h.startDocument();
        subWalk(tree, h, true);
        h.endDocument();
    }

    /**
     * This method does the actual walking. The outer walk method is
     * there to keep the interface clean, and to call the start/end
     * of the document.
     * @param tree the AST (sub)tree to be walked.
     * @param h the handler to which to report the events.
     * @param siblings indicates whether the siblings of this node
     * are to be printed.
     * @throws SAXException when the underlying handler reports an exception.
     */
    private static void subWalk(AST tree, ContentHandler h, boolean siblings) throws SAXException {
        if (tree == null) {
            // null trees are acceptable - they are considered empty
            return;
        }
        // Deal with the current node
        String typeStr = PQLParser._tokenNames[tree.getType()].toUpperCase().replaceAll("^\"", "").replaceAll("\"$", "");
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute("", "value", "", "string", tree.getText());
        h.startElement("", typeStr, "", attributes);
        // Show all of its children (note the siblings flag is set to true)
        subWalk(tree.getFirstChild(), h, true);
        h.endElement("", typeStr, "");
        while (siblings && tree != null) {
            tree = tree.getNextSibling();
            subWalk(tree, h, false);
        }
    }
}
