package net.jxta.module;

import net.jxta.peergroup.core.Module;

public interface IJxtaModuleManager<T extends Module> extends
		IModuleManager<T, IJxtaModuleFactory<T>> {

}
