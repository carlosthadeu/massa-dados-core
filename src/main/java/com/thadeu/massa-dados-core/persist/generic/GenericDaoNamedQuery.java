package com.thadeu.massa;

import br.gov.bnb.persist.interfaces.DaoQuery;

import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class GenericDaoNamedQuery<T extends Comparable<T> & Serializable, I> extends GenericDaoPu<T, I>
		implements DaoQuery<T, I> {

	public GenericDaoNamedQuery(String persistenceUnit, Class<T> persistedClass, String namedQuery) {
		super(persistenceUnit, persistedClass);
		setNamedQuery(namedQuery);
	}

	public GenericDaoNamedQuery(String persistenceUnit, Class<T> persistedClass, String namedQuery,
			Map<String, Object> parametros) {
		this(persistenceUnit, persistedClass, namedQuery);
		setParametros(parametros);
	}
	
	public GenericDaoNamedQuery(String persistenceUnit, Class<T> persistedClass, Map<String, Object> parametros) {
		super(persistenceUnit, persistedClass);
		setParametros(parametros);
	}

	@Override
	public List<T> encontrar() {
		getEntityManager().clear();
		TypedQuery<T> query = getEntityManager().createNamedQuery(getNamedQuery(), getPersistedClass());
		for (Map.Entry<String, Object> proPar : getParametros().entrySet()) {
			if ((proPar.getValue() instanceof String) && (!String.valueOf(proPar.getValue()).isEmpty()))
				query.setParameter(proPar.getKey(), '%' + String.valueOf(proPar.getValue()) + '%');
			else
				query.setParameter(proPar.getKey(), proPar.getValue());
		}
		return query.getResultList();
	}

	

	@Override
	public Long quantidadePesquisados() {
		return Long.valueOf(encontrar().size());
	}

	

}
