package net.jxta.impl.modulemanager;

import java.util.ArrayList;
import java.util.Collection;

import net.jxta.module.IModuleDescriptor;
import net.jxta.protocol.ModuleImplAdvertisement;

public class ModuleConfig<T extends Object> {

	/**
	 * The module config can present itself as a tree structure, where every node in the
	 * tree represents the peer groups that can load certain modules. In a FLAT representation (default), all the
	 * configured descriptors are made available 
	 * @author Kees
	 *
	 */
	public enum Styles{
		FLAT,
		TREE;
	}
	private Styles style;
	
	private IModuleDescriptor descriptor;
	private Collection<ModuleConfig<T>> children;
	
	public ModuleConfig( IModuleDescriptor descriptor ) {
		this( descriptor, Styles.FLAT );
	}

	protected ModuleConfig( IModuleDescriptor descriptor, Styles style ) {
		super();
		this.style = style;
		this.descriptor = descriptor;
		children = new ArrayList<ModuleConfig<T>>();
	}

	/**
	 * Get the style of the configuration
	 * @return
	 */
	public Styles getStyle() {
		return style;
	}

	/**
	 * Get the descriptor for this config element
	 * @return
	 */
	public IModuleDescriptor getDescriptor() {
		return descriptor;
	}


	protected void addChild( ModuleConfig<T> child ){
		this.children.add( child );
	}

	protected void removeChild( ModuleConfig<T> child ){
		this.children.remove( child );
	}
	
	/**
	 * Get the children for the given module
	 * @param module
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected ModuleConfig<T>[] getChildren( IModuleDescriptor descriptor ){
		return this.children.toArray( new ModuleConfig[ this.children.size() ]);
	}

	/**
	 * Get the children for the given module
	 * @param module
	 * @return
	 */
	public ModuleConfig<T> getModuleConfig( IModuleDescriptor descriptor ){
		if( this.descriptor.equals(descriptor ))
			return this;
		
		for( ModuleConfig<T> child: this.children ){
			ModuleConfig<T> result = child.getModuleConfig(descriptor);
			if(!( result == null ))
				return result;
		}
		return null;
	}

	/**
	 * Get the module configuration object that belongs to the given module implementation advertisement
	 * @param implAdv
	 * @return
	 */
	public static ModuleConfig<?> getModuleConfig( ModuleConfig<?> config, ModuleImplAdvertisement implAdv ){
		IModuleDescriptor descriptor = new ImplAdvDescriptor( implAdv ); 
		return config.getModuleConfig(descriptor);
	}

	/**
	 * Create a descriptor from the given impl advertisement
	 * @author Kees
	 *
	 */
	private static class ImplAdvDescriptor extends ImplAdvModuleDescriptor{
		
		protected ImplAdvDescriptor(ModuleImplAdvertisement implAdv ) {
			super(implAdv);
		}
	}
}
