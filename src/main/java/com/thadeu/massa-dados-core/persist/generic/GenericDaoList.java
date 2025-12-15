package com.thadeu.massa;

import br.gov.bnb.persist.interfaces.DaoList;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenericDaoList<T extends Comparable<T> & Serializable, I> extends GenericDaoPu<T, I>
		implements DaoList<T, I> {

	public GenericDaoList(String persistenceUnit, Class<T> persistedClass) {
		super(persistenceUnit, persistedClass);
	}

	public GenericDaoList(String persistenceUnit, Class<T> persistedClass, String namedQuery) {
		super(persistenceUnit, persistedClass);
		setNamedQuery(namedQuery);
	}

	public GenericDaoList(String persistenceUnit, Class<T> persistedClass, String namedQuery,
			Map<String, Object> parametros) {
		this(persistenceUnit, persistedClass, namedQuery);
		setParametros(parametros);
	}

	public GenericDaoList(String persistenceUnit, Class<T> persistedClass, Map<String, Object> parametros) {
		super(persistenceUnit, persistedClass);
		setParametros(parametros);
	}

	@Override
	public List<T> listar() {
		getEntityManager().clear();
		if (getNamedQuery() != null) {
			if (getParametros() == null) {
				getEntityManager().clear();
				TypedQuery<T> query = getEntityManager().createNamedQuery(getNamedQuery(), getPersistedClass());
				return query.getResultList();
			} else {
				getEntityManager().clear();
				TypedQuery<T> query = getEntityManager().createNamedQuery(getNamedQuery(), getPersistedClass());
				for (Map.Entry<String, Object> proPar : getParametros().entrySet()) {
					if ((proPar.getValue() instanceof String) && (!String.valueOf(proPar.getValue()).isEmpty()))
						query.setParameter(proPar.getKey(), '%' + String.valueOf(proPar.getValue()) + '%');
					else
						query.setParameter(proPar.getKey(), proPar.getValue());
				}
			}
		} else {
			if (getParametros() == null) {
				CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
				CriteriaQuery<T> query = builder.createQuery(getPersistedClass());
				query.from(getPersistedClass());
				return getEntityManager().createQuery(query).getResultList();
			} else {
				CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
				CriteriaQuery<T> query = builder.createQuery(getPersistedClass());
				query.from(getPersistedClass());
				return getEntityManager().createQuery(query).getResultList();
			}
		}
		return new ArrayList<T>();
	}

	@Override
	public Long quantidadeTotal() {
		return Long.valueOf(listar().size());
	}

}
