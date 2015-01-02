package net.jxta.module;

import net.jxta.document.Advertisement;
import net.jxta.impl.protocol.PlatformConfig;
import net.jxta.peergroup.core.ModuleClassID;
import net.jxta.peergroup.core.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;

public interface IJxtaModuleDescriptor extends IModuleDescriptor{

	/**
	 * Get a module spec ID
	 */
	public ModuleClassID getModuleClassID();

	/**
	 * Get the module spec ID for this module
	 * @return
	 */
	public ModuleSpecID getModuleSpecID();
	
	/**
	 * get a module implementation advertisement
	 * @return
	 */
	public ModuleImplAdvertisement getModuleImplAdvertisement();
	
	/**
	 * Get the class that is represented by this module
	 * @return
	 */
	public String getRepresentedClassName();
		
	/**
	 * Get the advertisement for this module
	 * @return
	 */
	public Advertisement getAdvertisement( PlatformConfig config );
}