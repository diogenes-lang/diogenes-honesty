package it.unica.co2.api.contract.newapi;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;


public class ExternalSum extends Sum<ExternalAction> {

	private static final long serialVersionUID = 1L;

	public ExternalSum(ExternalSum sum) {
		super(ExternalAction.class, new ExternalAction[sum.actions.length]);
		
		for (int i=0; i<sum.actions.length; i++) {
			actions[i] = new ExternalAction(sum.actions[i]);
		}
	}
	
	public ExternalSum(ExternalAction... actions) {
		super(ExternalAction.class, actions);
	}
	
	@Override
	public ExternalSum add(String name, Sort sort, Contract next) {
		ExternalAction[] newActions = Arrays.copyOf(actions, actions.length+1);
		newActions[newActions.length-1]=new ExternalAction(name, sort, next);
		return new ExternalSum(newActions);
	}
	
	@Override
	public String toString() {
		if (actions.length==1)
			return actions[0].toString();
		else
			return "("+StringUtils.join(actions, " + ")+")";
	}

	@Override
	public Contract deepCopy() {
		return new ExternalSum(this);
	}
}
