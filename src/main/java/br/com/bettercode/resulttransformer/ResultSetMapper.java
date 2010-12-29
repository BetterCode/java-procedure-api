package br.com.bettercode.resulttransformer;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.log4j.Logger;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Agrupa métodos para realização do mapeamento de result set.
 * 
 * @author saulo.borges
 * 
 */
public class ResultSetMapper {

	private static Logger log = Logger.getLogger(ResultSetMapper.class);

	/**
	 * Mapeia o result set e retorna lista de objetos.
	 * 
	 * @param rs
	 *            - result set
	 * @param clazz
	 *            - classe a ser mapeada
	 * @return
	 */
	public static <T> List<T> mapResultSet(ResultSet rs, Class<T> clazz) {

		// Validação
		if (rs == null || clazz == null) {
			return null;
		}

		List<T> retList = new ArrayList<T>();

		try {
			// Recupera o nome das colunas do result set
			String[] columnNames = retrieveColumnNames(rs);

			while (rs.next()) {

				// Mapeia objeto e adiciona-o na lista
				retList.add(retrieveObjectFromRow(rs, columnNames, clazz));

			}
		} catch (SQLException e) {
			logAndThrow("Error while mapping result set." + e.getMessage(), e);
		}

		return retList;
	}

	// /////////////////////////////////////////////////////////////////////////
	// MÉTODOS PRIVADOS
	// /////////////////////////////////////////////////////////////////////////

	/**
	 * Obtém nome das colunas a partir do result set.
	 * 
	 * @param rs
	 *            - result set
	 * @return
	 * @throws SQLException
	 */
	private static String[] retrieveColumnNames(ResultSet rs) throws SQLException {

		// Validação
		if (rs == null) {
			return null;
		}

		// Obtém meta dados
		ResultSetMetaData metaData = rs.getMetaData();

		String[] columnNames = new String[metaData.getColumnCount()];

		for (int i = 1; i <= metaData.getColumnCount(); i++) {

			// Recupera nome da coluna SQL
			columnNames[i - 1] = metaData.getColumnName(i);
		}

		return columnNames;

	}

	/**
	 * Recupera objeto a partir da linha do result set
	 * 
	 * @param rs
	 * @param columnNames
	 * @param clazz
	 * @return
	 */
	private static <T> T retrieveObjectFromRow(final ResultSet rs, final String[] columnNames, final Class<T> clazz) {
		final Map<Field, String[]> fields = mapFields(clazz);
		T bean = null;

		try {
			bean = clazz.newInstance();

			for (String columnName : columnNames) {
				// Busca propriedade mapeada para o nome da coluna corrente
				final Field field = searchFieldByColumnName(fields, columnName);

				if (field == null) {

					continue;
				}

				field.setAccessible(true);

				if(rs.getObject(columnName) != null){
					
					// Atribui o valor da coluna SQL à propriedade da classe
					field.set(bean, ConvertUtils.convert(rs.getObject(columnName), field.getType()));
				}
			}

		} catch (Exception e) {
			logAndThrow("Error while doing reflection operations." + e.getMessage(), e);
		}

		return bean;
	}

	/**
	 * Retorna a propriedade (field) que contém o nome da coluna SQL.
	 * 
	 * @param fields
	 *            - mapa de (campo, lista de nomes de colunas SQL)
	 * @param columnName
	 *            - nome da coluna
	 * @return
	 */
	private static Field searchFieldByColumnName(Map<Field, String[]> fields, String columnName) {

		// Validação
		if (fields == null || columnName == null) {
			return null;
		}

		for (Entry<Field, String[]> entry : fields.entrySet()) {

			if (Arrays.binarySearch(entry.getValue(), columnName) >= 0) {
				// Campo possui a coluna buscada

				return entry.getKey();
			}
		}

		return null;
	}

	/**
	 * Mapeiam propriedades e retorna mapa (propriedade, nomes de colunas SQL
	 * associadas)
	 * 
	 * @param type
	 * @return
	 */
	private static ImmutableMap<Field, String[]> mapFields(Class<?> type) {
		final Map<Field, String[]> fields = Maps.newHashMap();

		for (Field field : type.getDeclaredFields()) {

			if (field.isAnnotationPresent(Columns.class)) {
				// Anotação presente

				// Recupera os nomes de colunas SQL's associadas à propridade
				fields.put(field, retrieveColumnNames(field));
			}
		}
		return ImmutableMap.copyOf(fields);
	}

	/**
	 * Obtém nomes da coluna disponíveis para a propriedade.
	 * 
	 * @param field
	 * @return
	 */
	private static String[] retrieveColumnNames(Field field) {

		final String[] names = field.getAnnotation(Columns.class).names();

		if (names != null) {
			return names;
		}

		return new String[] { field.getName() };
	}

	/**
	 * Registra log e lança exceção.
	 * 
	 * @param errorMsg
	 * @param e
	 */
	private static void logAndThrow(String errorMsg, Exception e) {

		// Registra mensagem no log.
		log.error(errorMsg, e);

		// Lança exceção
		throw new ResultSetMapperException(errorMsg, e);
	}
}
