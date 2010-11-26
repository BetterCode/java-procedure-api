package br.com.ctbc.api.hibernate.procedure;

import java.util.Collection;

/**
 * 
 * @author andre
 * 
 */
public enum Validator {

	GET;

	public Validator notNull(Object... objects) {
		for (Object o : objects) {
			if (o == null) {
				throw new IllegalArgumentException("Objeto não pode ser nulo.");
			}
		}
		return this;
	}

	public Validator notEmpty(Collection<?> c) {
		notNull(c);
		if (c.isEmpty()) {
			throw new IllegalArgumentException("Lista não pode ser vazia.");
		}
		return this;
	}

	public Validator notEmpty(String... strings) {
		for (String s : strings) {
			if (s == null || s.trim().isEmpty()) {
				throw new IllegalArgumentException("String não pode ser vazia.");
			}
		}
		return this;
	}

	public Validator greaterOrEqualsZero(Number... numbers) {
		for (Number n : numbers) {
			if (n == null || n.doubleValue() < 0) {
				throw new IllegalArgumentException(String.format("Number [%s] cannot be null or less than zero",n));
			}
		}
		return this;
	}
	
	public Validator greaterThanZero(Number... numbers) {
		for (Number n : numbers) {
			if (n == null || n.doubleValue() <= 0) {
				throw new IllegalArgumentException(String.format("Number [%s] cannot be null, less or equal than zero",n));
			}
		}
		return this;
	}
	
	public Validator matches(String regex, String... strings){
		for (String string : strings) {
			if(string == null || !string.matches(regex)){
				throw new IllegalArgumentException("Não foi possível validar a string '"+string+"' com a expressão '"+regex+"'");
			}
		}
		return this;
	}
	
	
}
