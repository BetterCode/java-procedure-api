package br.com.bettercode.hibernate.procedure.test.unit;

import static org.junit.Assert.*;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.junit.Test;

import br.com.bettercode.procedure.Procedure;
import br.com.bettercode.procedure.ProcedureResult;
import br.com.bettercode.procedure.SqlType;


public class TestProcedure {

	private static final String SP_TEST = "sp_test(:arg1,:arg2,:out1)";

	EntityManager sinEntityManager;

	private Session session() {
		return (Session) sinEntityManager.getDelegate();
	}

	@PersistenceContext
	public void setSinEntityManager(EntityManager sinEntityManager) {
		this.sinEntityManager = sinEntityManager;
	}

	@Test
	public void testValidateProcedureCreation() throws Exception {
		Procedure.create(session(), SP_TEST);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testValidateProcedureCreationWithNullSession() throws Exception {
		Procedure.create(null, SP_TEST);
	}

	@Test(expected=IllegalArgumentException.class)	public void testValidateProcedureCreationWithNullQuery() throws Exception {
		Procedure.create(session(), null);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testValidateProcedureCreationWithEmptyQuery() throws Exception {
		Procedure.create(session(), "");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testProcedureWithUnsettedArgs1() {
		Procedure.create(session(), SP_TEST).execute();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testProcedureWithUnsettedArgs2() {
		Procedure.create(session(), SP_TEST).input("arg1", 1).execute();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testProcedureWithUnsettedArgs3() {
		Procedure.create(session(), SP_TEST).input("arg1", 1).input("arg2", 2).execute();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testProcedureWithUnsettedArgs4() {
		Procedure.create(session(), SP_TEST).input("arg1", 1).output("out1", SqlType.INTEGER).execute();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testProcedureWithInvalidInputParameter() {
		Procedure.create(session(), SP_TEST).input("argInvalid", 1).execute();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testProcedureWithInvalidOutputParameter() {
		Procedure.create(session(), SP_TEST).output("outGalo", SqlType.INTEGER).execute();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testProcedureWithNullTypeOutputParameter() {
		Procedure.create(session(), SP_TEST).output("outGalo", null).execute();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testProcedureWithNullInputParameter() {
		Procedure.create(session(), SP_TEST).input("arg1", null).execute();
	}
	
	@Test
	public void testChangeNamedParametersToInterrogationSign() throws Exception {
		Procedure proc = Procedure.create(session(), String.format("sp_TesteOutput(:%s, :%s, :%s, :%s)", "integerI",
				"decimalI", "varcharI", "dateI"));
		assertTrue(proc.toString().contains(String.format("JdbcQuery:  %s","{ call sp_TesteOutput(?, ?, ?, ?) }")));
	}
	
	@Test
	public void testKeepParametersOrder() throws Exception {
		Procedure proc = Procedure.create(session(), String.format("sp_TesteOutput(:%s, :%s, :%s, :%s)", "integerI",
				"decimalI", "varcharI", "dateI"));
		assertTrue(proc.toString().contains(String.format("Parameter 1: %s","integerI")));
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void runProcedureWithOutputParameters() {
		
		final ProcedureResult result = Procedure.create(
				session(),
				String.format("sp_TesteOutput(:%s, :%s, :%s, :%s, :%s, :%s, :%s, :%s )","integerI","decimalI","varcharI","dateI",
						"integerO","decimalO","varcharO","dateO"))
						.input("integerI", 22).input("decimalI", 2.34d).input("varcharI", "Galo").input("dateI", new java.sql.Date(2001,1,1))
						.output("integerO", SqlType.INTEGER).output("decimalO", SqlType.DOUBLE)
						.output("varcharO", SqlType.VARCHAR).output("dateO", SqlType.DATE).execute();
		
		final Integer integer = result.<Integer>get("integerO");
		final Double decimal = result.<Double>get("decimalO");
		final String string = result.<String>get("varcharO");
		final Date date = result.<Date>get("dateO");

		assertNotNull(integer);
		assertNotNull(decimal);
		assertNotNull(string);
		assertNotNull(date);
	}
	
	@Test
	public void runProcedureWithNullParameters() {
		
		final ProcedureResult result = Procedure.create(
				session(),
				String.format("sp_TesteOutput(:%s, :%s, :%s, :%s, :%s, :%s, :%s, :%s )","integerI","decimalI","varcharI","dateI",
						"integerO","decimalO","varcharO","dateO"))
						.input("integerI", null).input("decimalI", null)
						.input("varcharI", null).input("dateI", null)
						.output("integerO", SqlType.INTEGER).output("decimalO", SqlType.DOUBLE)
						.output("varcharO", SqlType.VARCHAR).output("dateO", SqlType.DATE).execute();
		
		final Integer integer = result.<Integer>get("integerO");
		final Double decimal = result.<Double>get("decimalO");
		final String string = result.<String>get("varcharO");
		final Date date = result.<Date>get("dateO");

		assertNull(integer);
		assertNull(decimal);
		assertNull(string);
		assertNull(date);
	}
}
