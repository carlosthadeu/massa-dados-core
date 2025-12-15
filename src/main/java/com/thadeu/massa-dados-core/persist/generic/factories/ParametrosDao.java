package com.thadeu.massa;

import java.io.Serializable;
import java.util.Map;

public class ParametrosDao<T extends Comparable<T> & Serializable, I> {
	private String persistenceUnit;
	private String namedQuery;
	private Map<String, Object> parametro;
	private Class<T> persistedClass;

	public ParametrosDao(String persistenceUnit, Class<T> persistedClass, String namedQuery, Map<String, Object> parametro) {
		this.persistenceUnit = persistenceUnit;
		this.namedQuery = namedQuery;
		this.parametro = parametro;
		this.persistedClass = persistedClass;
	}

	public String getPersistenceUnit() {
		return persistenceUnit;
	}

	public String getNamedQuery() {
		return namedQuery;
	}

	public Map<String, Object> getParametro() {
		return parametro;
	}

	public Class<T> getPersistedClass() {
		return persistedClass;
	}
}
