package it.unica.co2.api.contract.newapi;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;


public class InternalSum extends Sum<InternalAction> {
	
	private static final long serialVersionUID = 1L;

	public InternalSum(InternalSum sum) {
		super(InternalAction.class, new InternalAction[sum.actions.length]);
		
		for (int i=0; i<sum.actions.length; i++) {
			actions[i] = new InternalAction(sum.actions[i]);
		}
	}
	
	public InternalSum(InternalAction... actions) {
		super(InternalAction.class, actions);
	}
	
	@Override
	public InternalSum add(String name, Sort sort, Contract next) {
		InternalAction[] newActions = Arrays.copyOf(actions, actions.length+1);
		newActions[newActions.length-1]=new InternalAction(name, sort, next);
		return new InternalSum(newActions);
	}
	
	@Override
	public String toString() {
		if (actions.length==1)
			return actions[0].toString();
		else
			return "("+StringUtils.join(actions, " (+) ")+")";
	}
	
	@Override
	public Contract deepCopy() {
		return new InternalSum(this);
	}

}
