package br.com.ctbc.api.hibernate.procedure.test.unit;

import static org.junit.Assert.*;

import org.junit.Test;

import br.com.ctbc.api.hibernate.procedure.WithAliasResultTransformer;

/**
 * @author Luiz Fernando Signorelli
 * @since 07/05/2010
 */
public final class TestWithAliasResultTransformer {

	public static final String FIELD2_NAME = "fogao";
	public static final String FIELD1_NAME = "galo";
	private static final String FIELD2_VALUE = "1";
	private static final String FIELD1_VALUE = "2";

	
	@Test
	public void testColumnAnnotation() throws Exception {
		final Object[] data = { FIELD1_VALUE, FIELD2_VALUE };
		final String[] columns = { FIELD1_NAME, FIELD2_NAME };
		final WithAliasResultTransformer transformer = new WithAliasResultTransformer(TestTO.class);
		final TestTO t = (TestTO) transformer.transformTuple(data, columns);
		
		assertEquals(FIELD1_VALUE, t.getF1());
		assertNull(t.getF2());
	}
}
