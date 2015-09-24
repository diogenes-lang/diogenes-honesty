package it.unica.co2.api.contract;

import org.apache.commons.lang3.StringUtils;


public class ExternalSum extends Sum<ExternalAction> {

	private static final long serialVersionUID = 1L;

	public ExternalSum(ExternalAction... actions) {
		super(ExternalAction.class, actions);
	}
	
	@Override
	public ExternalSum add(String action, Sort sort, Contract next) {
		actions.add(new ExternalAction(action, sort, next));
		return this;
	}
	
	@Override
	public String toString() {
		if (actions.size()==1)
			return actions.get(0).toString();
		else
			return "("+StringUtils.join(actions, " + ")+")";
	}
}
