package net.jxta.module;

import net.jxta.impl.modulemanager.ModuleException;

public interface IModuleBuilder<T extends Object> {
  
	/**
	 * Add a module builder listener
	 * @param listener
	 */
	public void addBuilderListemer( IModuleBuilderListener<T> listener );
	
	/**
	 * Remove a module builder listener
	 * @param listener
	 */
	public void removeBuilderListemer( IModuleBuilderListener<T> listener );

	/* A module is defined by its descriptor, which at least consists of a unique name
	 * and a version. This list shows all the descriptors which are supported by this builder
	 * @return
	 */
	public IModuleDescriptor[] getSupportedDescriptors();
		
	/**
	 * Returns true if the module supports the given descriptor
	 * @param descriptor
	 * @return
	 */
	public boolean canBuild( IModuleDescriptor descriptor );

	/**
	 * Create the module, based on the given descriptor. Returns an exception if the
	 * descriptor is not supported
	 * @return
	 */
	public T buildModule( IModuleDescriptor descriptor) throws ModuleException;
}
