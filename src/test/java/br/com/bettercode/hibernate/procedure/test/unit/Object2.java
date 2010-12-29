package br.com.bettercode.hibernate.procedure.test.unit;

import br.com.bettercode.resulttransformer.Columns;

public class Object2 {
	
	@Columns(names = {"galo","atletico"})
	private String f1;

	private Integer f2;

	public String getF1() {
		return f1;
	}

	public Integer getF2() {
		return f2;
	}
}
