package br.com.bettercode.hibernate.procedure;

public final class UnsetedArgumentsException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UnsetedArgumentsException(String... args) {
		super(message(args));
	}

	public UnsetedArgumentsException(Throwable cause, String... args) {
		super(message(args), cause);
	}
	
	private static String message(String... args){
		StringBuilder sb = new StringBuilder("Args without value: ");
		for(String arg : args){
			sb.append(arg + " ");
		}
		return sb.toString();
	}

}
