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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.credential.AuthenticationCredential;
import net.jxta.credential.Credential;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.document.XMLSignature;
import net.jxta.document.XMLSignatureInfo;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.id.ID;
import net.jxta.impl.membership.pse.PSEUtils.IssuerInfo;
import net.jxta.impl.protocol.Certificate;
import net.jxta.impl.protocol.PSEConfigAdv;
import net.jxta.logging.Logging;
import net.jxta.membership.Authenticator;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.service.Service;

/**
 *  A JXTA Membership Service utilizing PKI to provide secure identities.
 *
 *  @see net.jxta.membership.MembershipService
 **/
public final class PSEMembershipService implements MembershipService {

    /**
     *  Log4J Logger
     **/
    private final static transient Logger LOG = Logger.getLogger(PSEMembershipService.class.getName());

    /**
     * Well known service specification identifier: pse membership
     */
    public final static ModuleSpecID pseMembershipSpecID = (ModuleSpecID) ID.create(
            URI.create(ID.URIEncodingName + ":" + ID.URNNamespace + ":uuid-DeadBeefDeafBabaFeedBabe000000050306"));

    /**
     * the peergroup to which this service is associated.
     **/
    private PeerGroup group = null;

    /**
     *  The ID assigned to this instance.
     **/
    private ID assignedID = null;

    /**
     * The ModuleImplAdvertisement which was used to instantiate this service.
     **/
    private ModuleImplAdvertisement implAdvertisement = null;

    /**
     * The current set of principals associated with this peer within this peergroup.
     **/
    private final List<PSECredential> principals = new ArrayList<PSECredential>();

    /**
     * The set of AuthenticationCredentials which were used to establish the principals.
     **/
    private final List<AuthenticationCredential> authCredentials = new ArrayList<AuthenticationCredential>();

    /**
     *  property change support
     **/
    private final PropertyChangeSupport support;

    /**
     *  the keystore we are working with.
     **/
    private PSEConfig pseStore = null;

    /**
     *  the default credential
     **/
    private PSECredential defaultCredential = null;

    /**
     *  The configuration we are using.
     **/
    private PSEConfigAdv config;

    /**
     * PSEPeerSecurityEngine loader
     */

    private PSEPeerSecurityEngine peerSecurityEngine = null;

    /**
     * PSEAuthenticatorEngine loader
     */

    private PSEAuthenticatorEngine authenticatorEngine = null;

    /**
     * PSEPeerValidationEngine loader
     */

    private PSEPeerValidationEngine peerValidationEngine = null;
    
    /**
     *  Default constructor. Normally only called by the peer group.
     **/
    public PSEMembershipService() throws PeerGroupException {
        support = new PropertyChangeSupport(getInterface());
    }

