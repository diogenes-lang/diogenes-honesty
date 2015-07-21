package it.unica.co2.honesty.dto;

import it.unica.co2.generators.MaudeCo2Generator;


public abstract class PrefixDTO {
	
	public ProcessDTO next;
	
	abstract public PrefixDTO copy();
	
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
