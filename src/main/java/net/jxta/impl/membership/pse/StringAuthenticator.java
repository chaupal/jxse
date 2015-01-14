/*
 * Copyright (c) 2001-2008 Sun Microsystems, Inc.  All rights reserved.
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

import net.jxta.credential.AuthenticationCredential;
import net.jxta.id.ID;
import net.jxta.membership.Authenticator;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.logging.Logger;
import net.jxta.logging.Logging;

import javax.crypto.EncryptedPrivateKeyInfo;

import java.io.IOException;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * An authenticator associated with the PSE membership service.
 *
 *@see net.jxta.membership.Authenticator
 *@see net.jxta.membership.MembershipService
 **/
public class StringAuthenticator implements Authenticator {

    private static final Logger LOG = Logging.getLogger(StringAuthenticator.class.getName());

    /**
     * The Membership Service which generated this authenticator.
     **/
    transient PSEMembershipService source;

    /**
     * The Authentication which was provided to the Apply operation of the
     * membership service.
     **/
    transient AuthenticationCredential application;

    /**
     *  The certficate which we are authenticating against
     **/
    transient X509Certificate seedCert;

    /**
     *  The encrypted private key which we must unlock.
     **/
    transient EncryptedPrivateKeyInfo seedKey;

    /**
     * the password for that identity.
     **/
    transient char[] storePassword = null;

    /**
     * the identity which is being claimed
     **/
    transient ID identity = null;

    /**
     * the password for that identity.
     **/
    transient char[] keyPassword = null;

    /**
     * Creates an authenticator for the PSE membership service. Anything entered
     * into the identity info section of the Authentication credential is
     * ignored.
     *
     *  @param source The instance of the PSE membership service which
     *  created this authenticator.
     *  @param application Anything entered into the identity info section of
     *  the Authentication credential is ignored.
     **/
    StringAuthenticator(PSEMembershipService source, AuthenticationCredential application, X509Certificate seedCert, EncryptedPrivateKeyInfo seedKey) {
        this(source, application);

        this.seedCert = seedCert;
        this.seedKey = seedKey;
    }

    /**
     * Creates an authenticator for the PSE membership service. Anything entered
     * into the identity info section of the Authentication credential is
     * ignored.
     *
     *  @param source The instance of the PSE membership service which created
     *  this authenticator.
     *  @param application Anything entered into the identity info section of
     *  the Authentication credential is ignored.
     **/
    StringAuthenticator(PSEMembershipService source, AuthenticationCredential application) {
        this.source = source;
        this.application = application;

        // XXX 20010328 bondolo@jxta.org Could do something with the authentication credential here.
    }

    /**
     * {@inheritDoc}
     * @throws java.lang.Throwable
     **/
    @Override
    protected void finalize() throws Throwable {
        if (null != storePassword) {
            Arrays.fill(storePassword, '\0');
        }

        if (null != keyPassword) {
            Arrays.fill(keyPassword, '\0');
        }

        super.finalize();
    }

    /**
     * {@inheritDoc}
     **/
    public MembershipService getSourceService() {
//        return (MembershipService) source.getInterface();
        return source;
    }

    /**
     * {@inheritDoc}
     **/
    public AuthenticationCredential getAuthenticationCredential() {
        return application;
    }

    /**
     * {@inheritDoc}
     **/
    public String getMethodName() {
        return "StringAuthentication";
    }

    /**
     * {@inheritDoc}
     **/
    synchronized public boolean isReadyForJoin() {
        if (null != seedCert) {
            Logging.logCheckedDebug(LOG, "seed certificate:\n", seedCert.toString());
            return null != PSEUtils.pkcs5_Decrypt_pbePrivateKey(keyPassword, seedCert.getPublicKey().getAlgorithm(), seedKey);
        } else {
            Logging.logCheckedDebug(LOG, "null seed certificate");
            return source.getPSEConfig().validPasswd(identity, storePassword, keyPassword);
        }
    }

    /**
     *  Get KeyStore password
     * @return 
     **/
    public char[] getAuth1KeyStorePassword() {
        return storePassword;
    }

    /**
     *  Set KeyStore password
     * @param storePassword
     **/
    public void setAuth1KeyStorePassword(String storePassword) {
        setAuth1KeyStorePassword((null != storePassword) ? storePassword.toCharArray() : (char[]) null);
    }

    /**
     *  Set KeyStore password
     * @param storePassword
     **/
    public void setAuth1KeyStorePassword(char[] storePassword) {
        if (null != this.storePassword) {
            Arrays.fill(this.storePassword, '\0');
        }

        if (null == storePassword) {
            this.storePassword = null;
        } else {
            this.storePassword = storePassword.clone();
        }
    }

    /**
     *  Return the available identities.
     * @param storePassword
     * @return PeerID array of available identities 
     **/
    public PeerID[] getIdentities(char[] storePassword) {

        if (seedCert != null) {
            PeerID[] seed = { source.getPeerGroup().getPeerID() };

            return seed;
        } else {
            try {
                ID[] allkeys = source.getPSEConfig().getKeysList(storePassword);

                // XXX bondolo 20040329 it may be appropriate to login
                // something other than a peer id.
                List peersOnly = new ArrayList();

                Iterator eachKey = Arrays.asList(allkeys).iterator();

                while (eachKey.hasNext()) {
                    ID aKey = (ID) eachKey.next();

                    if (aKey instanceof PeerID) {
                        peersOnly.add(aKey);
                    }
                }

                return (PeerID[]) peersOnly.toArray(new PeerID[peersOnly.size()]);
            } catch (IOException failed) {
                return null;
            } catch (KeyStoreException failed) {
                return null;
            }
        }
    }

    /**
     *  Returns the X509 Certificate associated with the specified ID.
     *
     *  @param storePassword   The password for the keystore.
     *  @param aPeer    The peer who's certificate is desired. For uninitialized
     *  keystores this must be the peerid of the registering peer.
     * @return 
     **/
    public X509Certificate getCertificate(char[] storePassword, ID aPeer) {
        if (seedCert != null) {
            if (aPeer.equals(source.getPeerGroup().getPeerID())) {
                return seedCert;
            } else {
                return null;
            }
        } else {
            try {
                return source.getPSEConfig().getTrustedCertificate(aPeer, storePassword);
            } catch (IOException failed) {
                return null;
            } catch (KeyStoreException failed) {
                return null;
            }
        }
    }

    /**
     *  Get Identity
     * @return 
     **/
    public ID getAuth2Identity() {
        return identity;
    }

    /**
     *  Set Identity
     * @param id
     **/
    public void setAuth2Identity(String id) {
        setAuth2Identity(ID.create(URI.create(id)));
    }

    /**
     *  Set Identity
     * @param identity
     **/
    public void setAuth2Identity(ID identity) {
        this.identity = identity;
    }

    /**
     *  Get identity password
     * @return 
     **/
    public char[] getAuth3IdentityPassword() {
        return keyPassword;
    }

    /**
     *  Set identity password
     * @param keyPassword
     **/
    public void setAuth3IdentityPassword(String keyPassword) {
        setAuth3IdentityPassword((null != keyPassword) ? keyPassword.toCharArray() : (char[]) null);
    }

    /**
     *  Set identity password
     * @param keyPassword
     **/
    public void setAuth3IdentityPassword(char[] keyPassword) {
        if (null != this.keyPassword) {
            Arrays.fill(this.keyPassword, '\0');
        }

        if (null == keyPassword) {
            this.keyPassword = null;
        } else {
            this.keyPassword = keyPassword.clone();
        }
    }
}
