package com.thadeu.massa;

import br.gov.bnb.persist.generic.GenericDaoList;
import br.gov.bnb.persist.generic.GenericDaoUnmodifiableList;
import br.gov.bnb.persist.interfaces.DaoList;

import java.io.Serializable;

public class DaoListFactory {
	public static <T extends Comparable<T> & Serializable, I> DaoList<T, I> createDaoList(ParametrosDao<T, I> parametrosDao,
			boolean isUnmodifiable) {
		DaoList<T, I> daoList = null;
		if (parametrosDao.getNamedQuery() != null) {
			if (parametrosDao.getParametro() == null) {
				if (isUnmodifiable) {
					daoList = new GenericDaoUnmodifiableList<T, I>(parametrosDao.getPersistenceUnit(),
							parametrosDao.getPersistedClass(), parametrosDao.getNamedQuery());
				} else {
					daoList = new GenericDaoList<T, I>(parametrosDao.getPersistenceUnit(),
							parametrosDao.getPersistedClass(), parametrosDao.getNamedQuery());
				}
			} else {
				if (isUnmodifiable) {
					daoList = new GenericDaoUnmodifiableList<T, I>(parametrosDao.getPersistenceUnit(),
							parametrosDao.getPersistedClass(), parametrosDao.getNamedQuery(), parametrosDao.getParametro());
				} else {
				daoList = new GenericDaoList<T, I>(parametrosDao.getPersistenceUnit(),
						parametrosDao.getPersistedClass(), parametrosDao.getNamedQuery(), parametrosDao.getParametro());
				}
			}
		} else {
			if (parametrosDao.getParametro() == null) {
				if (isUnmodifiable) {
					daoList = new GenericDaoUnmodifiableList<T, I>(parametrosDao.getPersistenceUnit(),
							parametrosDao.getPersistedClass());
				} else {
					daoList = new GenericDaoList<T, I>(parametrosDao.getPersistenceUnit(),
							parametrosDao.getPersistedClass());
				}
			} else {
				if (isUnmodifiable) {
					daoList = new GenericDaoUnmodifiableList<T, I>(parametrosDao.getPersistenceUnit(),
							parametrosDao.getPersistedClass(), parametrosDao.getParametro());
				} else {
				daoList = new GenericDaoList<T, I>(parametrosDao.getPersistenceUnit(),
						parametrosDao.getPersistedClass(), parametrosDao.getNamedQuery(), parametrosDao.getParametro());
				}
				
			}
			

		}
		return daoList;
	}
	
	
}
