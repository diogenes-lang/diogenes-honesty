package it.unica.co2.model.contract;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;


public class ExternalSum extends Contract{

	private static final long serialVersionUID = 1L;
	
	private final ExternalAction[] actions;

	public ExternalSum(ExternalAction... actions) {
		this.actions = actions;
	}
	
	public ExternalAction[] getActions() {
		return actions;
	}
	
	@Override
	public String toString() {
		if (actions.length==1)
			return actions[0].toString();
		else
			return "("+StringUtils.join(actions, " + ")+")"; 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(actions);
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
		if (!Arrays.equals(actions, other.actions))
			return false;
		return true;
	}
}
