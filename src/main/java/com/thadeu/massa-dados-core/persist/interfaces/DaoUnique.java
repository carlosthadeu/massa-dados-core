package com.thadeu.massa;

import java.io.Serializable;
import java.util.Map;

public interface DaoUnique<T extends Comparable<T> & Serializable, I> {
	T encontrarUnico(Map<String, Object>parametro);
	T encontrarUnico(String namedQuery);
	T encontrarUnico(I id);
	T encontrarUnico(Map<String, Object> parametro, String namedQuery);
}
