/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
 *
 *  The Sun Project JXTA(TM) Software License
 *
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.impl.document;

import net.jxta.document.Attributable;
import net.jxta.document.Attribute;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.TextElement;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is an implementation of the StructuredTextDocument interface using
 * simple text
 */
public class PlainTextElement implements TextElement<PlainTextElement>, Attributable {
    protected PlainTextDocument doc;

    protected PlainTextElement parent;

    protected final String name;

    protected final String val;

    private final List<PlainTextElement> children = new ArrayList<PlainTextElement>();

    private final Map<String,String> attributes = new HashMap<String,String>();

    /**
     * Creates new PlainTextElement
     */
    protected PlainTextElement(PlainTextDocument doc, String name) {
        this(doc, name, null);
    }

    /**
     * Creates new PlainTextElement
     */
    protected PlainTextElement(PlainTextDocument doc, String name, String val) {
        this.doc = doc;
        this.name = name;
        this.val = val;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object element) {
        if (this == element) {
            return true;
        }

        if (!(element instanceof PlainTextElement)) {
            return false;
        }

        PlainTextElement textElement = (PlainTextElement) element;

        if (doc != textElement.doc) {
            return false;
        }

        if (!getName().equals(textElement.getName())) {
            return false;
        }

        String val1 = getTextValue();
        String val2 = textElement.getTextValue();

        if ((null == val1) && (null == val2)) {
            return true;
        }

        if ((null == val1) || (null == val2)) {
            return false;
        }

        return val1.equals(val2);
    }

    /**
     * {@inheritDoc}
     */
    public StructuredTextDocument getRoot() {
        return (StructuredTextDocument) doc;
    }

    /**
     * Get the name associated with an element.
     *
     * @return A string containing the key of this element.
     */
    public String getKey() {
        return getName();
    }

    /**
     * Get the value (if any) associated with an element.
     *
     * @return A string containing the value of this element, if any, otherwise null.
     */
    public String getValue() {
        return getTextValue();
    }

    /**
     * {@inheritDoc}
     */
    public PlainTextElement getParent() {
        return parent;
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration<PlainTextElement> getChildren() {
        return Collections.enumeration(children);
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String getTextValue() {
        return val;
    }

    /**
     * {@inheritDoc}
     */
    public void appendChild(PlainTextElement element) {
        if (element.doc != this.doc) {
            throw new IllegalArgumentException("Wrong Document");
        }

        element.parent = this;
        children.add(element);
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration<PlainTextElement> getChildren(Object key) {
        if (key instanceof String)
            return getChildren((String) key);
        else
            throw new ClassCastException(key.getClass().getName() + " not supported by getChildren.");
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration<PlainTextElement> getChildren(String name) {
        List<PlainTextElement> result = new ArrayList<PlainTextElement>();

        for (PlainTextElement aChild : children) {
            if (name.equals(aChild.getName())) {
                result.add(aChild);
            }
        }

        return Collections.enumeration(result);
    }

    /**
     * Write the contents of this element and optionally its children. The
     * writing is done to a provided java.io.Writer. The writing can optionally
     * be indented
     *
     * @param into    The java.io.Writer that the output will be sent to.
     * @param indent  the number of tabs which will be inserted before each
     *                line.
     * @param recurse if true then also print the children of this element.
     */
    protected void printNice(Writer into, int indent, boolean recurse) throws IOException {

        // do indent
        for (int eachTab = 0; eachTab < indent; eachTab++) {
            into.write("\t");
        }

        // print node name
        into.write(name);

        // print attributes
        Enumeration<Attribute> attributes = getAttributes();

        if (attributes.hasMoreElements()) {
            into.write(" ( ");

            while (attributes.hasMoreElements()) {
                Attribute anAttr = attributes.nextElement();

                into.write(anAttr.getName() + "=\"" + anAttr.getValue() + "\" ");
            }
            into.write(")");
        }

        into.write(" : ");
        // print node value
        if (null != val) {
            into.write(val + "\n");
        } else {
            into.write("\n");
        }

        // recurse as needed
        if (recurse) {
            for (Enumeration<PlainTextElement> childrens = getChildren(); childrens.hasMoreElements();) {
                childrens.nextElement().printNice(into, indent + 1, recurse);
            }
        }
    }

    // Attributable methods

    /**
     * {@inheritDoc}
     */
    public String addAttribute(String name, String value) {

        String oldAttrValue = attributes.remove(name);

        attributes.put(name, value);

        return oldAttrValue;
    }

    /**
     * Adds an attribute with the given name and value. Some implementations
     * may support only a single value for each distinct name. Others may
     * support multiple values for each name. If the value being provided
     * replaces some other value then that value is returned otherwise null
     * is returned.
     *
     * @param newAttrib new attribute.
     * @return String  containing previous value for this name if the value
     *         is being replaced otherwise null.
     */
    public String addAttribute(Attribute newAttrib) {
        return addAttribute(newAttrib.getName(), newAttrib.getValue());
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration<Attribute> getAttributes() {

        List<Attribute> attrs = new ArrayList<Attribute>();

        for (Map.Entry<String,String> anAttr : attributes.entrySet()) {
            Attribute attr = new Attribute(this, anAttr.getKey(), anAttr.getValue());

            attrs.add(attr);
        }

        return Collections.enumeration(attrs);
    }

    /**
     * {@inheritDoc}
     */
    public Attribute getAttribute(String name) {
        String value = attributes.get(name);

        if (null == value) {
            return null;
        }

        // build the object
        return new Attribute(this, name, value);
    }
}
