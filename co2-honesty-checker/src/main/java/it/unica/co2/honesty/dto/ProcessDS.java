package it.unica.co2.honesty.dto;

import it.unica.co2.generators.MaudeCo2Generator;


public abstract class ProcessDS {

	@Override 
	public String toString() {
		return toMaude();
	}
	
	public String toMaude() {
		return toMaude("");
	}
	
	public String toMaude(String initialSpace) {
		return MaudeCo2Generator.toMaude(this, initialSpace);
	}
}
