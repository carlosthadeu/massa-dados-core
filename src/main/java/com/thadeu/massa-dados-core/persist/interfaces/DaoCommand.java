package com.thadeu.massa;

import java.io.Serializable;

public interface DaoCommand<T extends Comparable<T> & Serializable, I> {

	T salvar(T entity);

	T atualizar(T entity);

	void remover(I id);
}
