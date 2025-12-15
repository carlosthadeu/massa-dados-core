package com.thadeu.massa;

import java.io.Serializable;
import java.util.List;

public interface DaoList<T extends Comparable<T> & Serializable, I> {
	
	List<T> listar();

	Long quantidadeTotal();
}
