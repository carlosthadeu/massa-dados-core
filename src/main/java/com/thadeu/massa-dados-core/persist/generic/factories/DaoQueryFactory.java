package com.thadeu.massa;

import br.gov.bnb.persist.generic.GenericDaoNamedQuery;
import br.gov.bnb.persist.generic.GenericDaoQueryParametros;
import br.gov.bnb.persist.generic.GenericDaoUnmodifiableList;
import br.gov.bnb.persist.interfaces.DaoQuery;

import java.io.Serializable;
import java.security.InvalidParameterException;

public class DaoQueryFactory {
	public static <T extends Comparable<T> & Serializable, I> DaoQuery<T, I> createDaoQuery(ParametrosDao parametrosDao,
			boolean isUnmodifiable) {
		DaoQuery<T, I> daoQuery = null;
		if (parametrosDao.getNamedQuery() != null) {
			if (parametrosDao.getParametro() == null) {
				if (isUnmodifiable) {
					daoQuery = new GenericDaoUnmodifiableList<T, I>(parametrosDao.getPersistenceUnit(),
							parametrosDao.getPersistedClass(), parametrosDao.getNamedQuery());
				} else {
					daoQuery = new GenericDaoNamedQuery<T, I>(parametrosDao.getPersistenceUnit(),
							parametrosDao.getPersistedClass(), parametrosDao.getNamedQuery());
				}

			} else {
				if (isUnmodifiable) {
					daoQuery = new GenericDaoUnmodifiableList<T, I>(parametrosDao.getPersistenceUnit(),
							parametrosDao.getPersistedClass(), parametrosDao.getNamedQuery(),
							parametrosDao.getParametro());
				} else {
					daoQuery = new GenericDaoNamedQuery<T, I>(parametrosDao.getPersistenceUnit(),
							parametrosDao.getPersistedClass(), parametrosDao.getNamedQuery(),
							parametrosDao.getParametro());
				}
			}
		} else {
			if (isUnmodifiable) {
				if (parametrosDao.getParametro() == null) {
					daoQuery = new GenericDaoUnmodifiableList<T, I>(parametrosDao.getPersistenceUnit(),
							parametrosDao.getPersistedClass());
				} else {
					daoQuery = new GenericDaoUnmodifiableList<T, I>(parametrosDao.getPersistenceUnit(),
							parametrosDao.getPersistedClass(), parametrosDao.getParametro());
				}

			} else {
				if (parametrosDao.getParametro() != null) {
					daoQuery = new GenericDaoQueryParametros<T, I>(parametrosDao.getPersistenceUnit(),
							parametrosDao.getPersistedClass(), parametrosDao.getParametro());
				} else {
					throw new InvalidParameterException(
							"Não é possível criar DaoQuery sem parâmetros ou Named Query. Para listagens simples veja DaoList");
				}
			}
		}

		return daoQuery;
	}
}
