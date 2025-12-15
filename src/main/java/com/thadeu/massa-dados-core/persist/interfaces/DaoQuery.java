package com.thadeu.massa;

import java.io.Serializable;
import java.util.List;

public interface DaoQuery<T extends Comparable<T> & Serializable, I> {

	List<T> encontrar();

	Long quantidadePesquisados();

}
