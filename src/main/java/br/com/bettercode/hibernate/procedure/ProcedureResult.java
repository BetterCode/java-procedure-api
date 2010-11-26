package br.com.bettercode.hibernate.procedure;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

public final class ProcedureResult {
	
	private Map<String, Object>outputs = Maps.newHashMap();

	private List<?> rs = null;

	public void setResultSet(List<?> rs) {
		this.rs = rs;
	}

	public List<?> getResultSet() {
		return this.rs;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String output){
		return (T)outputs.get(output);
	}

	
	void register(String name, Object value){
		outputs.put(name, value);
	}


	/**
	 * Recupera lista de objetos a partir do result set
	 * 
	 * @param <T> - tipo da classe a ser retornada
	 * @param resultSetNameParam - parâmetro da procedure que corresponde ao result set
	 * @param clazz
	 * @return
	 */
	public <T> List<T> mapResultSet(String resultSetNameParam, Class<T> clazz) {
		
		// Validação
		if(resultSetNameParam == null || clazz == null){
			return null;
		}
		
		// Recupera resultSet
		ResultSet rs = get(resultSetNameParam);
		
		// Validação
		if(rs == null){
			return null;
		}
		
		return ResultSetMapper.mapResultSet(rs, clazz);
	}
	
}
