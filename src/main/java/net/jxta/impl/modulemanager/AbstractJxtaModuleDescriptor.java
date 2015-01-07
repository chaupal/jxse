package net.jxta.impl.modulemanager;

import java.net.URI;
import java.net.URL;

import net.jxta.document.Advertisement;
import net.jxta.impl.peergroup.CompatibilityUtils;
import net.jxta.impl.protocol.PlatformConfig;
import net.jxta.module.IJxtaModuleDescriptor;
import net.jxta.module.IModuleDescriptor;
import net.jxta.peergroup.core.ModuleClassID;
import net.jxta.peergroup.core.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.util.cardinality.Cardinality;
import net.jxta.util.cardinality.Cardinality.Denominator;

public abstract class AbstractJxtaModuleDescriptor extends AbstractModuleDescriptor
		implements IJxtaModuleDescriptor {

	private ModuleSpecID specID;
	private  ModuleImplAdvertisement implAdv;
	private Cardinality cardinality;
	private String version;

	protected AbstractJxtaModuleDescriptor() {
		this( null, Denominator.ONE );
	}

	protected AbstractJxtaModuleDescriptor( Denominator denominator) {
		this( null, denominator );
	}

	protected AbstractJxtaModuleDescriptor( String version, Denominator denominator) {
		super();
		this.version = version;
		this.cardinality = Cardinality.create(denominator);
		this.prepare();
	}

	public String getVersion() {
		return version;
	}

	public Cardinality getCardinality() {
		return cardinality;
	}

	/**
	 * Prepare the descriptor
	 */
	protected abstract void prepare();
	
	public ModuleClassID getModuleClassID() {
		return this.specID.getBaseClass();
	}

	public ModuleSpecID getModuleSpecID() {
		return this.specID;
	}

	protected void setSpecID( String specID) {
		this.specID = ModuleSpecID.create(URI.create(specID ));
		this.implAdv = CompatibilityUtils.createModuleImplAdvertisement( this.specID, this.getRepresentedClassName(), this.getDescription() ); 
	}
	
	protected void setSpecID(ModuleSpecID specID) {
		this.specID = specID;
		this.implAdv = CompatibilityUtils.createModuleImplAdvertisement( this.specID, this.getRepresentedClassName(), this.getDescription() ); 
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

	/**
	 * The location where the resource can be found
	 * @return
	 */
	public URL getLocation(){
		return null;
	}

	public Advertisement getAdvertisement(PlatformConfig config) {
		// TODO Auto-generated method stub
		return null;
	}

	public int compareTo(IModuleDescriptor o) {
		ModuleDescriptorComparable dc = new ModuleDescriptorComparable(this);
		return dc.compareTo(o);
	}
}
