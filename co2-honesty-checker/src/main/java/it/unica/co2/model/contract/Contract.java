package it.unica.co2.model.contract;

import it.unica.co2.generators.MaudeContractGenerator;
import it.unica.co2.generators.MiddlewareContractGenerator;

import java.io.Serializable;

public abstract class Contract implements Serializable {

	private static final long serialVersionUID = 1L;

	public String toMaude() {
		return new MaudeContractGenerator(this).generate();
	}
	
	public String toMiddleware() {
		return new MiddlewareContractGenerator(this).generate();
	}
}
