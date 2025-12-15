package com.thadeu.massa;

import io.cucumber.core.backend.ObjectFactory;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoBuilder;
import org.picocontainer.annotations.Inject;
import org.picocontainer.behaviors.Cached;
import org.picocontainer.lifecycle.DefaultLifecycleState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class CucumberPicoFactory implements ObjectFactory {
    
	private static final Set<Class<?>> classes = new HashSet<>();
	private static MutablePicoContainer pico;

	private static boolean isInstantiable(Class<?> clazz) {
		boolean isNonStaticInnerClass = !Modifier.isStatic(clazz.getModifiers()) && clazz.getEnclosingClass() != null;
		return Modifier.isPublic(clazz.getModifiers()) && !Modifier.isAbstract(clazz.getModifiers())
				&& !isNonStaticInnerClass;
	}

	@Override
	public void start() {
		if (pico == null) {
			pico = new PicoBuilder().withCaching().build();
			for (Class<?> clazz : classes) {
				pico.addComponent(clazz);
			}
		} else {
			pico.setLifecycleState(new DefaultLifecycleState());
			pico.getComponentAdapters().forEach(cached -> ((Cached<?>) cached).flush());
		}
		pico.start();
	}

	@Override
	public void stop() {
		pico.stop();
		pico.dispose();
		pico = null; // Reset pico to allow for restart
	}

	@Override
	public boolean addClass(Class<?> clazz) {
		if (isInstantiable(clazz) && classes.add(clazz)) {
			addConstructorDependencies(clazz);
			addFieldDependencies(clazz);
		}
		return true;
	}

	@Override
	public <T> T getInstance(Class<T> type) {
		return pico.getComponent(type);
	}

	private void addConstructorDependencies(Class<?> clazz) {
		for (Constructor<?> constructor : clazz.getConstructors()) {
			for (Class<?> paramClazz : constructor.getParameterTypes()) {
				addClass(paramClazz);
			}
		}
	}

	public void addFieldDependencies(Class<?> clazz) {
		for (Field field : clazz.getDeclaredFields()) {
			if (field.isAnnotationPresent(Inject.class)) {
				Class<?> fieldType = field.getType();
				addClass(fieldType);
			}
		}
	}

	public static void registerClass(Class<?> clazz) {
		if (pico != null) {
			pico.addComponent(clazz);
		} else {
			// If PicoContainer is not initialized, add to classes set to be registered
			// later
			CucumberPicoFactory factory = new CucumberPicoFactory();
			factory.addClass(clazz);
		}
	}
}
