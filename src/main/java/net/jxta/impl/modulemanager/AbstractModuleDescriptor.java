package net.jxta.impl.modulemanager;

import java.util.ArrayList;
import java.util.Collection;

import net.jxta.module.IModuleDescriptor;
import net.jxta.util.cardinality.Cardinality;

public abstract class AbstractModuleDescriptor implements IModuleDescriptor {

	private String identifier;
	private String refClass;
	private String description;
	private String version;
	private Cardinality cardinality;
	private boolean initialised = false;
	
	private Collection<IModuleDescriptor> dependencies;

	protected AbstractModuleDescriptor() {
		dependencies = new ArrayList<IModuleDescriptor>();
		this.initialised = false;
	}

	protected abstract boolean onInitialised();
	
	public void init() {
		this.initialised = this.onInitialised();
	}

	public boolean isInitialised() {
		return this.initialised;
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public String getDescription() {
		return this.description;
	}

	public String getVersion() {
		return this.version;
	}

	public String getRefClass() {
		return refClass;
	}

	public Cardinality getCardinality() {
		return cardinality;
	}

	protected void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	protected void setRefClass(String refClass) {
		this.refClass = refClass;
	}

	protected void setDescription(String description) {
		this.description = description;
	}

	protected void setVersion(String version) {
		this.version = version;
	}

	protected void setCardinality(Cardinality cardinality) {
		this.cardinality = cardinality;
	}

	protected void addDependency( IModuleDescriptor descriptor ){
		this.dependencies.add( descriptor );
	}

	protected void removeDependency( IModuleDescriptor descriptor ){
		this.dependencies.remove( descriptor );
	}

	public boolean hasDependencies() {
		return !this.dependencies.isEmpty();
	}

	public IModuleDescriptor[] getDependencies() {
		return dependencies.toArray( new IModuleDescriptor[ this.dependencies.size()]);
	}

	@Override
	public int hashCode() {
		String str = this.identifier + ":" + this.version;
		return str.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if( super.equals(obj))
			return true;
		if(!( obj instanceof IModuleDescriptor ))
			return false;
		IModuleDescriptor module = (IModuleDescriptor) obj;
		if( !this.identifier.equals(module.getIdentifier()))
			return false;
		return this.version.equals(module.getVersion());
	}

	@Override
	public String toString() {
		String str = this.identifier + ":"+ this.version;
		if( !( description == null ) && !( description.isEmpty() ))
				str += ":" + description;
		return str;
	}

	/**
	 * Two descriptors are considered comparable if their identifiers and versions match
	 */
	public int compareTo(IModuleDescriptor o) {
		int comp = identifier.compareTo( o.getDescription() );
		if( comp != 0 )
			return comp;
		return version.compareTo( o.getVersion() );
	}
}