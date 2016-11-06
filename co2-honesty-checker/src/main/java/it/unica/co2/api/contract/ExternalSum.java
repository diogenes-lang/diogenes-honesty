package it.unica.co2.api.contract;

import org.apache.commons.lang3.StringUtils;


public class ExternalSum extends Sum<ExternalAction> {

	private static final long serialVersionUID = 1L;

	public ExternalSum(ExternalSum sum) {
		for (ExternalAction a : sum.getActions()) {
			actions.add(new ExternalAction(a));
		}
	}
	
	public ExternalSum(ExternalAction... actions) {
		super(actions);
	}
	
	@Override
	public ExternalSum add(String name, Sort<?> sort, String guard, SessionType next) {
		actions.add(new ExternalAction(name, sort, guard, next));
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
	public SessionType deepCopy() {
		return new ExternalSum(this);
	}
}
