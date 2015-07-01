package it.unica.co2.model.contract;

import it.unica.co2.model.contract.generators.MaudeSyntaxGenerator;
import it.unica.co2.model.contract.generators.MiddlewareSyntaxGenerator;

import java.io.Serializable;

public abstract class Contract implements Serializable {

	private static final long serialVersionUID = 1L;

	public String toMaude() {
		return new MaudeSyntaxGenerator(this).generate();
	}
	
	public String toMiddleware() {
		return new MiddlewareSyntaxGenerator(this).generate();
	}
}
