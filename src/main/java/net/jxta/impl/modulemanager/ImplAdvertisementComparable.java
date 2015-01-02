package net.jxta.impl.modulemanager;

import net.jxta.protocol.ModuleImplAdvertisement;

public class ImplAdvertisementComparable implements Comparable<ModuleImplAdvertisement> {

	private ModuleImplAdvertisement thisOne;
	
	public ImplAdvertisementComparable( ModuleImplAdvertisement thisOne) {
		this.thisOne = thisOne;
	}

	public int compareTo(ModuleImplAdvertisement o) {
		if( thisOne.equals( o ))
			return 0;
		int comp = thisOne.getModuleSpecID().toString().compareTo( o.getModuleSpecID().toString() );
		if( comp != 0 )
			return comp;
		return  thisOne.getCode().compareTo( o.getCode() );
	}	
}