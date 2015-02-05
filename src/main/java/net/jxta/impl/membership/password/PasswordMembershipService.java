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

package net.jxta.impl.membership.password;

import net.jxta.credential.AuthenticationCredential;
import net.jxta.credential.Credential;
import net.jxta.credential.CredentialPCLSupport;
import net.jxta.document.*;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.membership.Authenticator;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.service.Service;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 *  The password membership service provides a Membership Service implementation
 *  which is based on a password scheme similar to the unix
 *  <code>/etc/password</code> system.</code>
 *
 * <p/><strong>This implementation is intended as an example of a
 *  simple Membership Service and <em>NOT</em> as a practical secure
 *  Membership Service.<strong>
 *
 * @see net.jxta.membership.MembershipService
 *
 */
public class PasswordMembershipService implements MembershipService {

    private static final Logger LOG = Logging.getLogger(PasswordMembershipService.class.getName());

    /**
     * Well known service specification identifier: password membership
     */
    public static final ModuleSpecID passwordMembershipSpecID = (ModuleSpecID)ID.create(URI.create("urn:jxta:uuid-DeadBeefDeafBabaFeedBabe000000050206"));

    /**
     * This class provides the sub-class of Credential which is associated
     * with the password membership service.
     */
    public final static class PasswordCredential implements Credential, CredentialPCLSupport {

        /**
         * The MembershipService service which generated this credential.
         */
        PasswordMembershipService source;

        /**
         * The identity associated with this credential
         */
        String identity;

        /**
         * The peerid associated with this credential.
         */
        ID peerId;

        /**
         * The peerid which has been "signed" so that the identity may be verified.
         */
        String signedPeerID;

        /**
         *  property change support
         */
        private PropertyChangeSupport support = new PropertyChangeSupport(this);

        /**
         *  Whether the credential is valid.
         */
        boolean valid = true;

        protected PasswordCredential(PasswordMembershipService source, String identity, String signedPeerID) {

            this.source = source;
            this.identity = identity;
            this.peerId = source.peergroup.getPeerID();
            this.signedPeerID = signedPeerID;
        }

        protected PasswordCredential(PasswordMembershipService source, Element root) throws PeerGroupException {
            this.source = source;
            initialize(root);
        }

        /**
         *  Add a listener
         *
         *  @param listener the listener
         */
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            support.addPropertyChangeListener(listener);
        }

