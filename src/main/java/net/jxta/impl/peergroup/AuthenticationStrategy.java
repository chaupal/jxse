package net.jxta.impl.peergroup;

import net.jxta.credential.AuthenticationCredential;
import net.jxta.exception.PeerGroupAuthenticationException;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.membership.Authenticator;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.PeerGroup;

/**
 *
 * @author mindarchitect
 */
abstract class AuthenticationStrategy {    
    protected final PeerGroup peerGroup;    
    
    protected AuthenticationStrategy(PeerGroup peerGroup) {
        this.peerGroup = peerGroup;
    }
    
    protected PeerGroup getPeerGroup() {
        return peerGroup;
    }
    
    protected <T extends Authenticator> T getAuthenticator(AuthenticationType authenticationType) throws PeerGroupException {        
        T authenticator = null;
        MembershipService membershipService = peerGroup.getMembershipService();
        AuthenticationCredential authenticationCredentials = new AuthenticationCredential(peerGroup, authenticationType.toString(), null);
        
        try {
            authenticator = (T) membershipService.apply(authenticationCredentials);
        } catch(ProtocolNotSupportedException ex) {
        }
        
        return authenticator;
    }
    
    protected enum AuthenticationType {
        StringAuthentication, 
        EngineAuthentication    
    }
    
    abstract void authenticate() throws PeerGroupException, PeerGroupAuthenticationException;        
}
