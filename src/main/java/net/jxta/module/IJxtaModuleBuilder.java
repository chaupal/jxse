package net.jxta.module;

import net.jxta.peergroup.core.Module;
import net.jxta.protocol.ModuleImplAdvertisement;

public interface IJxtaModuleBuilder<T extends Module> extends IModuleBuilder<T>{

	/**
	 * Get the descriptor for the given module impl advertisement, or null if it isn't supported
	 * @param adv
	 * @return
	 */
	public IJxtaModuleDescriptor getDescriptor(ModuleImplAdvertisement adv);

	/* (non-Javadoc)
	 * @see net.jxta.module.IJxtaModuleBuilder#getRepresentedClass(net.jxta.protocol.ModuleImplAdvertisement)
	 */
	public abstract Class<? extends Module> getRepresentedClass( IModuleDescriptor descriptor);

}