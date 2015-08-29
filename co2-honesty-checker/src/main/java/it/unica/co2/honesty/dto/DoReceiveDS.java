package it.unica.co2.honesty.dto;

import it.unica.co2.model.contract.Sort;

public class DoReceiveDS extends PrefixDS {

	public String session;
	public String action;
	public Sort sort;
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((session == null) ? 0 : session.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DoReceiveDS other = (DoReceiveDS) obj;
		if (action == null) {
			if (other.action != null)
				return false;
		}
		else if (!action.equals(other.action))
			return false;
		if (session == null) {
			if (other.session != null)
				return false;
		}
		else if (!session.equals(other.session))
			return false;
		return true;
	}
	
	
}