    /**
     *  @inheritDoc
     **/
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    /**
     *  @inheritDoc
     **/
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        support.addPropertyChangeListener(propertyName, listener);
    }

    /**
     *  @inheritDoc
     **/
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    /**
     *  @inheritDoc
     **/
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        support.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * {@inheritDoc}
     **/
    public void init(PeerGroup group, ID assignedID, Advertisement impl) throws PeerGroupException {
        this.group = group;
        this.assignedID = assignedID;
        this.implAdvertisement = (ModuleImplAdvertisement) impl;

        ConfigParams configAdv = group.getConfigAdvertisement();

        // Get our peer-defined parameters in the configAdv
        Element param = configAdv.getServiceParam(assignedID);

        Advertisement paramsAdv = null;

        if (null != param) {
            try {
                paramsAdv = AdvertisementFactory.newAdvertisement((XMLElement) param);
            } catch (NoSuchElementException ignored) {
                ;
            }

            if (!(paramsAdv instanceof PSEConfigAdv)) {
                throw new PeerGroupException("Provided Advertisement was not a " + PSEConfigAdv.getAdvertisementType());
            }

            config = (PSEConfigAdv) paramsAdv;
        } else {
            // Create the default advertisement.
            config = (PSEConfigAdv) AdvertisementFactory.newAdvertisement(PSEConfigAdv.getAdvertisementType());
        }

        peerSecurityEngine = getDefaultPSESecurityEngineFactory().getInstance(this, config);

        authenticatorEngine = getDefaultPSEAuthenticatorEngineFactory().getInstance(this, config);
        
        peerValidationEngine = getDefaultPSEPeerValidationEngineFactory().getInstance(this, config);

        KeyStoreManager storeManager = getDefaultKeyStoreManagerFactory().getInstance(this, config);

        pseStore = new PSEConfig(storeManager, null);

        if (Logging.SHOW_CONFIG && LOG.isLoggable(Level.CONFIG)) {

            StringBuilder configInfo = new StringBuilder("Configuring PSE Membership Service : " + assignedID);

            configInfo.append("\n\tImplementation :");
            configInfo.append("\n\t\tModule Spec ID: ").append(implAdvertisement.getModuleSpecID());
            configInfo.append("\n\t\tImpl Description : ").append(implAdvertisement.getDescription());
            configInfo.append("\n\t\tImpl URI : ").append(implAdvertisement.getUri());
            configInfo.append("\n\t\tImpl Code : ").append(implAdvertisement.getCode());
            configInfo.append("\n\tGroup Params :");
            configInfo.append("\n\t\tGroup : ").append(group.getPeerGroupName());
            configInfo.append("\n\t\tGroup ID : ").append(group.getPeerGroupID());
            configInfo.append("\n\t\tPeer ID : ").append(group.getPeerID());
            configInfo.append("\n\tConfiguration :");
            configInfo.append("\n\t\tPSE state : ").append(pseStore.isInitialized() ? "inited" : "new");
            configInfo.append("\n\t\tPSE KeyStore location : ").append((null != config.getKeyStoreLocation())
                    ? config.getKeyStoreLocation().toString()
                    : assignedID.toString());
            configInfo.append("\n\t\tPSE KeyStore type : ").append((null != config.getKeyStoreType()) ? config.getKeyStoreType() : "<default>");
            configInfo.append("\n\t\tPSE KeyStore provider : ").append((null != config.getKeyStoreProvider()) ? config.getKeyStoreProvider() : "<default>");

            LOG.config(configInfo.toString());
        }

        resign();
    }

    /**
     * {@inheritDoc}
     **/
    public Service getInterface() {
        return this;
    }

    /**
     * {@inheritDoc}
     **/
    public Advertisement getImplAdvertisement() {
        return implAdvertisement;
    }

    /**
     * {@inheritDoc}
     *
     * <p/>Currently this service starts by itself and does not expect
     * arguments.
     */
    public int startApp(String[] arg) {

        Logging.logCheckedInfo(LOG, "PSE Membmership Service started.");

        return 0;

    }

    /**
     * {@inheritDoc}
     **/
    public void stopApp() {

        resign();

        Logging.logCheckedInfo(LOG, "PSE Membmership Service stopped.");

    }

    public PeerGroup getGroup() {
        return group;
    }

    public ID getAssignedID() {
        return assignedID;
    }

    /**
     * {@inheritDoc}
     *
     * <p/>Supports methods <code>"StringAuthentication"</code>,
     * <code>"DialogAuthentication"</code>,
     * <code>"EngineAuthentication"</code> and
     * <code>"InteractiveAuthentication"</code> (an alias for
     * <code>"DialogAuthentication"</code>)
     **/
    public Authenticator apply(AuthenticationCredential application) throws ProtocolNotSupportedException {

        String method = application.getMethod();

        boolean newKey;

        if (!pseStore.isInitialized()) {
            // It is not inited, it's new.
            newKey = true;
        } else {
            X509Certificate configCert = config.getCertificate();

            if (null != configCert) {
                try {
                    ID allTrustedCerts[] = pseStore.getTrustedCertsList();

                    Iterator eachTrustedCert = Arrays.asList(allTrustedCerts).iterator();

                    newKey = true;

                    // See if the config cert is already in the keystore.
                    while (eachTrustedCert.hasNext()) {
                        ID aTrustedCertID = (ID) eachTrustedCert.next();

                        if (pseStore.isKey(aTrustedCertID)) {
                            X509Certificate aTrustedCert = pseStore.getTrustedCertificate(aTrustedCertID);

                            if (aTrustedCert.equals(configCert)) {
                                newKey = false;
                                break;
                            }
                        }
                    }
                } catch (KeyStoreException bad) {
                    // The keystore is probably initialized but locked. Nothing else we can do.
                    newKey = false;
                } catch (IOException bad) {
                    // Could not read the keystore. I'm not sure it wouldn't be better to just fail.
                    newKey = false;
                }
            } else {
                // don't have anything to validate against.
                newKey = false;
            }
        }

        if ("StringAuthentication".equals(method)) {
            if (newKey) {
                return new StringAuthenticator(this, application, config.getCertificate(), config.getEncryptedPrivateKey());
            } else {
                return new StringAuthenticator(this, application);
            }
        } else if ("EngineAuthentication".equals(method)) {
            if (pseStore.isInitialized()) {
                return new EngineAuthenticator(this, application, authenticatorEngine);
            } else {
                return new EngineAuthenticator(this, application, authenticatorEngine);
            }
        } else if ("DialogAuthentication".equals(method) || "InteractiveAuthentication".equals(method) || (null == method)) {
            if (newKey) {
                return new DialogAuthenticator(this, application, config.getCertificate(), config.getEncryptedPrivateKey());
            } else {
                return new DialogAuthenticator(this, application);
            }
        } else {
            throw new ProtocolNotSupportedException("Authentication method not recognized");
        }
    }

    /**
     * {@inheritDoc}
     **/
    public Credential getDefaultCredential() {
        return defaultCredential;
    }

    /**
     * Sets the default credential. Also updates the peer advertisement with
     * the certificate of the default credential.
     *
     *  @param newDefault the new default credential. May also be
     *  <code>null</code> if no default is desired.
     **/
    private void setDefaultCredential(PSECredential newDefault) {

        Credential oldDefault = defaultCredential;

        synchronized (this) {
            defaultCredential = newDefault;
        }

        Logging.logCheckedConfig(LOG, "New Default credential : ", newDefault);

        try {

            // include the root cert in the peer advertisement
            PeerAdvertisement peeradv = group.getPeerAdvertisement();

            if (null != newDefault) {
                // include the root cert in the peer advertisement
                XMLDocument paramDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");

                Certificate peerCerts = new Certificate();

                peerCerts.setCertificates(newDefault.getCertificateChain());

                XMLDocument peerCertsAsDoc = (XMLDocument) peerCerts.getDocument(MimeMediaType.XMLUTF8);

                StructuredDocumentUtils.copyElements(paramDoc, paramDoc, peerCertsAsDoc, "RootCert");

                peeradv.putServiceParam(PeerGroup.peerGroupClassID, paramDoc);
            } else {
                peeradv.removeServiceParam(PeerGroup.peerGroupClassID);
            }

        } catch (Exception ignored) {
        }

        support.firePropertyChange("defaultCredential", oldDefault, newDefault);
    }

    /**
     * {@inheritDoc}
     **/
    public Enumeration<Credential> getCurrentCredentials() {
        List<Credential> credList = new ArrayList<Credential>(principals);

        return Collections.enumeration(credList);
    }

    /**
     * {@inheritDoc}
     **/
    public Credential join(Authenticator authenticated) throws PeerGroupException {

        if (this != authenticated.getSourceService()) {
            throw new ClassCastException("This is not my authenticator!");
        }

        if (!authenticated.isReadyForJoin()) {
            throw new PeerGroupException("Authenticator not ready to join!");
        }

        PSECredential newCred;

        char[] store_password = null;
        ID identity;
        char[] key_password = null;

        try {
            if (authenticated instanceof StringAuthenticator) {
                StringAuthenticator auth = (StringAuthenticator) authenticated;

                store_password = auth.getAuth1_KeyStorePassword();
                identity = auth.getAuth2Identity();
                key_password = auth.getAuth3_IdentityPassword();
            } else  if (authenticated instanceof EngineAuthenticator) {
                EngineAuthenticator auth = (EngineAuthenticator) authenticated;

                store_password = auth.getAuth1_KeyStorePassword();
                identity = auth.getAuth2Identity();
                key_password = auth.getAuth3_IdentityPassword();

            } else {

                Logging.logCheckedWarning(LOG, "I dont know how to deal with this authenticator ", authenticated);
                throw new PeerGroupException("I dont know how to deal with this authenticator");

            }

            if (null != store_password) pseStore.setKeyStorePassword(store_password);

            if (!pseStore.isInitialized()) {

                Logging.logCheckedInfo(LOG, "Initializing the PSE key store.");

                try {

                    pseStore.initialize();

                } catch (KeyStoreException bad) {

                    throw new PeerGroupException("Could not initialize new PSE keystore.", bad);

                } catch (IOException bad) {

                    throw new PeerGroupException("Could not initialize new PSE keystore.", bad);

                }

            }

            try {
                ID[] allkeys = pseStore.getKeysList();

                if (!Arrays.asList(allkeys).contains(identity)) {
                    // Add this key to the keystore.
                    X509Certificate[] seed_cert = config.getCertificateChain();

                    if (null == seed_cert) {
                        throw new IOException("Could not read root certificate chain");
                    }

                    PrivateKey seedPrivKey = config.getPrivateKey(key_password);

                    if (null == seedPrivKey) {
                        throw new IOException("Could not read private key");
                    }

                    pseStore.setKey(identity, seed_cert, seedPrivKey, key_password);

                }

            } catch (IOException failed) {

                Logging.logCheckedWarning(LOG, "Could not save new key pair.\n", failed);
                throw new PeerGroupException("Could not save new key pair.", failed);

            } catch (KeyStoreException failed) {

                Logging.logCheckedWarning(LOG, "Could not save new key pair.\n", failed);
                throw new PeerGroupException("Could not save new key pair.", failed);

            }

            try {

                X509Certificate certList[] = pseStore.getTrustedCertificateChain(identity);

                if (null == certList) {
                    certList = new X509Certificate[1];

                    certList[0] = pseStore.getTrustedCertificate(identity);

                    if (certList[0] == null && authenticatorEngine != null) 
                        certList[0] = authenticatorEngine.getX509Certificate();

                }

                CertificateFactory cf = CertificateFactory.getInstance("X.509");

                CertPath certs = cf.generateCertPath(Arrays.asList(certList));

                PrivateKey privateKey = pseStore.getKey(identity, key_password);

                newCred = new PSECredential(this, identity, certs, privateKey);

                synchronized (this) {

                    principals.add(newCred);
                    authCredentials.add(authenticated.getAuthenticationCredential());

                }
            } catch (IOException failed) {

                Logging.logCheckedWarning(LOG, "Could not create credential.\n", failed);
                throw new PeerGroupException("Could not create credential.", failed);

            } catch (KeyStoreException failed) {

                Logging.logCheckedWarning(LOG, "Could not create credential.\n", failed);
                throw new PeerGroupException("Could not create credential.", failed);

            } catch (CertificateException failed) {

                Logging.logCheckedWarning(LOG, "Could not create credential.\n", failed);
                throw new PeerGroupException("Could not create credential.", failed);

            }

        } finally {
            if (null != store_password) {
                Arrays.fill(store_password, '\0');
            }

            if (null != key_password) {
                Arrays.fill(key_password, '\0');
            }
        }

        // XXX bondolo potential but unlikely race condition here.
        if (null == getDefaultCredential()) {
            setDefaultCredential(newCred);
        }

        support.firePropertyChange("addCredential", null, newCred);

        return newCred;
    }

    /**
     * {@inheritDoc}
     **/
    public void resign() {
        Iterator eachCred = Arrays.asList(principals.toArray()).iterator();

        synchronized (this) {
            principals.clear();
            authCredentials.clear();
        }

        setDefaultCredential(null);

        // clear the keystore password.
        pseStore.setKeyStorePassword(null);

        while (eachCred.hasNext()) {
            PSECredential aCred = (PSECredential) eachCred.next();

            aCred.setValid(false);
        }
    }

    /**
     * {@inheritDoc}
     **/
    public Credential makeCredential(Element element) {

        return new PSECredential(this, element);
    }

    /**
     *  Returns the key store object associated with this PSE Membership Service.
     **/
    public PSEConfig getPSEConfig() {
        return pseStore;
    }

    /**
     *  Returns the PeerGroup associated with this PSE Membership Service.
     **/
    public PeerGroup getPeerGroup() {
        return group;
    }
    
    /**
     * Service Certificates Support
     */

    /**
     *  Generate a new service certificate for the assigned ID given an authenticated local credential.
     *
     *  @param assignedID   The assigned ID of the service credential.
     *  @param credential   The issuer credential for the service credential.
     **/
    X509Certificate[] generateServiceCertificate(ID assignedID, PSECredential credential) throws  IOException, KeyStoreException, InvalidKeyException, SignatureException {

        Logging.logCheckedFine(LOG, "Generating new service cert for ", assignedID);

        IssuerInfo serviceinfo = peerSecurityEngine.generateCertificate(credential);

        // write the client root cert and private key
        X509Certificate[] serviceChain = { serviceinfo.cert, serviceinfo.issuer };

        char keyPass[];

        if (null != serviceinfo.issuerPkey) {
            ByteArrayInputStream bis = new ByteArrayInputStream(serviceinfo.issuerPkey.getEncoded());
            byte privateKeySignature[] = peerSecurityEngine.sign(null, credential, bis);

            keyPass = PSEUtils.base64Encode(privateKeySignature, false).toCharArray();
        } else {
            keyPass = authenticatorEngine.getKeyPass(group);
        }

        getPSEConfig().setKey(assignedID, serviceChain, serviceinfo.subjectPkey, keyPass);

        Logging.logCheckedFine(LOG, "Generated new service cert");

        return serviceChain;
    }

    /**
     *  Recover the service credential for the assigned ID given an authenticated local credential.
     *
     *  @param assignedID   The assigned ID of the service credential.
     *  @param credential   The issuer credential for the service credential.
     **/
    public PSECredential getServiceCredential(ID assignedID, PSECredential credential) throws IOException, PeerGroupException, InvalidKeyException, SignatureException {

        PSECredential pseCredential = null;

        Logging.logCheckedFine(LOG, "Getting service redential for ", assignedID);

        Authenticator authenticate = null;

        if (null != authenticatorEngine) {
            AuthenticationCredential authCred = new AuthenticationCredential(group, "EngineAuthentication", null);

            try {
                authenticate = apply(authCred);
            } catch (Exception failed) {
                ;
            }

            if (null == authenticate) {
                return null;
            }

            EngineAuthenticator auth = (EngineAuthenticator) authenticate;

            auth.setAuth1_KeyStorePassword(authenticatorEngine.getStorePass(group));
            auth.setAuth2Identity(assignedID);
            auth.setAuth3_IdentityPassword(authenticatorEngine.getKeyPass(group));
        } else {
            AuthenticationCredential authCred = new AuthenticationCredential(group, "StringAuthentication", null);

            try {
                authenticate = apply(authCred);
            } catch (Exception failed) {
                ;
            }

            if (null == authenticate) {
                return null;
            }

            PSECredentialBridge pseCredentialBridge = new PSECredentialBridge();
            credential.pseKeyBridge(pseCredentialBridge);
            PrivateKey privateKey = pseCredentialBridge.privateKey;

            // make a new service certificate
            ByteArrayInputStream bis = new ByteArrayInputStream(privateKey.getEncoded());
            byte privateKeySignature[] = peerSecurityEngine.sign(null, credential, bis);
            String passkey = PSEUtils.base64Encode(privateKeySignature, false);

            StringAuthenticator auth = (StringAuthenticator) authenticate;

            auth.setAuth1_KeyStorePassword((String) null);
            auth.setAuth2Identity(assignedID);
            auth.setAuth3_IdentityPassword(passkey);
        }

        if (authenticate.isReadyForJoin()) {

            pseCredential = (PSECredential) join(authenticate);

        } else {

            Logging.logCheckedWarning(LOG, "Could not authenticate service credential");

        }

        return pseCredential;
    }
    final static class PSECredentialBridge {
        private java.security.PrivateKey privateKey = null;
        private PSECredentialBridge() {

        }
        public void setPrivateKey(java.security.PrivateKey privateKey) {
            this.privateKey = privateKey;
        }
    }

    /**
     * Signs an advertisement for publication. The signed document needs to have
     * at least one key reference included - either the encoded key that was used
     * when signing or the peerid (so that the verifying peer can look up the key
     * in the PSEMembershipService keystore). If both the public key and the peerid
     * are sent then the receiving peer can use the enclosed key with the option
     * of occasionally verifying the key via the keystore.
     *
     * The returned PSEAdvertismentSignatureToken contains two XMLElement classes.
     * Both elements are appended to the advertisement during publication:
     * XMLSignatureInfo contains a digest of the original advertisement and key info
     * XMLSignature contains a digest of XMLSignatureInfo and a signature of that digest
     * Refer to the following for reasoning:
     * http://java.sun.com/developer/technicalArticles/xml/dig_signatures/
     *
     * @param advertismentDocument
     * @param includePublicKey
     * @param includePeerID
     * @return PSEAdvertismentSignatureToken
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException
     * @throws IOException
     */
    public PSEAdvertismentSignatureToken signAdvertisement(XMLDocument advertismentDocument, boolean includePublicKey, boolean includePeerID) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");

        PSEUtils.xmlElementDigest(advertismentDocument, messageDigest);
        byte[] advDigest = messageDigest.digest();

        PublicKey publicKey = defaultCredential.getCertificate().getPublicKey();
        XMLSignatureInfo xmlSignatureInfo = new XMLSignatureInfo(advDigest, group.getPeerID(), publicKey.getAlgorithm(), publicKey.getEncoded(), peerSecurityEngine.getSignatureAlgorithm(), includePublicKey, includePeerID);

        XMLDocument xmlSignatureInfoElement = xmlSignatureInfo.getXMLSignatureInfoDocument();

        messageDigest.reset();

        PSEUtils.xmlElementDigest(xmlSignatureInfoElement, messageDigest);
        byte[] xmlSignatureInfoElementDigest = messageDigest.digest();

        ByteArrayInputStream xmlSignatureInfoElementDigestIS = new ByteArrayInputStream(xmlSignatureInfoElementDigest);
        byte[] xmlSignatureInfoElementSignature = peerSecurityEngine.sign(peerSecurityEngine.getSignatureAlgorithm(), defaultCredential, xmlSignatureInfoElementDigestIS);
        xmlSignatureInfoElementDigestIS.close();

        XMLSignature xmlSignature = new XMLSignature(xmlSignatureInfoElementDigest, xmlSignatureInfoElementSignature);

        PSEAdvertismentSignatureToken pseAdvertismentSignatureToken = new PSEAdvertismentSignatureToken(xmlSignatureInfo, xmlSignature);

        return pseAdvertismentSignatureToken;
    }

    /**
     * PSEAdvertismentSignatureToken returned by signAdvertisement
     */
    public class PSEAdvertismentSignatureToken {
        private XMLSignatureInfo xmlSignatureInfo;
        private XMLSignature xmlSignature;
        private PSEAdvertismentSignatureToken(XMLSignatureInfo xmlSignatureInfo, XMLSignature xmlSignature) {
            this.xmlSignatureInfo = xmlSignatureInfo;
            this.xmlSignature = xmlSignature;
        }
        /**
         * If the advertisement is validated with the signature then true
         *
         * @return boolean isValid
         */
        public XMLSignatureInfo getXMLSignatureInfo() {
            return xmlSignatureInfo;
        }
        /**
         * If the peerid that signed the advertisment is present in the
         * membership keystore then true
         *
         * @return boolean isMember
         */
        public XMLSignature getXMLSignature() {
            return xmlSignature;
        }
    }

    private static List advertisementIgnoredElements = null;
    static {
        advertisementIgnoredElements = new ArrayList();
        advertisementIgnoredElements.add("XMLSignatureInfo");
        advertisementIgnoredElements.add("XMLSignature");
    }

    /**
     * Validates a signed advertisement and returns a PSEAdvertismentValidationToken
     * which specifies whether:
     * 1. The signature is good
     * 2. The enclosed peerid is present in the peergroups PSEMembership keystore
     * 3. The enclosed public key is the same as the key in the keystore
     * If there is no enclosed public key then an attempt is made to use the key in
     * the keystore if a peerid is enclosed.
     * If ignoreKeystore is true then a comparison of the enclosed key and the keystore
     * key is not undertaken.
     * If there is no key and no peerid enclosed then the signature will, obviously, fail.
     * @param advertismentDocument
     * @param verifyKeyWithKeystore
     * @return PSEAdvertismentValidationToken
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     * @throws KeyStoreException
     * @throws IOException
     * @throws SignatureException
     */
    public PSEAdvertismentValidationToken validateAdvertisement(XMLDocument advertismentDocument, boolean verifyKeyWithKeystore) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, KeyStoreException, IOException, SignatureException {

        XMLElement xmlSignatureInfoElement = null;
        XMLSignatureInfo xmlSignatureInfo = null;
        XMLSignature xmlSignature = null;

        Enumeration eachElem = advertismentDocument.getChildren();

        while (eachElem.hasMoreElements()) {

            XMLElement anElem = (XMLElement) eachElem.nextElement();

            if ("XMLSignatureInfo".equals(anElem.getName())) {
                xmlSignatureInfoElement = anElem;
                xmlSignatureInfo = new XMLSignatureInfo(anElem);
            } else if ("XMLSignature".equals(anElem.getName())) {
                xmlSignature = new XMLSignature(anElem);
            }
        }

        if (xmlSignatureInfo == null || xmlSignature == null)
            throw new SecurityException("xmlSignatureInfo == null || xmlSignature == null - advertisement missing signature elements");

        byte[] signatureInfoDigest = xmlSignatureInfo.getDigest();
        byte[] signatureDigest = xmlSignature.getDigest();
        byte[] signature = xmlSignature.getSignature();

        if (signatureInfoDigest == null || signatureDigest == null || signature == null)
            throw new SecurityException("signatureInfoDigest == null || signatureDigest == null || signature == null - advertisement missing signature data");

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");

        PSEUtils.xmlElementDigest(advertismentDocument, advertisementIgnoredElements, messageDigest);
        byte[] advertisementDigest = messageDigest.digest();

        // Check the advertisement first - the original digest of the advertisement without XMLSignatureInfo & XMLSignature is compared to one we calculate
        if (!PSEUtils.arrayCompare(advertisementDigest, signatureInfoDigest))
            throw new SecurityException("Digest comparison of original advertisement digest and locally calculated digest failed");

        messageDigest.reset();

        // Check the digests of XMLSignatureInfo
        PSEUtils.xmlElementDigest(xmlSignatureInfoElement, messageDigest);
        byte[] xmlSignatureInfoDigest = messageDigest.digest();
        if (!PSEUtils.arrayCompare(signatureDigest, xmlSignatureInfoDigest))
            throw new SecurityException("Digest comparison of original xmlSignatureInfo digest and locally calculated xmlSignatureInfo digest failed");

        // Get the publickey - try encoded first, otherwise get the certificate out of the keystore with the peerid
        PublicKey publicKey = null;
        boolean isKeystorePublicKey = false;
        if (xmlSignatureInfo.getEncodedKey() != null && xmlSignatureInfo.getKeyAlgorithm() != null) {
            byte[] encodedPublicKeyData = xmlSignatureInfo.getEncodedKey();
            X509EncodedKeySpec encodedPublicKeySpec = new X509EncodedKeySpec(encodedPublicKeyData);
            KeyFactory keyFactory = KeyFactory.getInstance(xmlSignatureInfo.getKeyAlgorithm());
            publicKey = keyFactory.generatePublic(encodedPublicKeySpec);

        } else if (xmlSignatureInfo.getPeerID() != null) {
            X509Certificate certificate = pseStore.getTrustedCertificate(xmlSignatureInfo.getPeerID());
            if (certificate != null) {
                publicKey = certificate.getPublicKey();
                isKeystorePublicKey = true;
            }
        }

        boolean verified = false;

        // do the verification
        if (publicKey != null) {
            Signature sig = Signature.getInstance(xmlSignatureInfo.getSignatureAlgorithm());
            sig.initVerify(publicKey);
            sig.update(signatureDigest);
            verified = sig.verify(signature);
        }

        // check that the key in the keystore matches that of the one sent with the advertisement
        // if isKeystorePublicKey is true then signature is correct with keystor
        boolean isMember = false;
        boolean isCorrectMembershipKey = false;
        if (verifyKeyWithKeystore && xmlSignatureInfo.getPeerID()!=null) {
            if (!isKeystorePublicKey) {
                X509Certificate certificate = pseStore.getTrustedCertificate(xmlSignatureInfo.getPeerID());
                if (certificate != null) {
                    isMember = true;
                    PublicKey storePublicKey = certificate.getPublicKey();
                    if (storePublicKey.equals(publicKey))
                        isCorrectMembershipKey = true;
                }
            } else {
                isMember = true;
                isCorrectMembershipKey = true;
            }
        }

        return new PSEAdvertismentValidationToken(verified, isMember, isCorrectMembershipKey);
    }


    /**
     * PSEAdvertismentValidationToken returned by validateAdvertisement
     */
    public class PSEAdvertismentValidationToken {
        private boolean isValid = false;
        private boolean isMember = false;
        private boolean isCorrectMembershipKey = false;
        private PSEAdvertismentValidationToken(boolean isValid, boolean isMember, boolean isCorrectMembershipKey) {
            this.isValid = isValid;
            this.isMember = isMember;
            this.isCorrectMembershipKey = isCorrectMembershipKey;
        }
        /**
         * If the advertisement is validated with the signature then true
         *
         * @return boolean isValid
         */
        public boolean isValid() {
            return isValid;
        }
        /**
         * If the peerid that signed the advertisment is present in the
         * membership keystore then true
         *
         * @return boolean isMember
         */
        public boolean isMember() {
            return isMember;
        }
        /**
         * If the publickey (corresponding to the peerid) in the keystore is
         * identical to the public key supplied with the advertisement then
         * true
         *
         * @return boolean isValid
         */
        public boolean isCorrectMembershipKey() {
            return isCorrectMembershipKey;
        }
    }

    /**
     *
     * AccessService support
     *
     * @param accessServiceOffererCredential
     * @param peerids
     * @throws CertPathValidatorException
     */
    public void validateOffererCredential(PSECredential accessServiceOffererCredential, String[] aliases) throws CertPathValidatorException {

        if(accessServiceOffererCredential == null)
            throw new CertPathValidatorException("accessServiceOffererCredential is null");

        if(aliases == null)
            throw new CertPathValidatorException("aliases is null");

        X509Certificate[] offererCerts = accessServiceOffererCredential.getCertificateChain();
        if(offererCerts == null)
            throw new CertPathValidatorException("No Certificates Found");

        CertPath certPath = null;
        try {

            KeyStore keyStore = KeyStore.getInstance("jks");
            for (int i=0; i<aliases.length; i++) {
                keyStore.setCertificateEntry(aliases[i], pseStore.getKeyStore().getCertificate(aliases[i]));
            }
            PKIXParameters params = new PKIXParameters(keyStore);
            params.setRevocationEnabled(false);
            CertificateFactory factory = CertificateFactory.getInstance("X509");
            certPath = factory.generateCertPath(Arrays.asList(offererCerts));
            CertPathValidator pathValidator = CertPathValidator.getInstance("PKIX");
            pathValidator.validate(certPath, params);

        } catch(KeyStoreException storeExp) {
            LOG.log(Level.WARNING, storeExp.getMessage(), storeExp);
            throw new CertPathValidatorException("Trusted Certificates could not be verified.");
        } catch(CertificateException certExp) {
            LOG.log(Level.WARNING, certExp.getMessage(), certExp);
            throw new CertPathValidatorException("Certificates could not be validated.");
        } catch(NoSuchAlgorithmException noAlgExp) {
            LOG.log(Level.WARNING, noAlgExp.getMessage(), noAlgExp);
            throw new CertPathValidatorException("Problem with Certificate Algorithm");
        } catch(CertPathValidatorException validateExp) {
            LOG.log(Level.WARNING, validateExp.getMessage(), validateExp);
            throw validateExp;
        } catch(InvalidAlgorithmParameterException paramExp) {
            LOG.log(Level.WARNING, paramExp.getMessage(), paramExp);
            throw new CertPathValidatorException("Problem with Certificate Algorithm");
        }
    }

    /**
     *
     * AccessService support
     *
     * @param accessServiceOffererCredential
     * @param peerids
     * @throws CertPathValidatorException
     */
    public void validateOffererCredential(PSECredential accessServiceOffererCredential) throws CertPathValidatorException {

        if(accessServiceOffererCredential == null)
            throw new CertPathValidatorException("accessServiceOffererCredential is null");

        X509Certificate[] offererCerts = accessServiceOffererCredential.getCertificateChain();
        if(offererCerts == null)
            throw new CertPathValidatorException("No Certificates Found");

        CertPath certPath = null;
        try {

            PKIXParameters params = new PKIXParameters(pseStore.getKeyStore());
            params.setRevocationEnabled(false);
            CertificateFactory factory = CertificateFactory.getInstance("X509");
            certPath = factory.generateCertPath(Arrays.asList(offererCerts));
            CertPathValidator pathValidator = CertPathValidator.getInstance("PKIX");
            pathValidator.validate(certPath, params);

        } catch(KeyStoreException storeExp) {
            LOG.log(Level.WARNING, storeExp.getMessage(), storeExp);
            throw new CertPathValidatorException("Trusted Certificates could not be verified.");
        } catch(CertificateException certExp) {
            LOG.log(Level.WARNING, certExp.getMessage(), certExp);
            throw new CertPathValidatorException("Certificates could not be validated.");
        } catch(NoSuchAlgorithmException noAlgExp) {
            LOG.log(Level.WARNING, noAlgExp.getMessage(), noAlgExp);
            throw new CertPathValidatorException("Problem with Certificate Algorithm");
        } catch(CertPathValidatorException validateExp) {
            LOG.log(Level.WARNING, validateExp.getMessage(), validateExp);
            throw validateExp;
        } catch(InvalidAlgorithmParameterException paramExp) {
            LOG.log(Level.WARNING, paramExp.getMessage(), paramExp);
            throw new CertPathValidatorException("Problem with Certificate Algorithm");
        }
    }

    /**
     * validatePeer Validates the certificate chain in the keystore of a peer
     * against a CA - the user must install a PSEPeerValidationEngineFactory for
     * this to be useful
     * @param peerID
     * @throws CertPathValidatorException
     * @throws KeyStoreException
     * @throws IOException
     */
    public void validatePeer(PeerID peerID) throws CertPathValidatorException, KeyStoreException, IOException {
        X509Certificate[] certList = pseStore.getTrustedCertificateChain(peerID);
        peerValidationEngine.validatePeer(peerID, certList);
    }

    /**
     * PSEKeyStoreManagerFactory
     */
    private static PSEKeyStoreManagerFactory defaultKeyStoreManagerFactory = null;
    /**
     *  Set the default PSEKeyStoreManagerFactory
     **/
    public static void setPSEKeyStoreManagerFactory(PSEKeyStoreManagerFactory newKeyStoreManagerFactory) {
        synchronized (PSEMembershipService.class) {
            if (defaultKeyStoreManagerFactory == null)
                defaultKeyStoreManagerFactory = newKeyStoreManagerFactory;
        }
    }
    /**
     *  A factory for PSE KeyStoreManagers.
     *
     * @see KeyStoreManager
     */
    public interface PSEKeyStoreManagerFactory {
        KeyStoreManager getInstance(PSEMembershipService service, PSEConfigAdv config) throws PeerGroupException;
    }
    /**
     *   Returns the default Authenticator Engine Factory.
     *
     *   @return The current default Authenticator Engine Factory.
     **/
    public static PSEKeyStoreManagerFactory getDefaultKeyStoreManagerFactory() {
        synchronized (PSEMembershipService.class) {
            if (defaultKeyStoreManagerFactory == null) {
                defaultKeyStoreManagerFactory = new PSEKeyStoreManagerFactory() {
                    public KeyStoreManager getInstance(PSEMembershipService service, PSEConfigAdv config) throws PeerGroupException {

                        URI location = config.getKeyStoreLocation();
                        KeyStoreManager store_manager;

                        try {
                            if (null == location) {
                                store_manager = new CMKeyStoreManager(config.getKeyStoreType(), config.getKeyStoreProvider()
                                        ,
                                        service.getGroup(), service.getAssignedID());
                            } else {
                                if (!location.isAbsolute()) {
                                    // Resolve the location of the keystore relative to our prefs location.
                                    location = service.getPeerGroup().getStoreHome().resolve(location);
                                }

                                store_manager = new URIKeyStoreManager(config.getKeyStoreType(), config.getKeyStoreProvider(), location);
                            }

                            return store_manager;
                        } catch (java.security.NoSuchProviderException not_available) {
                            throw new PeerGroupException("Requested KeyStore provider not available", not_available);
                        } catch (java.security.KeyStoreException bad) {
                            throw new PeerGroupException("KeyStore failure initializing KeyStoreManager", bad);
                        }
                    }
                };
            }

            return defaultKeyStoreManagerFactory;
        }
    }

    /**
     * PSEAuthenticatorEngineFactory
     */
    private static PSEAuthenticatorEngineFactory defaultAuthenticatorEngineFactory = null;
    /**
     *  Set the default PSEAuthenticatorEngineFactory
     **/
    public static void setPSEAuthenticatorEngineFactory(PSEAuthenticatorEngineFactory newAuthenticatorEngineFactory) {
        synchronized (PSEMembershipService.class) {
            if (defaultAuthenticatorEngineFactory == null)
                defaultAuthenticatorEngineFactory = newAuthenticatorEngineFactory;
        }
    }
    /**
     *  A factory for PSE Authenticator Engines.
     *
     * @see PSEPeerAuthenticatorEngine
     */
    public interface PSEAuthenticatorEngineFactory {
        PSEAuthenticatorEngine getInstance(PSEMembershipService service, PSEConfigAdv config) throws PeerGroupException;
    }
    /**
     *   Returns the default Authenticator Engine Factory.
     *
     *   @return The current default Authenticator Engine Factory.
     **/
    public static PSEAuthenticatorEngineFactory getDefaultPSEAuthenticatorEngineFactory() {
        synchronized (PSEMembershipService.class) {
            if (defaultAuthenticatorEngineFactory == null) {
                defaultAuthenticatorEngineFactory = new PSEAuthenticatorEngineFactory() {
                    public PSEAuthenticatorEngine getInstance(PSEMembershipService service, PSEConfigAdv config) throws PeerGroupException {
                        return null;
                    }
                };
            }

            return defaultAuthenticatorEngineFactory;
        }
    }

    /**
     * PSESecurityEngineFactory
     */
    private static PSESecurityEngineFactory defaultSecurityEngineFactory = null;
    /**
     *  Set the default PSESecurityEngineFactoryss
     **/
    public static void setPSESecurityEngineFactory(PSESecurityEngineFactory newSecurityEngineFactory) {
        synchronized (PSEMembershipService.class) {
            if (defaultSecurityEngineFactory == null)
                defaultSecurityEngineFactory = newSecurityEngineFactory;
        }
    }
    /**
     *  A factory for PSE Security Engines.
     *
     * @see PSEPeerSecurityEngine
     */
    public interface PSESecurityEngineFactory {
        PSEPeerSecurityEngine getInstance(PSEMembershipService service, PSEConfigAdv config) throws PeerGroupException;
    }
    /**
     *   Returns the default Security Engine Factory.
     *
     *   @return The current default Security Engine Factory.
     **/
    private static PSESecurityEngineFactory getDefaultPSESecurityEngineFactory() {
        synchronized (PSEMembershipService.class) {
            if (defaultSecurityEngineFactory == null) {
                defaultSecurityEngineFactory = new PSESecurityEngineFactory() {
                    public PSEPeerSecurityEngine getInstance(PSEMembershipService service, PSEConfigAdv config) throws PeerGroupException {
                        return new PSEPeerSecurityEngineDefault();
                    }
                };
            }

            return defaultSecurityEngineFactory;
        }
    }
    /**
     *   Default implementation which provides the default behaviour.
     **/
    private static class PSEPeerSecurityEngineDefault implements PSEPeerSecurityEngine {

        /**
         *  Log4J Logger
         **/
        private static final Logger LOG = Logger.getLogger(PSEPeerSecurityEngineDefault.class.getName());

        /**
         *   {@inheritDoc}
         **/
        public byte[] sign(String algorithm, PSECredential credential, InputStream bis)  throws InvalidKeyException, SignatureException, IOException {

            if (null == algorithm) {
                algorithm = getSignatureAlgorithm();
            }

            PSECredentialBridge pseCredentialBridge = new PSECredentialBridge();
            credential.pseKeyBridge(pseCredentialBridge);
            PrivateKey privateKey = pseCredentialBridge.privateKey;
            return PSEUtils.computeSignature(algorithm, privateKey, bis);
        }

        /**
         *   {@inheritDoc}
         **/
        public boolean verify(String algorithm, PSECredential credential, byte[] signature, InputStream bis) throws InvalidKeyException, SignatureException, IOException {
            if (null == algorithm) {
                algorithm = getSignatureAlgorithm();
            }

            return PSEUtils.verifySignature(algorithm, credential.getCertificate(), signature, bis);
        }

        /**
         *   {@inheritDoc}
         **/
        public IssuerInfo generateCertificate(PSECredential credential) throws SecurityException {

            PSECredentialBridge pseCredentialBridge = new PSECredentialBridge();
            credential.pseKeyBridge(pseCredentialBridge);
            PrivateKey privateKey = pseCredentialBridge.privateKey;

            // we need a new cert.
            IssuerInfo info = new IssuerInfo();

            info.cert = credential.getCertificate();
            info.subjectPkey = privateKey;
            String cname = PSEUtils.getCertSubjectCName(info.cert);

            if (null != cname) {
                // remove the -CA which is common to ca root certs.
                if (cname.endsWith("-CA"))
                    cname = cname.substring(0, cname.length() - 3);
            }

            Logging.logCheckedFine(LOG, "Generating new service cert for \'", cname, "\'");

            // generate the service cert and private key
            IssuerInfo serviceinfo = PSEUtils.genCert(cname, info);

            // IssuerInfo serviceinfo = membership.genCert( cname, info, "SHA1withRSA" );

            Logging.logCheckedFine(LOG, "Generated new service cert for \'", cname, "\'");

            return serviceinfo;
        }

        /**
         *   {@inheritDoc}
         **/
        public String getSignatureAlgorithm() {
            return "SHA1withRSA";
        }
    }

    /**
     * The signature algorithm used by the current PeerSecurityEngine
     * @return
     */
    String getPeerSecurityEngineSignatureAlgorithm() {
        if (peerSecurityEngine == null)
            return null;
        return peerSecurityEngine.getSignatureAlgorithm();
    }

    /**
     *
     * Support for PSECredential message signing
     * @param bridge
     * @return
     * @throws InvalidKeyException
     * @throws SignatureException
     * @throws IOException
     */
    byte[] signPSECredentialDocument(PSECredential.PSECredentialSignatureBridge bridge) throws InvalidKeyException, SignatureException, IOException, SecurityException {
        if (!this.getClass().getClassLoader().equals(bridge.getClass().getClassLoader()))
            throw new SecurityException("Illegal attempt to signPSECredentialDocument - wrong classloader");
        if (peerSecurityEngine == null)
            return null;
        return peerSecurityEngine.sign(bridge.getSignatureAlgorithm(), bridge.getPSECredential(), bridge.getInputStream());
    }

    /**
     * Support for WireFormatMessageBinary message signing
     * @param bridge
     * @return
     * @throws InvalidKeyException
     * @throws SignatureException
     * @throws IOException
     */
    public byte[] signWireFormatMessageBinary(net.jxta.impl.endpoint.WireFormatMessageBinary.WireFormatMessageBinarySignatureBridge bridge) throws InvalidKeyException, SignatureException, IOException, SecurityException {
        if (!this.getClass().getClassLoader().equals(bridge.getClass().getClassLoader()))
            throw new SecurityException("Illegal attempt to signWireFormatMessageBinary - wrong classloader");
        if (peerSecurityEngine == null)
            return null;
        return peerSecurityEngine.sign(bridge.getSignatureAlgorithm(), defaultCredential, bridge.getInputStream());
    }

    /**
     *
     * Support for EndpointRouterMessage message signing
     * @param bridge
     * @return
     * @throws InvalidKeyException
     * @throws SignatureException
     * @throws IOException
     */
    public byte[] signEndpointRouterMessage(net.jxta.impl.endpoint.router.EndpointRouterMessage.EndpointRouterMessageSignatureBridge bridge) throws InvalidKeyException, SignatureException, IOException, SecurityException {
        if (!this.getClass().getClassLoader().equals(bridge.getClass().getClassLoader()))
            throw new SecurityException("Illegal attempt to signEndpointRouterMessage - wrong classloader");
        if (peerSecurityEngine == null)
            return null;
        return peerSecurityEngine.sign(bridge.getSignatureAlgorithm(), defaultCredential, bridge.getInputStream());
    }

    /**
     * PSEPeerValidationEngineFactory
     */
    private static PSEPeerValidationEngineFactory defaultPeerValidationEngineFactory = null;
    /**
     *  Set the default PSEPeerValidationEngineFactory
     **/
    public static void setPSEPeerValidationEngineFactory(PSEPeerValidationEngineFactory newPeerValidationEngineFactory) {
        synchronized (PSEMembershipService.class) {
            if (defaultPeerValidationEngineFactory == null)
                defaultPeerValidationEngineFactory = newPeerValidationEngineFactory;
        }
    }
    /**
     *  A factory for PSE Peer Validation Engines.
     *
     * @see PSEPeerValidationEngine
     */
    public interface PSEPeerValidationEngineFactory {
        PSEPeerValidationEngine getInstance(PSEMembershipService service, PSEConfigAdv config) throws PeerGroupException;
    }
    /**
     *   Returns the default Peer Validation Engine Factory.
     *
     *   @return The current default Peer Validation Engine Factory.
     **/
    public static PSEPeerValidationEngineFactory getDefaultPSEPeerValidationEngineFactory() {
        synchronized (PSEMembershipService.class) {
            if (defaultPeerValidationEngineFactory == null) {
                defaultPeerValidationEngineFactory = new PSEPeerValidationEngineFactory() {
                    public PSEPeerValidationEngine getInstance(PSEMembershipService service, PSEConfigAdv config) throws PeerGroupException {
                        return new PSEPeerValidationEngineDefault();
                    }
                };
            }

            return defaultPeerValidationEngineFactory;
        }
    }


    /**
     *   Default implementation which provides the default behaviour.
     **/
    private static class PSEPeerValidationEngineDefault implements PSEPeerValidationEngine {

        /**
         *   {@inheritDoc}
         **/
        public void validatePeer(PeerID peerID, X509Certificate[] certList) throws CertPathValidatorException {
            // could benefit by doing a signature check on the chain,
            // but real purpose of PSEPeerValidationEngine is for users to set
            // up their own CA and use this mechanism to permit PSEMembershipService
            // to do a real check on the certificate chain.
        }
    }

}
