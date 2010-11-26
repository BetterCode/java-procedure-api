package br.com.ctbc.api.hibernate.procedure;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Embedded;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.hibernate.transform.ResultTransformer;
import org.joda.time.DateTime;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * 
 * @author Andre Goncalves
 * 
 */

public final class WithAliasResultTransformer implements ResultTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Class<?> clazz;

	public WithAliasResultTransformer(Class<?> clazz) {
		super();
		Validator.GET.notNull(clazz);
		this.clazz = clazz;
		ConvertUtils.register(new JodaDateTimeConverter(), DateTime.class);
	}

	@SuppressWarnings("unchecked")
	public List<?> transformList(List list) {
		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.transform.ResultTransformer#transformTuple(java.lang.Object
	 * [], java.lang.String[])
	 */
	public Object transformTuple(Object[] data, String[] columnNames) {
		try {
			// transform the simple methods.
			final Map<String, Field> fields = fieldMap(this.clazz);
			final Object bean = (this.clazz).newInstance();

			if (!fields.isEmpty()) {
				scanResult(data, columnNames, fields, bean);
			}
			// search for the fields with the lots of aliases.
			final Map<String, Field> fieldsWithColumnAliases = fieldMapWithColumnAliases(this.clazz);

			if (!fieldsWithColumnAliases.isEmpty()) {
				scanResult(data, columnNames, fieldsWithColumnAliases, bean);
			}

			// transform the complex objects, like pojo's.
			final Map<String, Field> embeddedFields = fieldMapWithEmbedded(this.clazz);

			if (!embeddedFields.isEmpty()) {
				for (String key : embeddedFields.keySet()) {

					Field embedField = embeddedFields.get(key);

					final Map<String, Field> embedFields = fieldMap(embedField.getType());
					final Object embedBean = embedField.getType().newInstance();

					scanResult(data, columnNames, embedFields, embedBean);

					final Map<String, Field> embedFieldsWithColumnAliases = fieldMapWithColumnAliases(embedField
							.getType());

					scanResult(data, columnNames, embedFieldsWithColumnAliases, embedBean);

					// sets the embedded.
					embedField.setAccessible(true);
					embedField.set(bean, ConvertUtils.convert(embedBean, embedField.getType()));
				}
			}

			return bean;
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}

	}

	/**
	 * This method makes a full scan in the result for the
	 * <code>columnNames</code> related with the <code>fields</code>.
	 * 
	 * @param data
	 * @param columnNames
	 * @param fields
	 * @param bean
	 * @throws IllegalAccessException
	 */
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

	/**
	 * Returns all fields of class <code>type</code> marked with
	 * {@link Embedded} annotation meaning that should be a scan on this
	 * non-simple field type, finding for mapping between this other fields and
	 * the procedure result.
	 * 
	 * @param type
	 * @return
	 */
	private Map<String, Field> fieldMapWithEmbedded(Class<?> type) {
		final Map<String, Field> fields = Maps.newHashMap();
		for (Field field : type.getDeclaredFields()) {
			if (field.isAnnotationPresent(Embedded.class)) {
				fields.put(field.getName(), field);
			}
		}
		return ImmutableMap.copyOf(fields);
	}

	private ImmutableMap<String, Field> fieldMap(Class<?> type) {
		final Map<String, Field> fields = Maps.newHashMap();
		for (Field field : type.getDeclaredFields()) {
			if (field.isAnnotationPresent(Column.class)) {
				fields.put(columnName(field), field);
			}
		}
		return ImmutableMap.copyOf(fields);
	}

	/**
	 * Returns the {@link Map} with the fields annotated with
	 * {@link ColumnAliases}.
	 * 
	 * @param type
	 * @return
	 */
	private ImmutableMap<String, Field> fieldMapWithColumnAliases(Class<?> type) {
		final Map<String, Field> fields = Maps.newHashMap();
		for (Field field : type.getDeclaredFields()) {
			if (field.isAnnotationPresent(ColumnAliases.class)) {
				for (String alias : getColumnAliasesAnnotation(field)) {
					fields.put(alias, field);
				}
			}
		}
		return ImmutableMap.copyOf(fields);
	}

	/**
	 * Method that returns the value of the {@link ColumnAliases}.
	 * 
	 * @param field
	 * @return
	 */
	private String[] getColumnAliasesAnnotation(Field field) {
		final String[] columnsAliases = field.getAnnotation(ColumnAliases.class).nameAliases();
		if (columnsAliases != null && columnsAliases.length > 0) {
			return columnsAliases;
		}

		return new String[] { field.getName() };
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
