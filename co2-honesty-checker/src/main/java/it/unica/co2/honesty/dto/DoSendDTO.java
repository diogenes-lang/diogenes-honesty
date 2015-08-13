package it.unica.co2.honesty.dto;

import it.unica.co2.model.contract.Sort;

public class DoSendDTO extends PrefixDTO {

	public String session;
	public String action;
	public Sort sort;
	
	@Override
	public PrefixDTO copy() {
		DoSendDTO tmp = new DoSendDTO();
		tmp.session = session;
		tmp.action = action;
		tmp.sort = sort;
		tmp.next = next!=null? next.copy(): null;
		return tmp;
	}
}
