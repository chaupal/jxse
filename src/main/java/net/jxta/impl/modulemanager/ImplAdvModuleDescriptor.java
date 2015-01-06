package net.jxta.impl.modulemanager;

import net.jxta.document.Advertisement;
import net.jxta.impl.protocol.PlatformConfig;
import net.jxta.module.IJxtaModuleDescriptor;
import net.jxta.module.IModuleDescriptor;
import net.jxta.peergroup.core.ModuleClassID;
import net.jxta.peergroup.core.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;

public class ImplAdvModuleDescriptor extends AbstractModuleDescriptor
		implements IJxtaModuleDescriptor {

	private ModuleImplAdvertisement implAdv;
	private String version;

	public ImplAdvModuleDescriptor( ModuleImplAdvertisement implAdv) {
		super();
		this.implAdv = implAdv;
		this.prepare();
	}

	public ImplAdvModuleDescriptor( ModuleImplAdvertisement implAdv, String version ) {
		super();
		this.implAdv = implAdv;
		this.version = version;
		this.prepare();
	}

	/**
	 * Prepare the descriptor
	 */
	public void prepare(){ /* do nothing */}

	
	public String getVersion() {
		return version;
	}

	public ModuleClassID getModuleClassID() {
		return this.implAdv.getModuleSpecID().getBaseClass();
	}

	public ModuleSpecID getModuleSpecID() {
		return this.implAdv.getModuleSpecID();
	}

	public ModuleImplAdvertisement getModuleImplAdvertisement() {
		return implAdv;
	}

	protected void setImplAdv(ModuleImplAdvertisement implAdv) {
		this.implAdv = implAdv;
	}

	public String getRepresentedClassName() {
		return super.getIdentifier();
	}

	public Advertisement getAdvertisement(PlatformConfig config) {
		// TODO Auto-generated method stub
		return null;
	}

	public int compareTo(IModuleDescriptor o) {
		ModuleDescriptorComparable dc = new ModuleDescriptorComparable(this);
		return dc.compareTo(o);
	}

	@Override
	protected boolean onInitialised() {
		// TODO Auto-generated method stub
		return false;
	}
}
