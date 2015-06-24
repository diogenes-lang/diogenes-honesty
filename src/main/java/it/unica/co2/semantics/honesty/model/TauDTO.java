package it.unica.co2.semantics.honesty.model;


public class TauDTO extends PrefixDTO {

	@Override
	public String toMaude() {
		return "t . "+(next==null? "0": next.toMaude());
	}

	@Override
	public PrefixDTO copy() {
		TauDTO tau = new TauDTO();
		tau.next = next.copy();
		return tau;
	}

}
