package it.unica.co2.honesty.dto;


public class DoSendDTO extends PrefixDTO {

	public String session;
	public String action;
	
	@Override
	public PrefixDTO copy() {
		DoSendDTO tmp = new DoSendDTO();
		tmp.session = session;
		tmp.action = action;
		tmp.next = next!=null? next.copy(): null;
		return tmp;
	}
}
