package net.jxta.impl.modulemanager;

import java.util.EventObject;

import net.jxta.module.IModuleBuilder;
import net.jxta.module.IModuleBuilderListener.BuildEvents;
import net.jxta.module.IModuleDescriptor;

public class ModuleEvent<T extends Object> extends EventObject {
	private static final long serialVersionUID = 1L;

	private BuildEvents event;
	private IModuleDescriptor descriptor;
	
	private T module;

	public ModuleEvent( IModuleBuilder<T> builder, IModuleDescriptor descriptor) {
		super(builder);
		this.descriptor = descriptor;
		this.event = BuildEvents.INIT;
	}

	public ModuleEvent( IModuleBuilder<T> builder, IModuleDescriptor descriptor, T module ) {
		super(builder );
		this.descriptor = descriptor;
		this.module = module;
	}

	public BuildEvents getEvent() {
		return event;
	}

	public IModuleDescriptor getDescriptor() {
		return descriptor;
	}

	public T getModule() {
		return module;
	}
}
