package br.com.bettercode.resulttransformer;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Embedded;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.hibernate.transform.ResultTransformer;
import org.joda.time.DateTime;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import br.com.bettercode.procedure.Validator;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * 
 * @author Andre Goncalves
 * 
 */

public final class MappedResultTransformer<T> implements ResultTransformer, RowMapper<T> {

	private static final long serialVersionUID = 1L;

	private final Class<?> clazz;

	public MappedResultTransformer(Class<?> clazz) {
		super();
		Validator.GET.notNull(clazz);
		this.clazz = clazz;
		ConvertUtils.register(new JodaDateTimeConverter(), DateTime.class);
	}

	@SuppressWarnings("unchecked")
	public List<?> transformList(List list) {
		return list;
	}

	public Object transformTuple(Object[] data, String[] columnNames) {
		try {
			final Map<String, Field> allFileds = new HashMap<String, Field>();
			final Object bean = (this.clazz).newInstance();

			allFileds.putAll(fieldsAnnotatedWithColumn(this.clazz));
			allFileds.putAll(fieldsAnnotatedWithColumns(this.clazz));

			scanResult(data, columnNames, allFileds, bean);

			// transform complex objects
			final Map<String, Field> embeddedFields = fieldMapWithEmbedded(this.clazz);

			for (String key : embeddedFields.keySet()) {

				Field embedField = embeddedFields.get(key);

				final Map<String, Field> embedFields = new HashMap<String, Field>();
				
				final Object embedBean = embedField.getType().newInstance();

				embeddedFields.putAll(fieldsAnnotatedWithColumn(embedField.getType()));
				embeddedFields.putAll(fieldsAnnotatedWithColumns(embedField.getType()));

				scanResult(data, columnNames, embedFields, embedBean);

				// sets the embedded.
				embedField.setAccessible(true);
				embedField.set(bean, ConvertUtils.convert(embedBean, embedField.getType()));
			}

			return bean;
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public T mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		final ResultSetMetaData metaData = resultSet.getMetaData();
		int columnCount = metaData.getColumnCount();
		final Object[] data = new Object[columnCount+1];
		final String[] columnNames = new String[columnCount+1];
		
		for (int index = 1; index <= columnCount; index++) {
			data[index] = JdbcUtils.getResultSetValue(resultSet, index);
			columnNames[index] = JdbcUtils.lookupColumnName(metaData, index);
		}

		return (T)transformTuple(data, columnNames);
	}
	
	private void scanResult(Object[] data, String[] columnNames, final Map<String, Field> fields, final Object bean)
			throws IllegalAccessException {

		final Iterator<String> it = Arrays.asList(columnNames).iterator();

		for (Object obj : data) {
			final Field field = fields.get(it.next());
			if (field == null) {
				continue;
			}
			if (obj != null) {
				field.setAccessible(true);
				field.set(bean, ConvertUtils.convert(obj, field.getType()));
			}
		}
	}

	private Map<String, Field> fieldMapWithEmbedded(Class<?> type) {
		final Map<String, Field> fields = Maps.newHashMap();
		for (Field field : type.getDeclaredFields()) {
			if (field.isAnnotationPresent(Embedded.class)) {
				fields.put(field.getName(), field);
			}
		}
		return ImmutableMap.copyOf(fields);
	}

	private ImmutableMap<String, Field> fieldsAnnotatedWithColumn(Class<?> type) {
		final Map<String, Field> fields = Maps.newHashMap();
		for (Field field : type.getDeclaredFields()) {
			if (field.isAnnotationPresent(Column.class)) {
				fields.put(columnName(field), field);
			}
		}
		return ImmutableMap.copyOf(fields);
	}

	private ImmutableMap<String, Field> fieldsAnnotatedWithColumns(Class<?> type) {
		final Map<String, Field> fields = Maps.newHashMap();
		for (Field field : type.getDeclaredFields()) {
			if (field.isAnnotationPresent(Columns.class)) {
				for (String alias : field.getAnnotation(Columns.class).names()) {
					fields.put(alias, field);
				}
			}
		}
		return ImmutableMap.copyOf(fields);
	}

	private String columnName(Field field) {
		final String name = field.getAnnotation(Column.class).name();
		if (name != null) {
			return name;
		}
		return field.getName();
	}
	
	private static final class JodaDateTimeConverter implements Converter {

		@SuppressWarnings("unchecked")
		public Object convert(Class type, Object obj) {
			try {
				if (obj == null) {
					throw new IllegalArgumentException("The object passed to convertion is null.");
				}
				return DateType.fromType(obj.getClass()).convert(obj);
			} catch (IllegalArgumentException e) {
				throw new UnsupportedOperationException(String.format("Could not convert %s to org.joda.time.DateTime",
						obj), e);
			}
		}
	}

	private static enum DateType {
		LONG {
			@Override
			public Class<?> type() {
				return Long.class;
			}

			@Override
			public DateTime convert(Object obj) {
				long date = ((Long) obj).longValue();
				return new DateTime(date);
			}
		},
		TIMESTAMP {
			@Override
			public Class<?> type() {
				return Timestamp.class;
			}

			@Override
			public DateTime convert(Object obj) {
				long date = ((Timestamp) obj).getTime();
				return new DateTime(date);
			}
		},
		DATE {
			@Override
			public Class<?> type() {
				return Date.class;
			}

			@Override
			public DateTime convert(Object obj) {
				long date = ((Date) obj).getTime();
				return new DateTime(date);
			}
		},
		SQL_DATE {
			@Override
			public Class<?> type() {
				return java.sql.Date.class;
			}

			@Override
			public DateTime convert(Object obj) {
				long date = ((java.sql.Date) obj).getTime();
				return new DateTime(date);
			}
		};

		static DateType fromType(Class<?> type) {
			for (DateType t : DateType.values()) {
				if (t.type().equals(type)) {
					return t;
				}
			}
			throw new IllegalArgumentException();
		}

		abstract Class<?> type();

		abstract DateTime convert(Object obj);

	}

	

}
