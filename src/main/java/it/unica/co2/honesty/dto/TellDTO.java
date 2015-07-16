package it.unica.co2.honesty.dto;


public class TellDTO extends PrefixDTO {

	public String session;
	public String contractName;
	
	@Override
	public PrefixDTO copy() {
		TellDTO tell = new TellDTO();
		tell.contractName = contractName;
		tell.session = session;
		tell.next = next!=null? next.copy(): null;
		return tell;
	}
}
