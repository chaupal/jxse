/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jxta.impl.peergroup;

import net.jxta.exception.PeerGroupAuthenticationException;
import net.jxta.exception.PeerGroupException;
import net.jxta.impl.membership.pse.StringAuthenticator;
import net.jxta.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.PeerGroup;

/**
 *
 * @author mindarchitect
 */
class StringAuthenticationStrategy extends AuthenticationStrategy {    
    private final static transient Logger LOG = Logging.getLogger(StringAuthenticationStrategy.class.getName());
    
    private final String keyStorePassword;
    private final String identity;
    private final String identityPassword;
    
    StringAuthenticationStrategy(PeerGroup peerGroup, String keyStorePassword, String identity, String identityPassword) {
        super(peerGroup); 
        this.identity = identity;
        this.identityPassword = identityPassword;
        this.keyStorePassword = keyStorePassword;
    }
    
    @Override
    void authenticate() throws PeerGroupException, PeerGroupAuthenticationException {        
        MembershipService membershipService = peerGroup.getMembershipService();        
        
        StringAuthenticator stringAuthenticator = (StringAuthenticator) getAuthenticator(AuthenticationType.StringAuthentication);                                       

        if (stringAuthenticator == null) {
            throw new PeerGroupException("Failed to get a StringAuthenticator for group: " + peerGroup.getPeerGroupName());
        } else {
            stringAuthenticator.setKeyStorePassword(keyStorePassword);
            stringAuthenticator.setIdentity(identity);
            stringAuthenticator.setIdentityPassword(identityPassword);

            if (stringAuthenticator.isReadyForJoin()) {

                membershipService.join(stringAuthenticator);
                
                if (membershipService.getDefaultCredential() == null) {
                    throw new PeerGroupException("Failed to login to group: " + peerGroup.getPeerGroupName());
                }                
            } else {
                String errorMessage = "Failed to join the group: " + peerGroup.getPeerGroupName();
                LOG.error(errorMessage);
                throw new PeerGroupAuthenticationException(errorMessage);
            }
        }
    }        
}
