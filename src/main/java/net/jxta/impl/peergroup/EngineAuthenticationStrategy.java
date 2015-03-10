/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jxta.impl.peergroup;

import net.jxta.exception.PeerGroupAuthenticationException;
import net.jxta.exception.PeerGroupException;
import net.jxta.impl.membership.pse.EngineAuthenticator;
import net.jxta.logging.Logger;
import net.jxta.logging.Logging;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.PeerGroup;

/**
 *
 * @author mindarchitect
 */
class EngineAuthenticationStrategy extends AuthenticationStrategy {    
    private final static transient Logger LOG = Logging.getLogger(EngineAuthenticationStrategy.class.getName());        
    
    EngineAuthenticationStrategy(PeerGroup peerGroup) {
        super(peerGroup);         
    }
    
    @Override
    void authenticate() throws PeerGroupException {        
        MembershipService membershipService = peerGroup.getMembershipService();        
        
        EngineAuthenticator engineAuthenticator = (EngineAuthenticator) getAuthenticator(AuthenticationType.EngineAuthentication);                                       

        if (engineAuthenticator == null) {
            throw new PeerGroupException("Failed to get a engine authenticator for group: " + peerGroup.getPeerGroupName());
        } else {            
            if (engineAuthenticator.isReadyForJoin()) {

                membershipService.join(engineAuthenticator);
                
                if (membershipService.getDefaultCredential() == null) {
                    throw new PeerGroupException("Engine authenticator failed to login to group: " + peerGroup.getPeerGroupName());
                }                
            } else {
                String errorMessage = "Engine authenticator failed to join the group: " + peerGroup.getPeerGroupName();
                LOG.error(errorMessage);
                throw new PeerGroupAuthenticationException(errorMessage);
            }
        }
    }        
}