        /**
         *  Add a listener
         *
         *  @param propertyName the property to watch
         *  @param listener the listener
         */
        public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
            support.addPropertyChangeListener(propertyName, listener);
        }

        /**
         *  Remove a listener
         *
         *  @param listener the listener
         */
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            support.removePropertyChangeListener(listener);
        }

        /**
         *  Remove a listener
         *
         *  @param propertyName the property which was watched
         *  @param listener the listener
         */
        public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
            support.removePropertyChangeListener(propertyName, listener);
        }

        /**
         * {@inheritDoc}
         */
        public ID getPeerGroupID() {
            return source.peergroup.getPeerGroupID();
        }

        /**
         * {@inheritDoc}
         */
        public ID getPeerID() {
            return peerId;
        }

        /**
         *  Set the peerid for this credential.
         *
         *  @param  peerid   the peerid for this credential
         */
        private void setPeerID(PeerID peerid) {
            this.peerId = peerid;
        }

        /**
         * {@inheritDoc}
         *
         * <p/>PasswordCredential never expire.
         */
        public boolean isExpired() {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * <p/>PasswordCredential are almost always valid.
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * {@inheritDoc}
         *
         * <p/>PasswordCredential are always valid except after resign.
         */
        private void setValid(boolean valid) {
            boolean oldValid = isValid();

            this.valid = valid;

            if (oldValid != valid) {
                support.firePropertyChange("valid", oldValid, valid);
            }
        }

        /**
         * {@inheritDoc}
         */
        public Object getSubject() {
            return identity;
        }

        /**
         *  Sets the subject for this Credential
         *
         *  @param  subject The subject for this credential.
         */
        private void setSubject(String subject) {
            identity = subject;
        }

        /**
         * {@inheritDoc}
         */
        public Service getSourceService() {
            return source;
        }

        /**
         * {@inheritDoc}
         */
        public StructuredDocument getDocument(MimeMediaType as) throws Exception {
            StructuredDocument doc = StructuredDocumentFactory.newStructuredDocument(as, "jxta:Cred");

            if (doc instanceof XMLDocument) {
                ((Attributable) doc).addAttribute("xmlns:jxta", "http://jxta.org");
                ((Attributable) doc).addAttribute("xml:space", "preserve");
            }

            if (doc instanceof Attributable) {
                ((Attributable) doc).addAttribute("type", "jxta:PasswdCred");
            }

            Element e = doc.createElement("PeerGroupID", getPeerGroupID().toString());

            doc.appendChild(e);

            e = doc.createElement("PeerID", getPeerID().toString());
            doc.appendChild(e);

            e = doc.createElement("Identity", identity);
            doc.appendChild(e);

            // FIXME 20010327   Do some kind of signing here based on password.
            e = doc.createElement("ReallyInsecureSignature", signedPeerID);
            doc.appendChild(e);

            return doc;
        }

        /**
         *  Process an individual element from the document.
         *
         *  @param elem the element to be processed.
         *  @return true if the element was recognized, otherwise false.
         */
        protected boolean handleElement(XMLElement elem) {
            if (elem.getName().equals("PeerGroupID")) {
                try {
                    URI gID = new URI(elem.getTextValue());
                    ID pgid = IDFactory.fromURI(gID);

                    if (!pgid.equals(getPeerGroupID())) {
                        throw new IllegalArgumentException("Operation is from a different group. " + pgid + " != " + getPeerGroupID());
                    }
                } catch (URISyntaxException badID) {
                    throw new IllegalArgumentException("Bad PeerGroupID in advertisement: " + elem.getTextValue());
                }
                return true;
            }

            if (elem.getName().equals("PeerID")) {
                try {
                    URI pID = new URI(elem.getTextValue());
                    ID pid = IDFactory.fromURI(pID);

                    setPeerID((PeerID) pid);
                } catch (URISyntaxException badID) {
                    throw new IllegalArgumentException("Bad Peer ID in advertisement: " + elem.getTextValue());
                } catch (ClassCastException badID) {
                    throw new IllegalArgumentException("Id is not a peer id: " + elem.getTextValue());
                }
                return true;
            }

            if (elem.getName().equals("Identity")) {
                setSubject(elem.getTextValue());
                return true;
            }

            if (elem.getName().equals("ReallyInsecureSignature")) {
                signedPeerID = elem.getTextValue();
                return true;
            }

            // element was not handled
            return false;
        }

        /**
         *  Intialize from a portion of a structured document.
         */
        protected void initialize(Element root) {

            if (!XMLElement.class.isInstance(root)) {
                throw new IllegalArgumentException(getClass().getName() + " only supports XMLElement");
            }

            XMLElement doc = (XMLElement) root;

            String typedoctype = "";
            Attribute itsType = ((Attributable) root).getAttribute("type");

            if (null != itsType) {
                typedoctype = itsType.getValue();
            }

            String doctype = doc.getName();

            if (!doctype.equals("jxta:PasswdCred") && !typedoctype.equals("jxta:PasswdCred")) {
                throw new IllegalArgumentException(
                        "Could not construct : " + getClass().getName() + "from doc containing a " + doctype);
            }

            Enumeration elements = doc.getChildren();

            while (elements.hasMoreElements()) {

                XMLElement elem = (XMLElement) elements.nextElement();

                if (!handleElement(elem)) {
                    Logging.logCheckedWarning(LOG, "Unhandleded element \'", elem.getName(), "\' in ", doc.getName());
                }

            }

            // sanity check time!

            if (null == getSubject()) 
                throw new IllegalArgumentException("subject was never initialized.");

            if (null == getPeerID()) 
                throw new IllegalArgumentException("peer id was never initialized.");

            if (null == signedPeerID) 
                throw new IllegalArgumentException("signed peer id was never initialized.");

            // FIXME bondolo@jxta.org 20030409 should check for duplicate elements and for peergroup element

        }
    }

    /**
     * Creates an authenticator for the password membership service. Anything
     *  entered into the identity info section of the Authentication credential
     *  is ignored.
     */
    public final static class PasswordAuthenticator implements Authenticator {

        /**
         * The Membership Service which generated this authenticator.
         */
        PasswordMembershipService source;

        /**
         * The Authentication which was provided to the Apply operation of the
         * membership service.
         */
        AuthenticationCredential application;

        /**
         * the identity which is being claimed
         */
        String identity = null;

        /**
         * the password for that identity.
         */
        String password = null;

        /**
         * Creates an authenticator for the password MembershipService service. The only method
         * supported is "PasswordAuthentication". Anything entered into the identity info
         * section of the Authentication credential is ignored.
         *
         * @param source The instance of the password membership service which created this
         * authenticator.
         * @param application The Anything entered into the identity info section of the Authentication
         * credential is ignored.
         */
        PasswordAuthenticator(PasswordMembershipService source, AuthenticationCredential application) {
            this.source = source;
            this.application = application;

            // XXX 20010328 bondolo@jxta.org Could do something with the authentication credential here.
        }

        /**
         * {@inheritDoc}
         */
        public MembershipService getSourceService() {
//            return (MembershipService) source.getInterface();
            return source;
        }

        /**
         * {@inheritDoc}
         */
        synchronized public boolean isReadyForJoin() {
            if ( null == password )
                Logging.logCheckedDebug(LOG, "null password");
            if ( null == password )
                Logging.logCheckedDebug(LOG, "null identity");
            return ((null != password) && (null != identity));
        }

        /**
         * {@inheritDoc}
         */
        public String getMethodName() {
            return "PasswordAuthentication";
        }

        /**
         * {@inheritDoc}
         */
        public AuthenticationCredential getAuthenticationCredential() {
            return application;
        }

        public void setIdentity(String who) {
            identity = who;
        }

        public String getIdentity() {
            return identity;
        }

        public void setPassword(String secret) {
            password = secret;
        }

        protected String getPassword() {
            return password;
        }
    }

    /**
     * the peergroup to which this service is associated.
     */
    private PeerGroup peergroup = null;

    /**
     *  the default "nobody" credential
     */
    private Credential  defaultCredential = null;

    /**
     * The current set of principals associated with this peer within this peegroup.
     */
    private List principals;

    /**
     * The set of AuthenticationCredentials which were used to establish the principals.
     */
    private List authCredentials;

    /**
     * The ModuleImplAdvertisement which was used to instantiate this service.
     */
    private ModuleImplAdvertisement implAdvertisement = null;

    /**
     * An internal table containing the identity and password pairs as parsed from the
     * the PeerGroupAdvertisement.
     */
    private Map logins = null;

    /**
     *  property change support
     */
    private PropertyChangeSupport support;

    /**
     *  Default constructor. Normally only called by the peer group.
     * @throws net.jxta.exception.PeerGroupException
     */
    public PasswordMembershipService() throws PeerGroupException {
        principals = new ArrayList();
        authCredentials = new ArrayList();

//        support = new PropertyChangeSupport(getInterface());
        support = new PropertyChangeSupport(this);
    }

    /**
     *  Add a listener
     *
     *  @param listener the listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    /**
     *  Add a listener
     *
     *  @param propertyName the property to watch
     *  @param listener the listener
     */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        support.addPropertyChangeListener(propertyName, listener);
    }

    /**
     *  Remove a listener
     *
     *  @param listener the listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    /**
     *  Remove a listener
     *
     *  @param propertyName the property which was watched
     *  @param listener the listener
     */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        support.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * {@inheritDoc}
     * @param impl
     * @throws net.jxta.exception.PeerGroupException
     */
    public void init(PeerGroup group, ID assignedID, Advertisement impl) throws PeerGroupException {

        peergroup = group;
        implAdvertisement = (ModuleImplAdvertisement) impl;

        if (Logging.SHOW_CONFIG && LOG.isConfigEnabled()) {

            StringBuilder configInfo = new StringBuilder("Configuring Password Membership Service : " + assignedID);

            configInfo.append("\n\tImplementation:");
            configInfo.append("\n\t\tModule Spec ID: ").append(implAdvertisement.getModuleSpecID());
            configInfo.append("\n\t\tImpl Description : ").append(implAdvertisement.getDescription());
            configInfo.append("\n\t\tImpl URI : ").append(implAdvertisement.getUri());
            configInfo.append("\n\t\tImpl Code : ").append(implAdvertisement.getCode());
            configInfo.append("\n\tGroup Params:");
            configInfo.append("\n\t\tGroup: ").append(group.getPeerGroupName());
            configInfo.append("\n\t\tGroup ID: ").append(group.getPeerGroupID());
            configInfo.append("\n\t\tPeer ID: ").append(group.getPeerID());

            LOG.config(configInfo.toString());
        }

        PeerGroupAdvertisement configAdv = group.getPeerGroupAdvertisement();

        XMLElement myParam = (XMLElement) configAdv.getServiceParam(assignedID);

        logins = new HashMap();

        if (null == myParam) {
            throw new PeerGroupException("parameters for group passwords missing");
        }

        for (Enumeration allLogins = myParam.getChildren(); allLogins.hasMoreElements();) {
            XMLElement loginElement = (XMLElement) allLogins.nextElement();

            if (loginElement.getName().equals("login")) {
                String etcPassword = loginElement.getTextValue();
                int nextDelim = etcPassword.indexOf(':');

                if (-1 == nextDelim) {
                    continue;
                }
                String login = etcPassword.substring(0, nextDelim).trim();
                int lastDelim = etcPassword.indexOf(':', nextDelim + 1);
                String password = etcPassword.substring(nextDelim + 1, lastDelim);

                Logging.logCheckedDebug(LOG, "Adding login : \'", login, "\' with encoded password : \'", password, "\'");
                logins.put(login, password);

            }
        }

        // FIXME    20010327    bondolo@jxta.org Make up the signed bit.

        // We initialise our set of principals to the resigned state.
        resign();

    }

