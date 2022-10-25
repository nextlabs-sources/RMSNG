package com.bluejungle.domain.log;

/*
 * All sources, binaries and HTML pages (C) copyright 2004-2007 by NextLabs, Inc.,
 * San Mateo CA, Ownership remains with NextLabs, Inc, All rights reserved
 * worldwide.
 */

import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.IMultivalue;
import com.bluejungle.framework.utils.DynamicAttributes;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GenericAttributeExternalizer {

    static Set<String> internalAttributes;

    static {
        internalAttributes = new HashSet<String>();

        internalAttributes.add("access_date");
        internalAttributes.add("created_date");
        internalAttributes.add("isdirectory");
        internalAttributes.add("size");
        internalAttributes.add("created");
        internalAttributes.add("modified");
        internalAttributes.add("modified_by");
        internalAttributes.add("filegroup");
        internalAttributes.add("modified_date");
        internalAttributes.add("owner");
        internalAttributes.add("owner_ldap_group");
        internalAttributes.add("name");
    }

    // Used by the V2 code
    public static void externalizeAttributes(ObjectOutput out, DynamicAttributes attr) throws IOException {
        if (attr != null) {
            // We don't write everything - keys that start ce:: or are a known 'internal' key
            // should not be logged.
            int attrSize = 0;
            for (Map.Entry<String, IEvalValue> entry : attr.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith("ce::") && !internalAttributes.contains(key)) {
                    attrSize++;
                }
            }

            // Can't just use attr.size()
            out.writeInt(attrSize);
            for (Map.Entry<String, IEvalValue> entry : attr.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith("ce::") && !internalAttributes.contains(key)) {
                    out.writeUTF(key + ", " + entry.getValue().getValue());
                }
            }
        } else {
            out.writeInt(0);
        }

        return;
    }

    // Used by the V5 code
    public static Map<String, DynamicAttributes> readAttributes(ObjectInput in) throws IOException {
        Map<String, DynamicAttributes> attrMap = null;
        DynamicAttributes attr = null;

        int numAttrs = in.readInt();
        String attrType = in.readUTF();
        if (numAttrs >= 0) {
            attrMap = new HashMap<String, DynamicAttributes>();
            attr = new DynamicAttributes();

            for (int i = 0; i < numAttrs; i++) {
                String key = in.readUTF();

                char flag = in.readChar();

                switch (flag) {
                    case 'S':
                        // Single value
                        String value = in.readUTF();
                        attr.put(key, value);
                        break;
                    case 'M':
                        // Multivalue
                        int numValues = in.readInt();
                        for (int j = 0; j < numValues; j++) {
                            String mvalue = in.readUTF();
                            attr.add(key, mvalue);
                        }
                        break;
                    case 'U':
                        // Unknown value. Ignore
                        break;
                    default:
                        throw new IOException("Illegal flag in attributes. Got " + flag + ", but expecting S or M");
                }
            }
        }
        attrMap.put(attrType, attr);
        return attrMap;
    }

    public static void writeAttributes(ObjectOutput out, String attrType, DynamicAttributes attr) throws IOException {
        if (attr != null) {
            // We don't write everything - keys that start ce:: or are a known 'internal' key
            // should not be logged.
            int attrSize = 0;
            for (Map.Entry<String, IEvalValue> entry : attr.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith("ce::") && !internalAttributes.contains(key)) {
                    attrSize++;
                }
            }

            // Can't just use attr.size()
            out.writeInt(attrSize);
            out.writeUTF(attrType);
            for (Map.Entry<String, IEvalValue> entry : attr.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith("ce::") && !internalAttributes.contains(key)) {
                    Object value = entry.getValue().getValue();
                    out.writeUTF(key);

                    if (value instanceof String) {
                        out.writeChar('S');
                        writeAttributeValue(out, (String)value);
                    } else if (value instanceof IMultivalue) {
                        out.writeChar('M');
                        IMultivalue mvalue = (IMultivalue)value;

                        out.writeInt(mvalue.size());
                        for (IEvalValue subvalue : (IMultivalue)value) {
                            writeAttributeValue(out, (String)subvalue.getValue());
                        }
                    } else {
                        // Unknown. We have to write something, because we've already specified the number of attributes
                        out.writeChar('U');
                    }
                }
            }
        } else {
            // We need a way to distinguish between a 0-sized set of attributes and a null
            out.writeInt(-1);
        }

        return;
    }

    static void writeAttributeValue(ObjectOutput out, String value) throws IOException {
        // Remarkably, writeUTF will throw an exception if the string is > 64K bytes in size.
        // We don't actually need anywhere near that much, because we only store 4K of the value in
        // Determining the actual number of bytes of a string depends on the character encoding, so
        // rather than get cute, I'll truncate the value to 8K. This should be (a) small enough
        // that we can externalize it and (b) bigger than the largest amount we can store in the DB

        if (value.length() > 8192) {
            value = value.substring(0, 8192);
        }
        out.writeUTF(value);
    }
}
