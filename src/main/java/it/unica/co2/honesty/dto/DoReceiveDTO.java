package it.unica.co2.honesty.dto;


public class DoReceiveDTO extends PrefixDTO {

	public String session;
	public String action;
	
	@Override
	public String toMaude() {
		return "do \""+session+"\" \""+action+"\" ? unit . "+(next==null? "0": next.toMaude());
	}
	
	@Override
	public PrefixDTO copy() {
		DoReceiveDTO tmp = new DoReceiveDTO();
		tmp.session = session;
		tmp.action = action;
		tmp.next = next!=null? next.copy(): null;
		return tmp;
	}

	
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
		DoReceiveDTO other = (DoReceiveDTO) obj;
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