//    /**
//     * {@inheritDoc}
//     */
//    public Service getInterface() {
//        return this;
//    }

    /**
     * {@inheritDoc}
     */
    public Advertisement getImplAdvertisement() {
        return implAdvertisement;
    }

    /**
     * {@inheritDoc}
     *
     * <p/>Currently this service starts by itself and does not expect
     * arguments.
     * @param arg
     */
    public int startApp(String[] arg) {
        return START_OK;
    }

    /**
     * {@inheritDoc}
     *
     * <p/>This request is currently ignored.
     */
    public void stopApp() {
        resign();
    }

    /**
     * {@inheritDoc}
     * @throws net.jxta.exception.PeerGroupException
     * @throws net.jxta.exception.ProtocolNotSupportedException
     */
    public Authenticator apply(AuthenticationCredential application) throws PeerGroupException, ProtocolNotSupportedException {

        String method = application.getMethod();

        if ((null != method) && !"StringAuthentication".equals(method) && !"PasswordAuthentication".equals(method)) {
            throw new ProtocolNotSupportedException("Authentication method not supported!");
        }

        return new PasswordAuthenticator(this, application);
    }

    /**
     * {@inheritDoc}
     */
    public Credential getDefaultCredential() {
        return defaultCredential;
    }

    /**
     * {@inheritDoc}
     */
    private void setDefaultCredential(Credential newDefault) {
        Credential oldDefault = defaultCredential;

        defaultCredential = newDefault;

        support.firePropertyChange("defaultCredential", oldDefault, newDefault);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Enumeration<Credential> getCurrentCredentials() {
        return Collections.enumeration(principals);
    }

    /**
     * {@inheritDoc}
     * @throws net.jxta.exception.PeerGroupException
     */
    public Credential join(Authenticator authenticated) throws PeerGroupException {

        if (!(authenticated instanceof PasswordAuthenticator)) {
            throw new ClassCastException("This is not my authenticator!");
        }

        if (this != authenticated.getSourceService()) {
            throw new ClassCastException("This is not my authenticator!");
        }

        if (!authenticated.isReadyForJoin()) {
            throw new PeerGroupException("Not Ready to join!");
        }

        String identity = ((PasswordAuthenticator) authenticated).getIdentity();
        String password = ((PasswordAuthenticator) authenticated).getPassword();

        if (!checkPassword(identity, password)) {
            throw new PeerGroupException("Incorrect Password!");
        }

        // FIXME    20010327    bondolo@jxta.org Make up the signed bit.

        Credential newCred;

        synchronized (this) {
            newCred = new PasswordCredential(this, identity, "blah");

            principals.add(newCred);

            authCredentials.add(authenticated.getAuthenticationCredential());
        }

        support.firePropertyChange("addCredential", null, newCred);

        if (null == getDefaultCredential()) {
            setDefaultCredential(newCred);
        }

        return newCred;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void resign() {
        Iterator eachCred = Arrays.asList(principals.toArray()).iterator();

        synchronized (this) {
            principals.clear();
            authCredentials.clear();
        }

        setDefaultCredential(null);

        while (eachCred.hasNext()) {
            PasswordCredential aCred = (PasswordCredential) eachCred.next();

            aCred.setValid(false);
        }
    }

    /**
     * {@inheritDoc}
     * @throws net.jxta.exception.PeerGroupException
     */
    public Credential makeCredential(Element element) throws PeerGroupException, Exception {
        return new PasswordCredential(this, element);
    }

    /**
     * Given an identity and an encoded password determine if the password is
     * correct.
     *
     * @param identity the identity which the user is trying to claim
     * @param password the password guess being tested.
     * @return true if the password was correct for the specified identity
     * otherwise false.
     */
    private boolean checkPassword(String identity, String password) {
        boolean result;

        if (!logins.containsKey(identity)) {
            return false;
        }

        String encodedPW = makePssword(password);
        Logging.logCheckedDebug(LOG, "Password \'", password, "\' encodes as: \'", encodedPW, "\'");

        String mustMatch = (String) logins.get(identity);

        // if there is a null password for this identity then match everything.
        if (mustMatch.equals("")) {
            return true;
        }

        result = encodedPW.equals(mustMatch);

        return result;
    }

    /**
     *  This is the method used to make the password strings. We only provide
     *  one way encoding since we can compare the encoded strings.
     *
     *  <p/>FIXME 20010402  bondolo : switch to use the standard
     *  crypt(3) algorithm for encoding the passwords. The current algorithm has
     *  been breakable since ancient times, crypt(3) is also weak, but harder to
     *  break.
     *
     *   @param source  the string to encode
     *   @return String the encoded version of the password.
     *
     */
    public static String makePssword(String source) {

        /**
         *
         * A->D  B->Q  C->K  D->W  E->H  F->R  G->T  H->E  I->N  J->O  K->G  L->X  M->C
         * N->V  O->Y  P->S  Q->F  R->J  S->P  T->I  U->L  V->Z  W->A  X->B  Y->M  Z->U
         *
         */

        final String xlateTable = "DQKWHRTENOGXCVYSFJPILZABMU";

        StringBuilder work = new StringBuilder(source);

        for (int eachChar = work.length() - 1; eachChar >= 0; eachChar--) {
            char aChar = Character.toUpperCase(work.charAt(eachChar));

            int replaceIdx = xlateTable.indexOf(aChar);

            if (-1 != replaceIdx) {
                work.setCharAt(eachChar, (char) ('A' + replaceIdx));
            }
        }

        return work.toString();
    }
}

