package it.unica.co2.honesty.dto;


public class DoSendDTO extends PrefixDTO {

	public String session;
	public String action;
	
	@Override
	public String toMaude() {
		return "do \""+session+"\" \""+action+"\" ! unit . "+(next==null? "0": next.toMaude());
	}

	@Override
	public PrefixDTO copy() {
		DoSendDTO tmp = new DoSendDTO();
		tmp.session = session;
		tmp.action = action;
		tmp.next = next!=null? next.copy(): null;
		return tmp;
	}
}
