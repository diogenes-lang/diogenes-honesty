package it.unica.co2.honesty.dto;


public class TauDTO extends PrefixDTO {

	@Override
	public PrefixDTO copy() {
		TauDTO tau = new TauDTO();
		tau.next = next!=null?next.copy():null;
		return tau;
	}

}
