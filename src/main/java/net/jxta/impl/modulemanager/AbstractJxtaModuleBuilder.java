package net.jxta.impl.modulemanager;

import net.jxta.module.IJxtaModuleBuilder;
import net.jxta.module.IJxtaModuleDescriptor;
import net.jxta.module.IModuleDescriptor;
import net.jxta.peergroup.core.Module;
import net.jxta.protocol.ModuleImplAdvertisement;

public abstract class AbstractJxtaModuleBuilder<T extends Module> extends AbstractModuleBuilder<T> implements IJxtaModuleBuilder<T>{

	public AbstractJxtaModuleBuilder() {
	}

	@Override
	protected boolean onInitBuilder(IModuleDescriptor descriptor) {
		return true;
	}
	
	/**
	 * Get the descriptor for the given impl advertisement, or null if it is not supported by this builder
	 */
	public IJxtaModuleDescriptor getDescriptor(ModuleImplAdvertisement implAdv) {
		ImplAdvertisementComparable comp = new ImplAdvertisementComparable( implAdv );
		for( IModuleDescriptor descriptor: super.getSupportedDescriptors() ){
			if(!( descriptor instanceof IJxtaModuleDescriptor ))
				continue;
			IJxtaModuleDescriptor jd = (IJxtaModuleDescriptor) descriptor;
			if( comp.compareTo( jd.getModuleImplAdvertisement() ) == 0 )
				return jd;
		}
		return null;
	}
}