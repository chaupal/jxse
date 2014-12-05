/*
 * Copyright (c) 2006-2007 Sun Microsystems, Inc.  All rights reserved.
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

package net.jxta.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Generic JXTA configuration object based on the {@code Properties} object, which can be
 * saved as an XML document.
 * 
 */
public class JxtaConfiguration extends Properties {

    /**
     *  Logger.
     */
    private final static Logger LOG = Logger.getLogger(JxtaConfiguration.class.getName());

    /**
     * This constructor copies all entries from the provided parameter into this object,
     * including defaults.
     * 
     * @param toCopy entries to copy in this object.
     */
    public JxtaConfiguration(JxtaConfiguration toCopy) {
        
        // Calling super
        super();
        
        // Copying entries
        for (String Item : PropertiesUtil.stringPropertyNames(toCopy)) {
            this.setProperty(Item, toCopy.getProperty(Item));
        }

        // Initializing defaults
        this.defaults = new Properties();

        // Copying defaults
        for (String Item : PropertiesUtil.stringPropertyNames(toCopy.defaults)) {
            this.defaults.setProperty(Item, toCopy.defaults.getProperty(Item));
        }
        
    }

    /**
     * Simple constructor
     */
    public JxtaConfiguration() {

        // Calling super
        super();

        // Initializing defaults
        defaults = new Properties();

    }

    /**
     * {@inheritDoc}
     *
     * Properties keys starting with {@code DEFAULT_} trigger an {@code InvalidParameterException}.
     */
    @Override
    public Object setProperty(String key, String value) {

        return super.setProperty(key, value);

    }

    /**
     * Registers a default value for a properties, or removes it if {@code key} parameter
     * is {@code null}.
     *
     * @param key The property key
     * @param value The property value
     *
     */
    protected void setDefaultPropertyValue(String key, String value) {

        if (key==null) {
            this.defaults.remove(key);
            return;
        }

        this.defaults.setProperty(key, value);

    }

    /**
     * Returns the default value for a property, or {@code null} if none is available.
     * 
     * @param key The property key
     *
     */
    protected String getDefaultPropertyValue(String key) {

        return this.defaults.getProperty(key);

    }

    /**
     * Return a Properties object containing the defaults in this object.
     *
     * @return a Properties object.
     */
    public Properties getDefaultsCopy() {

        // Preparing result
        Properties Result = new Properties();

        for (String Items : PropertiesUtil.stringPropertyNames(this.defaults)) {
            Result.setProperty(Items, this.defaults.getProperty(Items));
        }

        // Returning result
        return Result;

    }

    /**
     * Required to prepare default entries saving
     */
    private static final String DEFAULT_PREFIX = "DEFAULT_";

    /**
     * Prepares entries for Defaults, before storage/saving
     */
    private void prepareDefaultEntries() {

        // Removing any existing entries first
        removeExistingDefaultEntries();

        for (String Item : PropertiesUtil.stringPropertyNames(this.defaults)) {
            super.setProperty(DEFAULT_PREFIX + Item, this.defaults.getProperty(Item));
        }

    }

    /**
     * Removing any existing transport configuration sub-entries.
     */
    private void removeExistingDefaultEntries() {

        // Removing any existing sub-entries
        for (String Item : PropertiesUtil.stringPropertyNames(this)) {
            if ( Item.startsWith(DEFAULT_PREFIX) ) {
                this.remove(Item);
            }
        }

    }

    /**
     * Restore defaults entries
     */
    private void recreateTransportConfiguration() {

        // Creating new defaults
        this.defaults = new Properties();

        // Searching for sub-entries
        for (String Item : PropertiesUtil.stringPropertyNames(this)) {

            if (Item.startsWith(DEFAULT_PREFIX)) {

                // Registering entry in the transport
                this.defaults.setProperty(Item.substring(DEFAULT_PREFIX.length()), this.getProperty(Item));
                this.remove(Item);

            } // else the entry should stay at this level

        }

    }


    /**
     * {@inheritDoc}
     *
     * <p>This method loads defaults too.
     */
    @Override
    public void loadFromXML(InputStream in) throws IOException {

        // Calling super
        super.loadFromXML(in);

        // ...and recreating transport configuration
        recreateTransportConfiguration();

    }

    /**
     * {@inheritDoc}
     *
     * <p>This method stores defaults too.
     */
    @Override
    public void storeToXML(OutputStream os, String comment) throws IOException {

        // Preparing transport configuration entries
        prepareDefaultEntries();

        // ...and calling super
        super.storeToXML(os, comment);

        // Cleaning the mess
        removeExistingDefaultEntries();

    }

    /**
     * {@inheritDoc}
     *
     * <p>This method stores defaults too.
     */
    @Override
    public void storeToXML(OutputStream os, String comment, String encoding) throws IOException {

        // Preparing transport configuration entries
        prepareDefaultEntries();

        // ...and calling super
        super.storeToXML(os, comment, encoding);

        // Cleaning the mess
        removeExistingDefaultEntries();

    }

    /**
     * {@inheritDoc}
     *
     * <p>This method stores defaults too.
     */
    @Override
    public void store(OutputStream out, String comments) throws IOException {

        // Preparing transport configuration entries
        prepareDefaultEntries();

        // ...and calling super
        super.store(out, comments);

        // Cleaning the mess
        removeExistingDefaultEntries();

    }

    /**
     * {@inheritDoc}
     *
     * <p>This method stores defaults too.
     */
    @Override
    public synchronized void load(InputStream in) throws IOException {

        // Calling super
        super.load(in);

        // ...and recreating transport configuration
        recreateTransportConfiguration();

    }

}
