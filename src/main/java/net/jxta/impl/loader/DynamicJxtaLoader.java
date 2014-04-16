package net.jxta.impl.loader;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import net.jxse.module.IJxtaModuleService;
import net.jxta.document.Element;
import net.jxta.impl.peergroup.CompatibilityEquater;
import net.jxta.impl.peergroup.CompatibilityUtils;
import net.jxta.platform.IJxtaLoader;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;

public class DynamicJxtaLoader implements IJxtaLoader {

    /**
     * Default compatibility equater instance.
     */
    private static final CompatibilityEquater COMP_EQ =
    	new CompatibilityEquater() {
        @SuppressWarnings("rawtypes")
		public boolean compatible(Element test) {
            return CompatibilityUtils.isCompatible(test);
        }
    };

    private static DynamicJxtaLoader loader = new DynamicJxtaLoader();
    private IJxtaLoader ref;
    
    
    private Collection<IJxtaLoader> loaders;
    private JxtaModuleServiceLoader moduleLoader;
    
	private DynamicJxtaLoader() {
		loaders = new ArrayList<IJxtaLoader>();
		moduleLoader = new JxtaModuleServiceLoader();
		loaders.add( moduleLoader );
		ref = new RefJxtaLoader( new URL[0], COMP_EQ );
		loaders.add( ref);
	}

	public static DynamicJxtaLoader getInstance(){
		return loader;
	}

	public void addModuleService( IJxtaModuleService<Module> service ){
		moduleLoader.addModuleService( service );
	}

	public void removeModuleService( IJxtaModuleService<Module> service ){
		moduleLoader.removeModuleService( service );
	}

	public void addURL(URL url) {
		ref.addURL(url);
	}

	public ClassLoader getClassLoader() {
		return (ClassLoader) ref;
	}

	public Class<? extends Module> findClass(ModuleSpecID spec) throws ClassNotFoundException {
		for( IJxtaLoader loader: loaders ){
			Class<? extends Module> clss = loader.findClass(spec);
			if( clss != null )
				return clss;
		}
		return null;
	}

	public Class<? extends Module> loadClass(ModuleSpecID spec)
			throws ClassNotFoundException {
		for( IJxtaLoader loader: loaders ){
			Class<? extends Module> clss = loader.loadClass(spec);
			if( clss != null )
				return clss;
		}
		return null;
	}

	public Class<? extends Module> defineClass(ModuleImplAdvertisement impl) {
		for( IJxtaLoader loader: loaders ){
			Class<? extends Module> clss = loader.defineClass(impl);
			if( clss != null )
				return clss;
		}
		return null;
	}

	public ModuleImplAdvertisement findModuleImplAdvertisement(
			Class<? extends Module> clazz) {
		for( IJxtaLoader loader: loaders ){
			ModuleImplAdvertisement implAdv = loader.findModuleImplAdvertisement(clazz);
			if( implAdv != null )
				return implAdv;
		}
		return null;
	}

	public ModuleImplAdvertisement findModuleImplAdvertisement(ModuleSpecID msid) {
		for( IJxtaLoader loader: loaders ){
			ModuleImplAdvertisement implAdv = loader.findModuleImplAdvertisement( msid );
			if( implAdv != null )
				return implAdv;
		}
		return null;
	}

	private class JxtaModuleServiceLoader implements IJxtaLoader{

		private Collection<IJxtaModuleService<Module>> services;
		
		
		JxtaModuleServiceLoader() {
			services = new ArrayList<IJxtaModuleService<Module>>();
		}

		public Class<? extends Module> findClass(ModuleSpecID spec)
				throws ClassNotFoundException {
			for( IJxtaModuleService<Module> service: services ){
				if( service.getModuleSpecID().equals( spec ))
					return service.getModule().getClass();
			}
			return null;
		}

		public Class<? extends Module> loadClass(ModuleSpecID spec)
				throws ClassNotFoundException {
			return this.findClass(spec);
		}

		void addModuleService( IJxtaModuleService<Module> service ){
			this.services.add( service );
		}

		void removeModuleService( IJxtaModuleService<Module> service ){
			this.services.remove( service );
		}

		public Class<? extends Module> defineClass(ModuleImplAdvertisement impl) {
			for( IJxtaModuleService<Module> service: services ){
				if( service.getModuleImplAdvertisement().equals( impl ))
					return service.getModule().getClass();
			}
			return null;
		}

		public ModuleImplAdvertisement findModuleImplAdvertisement(
				Class<? extends Module> clazz) {
			for( IJxtaModuleService<Module> service: services ){
				if( service.getRepresentedClassName().equals( clazz.getCanonicalName() ))
					return service.getModuleImplAdvertisement();
			}
			return null;
		}

		public ModuleImplAdvertisement findModuleImplAdvertisement(
				ModuleSpecID msid) {
			for( IJxtaModuleService<Module> service: services ){
				if( service.getModuleSpecID().equals( msid ))
					return service.getModuleImplAdvertisement();
			}
			return null;
		}

		public void addURL(URL url) {
		}

		public ClassLoader getClassLoader() {
			return null;
		}
		
	}
}