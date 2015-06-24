package it.unica.co2.model.prefix;

import it.unica.co2.model.contract.Sort;
import co2api.ContractException;
import co2api.Message;


public class Variable {

	private Message msg;
	private Sort sort;
	
	public Variable(Sort sort) {
		this.sort = sort;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}
	
	

	
	public Sort getSort() {
		return sort;
	}

	public String getLabel() {
		return msg.getLabel();
	}
	
	public <T> T getValue(Class<T> clazz) {
		return clazz.cast(getValue());
	}
	
	public Object getValue() {
		
		Object val = null;
		
		try {
			switch (sort) {
			case INT:
				val = msg.getIntegerValue();
				break;
				
			case STRING:
				val = msg.getStringValue();
				break;

			case UNIT:
				break;

			default:
				throw new AssertionError("unexpected sort "+sort);
			}
		}
		catch (ContractException e) {
			new RuntimeException(e);
		}
		
		return val;
	}
}
