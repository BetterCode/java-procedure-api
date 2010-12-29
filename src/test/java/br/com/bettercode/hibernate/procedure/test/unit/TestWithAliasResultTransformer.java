package br.com.bettercode.hibernate.procedure.test.unit;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.jdbc.core.RowMapper;

import br.com.bettercode.resulttransformer.MappedResultTransformer;

import com.mockrunner.mock.jdbc.MockResultSet;

/**
 * @author Luiz Fernando Signorelli
 * @since 07/05/2010
 */
@SuppressWarnings("unchecked")
public final class TestWithAliasResultTransformer {

	public static final String FIELD2_NAME = "fogao";
	private static final String FIELD2_VALUE = "1";
	public static final String FIELD1_NAME = "galo";
	private static final String FIELD1_VALUE = "teste";

	
	@Test
	public void shouldMapFieldWithColumnAnnotation() throws Exception {
		final Object[] data = { FIELD1_VALUE, FIELD2_VALUE };
		final String[] columns = { FIELD1_NAME, FIELD2_NAME };
		final MappedResultTransformer transformer = new MappedResultTransformer(Object1.class);
		final Object1 t = (Object1) transformer.transformTuple(data, columns);
		
		assertEquals(FIELD1_VALUE, t.getF1());
		assertNull(t.getF2());
	}
	
	@Test
	public void shouldMapFieldWithColumnsAnnotation() throws Exception {
		final Object[] data = { FIELD1_VALUE, FIELD2_VALUE };
		final String[] columns = { FIELD1_NAME, FIELD2_NAME };
		final MappedResultTransformer transformer = new MappedResultTransformer(Object2.class);
		final Object2 t = (Object2) transformer.transformTuple(data, columns);
		
		assertEquals(FIELD1_VALUE, t.getF1());
		assertNull(t.getF2());
	}
	
	
	@Test
	public void shouldNotMapWhenThereIsNoColumnMatches() throws Exception {
		final Object[] data = { FIELD1_VALUE, FIELD2_VALUE };
		final String[] columns = { "bla", "ble" };
		final MappedResultTransformer transformer = new MappedResultTransformer(Object2.class);
		final Object2 t = (Object2) transformer.transformTuple(data, columns);
		
		assertNull(t.getF1());
		assertNull(t.getF2());
	}
	
	@Test
	public void shouldImplementSpringRowMapper() throws Exception {
		final RowMapper<Object1> rowMapper = new MappedResultTransformer<Object1>(Object1.class);
		
		final MockResultSet rs = new MockResultSet("1");
		rs.addColumn("galo", new String[]{ FIELD1_VALUE });
		rs.addColumn("f2", new Integer[]{ 2 });
		rs.next();
		final Object1 t = (Object1) rowMapper.mapRow(rs, 1);
		
		assertEquals(FIELD1_VALUE, t.getF1());
		assertNull(t.getF2());	
		
		final RowMapper<Object2> rowMapper2 = new MappedResultTransformer<Object2>(Object2.class);
		final Object2 t2 = (Object2) rowMapper2.mapRow(rs, 1);
		
		assertEquals(FIELD1_VALUE, t2.getF1());
		assertNull(t2.getF2());
	}
}
