package br.com.ctbc.api.hibernate.procedure;

public final class CannotInvokeProcedureException extends RuntimeException {

	private static final long serialVersionUID = 1L;


	public CannotInvokeProcedureException(String message, Throwable cause) {
		super(message, cause);
	}
	

}
