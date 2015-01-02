package net.jxta.impl.modulemanager;

import net.jxta.document.Advertisement;
import net.jxta.impl.protocol.PlatformConfig;
import net.jxta.module.IJxtaModuleDescriptor;
import net.jxta.module.IModuleDescriptor;
import net.jxta.peergroup.core.IJxtaLoader;
import net.jxta.peergroup.core.Module;
import net.jxta.peergroup.core.ModuleClassID;
import net.jxta.peergroup.core.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.util.cardinality.Cardinality;
import net.jxta.util.cardinality.Cardinality.Denominator;

public class JxtaModuleBuilder<T extends Module> extends AbstractModuleBuilder<T>{

	private IJxtaLoader loader;
	
	public JxtaModuleBuilder( IJxtaLoader loader) {
		this.loader = loader;
	}

	/* (non-Javadoc)
	 * @see net.jxta.module.IJxtaModuleBuilder#getRepresentedClass(net.jxta.protocol.ModuleImplAdvertisement)
	 */
	@SuppressWarnings("unchecked")
	public Class<T> getRepresentedClass( ModuleImplAdvertisement implAdv){
		Class<T> clss = (Class<T>) loader.defineClass( implAdv );
		super.addDescriptor( new JxtaModuleDescriptor( implAdv ));
		return clss;
	}
	
	/**
	 * Build the module. We know the class should be correct, so we override the warning
	 */
	@SuppressWarnings("unchecked")
	public T buildModule(IModuleDescriptor descriptor) throws ModuleException {
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
	

	private static class JxtaModuleDescriptor implements IJxtaModuleDescriptor{

		private ModuleImplAdvertisement implAdv;
		
		JxtaModuleDescriptor( ModuleImplAdvertisement implAdv ) {
			super();
			this.implAdv = implAdv;
		}

		public boolean isInitialised() {
			// TODO Auto-generated method stub
			return false;
		}

		public void init() {
			// TODO Auto-generated method stub	
		}

		public String getIdentifier() {
			return this.implAdv.getCode();
		}

		public String getDescription() {
			return this.implAdv.getDescription();
		}

		public String getVersion() {
			return null;
		}

		public Cardinality getCardinality() {
			return Cardinality.create( Denominator.ONE );
		}

		public boolean hasDependencies() {
			return false;
		}

		public IModuleDescriptor[] dependencies() {
			return null;
		}

		public int compareTo(IModuleDescriptor o) {
			ModuleDescriptorComparable dc = new ModuleDescriptorComparable(this);
			return dc.compareTo(o);
		}

		public ModuleClassID getModuleClassID() {
			return implAdv.getModuleSpecID().getBaseClass();
		}

		public ModuleSpecID getModuleSpecID() {
			return this.implAdv.getModuleSpecID();
		}

		public ModuleImplAdvertisement getModuleImplAdvertisement() {
			return this.implAdv;
		}

		public String getRepresentedClassName() {
			return this.implAdv.getCode();
		}

		public Advertisement getAdvertisement(PlatformConfig config) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
