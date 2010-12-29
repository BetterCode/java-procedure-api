package br.com.bettercode.procedure;

import java.math.BigInteger;
import java.sql.Types;
import java.sql.Date;

import oracle.jdbc.driver.OracleTypes;

public enum SqlType {

	BIGINT(Types.BIGINT, BigInteger.class), LONG(Types.BIGINT, Long.class), BOOLEAN(Types.BOOLEAN, Boolean.class), DATE(
			Types.DATE, Date.class), DOUBLE(Types.DOUBLE, Double.class), FLOAT(Types.FLOAT, Float.class), INTEGER(
			Types.INTEGER, Integer.class), VARCHAR(Types.VARCHAR, String.class), ORACLE_CURSOR(OracleTypes.CURSOR, Object.class);

	private final Integer sqlType;
	private final Class<?> type;

	private SqlType(Integer sqlType, Class<?> type) {
		this.sqlType = sqlType;
		this.type = type;
	}

	final Class<?> type() {
		return type;
	}

	public final Integer sqlType() {
		return sqlType;
	}

	static SqlType fromType(Class<?> type) {
		if (type == null) {
			throw new IllegalArgumentException("null type is invalid");
		}
		for (SqlType t : SqlType.values()) {
			if (type.equals(t.type())) {
				return t;
			}
		}
		throw new IllegalArgumentException("type not supported");
	}

}
