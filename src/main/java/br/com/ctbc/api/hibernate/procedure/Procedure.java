package br.com.ctbc.api.hibernate.procedure;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.impl.SessionImpl;
import org.hibernate.jdbc.Work;


import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public final class Procedure {

	Session session;
	String query;
	Map<String, Object> inputs;
	Map<String, SqlType> outputs;
	Map<String, Boolean> registeredParameters;
	Map<String, Integer> parametersOrder;
	Class<?> resultSetClazz;

	/**
	 * LOG
	 */
	Logger log = Logger.getLogger(getClass());

	private Procedure(Session session, String query, Map<String, Boolean> registeredParameters,
			Map<String, Integer> parametersOrder, Class<?> clazz) {
		this.session = session;
		this.query = query;
		this.inputs = Maps.newHashMap();
		this.outputs = Maps.newHashMap();
		this.registeredParameters = registeredParameters;
		this.parametersOrder = parametersOrder;
		this.resultSetClazz = clazz;
	}

	public static Procedure create(Session session, String procedure, Class<?> clazz) {
		Validator.GET.notNull(session, procedure, clazz).notEmpty(procedure);
		return new Procedure(session, query(procedure), registerParameters(procedure),
				registerParametersOrder(procedure), clazz);
	}

	public static Procedure create(Session session, String procedure) {
		Validator.GET.notNull(session, procedure).notEmpty(procedure);
		return new Procedure(session, query(procedure), registerParameters(procedure),
				registerParametersOrder(procedure), null);
	}

	/**
	 * Cria função.
	 * 
	 * @param session
	 *            - sessão do Hibernate
	 * @param function
	 *            - assinatura da função obedecendo o seguinte formato
	 *            :PARAM_RETORNO = NOMEDAFUNCAO(:PARAM1,:PARAM2,...)
	 * @return
	 */
	public static Procedure createFunction(Session session, String function) {
		Validator.GET.notNull(session, function).notEmpty(function);

		Pattern p = Pattern.compile("\\w+\\s+=\\s+");
		Matcher m = p.matcher(function);
		String funcWithoutReturnParam = null;

		if (m.find()) {
			// Obtém assinatura da função sem o parâmetro de retorno
			funcWithoutReturnParam = function.substring(m.end());
		} else {
			// Não condiz com o formato esperado
			throw new IllegalArgumentException(String.format("Parameter [%s] has illegal format.", function));
		}

		return new Procedure(session, queryFunction(funcWithoutReturnParam), registerParameters(function),
				registerParametersOrder(function), null);
	}

	private static String queryFunction(String function) {
		for (String param : registerParameters(function).keySet()) {
			function = function.replace(":" + param, "?");
		}
		return String.format("{ ? = call %s }", function);
	}

	private static String query(String procedure) {
		for (String param : registerParameters(procedure).keySet()) {
			procedure = procedure.replace(":" + param, "?");
		}
		return String.format("{ call %s }", procedure);
	}

	private static Map<String, Boolean> registerParameters(String query) {
		final Scanner scanner = new Scanner(query);
		final Map<String, Boolean> registeredParameters = Maps.newHashMap();

		String param = nextParameterName(scanner);
		while (param != null) {
			registeredParameters.put(param, false);
			param = nextParameterName(scanner);
		}

		return registeredParameters;
	}

	private static Map<String, Integer> registerParametersOrder(String query) {
		final Scanner scanner = new Scanner(query);
		final Map<String, Integer> parametersOrder = new LinkedHashMap<String, Integer>();
		int order = 1;

		String param = nextParameterName(scanner);
		while (param != null) {
			parametersOrder.put(param, order);
			param = nextParameterName(scanner);
			order++;
		}

		return parametersOrder;
	}

	private static String nextParameterName(Scanner scanner) {
		final String paramPattern = ":\\w+";
		final String param = scanner.findInLine(paramPattern);
		return param == null ? null : param.substring(1);
	}

	public Procedure input(String input, Object value) {
		Validator.GET.notNull(input);
		parameterExists(input);

		inputs.put(input, value);
		registeredParameters.put(input, true);
		return this;
	}

	public Procedure output(String output, SqlType type) {
		Validator.GET.notNull(output, type);
		parameterExists(output);

		outputs.put(output, type);
		registeredParameters.put(output, true);
		return this;
	}

	private void parameterExists(String parameter) {
		if (registeredParameters.get(parameter) == null) {
			throw new IllegalArgumentException(String.format("Parameter %s does not exist.", parameter));
		}
	}

	public ProcedureResult execute() {
		checkIfAllParametersHaveBeenSet();

		ProcedureWork work = new ProcedureWork();
		session.doWork(work);
		return work.getResult();
	}

	private void checkIfAllParametersHaveBeenSet() {
		final Set<String> unregisteredParams = Sets.newHashSet();
		for (Entry<String, Boolean> entry : registeredParameters.entrySet()) {
			if (!entry.getValue()) {
				unregisteredParams.add(entry.getKey());
			}
		}
		if (unregisteredParams.size() > 0) {
			throw new UnsetedArgumentsException(unregisteredParams.toArray(new String[unregisteredParams.size()]));
		}
	}

	private class ProcedureWork implements Work {

		final ProcedureResult result = new ProcedureResult();

		public ProcedureResult getResult() {
			return result;
		}

		@Override
		public void execute(Connection connection) throws SQLException {
			final CallableStatement call = connection.prepareCall(query);
			withParameters(call).execute();

			if (resultSetClazz != null) {
				result.setResultSet(ResultSetMapper.mapResultSet(call.getResultSet(), resultSetClazz));
			}

			for (final Entry<String, SqlType> entry : outputs.entrySet()) {
				result.register(entry.getKey(), call.getObject(parametersOrder.get(entry.getKey())));
			}
		}

		private CallableStatement withParameters(CallableStatement statement) throws SQLException {
			withInput(statement).withOutput(statement);
			return statement;
		}

		private ProcedureWork withInput(CallableStatement statement) throws SQLException {
			for (final Entry<String, Object> entry : inputs.entrySet()) {
				statement.setObject(parametersOrder.get(entry.getKey()), entry.getValue(), intType(entry.getValue()));
			}
			return this;
		}

		private int intType(Object parameter) {
			if (parameter == null) {
				return inferNullType();
			}
			return SqlType.fromType(parameter.getClass()).sqlType();
		}

		private int inferNullType() {
			try {
				if ((session instanceof SessionImpl)) {
					final String dialect = ((SessionImpl) session).getFactory().getDialect().toString();
					if (dialect.matches(".*Oracle.*")) {
						// Database compatibility: Oracle demands to use
						// SqlType.NULL for null parameters
						return Types.NULL;
					}
				}
				return Types.OTHER;
			} catch (Exception e) {
				log.error("Error while retrieving hibernate dialect. " + e.getMessage(), e);
				return Types.OTHER;
			}
		}

		private ProcedureWork withOutput(CallableStatement statement) throws SQLException {
			for (final Entry<String, SqlType> entry : outputs.entrySet()) {
				statement.registerOutParameter(parametersOrder.get(entry.getKey()), entry.getValue().sqlType());
			}
			return this;
		}
	}

	@Override
	public String toString() {
		return String.format("JdbcQuery:  %s\n%s", query, printParameters());
	}

	private String printParameters() {
		StringBuilder params = new StringBuilder();
		for (Entry<String, Integer> entry : parametersOrder.entrySet()) {

			String key = entry.getKey();

			params.append("Parameter ").append(entry.getValue()).append(": ").append(key);

			if (this.inputs.containsKey(key)) {
				// Appending the value passed to procedure
				params.append(String.format(" [%s]", this.inputs.get(key)));
			}

			params.append("\n");
		}
		return params.toString();
	}
}
