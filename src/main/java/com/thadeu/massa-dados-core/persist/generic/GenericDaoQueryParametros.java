package com.thadeu.massa;

import br.gov.bnb.persist.interfaces.DaoQuery;

import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class GenericDaoQueryParametros<T extends Comparable<T> & Serializable, I> extends GenericDaoPu<T, I>
		implements DaoQuery<T, I>{

	public GenericDaoQueryParametros(String persistenceUnit, Class<T> persistedClass, Map<String, Object> parametros) {
		super(persistenceUnit, persistedClass);
		setParametros(parametros);
	}

	

	@Override
	public List<T> encontrar() {
		getEntityManager().clear();
		StringBuilder queryString = new StringBuilder("SELECT e FROM ");
		queryString.append(this.getPersistedClass().getSimpleName()).append(" e WHERE ");
		int i = 0;
		for (Map.Entry<String, Object> proPar : getParametros().entrySet()) {
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

		for (Map.Entry<String, Object> proPar : getParametros().entrySet()) {
			if ((proPar.getValue() instanceof String) && (!String.valueOf(proPar.getValue()).isEmpty()))
				query.setParameter("Par" + i, '%' + String.valueOf(proPar.getValue()) + '%');
			else
				query.setParameter("Par" + i, proPar.getValue());
			i++;
		}

		return query.getResultList();
	}

	@Override
	public Long quantidadePesquisados() {
		getEntityManager().clear();
		StringBuilder queryString = new StringBuilder("SELECT count(e) FROM ");
		queryString.append(this.getPersistedClass().getSimpleName()).append(" e WHERE ");
		int i = 0;
		for (Map.Entry<String, Object> proPar : getParametros().entrySet()) {
			if ((proPar.getValue() instanceof String) && (!String.valueOf(proPar.getValue()).isEmpty()))
				queryString.append("e.").append(proPar.getKey()).append(" LIKE  :").append("Par").append(i)
						.append(" AND ");
			else
				queryString.append("e.").append(proPar.getKey()).append(" = :").append("Par").append(i).append(" AND ");
			i++;
		}
		queryString.delete(queryString.length() - 5, queryString.length()); // Remove trailing "AND"

		TypedQuery<Long> query = getEntityManager().createQuery(queryString.toString(), Long.class);

		i = 0;

		for (Map.Entry<String, Object> proPar : getParametros().entrySet()) {
			if ((proPar.getValue() instanceof String) && (!String.valueOf(proPar.getValue()).isEmpty()))
				query.setParameter("Par" + i, '%' + String.valueOf(proPar.getValue()) + '%');
			else
				query.setParameter("Par" + i, proPar.getValue());
			i++;
		}

		return query.getSingleResult();
	}

	

}
