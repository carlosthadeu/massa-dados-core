package com.thadeu.massa;

import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;

public class InjectPageFactory {

	private InjectPageFactory() {

	}

	private static MutablePicoContainer picoContainer;

	static MutablePicoContainer getPicoContainer() {
		if (picoContainer == null) {
			picoContainer = new DefaultPicoContainer();
		}
		return picoContainer;
	}

	public static <T> T getInjectedClassPicoContainer(Class<T> classT) {
		MutablePicoContainer picoContainer = getPicoContainer();
		if (picoContainer.getComponentAdapters().stream().noneMatch(adapter -> adapter.getComponentKey().equals(classT))) {
			picoContainer.addComponent(classT);
		}
		return picoContainer.getComponent(classT);
	}
	
	
}
