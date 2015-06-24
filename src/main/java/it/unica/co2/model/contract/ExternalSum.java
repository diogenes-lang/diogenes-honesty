package it.unica.co2.model.contract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;


public class ExternalSum extends Contract {

	private static final long serialVersionUID = 1L;
	
	private final List<ExternalAction> actions = new ArrayList<>();

	public ExternalSum(ExternalAction... actions) {
		this.actions.addAll(Arrays.asList(actions));
	}
	
	public ExternalAction[] getActions() {
		return actions.toArray(new ExternalAction[]{});
	}
	
	
	public ExternalSum add(String action) {
		actions.add(new ExternalAction(action, Sort.UNIT, null));
		return this;
	}
	
	public ExternalSum add(ExternalAction action) {
		actions.add(action);
		return this;
	}
	
	
	
	@Override
	public String toString() {
		if (actions.size()==1)
			return actions.get(0).toString();
		else
			return "("+StringUtils.join(actions, " + ")+")"; 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actions == null) ? 0 : actions.hashCode());
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
		ExternalSum other = (ExternalSum) obj;
		if (actions == null) {
			if (other.actions != null)
				return false;
		}
		else if (!actions.equals(other.actions))
			return false;
		return true;
	}
	
}
