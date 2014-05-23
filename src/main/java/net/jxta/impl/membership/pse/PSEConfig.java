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

import net.jxta.id.ID;
import net.jxta.id.IDFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import net.jxta.logging.Logging;

/**
 * Manages the state of a Personal Security Enviroment.
 */
public final class PSEConfig {

    /**
     * Log4J Logger
     */
    private final static transient Logger LOG = Logger.getLogger(PSEConfig.class.getName());

    /**
     * Manager for the keystore we are using.
     */
    private final KeyStoreManager keystoreManager;

    /**
     * The keystore passphrase.
     */
    private char[] keystorePassword = null;

    /**
     * Standard constructor.
     *
     * @param storeManager   The StoreManager to be used for this PSEConfig
     *                       instance.
     * @param storePassword The passphrase for the keystore or <tt>null</tt>.
     *                       The passphrase may be set independantly via
     *                       {@link #setKeyStorePassword(char[])}.
     */
    PSEConfig(KeyStoreManager storeManager, char[] storePassword) {
        this.keystoreManager = storeManager;
        setKeyStorePassword(storePassword);
    }

    /**
     * Sets the passphrase to be used when unlocking the keystore.
     *
     * @param storePassword The passphrase used to unlock the keystore may be
     *                       {@code null} for keystores with no passphrase.
     */
    public final void setKeyStorePassword(char[] storePassword) {
        if (null != this.keystorePassword) {
            Arrays.fill(this.keystorePassword, '\0');
        }

        if (null == storePassword) {
            this.keystorePassword = null;
        } else {
            this.keystorePassword = storePassword.clone();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        if (null != keystorePassword) {
            Arrays.fill(keystorePassword, '\0');
        }

        super.finalize();
    }

    /**
     * Returns {@code true} if the PSE has been initialized (created). Some
     * keystore formats may not require initialization and may always return
     * {@code true}. {@code false} may also be returned if the keystore passphrase is
     * incorrect.
     *
     * @return {@code true} if the PSE has been previously initialized
     *         otherwise {@code false}.
     */
    public boolean isInitialized() {
        try {
            if (keystorePassword != null) {
                return keystoreManager.isInitialized(keystorePassword);
            } else {
                return keystoreManager.isInitialized();
            }
        } catch (KeyStoreException ignored) {
            return false;
        }
    }

    /**
     * Initializes the PSE environment.
     *
     * @throws KeyStoreException When the wrong keystore has been provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    public void initialize() throws KeyStoreException, IOException {
        Logging.logCheckedInfo(LOG, "Initializing new PSE keystore...");

        synchronized (keystoreManager) {
            try {

                if (keystoreManager.isInitialized(keystorePassword)) {
                    return;
                }

                keystoreManager.createKeyStore(keystorePassword);

            } catch (KeyStoreException failed) {

                Logging.logCheckedSevere(LOG, "Failure accessing or creating keystore.\n", failed);
                keystoreManager.eraseKeyStore();
                throw failed;
            }
        }
    }

    /**
     * Removes an existing PSE enviroment.
     *
     * @throws IOException If the PSE cannot be successfully deleted.
     */
    public void erase() throws IOException {
        synchronized (keystoreManager) {
            keystoreManager.eraseKeyStore();
        }
    }

    /**
     * Gets a copy of the KeyStore associated with this PSE instance. The
     * returned KeyStore is a copy and not tied to the instance maintained by
     * the PSE. Changing the returned keystore will not result in changes to
     * the PSE.
     *
     * @return The keystore or {@code null} if it cannot be retrieved.
     */
    public KeyStore getKeyStore() {

        Throwable failure;

        try {

            return getKeyStore(keystorePassword);

        } catch (KeyStoreException failed) {

            failure = failed;

        } catch (IOException failed) {

            failure = failed;

        }

        Logging.logCheckedWarning(LOG, "Failure recovering keystore : \n", failure);

        return null;
    }

    /**
     * Gets a copy of the KeyStore associated with this PSE instance. The
     * returned KeyStore is a copy and not tied to the instance maintained by
     * the PSE. Changing the returned keystore will not result in changes to
     * the PSE.
     *
     * @param storePassword The passphrase used to unlock the keystore may be
     *                       {@code null} for keystores with no passphrase.
     * @return The keystore.
     * @throws KeyStoreException When the wrong keystore has been provided.
     * @throws IOException       For errors related to processing the keystore.
     * @since JXTA 2.4
     */
    public KeyStore getKeyStore(char[] storePassword) throws KeyStoreException, IOException {
        synchronized (keystoreManager) {
            KeyStore store = keystoreManager.loadKeyStore(storePassword);

            return store;
        }
    }

    /**
     * Check if the provided passwords are correct for the specified identity.
     *
     * @param id             The identity to be validated.
     * @param storePassword The passphrase used to unlock the keystore may be
     *                       {@code null} for keystores with no passphrase.
     * @param key_password   The passphrase associated with the private key or
     *                       {@code null} if the key has no passphrase.
     * @return {@code true} if the passwords were valid for the given id
     *         otherwise {@code false}.
     */
    boolean validPasswd(ID id, char[] storePassword, char[] key_password) {

        if (null == id) {
            Logging.logCheckedFine(LOG, "null id");
            return false;
        }

        Throwable failure;

        try {
            synchronized (keystoreManager) {
                KeyStore store;

                if (null != storePassword) {
                    store = keystoreManager.loadKeyStore(storePassword);
                } else {
                    if (null != keystorePassword) {
                        store = keystoreManager.loadKeyStore(keystorePassword);
                    } else {
                        throw new UnrecoverableKeyException("KeyStore passphrase not initialized");
                    }
                }

                String alias = id.toString();

                Key key = store.getKey(alias, key_password);

                if ( key == null )
                    Logging.logCheckedFine(LOG, "Can't retrieve key for alias: ", alias);

                return (null != key);
            }

        } catch (UnrecoverableKeyException failed) {

            failure = failed;

        } catch (NoSuchAlgorithmException failed) {

            failure = failed;

        } catch (KeyStoreException failed) {

            failure = failed;

        } catch (IOException failed) {

            failure = failed;

        }

        Logging.logCheckedWarning(LOG, "Failure checking passphrase : \n", failure);

        return false;
    }

    /**
     * Returns the list of the trusted certificates available in this keystore.
     *
     * @return an array of the IDs of the available trusted certificates.
     * @throws KeyStoreException When the wrong keystore has been provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    public ID[] getTrustedCertsList() throws KeyStoreException, IOException {
        List<ID> trustedCertsList = new ArrayList<ID>();

        synchronized (keystoreManager) {
            KeyStore store = keystoreManager.loadKeyStore(keystorePassword);

            Enumeration<String> eachAlias = store.aliases();

            while (eachAlias.hasMoreElements()) {
                String anAlias = eachAlias.nextElement();

                if (store.isCertificateEntry(anAlias) || store.isKeyEntry(anAlias)) {
                    try {
                        URI id = new URI(anAlias);

                        trustedCertsList.add(IDFactory.fromURI(id));
                    } catch (URISyntaxException badID) {// ignored
                    }
                }
            }

            return trustedCertsList.toArray(new ID[trustedCertsList.size()]);
        }
    }

    /**
     * Returns the list of root certificates for which there is an associated
     * local private key.
     *
     * @return an array of the available keys. May be an empty array.
     * @throws KeyStoreException When the wrong keystore has been provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    public ID[] getKeysList() throws KeyStoreException, IOException {
        return getKeysList(keystorePassword);
    }

    /**
     * Returns the list of root certificates for which there is an associated
     * local private key.
     *
     * @param storePassword The passphrase used to unlock the keystore may be
     *                       {@code null} for keystores with no passphrase.
     * @return an array of the available keys. May be an empty array.
     * @throws KeyStoreException When the wrong keystore has been provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    ID[] getKeysList(char[] storePassword) throws KeyStoreException, IOException {
        List<ID> keyedRootsList = new ArrayList<ID>();

        synchronized (keystoreManager) {
            KeyStore store = keystoreManager.loadKeyStore(storePassword);

            Enumeration<String> eachAlias = store.aliases();

            while (eachAlias.hasMoreElements()) {
                String anAlias = eachAlias.nextElement();

                if (store.isKeyEntry(anAlias)) {
                    try {
                        URI id = new URI(anAlias);

                        keyedRootsList.add(IDFactory.fromURI(id));
                    } catch (URISyntaxException badID) {// ignored
                    }
                }
            }

            return keyedRootsList.toArray(new ID[keyedRootsList.size()]);
        }
    }

    /**
     * Returns the ID of the provided certificate or null if the certificate is
     * not found in the keystore.
     *
     * @param cert The certificate who's ID is desired.
     * @return The ID of the certificate or <tt>null</tt> if no matching
     *         Certificate was found.
     * @throws KeyStoreException When the wrong keystore has been provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    public ID getTrustedCertificateID(X509Certificate cert) throws KeyStoreException, IOException {

        String anAlias = null;

        synchronized (keystoreManager) {
            KeyStore store = keystoreManager.loadKeyStore(keystorePassword);

            anAlias = store.getCertificateAlias(cert);
        }

        // not found.
        if (null == anAlias) {
            return null;
        }

        try {
            URI id = new URI(anAlias);

            return IDFactory.fromURI(id);
        } catch (URISyntaxException badID) {
            return null;
        }
    }

    /**
     * Returns the trusted cert for the specified id.
     *
     * @param id The id of the Certificate to retrieve.
     * @return Certificate for the specified ID or null if the store does not
     *         contain the specified certificate.
     * @throws KeyStoreException When the wrong keystore key has been provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    public X509Certificate getTrustedCertificate(ID id) throws KeyStoreException, IOException {

        return getTrustedCertificate(id, keystorePassword);
    }

    /**
     * Returns the trusted cert for the specified id.
     *
     * @param id             The id of the Certificate to retrieve.
     * @param storePassword The passphrase used to unlock the keystore may be
     *                       {@code null} for keystores with no passphrase.
     * @return Certificate for the specified ID or null if the store does not
     *         contain the specified certificate.
     * @throws KeyStoreException When the wrong keystore has been provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    X509Certificate getTrustedCertificate(ID id, char[] storePassword) throws KeyStoreException, IOException {

        String alias = id.toString();

        synchronized (keystoreManager) {
            KeyStore store = keystoreManager.loadKeyStore(storePassword);

            if (!store.containsAlias(alias)) {
                return null;
            }

            return (X509Certificate) store.getCertificate(alias);
        }
    }

    /**
     * Returns the trusted cert chain for the specified id.
     *
     * @param id The ID of the certificate who's certificate chain is desired.
     * @return Certificate chain for the specified ID or null if the PSE does
     *         not contain the specified certificate.
     * @throws KeyStoreException When the wrong keystore has been provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    public X509Certificate[] getTrustedCertificateChain(ID id) throws KeyStoreException, IOException {

        String alias = id.toString();

        synchronized (keystoreManager) {
            KeyStore store = keystoreManager.loadKeyStore(keystorePassword);

            if (!store.containsAlias(alias)) {
                return null;
            }

            Certificate certs[] = store.getCertificateChain(alias);

            if (null == certs) {
                return null;
            }

            X509Certificate x509certs[] = new X509Certificate[certs.length];

            System.arraycopy(certs, 0, x509certs, 0, certs.length);

            return x509certs;
        }
    }

    /**
     * Returns the private key for the specified ID.
     *
     * @param id           The ID of the requested private key.
     * @param key_password The passphrase associated with the private key or
     *                     {@code null} if the key has no passphrase.
     * @return PrivateKey for the specified ID.
     * @throws KeyStoreException When the wrong keystore has been provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    public PrivateKey getKey(ID id, char[] key_password) throws KeyStoreException, IOException {

        String alias = id.toString();

        try {
            synchronized (keystoreManager) {
                KeyStore store = keystoreManager.loadKeyStore(keystorePassword);

                if (!store.containsAlias(alias) || !store.isKeyEntry(alias)) {
                    return null;
                }

                return (PrivateKey) store.getKey(alias, key_password);
            }

        } catch (NoSuchAlgorithmException failed) {

            Logging.logCheckedSevere(LOG, "Something failed\n", failed);

            KeyStoreException failure = new KeyStoreException("Something Failed");

            failure.initCause(failed);
            throw failure;

        } catch (UnrecoverableKeyException failed) {

            Logging.logCheckedSevere(LOG, "Key passphrase failure\n", failed);

            KeyStoreException failure = new KeyStoreException("Key passphrase failure");
            failure.initCause(failed);
            throw failure;

        }
    }

    /**
     * Returns <tt>true</tt> if the specified id is associated with a private
     * key.
     *
     * @param id The ID of the requested private key.
     * @return <tt>true</tt> if a private key with the specified ID is present
     *         otherwise <tt>false</tt>
     * @throws KeyStoreException When the wrong keystore has been provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    public boolean isKey(ID id) throws KeyStoreException, IOException {
        return isKey(id, keystorePassword);
    }

    /**
     * Returns <tt>true</tt> if the specified id is associated with a private
     * key.
     *
     * @param id             The ID of the requested private key.
     * @param storePassword The passphrase used to unlock the keystore may be
     *                       {@code null} for keystores with no passphrase.
     * @return <tt>true</tt> if a private key with the specified ID is present
     *         otherwise <tt>false</tt>
     * @throws KeyStoreException When the wrong keystore has been provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    public boolean isKey(ID id, char[] storePassword) throws KeyStoreException, IOException {
        String alias = id.toString();

        synchronized (keystoreManager) {
            KeyStore store = keystoreManager.loadKeyStore(storePassword);

            return store.containsAlias(alias) & store.isKeyEntry(alias);
        }
    }

    /**
     * Adds a trusted certificate with the specified id to the key store. The
     * certificate replaces any existing certificate or private key stored at
     * this ID.
     *
     * @param id   The ID under which the certificate will be stored.
     * @param cert Certificate for the specified ID.
     * @throws KeyStoreException When the wrong keystore has been provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    public void setTrustedCertificate(ID id, X509Certificate cert) throws KeyStoreException, IOException {
        String alias = id.toString();

        synchronized (keystoreManager) {
            KeyStore store = keystoreManager.loadKeyStore(keystorePassword);

            store.deleteEntry(alias);

            store.setCertificateEntry(alias, cert);

            keystoreManager.saveKeyStore(store, keystorePassword);
        }
    }

    /**
     * Adds a private key to the PSE using the specified ID. The key replaces
     * any existing certificate or private key stored at this ID. The key is
     * stored using the provided key passphrase.
     *
     * @param id           The ID under which the certificate chain and private key will be stored.
     * @param certchain    The certificate chain matching the private key.
     * @param key          The private key to be stored in the kestore.
     * @param key_password The passphrase associated with the private key or
     *                     {@code null} if the key has no passphrase.
     * @throws KeyStoreException When the wrong keystore key has been provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    public void setKey(ID id, Certificate[] certchain, PrivateKey key, char[] key_password) throws KeyStoreException, IOException {

        String alias = id.toString();

        synchronized (keystoreManager) {
            KeyStore store = keystoreManager.loadKeyStore(keystorePassword);

            // Remove any existing entry.
            if (store.isKeyEntry(alias))
                store.deleteEntry(alias);

            store.setKeyEntry(alias, key, key_password, certchain);

            keystoreManager.saveKeyStore(store, keystorePassword);
        }
    }

    /**
     * Erases the specified id from the keystore.
     *
     * @param id The ID of the key or certificate to be deleted.
     * @throws KeyStoreException When the wrong keystore password has been
     *                           provided.
     * @throws IOException       For errors related to processing the keystore.
     */
    public void erase(ID id) throws KeyStoreException, IOException {
        String alias = id.toString();

        synchronized (keystoreManager) {
            KeyStore store = keystoreManager.loadKeyStore(keystorePassword);

            store.deleteEntry(alias);

            keystoreManager.saveKeyStore(store, keystorePassword);
        }
    }
}
