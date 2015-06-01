package it.unica.co2.model.contract;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;


public class InternalSum extends Contract{

	private static final long serialVersionUID = 1L;
	
	private final InternalAction[] actions;
	
	public InternalSum(InternalAction... actions) {
		this.actions = actions;
	}
	
	public InternalAction[] getActions() {
		return actions;
	}
	
	@Override
	public String toString() {
		return "("+StringUtils.join(actions, " (+) ")+")"; 
//		return Arrays.stream(actions).map(a -> a.toString()).collect(Collectors.joining(" (+) ", "(", ")"));
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
		InternalSum other = (InternalSum) obj;
		if (!Arrays.equals(actions, other.actions))
			return false;
		return true;
	}
}
