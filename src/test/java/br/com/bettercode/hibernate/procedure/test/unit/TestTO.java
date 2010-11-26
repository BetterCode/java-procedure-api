package br.com.bettercode.hibernate.procedure.test.unit;

import javax.persistence.Column;

public class TestTO {
	
	@Column(name = TestWithAliasResultTransformer.FIELD1_NAME)
	private String f1;

	private Integer f2;

	public String getF1() {
		return f1;
	}

	public Integer getF2() {
		return f2;
	}
}
