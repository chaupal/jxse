package net.jxta.platform;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import net.jxta.exception.JxtaException;

/** 
 * JxtaApplication provides a JXTA platform bootstrapping and NetworkManager factory. 
 * The purpose of this class is to provide and define initial entry point for JXTA platform initializing and bootstrapping.
 * One of the main goals of this class is to provide centric-aware approach to the usage of NetworkManager objects created in client application. 
 * 
 * @author Andriy N. Lavrusha
 */
public final class JxtaApplication {        
    
    /**
     * Creates new instance of NetworkManager or retrieves an existing instance if already created.
     * 
     * @param mode  Node operating mode {@link ConfigMode}
     * @param instanceName  Node name
     * @param instanceHome  Node home directory
     * @return  Instance of {@link NetworkManager}     
     * @throws JxtaException Throws exception of type net.jxta.exception.JxtaException if instance names do not match on existing instance
     * 
     * @see net.jxta.platform.NetworkManager
     */
    public static NetworkManager getNetworkManager(NetworkManager.ConfigMode mode, String instanceName, URI instanceHome) throws JxtaException {                                                      
        return NetworkManagerProvider.getNetworkManager(mode, instanceName, instanceHome);
    } 
    
    /**
     *
     * Performs search of created instances of NetworkManager
     * 
     * @param instanceHome  Node home directory
     * @return  Instance of {@link NetworkManager} 
     */
    public static NetworkManager findNetworkManager(URI instanceHome) {                                                      
        return NetworkManagerProvider.findNetworkManager(instanceHome);
    }    
    
    private static class NetworkManagerProvider {                                           
        private static final Map<URI, NetworkManager> networkManagerInstances;
        
        static {
            networkManagerInstances = new HashMap<URI, NetworkManager>(10); 
        } 
        
        private static synchronized NetworkManager findNetworkManager(URI instanceHome) {            
            
            if (instanceHome == null) {
                instanceHome = URI.create(NetworkManager.DEFAULT_INSTANCE_HOME);
            }
            
            if (networkManagerInstances.containsKey(instanceHome)) {
                return networkManagerInstances.get(instanceHome);
            }      
            
            return null;
        }

        private static synchronized NetworkManager getNetworkManager(NetworkManager.ConfigMode mode, String instanceName, URI instanceHome) throws JxtaException {
            NetworkManager networkManager;
            
            if (instanceHome == null) {
                instanceHome = URI.create(NetworkManager.DEFAULT_INSTANCE_HOME);
            }
            
            if (networkManagerInstances.containsKey(instanceHome)) {
                networkManager = networkManagerInstances.get(instanceHome);
                
                //Check instance name should be equal
                if (!instanceName.isEmpty() && !networkManager.getInstanceName().equals(instanceName)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error in JxtaApplication.NetworkManagerProvider.getNetworkManager()!")
                    .append(System.getProperty("line.separator"))
                    .append("Existing network manager instance name does not match requested instance name.")
                    .append(System.getProperty("line.separator"))
                    .append("Requested instance name: ").append(instanceName)
                    .append(System.getProperty("line.separator"))
                    .append("Existing instance name: ").append(networkManager.getInstanceName());
                    throw new JxtaException(stringBuilder.toString());
                }
                return networkManagerInstances.get(instanceHome);
            }

            try {                
                networkManager = new NetworkManager(mode, instanceName, instanceHome);
                networkManagerInstances.put(instanceHome, networkManager);                
            } catch (IOException e) {
                networkManager = null;
            }
            
            return networkManager;
        }
    }
}


