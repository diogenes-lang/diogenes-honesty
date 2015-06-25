package it.unica.co2.model.contract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;


public class InternalSum extends Contract{

	private static final long serialVersionUID = 1L;
	
	private final List<InternalAction> actions = new ArrayList<>();
	
	public InternalSum(InternalAction... actions) {
		this.actions.addAll(Arrays.asList(actions));
	}
	
	public InternalAction[] getActions() {
		return actions.toArray(new InternalAction[]{});
	}
	
	
	public InternalSum add(String action) {
		return add(action, Sort.UNIT);
	}

	public InternalSum add(String action, Sort sort) {
		return add(action, sort, null);
	}
	
	public InternalSum add(String action, Contract next) {
		return add(action, Sort.UNIT, next);
	}
	
	public InternalSum add(String action, Sort sort, Contract next) {
		actions.add(new InternalAction(action, sort, next));
		return this;
	}
	
	public InternalSum add(InternalAction action) {
		actions.add(action);
		return this;
	}
	
	
	
	@Override
	public String toString() {
		if (actions.size()==1)
			return actions.get(0).toString();
		else
			return "("+StringUtils.join(actions, " (+) ")+")";
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
		InternalSum other = (InternalSum) obj;
		if (actions == null) {
			if (other.actions != null)
				return false;
		}
		else if (!actions.equals(other.actions))
			return false;
		return true;
	}

}
