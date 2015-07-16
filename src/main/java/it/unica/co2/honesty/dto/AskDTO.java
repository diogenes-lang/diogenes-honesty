package it.unica.co2.honesty.dto;


public class AskDTO extends PrefixDTO {
	
	public String session;
	
	@Override
	public String toMaude() {
		return "ask \""+session+"\" (True) . "+(next==null? "0": next.toMaude());
	}

	@Override
	public PrefixDTO copy() {
		AskDTO tmp = new AskDTO();
		tmp.session = session;
		return null;
	}

}
