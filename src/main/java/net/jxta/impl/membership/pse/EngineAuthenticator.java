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

import net.jxta.credential.AuthenticationCredential;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.membership.Authenticator;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.logging.Logger;
import net.jxta.logging.Logging;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
public class EngineAuthenticator implements Authenticator {

    private static final Logger LOG = Logging.getLogger(EngineAuthenticator.class.getName());

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
     *
     **/
    transient X509Certificate seedCertificate;

    /**
     *
     **/
    transient PSEAuthenticatorEngine authenticatorEngine;

    /**
     *
     **/
    // transient EncryptedPrivateKeyInfo seedKey;

    /**
     * the password for that identity.
     **/
    transient char[] keyStorePassword = null;

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
    EngineAuthenticator(PSEMembershipService source, AuthenticationCredential application, PSEAuthenticatorEngine authenticatorEngine) {
        // this( source, application );

        this.source = source;
        this.application = application;
        this.seedCertificate = authenticatorEngine.getX509Certificate();
        this.authenticatorEngine = authenticatorEngine;
    }

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
    EngineAuthenticator(PSEMembershipService source, AuthenticationCredential application) {
        this.source = source;
        this.application = application;

        // XXX 20010328 bondolo@jxta.org Could do something with the authentication credential here.
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    protected void finalize() throws Throwable {
        if (null != keyStorePassword) {
            Arrays.fill(keyStorePassword, '\0');
        }

        if (null != keyPassword) {
            Arrays.fill(keyPassword, '\0');
        }

        super.finalize();
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public MembershipService getSourceService() {
        return (MembershipService) source;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public AuthenticationCredential getAuthenticationCredential() {
        return application;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public String getMethodName() {
        return "EngineAuthentication";
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    synchronized public boolean isReadyForJoin() {
        if (null != seedCertificate) {
            Logging.logCheckedDebug(LOG, "null seed certificate");
            return authenticatorEngine.isEnginePresent();
        } else {
            return source.getPSEConfig().validPasswd(identity, keyStorePassword, keyPassword);
        }
    }

    /**
     *  Get KeyStore password
     * @return 
     **/
    public char[] getKeyStorePassword() {
        return keyStorePassword;
    }

    /**
     *  Set KeyStore password
     * @param keyStorePassword
     **/
    public void setKeyStorePassword(String keyStorePassword) {
        if (keyStorePassword != null) {            
            setKeyStorePassword(keyStorePassword.toCharArray());
        }
    }

    /**
     *  Set KeyStore password
     * @param keyStorePassword
     **/
    public void setKeyStorePassword(char[] keyStorePassword) {
        if (null != this.keyStorePassword) {
            Arrays.fill(this.keyStorePassword, '\0');
        }

        if (keyStorePassword == null) {
            this.keyStorePassword = null;
        } else {
            this.keyStorePassword = keyStorePassword.clone();
        }
    }

    /**
     *  Return the available identities.
     * @param keyStorePassword
     * @return 
     **/
    public PeerID[] getIdentities(char[] keyStorePassword) {

        if (seedCertificate != null) {
            PeerID[] seed = { source.getPeerGroup().getPeerID() };

            return seed;
        } else {
            try {
                ID[] allkeys = source.getPSEConfig().getKeysList(keyStorePassword);

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

    public X509Certificate getCertificate(char[] keyStorePassword, ID aPeer) {
        if (seedCertificate != null) {
            if (aPeer.equals(source.getPeerGroup().getPeerID())) {
                return seedCertificate;
            } else {
                return null;
            }
        } else {
            try {
                return source.getPSEConfig().getTrustedCertificate(aPeer, keyStorePassword);
            } catch (IOException | KeyStoreException failed) {
                return null;
            }
        }
    }

    /**
     *  Get Identity
     * @return 
     **/
    public ID getIdentity() {
        return identity;
    }

    /**
     *  Set Identity
     * @param id
     **/
    public void setIdentity(String id) {
        try {
            URI idURI = new URI(id);
            ID identity = IDFactory.fromURI(idURI);

            setIdentity(identity);
        } catch (URISyntaxException badID) {
            throw new IllegalArgumentException("Bad ID");
        } 
    }

    /**
     *  Set Identity
     * @param identity
     **/
    public void setIdentity(ID identity) {
        this.identity = identity;
    }

    /**
     *  Get identity password
     * @return 
     **/
    public char[] getIdentityPassword() {
        return keyPassword;
    }

    /**
     *  Set identity password
     * @param keyPassword
     **/
    public void setIdentityPassword(String keyPassword) {
        if (keyPassword != null) {
            setIdentityPassword(keyPassword.toCharArray());
        }
    }

    /**
     *  Set identity password
     * @param keyPassword
     **/
    public void setIdentityPassword(char[] keyPassword) {
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
