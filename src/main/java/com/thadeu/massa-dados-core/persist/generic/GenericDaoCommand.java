package com.thadeu.massa;

import br.gov.bnb.persist.interfaces.DaoCommand;

import javax.persistence.EntityTransaction;
import java.io.Serializable;

public class GenericDaoCommand<T extends Comparable<T> & Serializable, I> extends GenericDaoPu<T, I> implements DaoCommand<T, I> {
	
	public GenericDaoCommand(String persistenceUnit, Class<T> persistedClass) {
		super(persistenceUnit, persistedClass);
	}

	public T salvar(T entity) {
		EntityTransaction t = getEntityManager().getTransaction();
		t.begin();
		getEntityManager().persist(entity);
		getEntityManager().flush();
		t.commit();
		return entity;
	}

	public T atualizar(T entity) {
		EntityTransaction t = getEntityManager().getTransaction();
		t.begin();
		getEntityManager().merge(entity);
		getEntityManager().flush();
		t.commit();
		return entity;
	}

	public void remover(I id) {
		getEntityManager().clear();
		T entity = getEntityManager().find(getPersistedClass(), id);;
		EntityTransaction tx = getEntityManager().getTransaction();
		tx.begin();
		T mergedEntity = getEntityManager().merge(entity);
		getEntityManager().remove(mergedEntity);
		getEntityManager().flush();
		tx.commit();
	}
}
