package it.unica.co2.semantics.honesty.model;


public abstract class PrefixDTO {
	
	public ProcessDTO next;
	
	abstract public String toMaude();
	
	abstract public PrefixDTO copy();
	
	@Override 
	public String toString() {
		return toMaude();
	}
}
