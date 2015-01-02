package net.jxta.impl.modulemanager;

import java.util.EventObject;

import net.jxta.module.IModuleBuilder;

public class ModuleEvent<T extends Object> extends EventObject {
	private static final long serialVersionUID = 1L;

	private T module;
	
	public ModuleEvent( IModuleBuilder<T> builder, T module ) {
		super(builder);
		this.module = module;
	}

	public T getModule() {
		return module;
	}
}
