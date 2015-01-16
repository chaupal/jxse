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

package net.jxta.impl.membership.pse;

import net.jxta.logging.Logger;
import net.jxta.logging.Logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

/**
 * Manages a Keystore located within a single File.
 */
public class FileKeyStoreManager implements KeyStoreManager {

    private final static transient Logger LOG = Logging.getLogger(URIKeyStoreManager.class.getName());

    private final static String DEFAULT_KEYSTORE_TYPE = "jks";

    private final static String DEFAULT_KEYSTORE_FILENAME = "jxta_keystore";

    /**
     * The keystore type
     */
    private final String keystoreType;

    /**
     * The keystore type
     */
    private final String keystoreProvider;

    /**
     * The file where the keystore lives. This must be a file even if the
     * keystore really is a set of files or a directory.
     */
    private final File keystoreLocation;

    /**
     * Default constructor.
     *
     * @param type keystore type
     * @param provider the provider
     * @param location Store location
     * @throws NoSuchProviderException if the security provider requested is not available in the environment.
     * @throws KeyStoreException if a keystore error occurs
     */
    public FileKeyStoreManager(String type, String provider, File location) throws NoSuchProviderException, KeyStoreException {

        if (null == type) {
            type = DEFAULT_KEYSTORE_TYPE;
            provider = null;
        }

        // if provided a directory, use the default file name.
        if (location.isDirectory()) {
            location = new File(location, DEFAULT_KEYSTORE_FILENAME);
        }

        Logging.logCheckedConfig(LOG, "pse location = ", location);

        keystoreType = type;
        keystoreProvider = provider;
        keystoreLocation = location;

        // check if we can get an instance.
        if (null == keystoreProvider) {
            KeyStore.getInstance(keystoreType);
        } else {
            KeyStore.getInstance(keystoreType, keystoreProvider);
        }

    }

    /**
     * {@inheritDoc}
     */
    public boolean isInitialized() {
        return isInitialized(null);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isInitialized(char[] keyStorePassword) {
        try {
            KeyStore store;

            if (null == keystoreProvider) {
                store = KeyStore.getInstance(keystoreType);
            } else {
                store = KeyStore.getInstance(keystoreType, keystoreProvider);
            }

            store.load(keystoreLocation.toURI().toURL().openStream(), keyStorePassword);

            return true;
        } catch (Exception failed) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void createKeyStore(char[] keyStorePassword) throws KeyStoreException, IOException {
        try {
            KeyStore store;

            if (null == keystoreProvider) {
                store = KeyStore.getInstance(keystoreType);
            } else {
                store = KeyStore.getInstance(keystoreType, keystoreProvider);
            }

            store.load(null, keyStorePassword);

            saveKeyStore(store, keyStorePassword);
        } catch (NoSuchProviderException failed) {
            throw new KeyStoreException("NoSuchProviderException during keystore processing", failed);
        } catch (NoSuchAlgorithmException failed) {
            throw new KeyStoreException("NoSuchAlgorithmException during keystore processing", failed);
        } catch (CertificateException failed) {
            throw new KeyStoreException("CertificateException during keystore processing", failed);
        }
    }

    /**
     * {@inheritDoc}
     */
    public KeyStore loadKeyStore(char[] password) throws KeyStoreException, IOException {

        Logging.logCheckedDebug(LOG, "Loading (", keystoreType, ",", keystoreProvider, ") store from ", keystoreLocation);

        try {

            KeyStore store;

            if (null == keystoreProvider) {
                store = KeyStore.getInstance(keystoreType);
            } else {
                store = KeyStore.getInstance(keystoreType, keystoreProvider);
            }

            store.load(keystoreLocation.toURI().toURL().openStream(), password);

            return store;
        } catch (NoSuchAlgorithmException failed) {
            throw new KeyStoreException("NoSuchAlgorithmException during keystore processing", failed);
        } catch (CertificateException failed) {
            throw new KeyStoreException("CertificateException during keystore processing", failed);
        } catch (NoSuchProviderException failed) {
            throw new KeyStoreException("NoSuchProviderException during keystore processing", failed);
        }
    }

    /**
     * {@inheritDoc}
     * @throws java.security.KeyStoreException
     * @throws java.io.IOException
     */
    public void saveKeyStore(KeyStore store, char[] password) throws KeyStoreException, IOException {

        Logging.logCheckedDebug(LOG, "Writing ", store, " to ", keystoreLocation);

        try {
            OutputStream os = new FileOutputStream(keystoreLocation);

            store.store(os, password);
        } catch (NoSuchAlgorithmException failed) {
            throw new KeyStoreException("NoSuchAlgorithmException during keystore processing", failed);
        } catch (CertificateException failed) {
            throw new KeyStoreException("CertificateException during keystore processing", failed);
        }
    }

    /**
     * {@inheritDoc}
     * @throws java.io.IOException
     */
    public void eraseKeyStore() throws IOException {

        if (keystoreLocation.isFile() && keystoreLocation.canWrite()) {
            keystoreLocation.delete();
        } else {
            throw new UnsupportedOperationException("Unable to delete");
        }
    }

    /**
     *  {@inheritDoc}
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PSE keystore details:  \n");
        sb.append("   Class:  ").append(this.getClass().getName()).append("\n");
        sb.append("   Type:  ").append(keystoreType==null ? "<default>" : keystoreType).append("\n");
        sb.append("   Provider:  ").append(keystoreProvider==null ? "<default>" : keystoreProvider).append("\n");
        sb.append("   Location:  ").append(keystoreLocation==null ? "<default>" : keystoreLocation.toString()).append("\n");
        return sb.toString();
    }
}
