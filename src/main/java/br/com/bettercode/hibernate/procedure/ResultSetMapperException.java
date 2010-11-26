package br.com.bettercode.hibernate.procedure;


/**
 * Lançada quando ocorre erro nas operações de mapeamento do result set.
 * 
 * @author saulo.borges
 *
 */
public class ResultSetMapperException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ResultSetMapperException() {
		super();
	}

	public ResultSetMapperException(String msg) {
		super(msg);
	}

	public ResultSetMapperException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public ResultSetMapperException(Throwable cause) {
		super(cause);
	}
}