package com.thadeu.massa;

import br.gov.bnb.helper.EntityManagerHelper;
import br.gov.bnb.persist.interfaces.DaoList;
import br.gov.bnb.persist.interfaces.DaoQuery;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenericDaoUnmodifiableList<T extends Comparable<T> & Serializable, I>
		implements DaoQuery<T, I>, DaoList<T, I> {
	
	private EntityManager entityManager;
	private String persistenceUnit;
	private String namedQuery;
	private Map<String, Object> parametros;
	private List<T> listT = new ArrayList<>();
	private Class<T> persistedClass;

	public GenericDaoUnmodifiableList(String persistenceUnit, Class<T> persistedClass) {
		this.persistenceUnit = persistenceUnit;
		this.persistedClass = persistedClass;
	}
	
	public GenericDaoUnmodifiableList(String persistenceUnit, Class<T> persistedClass, String namedQuery){
		this(persistenceUnit, persistedClass);
		this.namedQuery = namedQuery;
	}
	public GenericDaoUnmodifiableList(String persistenceUnit, Class<T> persistedClass, String namedQuery, Map<String, Object> parametros){
		this(persistenceUnit, persistedClass, namedQuery);
		this.parametros = parametros;
	}
	public GenericDaoUnmodifiableList(String persistenceUnit, Class<T> persistedClass,  Map<String, Object> parametros){
		this(persistenceUnit, persistedClass);
		this.parametros = parametros;
	}
	
	private List<T> getListT() {
		if (this.listT.isEmpty()) {
			cargaInicial();
		}
		return this.listT;
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

	@Override
	public List<T> listar() {
		return getListT();
	}
	
	private void cargaInicial() {
		if (this.namedQuery != null) {
			cargaInicialNamedQuery();
		} else {
			if (this.parametros== null) {
				cargaInicialSomenteLista();
			} else {
				cargaInicialParametros();
			}
		}
		
	}
	
	private void cargaInicialSomenteLista() {
		CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
		CriteriaQuery<T> query = builder.createQuery(this.persistedClass);
		query.from(this.persistedClass);
		this.listT = getEntityManager().createQuery(query).getResultList();
	}
	
	private void cargaInicialParametros() {
		getEntityManager().clear();
		StringBuilder queryString = new StringBuilder("SELECT e FROM ");
		queryString.append(this.persistedClass.getSimpleName()).append(" e WHERE ");
		int i = 0;
		for (Map.Entry<String, Object> proPar : this.parametros.entrySet()) {
			if ((proPar.getValue() instanceof String) && (!String.valueOf(proPar.getValue()).isEmpty()))
				queryString.append("e.").append(proPar.getKey()).append(" LIKE  :").append("Par").append(i)
						.append(" AND ");
			else
				queryString.append("e.").append(proPar.getKey()).append(" = :").append("Par").append(i).append(" AND ");
			i++;
		}
		queryString.delete(queryString.length() - 5, queryString.length()); // Remove trailing "AND"

		TypedQuery<T> query = getEntityManager().createQuery(queryString.toString(), this.persistedClass);

		i = 0;

		for (Map.Entry<String, Object> proPar : this.parametros.entrySet()) {
			if ((proPar.getValue() instanceof String) && (!String.valueOf(proPar.getValue()).isEmpty()))
				query.setParameter("Par" + i, '%' + String.valueOf(proPar.getValue()) + '%');
			else
				query.setParameter("Par" + i, proPar.getValue());
			i++;
		}

		this.listT = query.getResultList();
	}
	
	private void cargaInicialNamedQuery() {
		getEntityManager().clear();
		if (this.parametros == null) {
			TypedQuery<T> query = getEntityManager().createNamedQuery(this.namedQuery, this.persistedClass);
			for (Map.Entry<String, Object> proPar : this.parametros.entrySet()) {
				if ((proPar.getValue() instanceof String) && (!String.valueOf(proPar.getValue()).isEmpty()))
					query.setParameter(proPar.getKey(), '%' + String.valueOf(proPar.getValue()) + '%');
				else
					query.setParameter(proPar.getKey(), proPar.getValue());
			}
			this.listT = query.getResultList();
		} else {
			CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
			CriteriaQuery<T> query = builder.createQuery(this.persistedClass);
			query.from(this.persistedClass);
			this.listT = getEntityManager().createQuery(query).getResultList();
		}
	}



	@Override
	public List<T> encontrar() {
		return getListT();
	}
	
	
	public List<T> encontrar(Map<String, Object> parFiltro) {
		return filtarPorMapa(parFiltro).collect(Collectors.toList());
	}

	private Stream<T> filtarPorMapa(Map<String, Object> properties) {
		Stream<T> streamList = this.listT.stream();
		try {
			for (Map.Entry<String, Object> entry : properties.entrySet()) {
				String nomeCampo = entry.getKey();
				Field campo = this.persistedClass.getDeclaredField(nomeCampo);
				campo.setAccessible(true);
				streamList = streamList.filter(p -> {
					try {
						return campo.get(p).equals(entry.getValue());
					} catch (IllegalAccessException e) {
						e.printStackTrace();
						return false;
					}
				});
			}
			return streamList;
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Long quantidadePesquisados() {
		return quantidadeTotal();
	}
	
	public Long quantidadePesquisados(Map<String, Object> parFiltro) {
		return filtarPorMapa(parFiltro).count();
	}

	@Override
	public Long quantidadeTotal() {
		return Long.valueOf(this.listT.size());
	}


}
