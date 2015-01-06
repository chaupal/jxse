package net.jxta.impl.modulemanager;

import java.util.EventObject;

import net.jxta.module.IModuleBuilder;
import net.jxta.module.IModuleBuilderListener.BuildEvents;

public class ModuleEvent<T extends Object> extends EventObject {
	private static final long serialVersionUID = 1L;

	private T module;
	private BuildEvents event;

	public ModuleEvent( IModuleBuilder<T> builder, BuildEvents event ) {
		super(builder);
		this.event = event;
	}

	public ModuleEvent( IModuleBuilder<T> builder, T module ) {
		super(builder);
		this.event = BuildEvents.BUILT;
		this.module = module;
	}

	protected BuildEvents getBuildEvent() {
		return event;
	}

	public T getModule() {
		return module;
	}
}
