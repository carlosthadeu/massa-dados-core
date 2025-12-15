package com.thadeu.massa;

import br.gov.bnb.helper.EntityManagerHelper;
import br.gov.bnb.persist.interfaces.DaoUnique;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class GenericDaoUnico<T extends Comparable<T> & Serializable, I> implements DaoUnique<T, I> {
	private EntityManager entityManager;
	private String persistenceUnit;
	private Class<T> persistedClass;

	public GenericDaoUnico(String persistenceUnit, Class<T> persistedClass) {
		this.persistedClass = persistedClass;
		this.persistenceUnit = persistenceUnit;
	}

	private EntityManager getEntityManager() {
		EntityManagerFactory emf = EntityManagerHelper.getEntityManagerFactory(this.persistenceUnit);
		if (this.entityManager == null) {
			this.entityManager = emf.createEntityManager();
		}

		if (!this.entityManager.isOpen()) {
			this.entityManager = emf.createEntityManager();
		}

		return this.entityManager;
	}

	private Class<T> getPersistedClass() {
		return persistedClass;
	}

	public T encontrarUnico(Map<String, Object> parametro) {
		getEntityManager().clear();
		StringBuilder queryString = new StringBuilder("SELECT e FROM ");
		queryString.append(this.getPersistedClass().getSimpleName()).append(" e WHERE ");
		int i = 0;
		for (Map.Entry<String, Object> proPar : parametro.entrySet()) {
			if ((proPar.getValue() instanceof String) && (!String.valueOf(proPar.getValue()).isEmpty()))
				queryString.append("e.").append(proPar.getKey()).append(" LIKE  :").append("Par").append(i)
						.append(" AND ");
			else
				queryString.append("e.").append(proPar.getKey()).append(" = :").append("Par").append(i).append(" AND ");
			i++;
		}
		queryString.delete(queryString.length() - 5, queryString.length()); // Remove trailing "AND"

		TypedQuery<T> query = getEntityManager().createQuery(queryString.toString(), getPersistedClass());

		i = 0;

		for (Map.Entry<String, Object> proPar : parametro.entrySet()) {
			if ((proPar.getValue() instanceof String) && (!String.valueOf(proPar.getValue()).isEmpty()))
				query.setParameter("Par" + i, '%' + String.valueOf(proPar.getValue()) + '%');
			else
				query.setParameter("Par" + i, proPar.getValue());
			i++;
		}
		List<T> results = query.getResultList();
		if (results.isEmpty()) {
			return null;
		} else {
			return results.get(0);
		}
	}

	@Override
	public T encontrarUnico(Map<String, Object> parametro, String namedQuery) {
		getEntityManager().clear();
		TypedQuery<T> query = getEntityManager().createNamedQuery(namedQuery, getPersistedClass());
		for (Map.Entry<String, Object> proPar : parametro.entrySet()) {
			if ((proPar.getValue() instanceof String) && (!String.valueOf(proPar.getValue()).isEmpty()))
				query.setParameter(proPar.getKey(), '%' + String.valueOf(proPar.getValue()) + '%');
			else
				query.setParameter(proPar.getKey(), proPar.getValue());
		}
		List<T> results = query.getResultList();
		if (results.isEmpty()) {
			return null;
		} else {
			return results.get(0);
		}
	}

	@Override
	public T encontrarUnico(I id) {
		getEntityManager().clear();
		return getEntityManager().find(getPersistedClass(), id);
	}

	@Override
	public T encontrarUnico(String namedQuery) {
		getEntityManager().clear();
		TypedQuery<T> query = getEntityManager().createNamedQuery(namedQuery, getPersistedClass());
		return query.getSingleResult();
	}

}
