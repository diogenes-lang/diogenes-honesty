package it.unica.co2.model.contract;

public class CtxAction extends Contract {

	private static final long serialVersionUID = 1L;
	private final InternalAction action;

	public CtxAction(InternalAction action) {
		this.action = action;
	}
	
	public Contract consumeAction() {
		return action.getNext();
	}
	
	public String getActionName() {
		return action.getName();
	}
	
	@Override
	public String toString() {
		return "rdy ["+action.toString()+"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
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
		CtxAction other = (CtxAction) obj;
		if (action == null) {
			if (other.action != null)
				return false;
		} else if (!action.equals(other.action))
			return false;
		return true;
	}
}
