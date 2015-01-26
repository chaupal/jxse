package net.jxta.module;

import net.jxta.util.cardinality.Cardinality;
import net.jxta.util.cardinality.ICardinality;

public interface IModuleDescriptor extends ICardinality, Comparable<IModuleDescriptor>{
    
	public static final String VERSION_REGEX = "^(\\d+\\.)?(\\d+\\.)?(\\d+)?(v\\d+)$";	
	
	/**
	 * Initialise the descriptor. After the initialisation, all fields must be filled in. The init method is called by 
	 * the builder 
	 */
	public void init();

	/**
	 * Return true if the descriptor is initialised
	 * @return
	 */
	public boolean isInitialised();
	
	public String getIdentifier();

	public String getDescription();
	
	/**
	 * The version for this builder
	 * @return
	 */
	public String getVersion();
	
	/**
	 * Returns a cardinality of the descriptor.
	 * @return
	 */
	public Cardinality getCardinality();
	
	/**
	 * Returns true if the represented module has dependencies with other modules
	 * @return
	 */
	public boolean hasDependencies();
	
	/**
	 * An optional list of dependencies with other modules
	 * @return
	 */
	public IModuleDescriptor[] getDependencies();
}
