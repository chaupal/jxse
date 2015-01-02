package net.jxta.impl.modulemanager;

import java.util.ArrayList;
import java.util.Collection;

import net.jxta.module.IModuleBuilder;
import net.jxta.module.IModuleBuilderListener;
import net.jxta.module.IModuleDescriptor;

public abstract class AbstractModuleBuilder<T extends Object> implements IModuleBuilder<T> {

	private Collection<IModuleBuilderListener<T>> listeners; 
	private Collection<IModuleDescriptor> descriptors; 
	
	private static final String S_ERR_INVALID_DESCRIPTOR = "The builder cannot build a module from the given descriptor: ";
	
	public AbstractModuleBuilder() {
		super();
		listeners = new ArrayList<IModuleBuilderListener<T>>();
		descriptors = new ArrayList<IModuleDescriptor>();
	}

	public void activate(){ /* DO NOTHING */};
	public void deactivate(){ /* DO NOTHING */};
	
	protected void addDescriptor(IModuleDescriptor descriptor) {
		this.descriptors.add(descriptor);
	}

	protected void removeDescriptor(IModuleDescriptor descriptor) {
		this.descriptors.remove(descriptor);
	}

	public void addBuilderListemer(IModuleBuilderListener<T> listener) {
		this.listeners.add(listener);
	}

	public void removeBuilderListemer(IModuleBuilderListener<T> listener) {
		this.listeners.remove(listener);
	}

	public IModuleDescriptor[] getSupportedDescriptors() {
		return descriptors.toArray( new IModuleDescriptor[ this.descriptors.size()]);
	}

	/**
	 * Build the module
	 * @return
	 */
	protected abstract boolean onInitBuilder( IModuleDescriptor descriptor);
	
	public boolean initBuilder(IModuleDescriptor descriptor) throws ModuleException {
		boolean result = this.onInitBuilder( descriptor);
		for( IModuleBuilderListener<T> listener: listeners )
			listener.notifyModuleBuilt( new ModuleEvent<T>( this, descriptor ));
		return result;
	}

	public boolean canBuild(IModuleDescriptor descriptor) {
		for( IModuleDescriptor desc: descriptors )
			if( desc.equals( descriptor ))
				return true;
		return false;
	}

	/**
	 * Build the module
	 * @return
	 */
	protected abstract T onBuildModule( IModuleDescriptor descriptor);
	
	public T buildModule(IModuleDescriptor descriptor) throws ModuleException {
		if(!this.canBuild(descriptor))
			throw new ModuleException( S_ERR_INVALID_DESCRIPTOR + descriptor );
		T module = this.onBuildModule( descriptor);
		for( IModuleBuilderListener<T> listener: listeners )
			listener.notifyModuleBuilt( new ModuleEvent<T>( this, descriptor, module));
		return module;
	}	
}
