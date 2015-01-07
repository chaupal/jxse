package net.jxta.impl.modulemanager;

import net.jxta.module.IJxtaModuleDescriptor;
import net.jxta.module.IModuleDescriptor;
import net.jxta.peergroup.core.IJxtaLoader;
import net.jxta.peergroup.core.Module;
import net.jxta.protocol.ModuleImplAdvertisement;

public class JxtaModuleBuilder<T extends Module> extends AbstractJxtaModuleBuilder<T>{

	private IJxtaLoader loader;
	
	public JxtaModuleBuilder( IJxtaLoader loader) {
		this.loader = loader;
	}

	/* (non-Javadoc)
	 * @see net.jxta.module.IJxtaModuleBuilder#getRepresentedClass(net.jxta.protocol.ModuleImplAdvertisement)
	 */
	/* (non-Javadoc)
	 * @see net.jxta.impl.modulemanager.IJxtaModuleBuilder#getRepresentedClass(net.jxta.protocol.ModuleImplAdvertisement)
	 */
	@SuppressWarnings("unchecked")
	public Class<T> getRepresentedClass( ModuleImplAdvertisement implAdv){
		Class<T> clss = (Class<T>) loader.defineClass( implAdv );
		super.addDescriptor( new JxtaModuleDescriptor( implAdv ));
		return clss;
	}

	
	public Class<? extends Module> getRepresentedClass(
			IModuleDescriptor descriptor) {
		if(!( descriptor instanceof IJxtaModuleDescriptor ))
			return null;
		IJxtaModuleDescriptor jd = (IJxtaModuleDescriptor) descriptor;
		return this.getRepresentedClass( jd.getModuleImplAdvertisement() );
	}


	@SuppressWarnings("unchecked")
	@Override
	protected T onBuildModule(IModuleDescriptor descriptor) {
		if( !super.canBuild(descriptor))
			return null;
		
		IJxtaModuleDescriptor jd = (IJxtaModuleDescriptor) descriptor;
		Class<T> clss = (Class<T>) loader.defineClass( jd.getModuleImplAdvertisement());
		try {
			return clss.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
		
	/**
	 * Create a module descriptor
	 * @author Kees
	 *
	 */
	private static class JxtaModuleDescriptor extends ImplAdvModuleDescriptor{

		public JxtaModuleDescriptor(ModuleImplAdvertisement implAdv) {
			super(implAdv);
		}	
	}
}
