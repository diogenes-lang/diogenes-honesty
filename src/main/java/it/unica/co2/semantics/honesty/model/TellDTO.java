package it.unica.co2.semantics.honesty.model;


public class TellDTO extends PrefixDTO {

	public String session;
	public String contractName;
	
	@Override
	public String toMaude() {
		return "tell \""+session+"\" "+" "+contractName+" . "+(next==null? "0": next.toMaude());
	}

	@Override
	public PrefixDTO copy() {
		TellDTO tell = new TellDTO();
		tell.contractName = contractName;
		tell.session = session;
		tell.next = next!=null? next.copy(): null;
		return tell;
	}
}
