package it.unica.co2.semantics.honesty.model;


public class DoReceiveDTO extends PrefixDTO {

	public String session;
	public String action;
	
	@Override
	public String toMaude() {
		return "do \""+session+"\" "+action+" ? unit . "+(next==null? "0": next.toMaude());
	}
	
	@Override
	public PrefixDTO copy() {
		DoReceiveDTO tmp = new DoReceiveDTO();
		tmp.session = session;
		tmp.action = action;
		tmp.next = next!=null? next.copy(): null;
		return tmp;
	}
}
