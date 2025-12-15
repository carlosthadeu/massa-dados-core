package com.thadeu.massa;

import br.gov.bnb.helper.EntityManagerHelper;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;

abstract class GenericDaoPu<T extends Comparable<T> & Serializable, I> {
	
	private EntityManager entityManager;
	private String persistenceUnit;
	private String namedQuery;
	private Map<String, Object> parametros;
	private Class<T> persistedClass;
	
	protected GenericDaoPu(String persistenceUnit, Class<T> persistedClass) {
		this.persistenceUnit = persistenceUnit;
		this.persistedClass = persistedClass;
	}
	
	public Class<T> getPersistedClass() {
		return this.persistedClass;
	}
	
	protected EntityManager getEntityManager() {
		EntityManagerFactory emf = EntityManagerHelper.getEntityManagerFactory(this.persistenceUnit);
		if (this.entityManager == null) {
			this.entityManager = emf.createEntityManager();
		}

		if (!this.entityManager.isOpen()) {
			this.entityManager = emf.createEntityManager();
		}
		
		return this.entityManager;
	}
	
	protected Field getIdField() {
		Field[] campos = this.persistedClass.getDeclaredFields();
		for (Field campo : campos) {
			if (campo.isAnnotationPresent(Id.class))
				return campo;
		}
		return null;
	}
	
	protected Object getIdValue(T entity) {
		try {
			Field idField = getIdField();
			if (idField != null) {
				idField.setAccessible(true);
				return idField.get(entity);
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected String getPersistenceUnit() {
		return persistenceUnit;
	}

	protected String getNamedQuery() {
		return namedQuery;
	}
	
	protected void setNamedQuery(String namedQuery) {
		this.namedQuery = namedQuery;
	}

	protected Map<String, Object> getParametros() {
		return parametros;
	}

	protected void setParametros(Map<String, Object> parametros) {
		this.parametros = parametros;
	}
	

}
