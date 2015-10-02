package it.unica.co2.api.contract;

import org.apache.commons.lang3.StringUtils;


public class InternalSum extends Sum<InternalAction> {
	
	private static final long serialVersionUID = 1L;

	public InternalSum(InternalSum sum) {
		for (InternalAction a : sum.getActions()) {
			actions.add(new InternalAction(a));
		}
	}
	
	public InternalSum(InternalAction... actions) {
		super(actions);
	}
	
	@Override
	public InternalSum add(String name, Sort sort, Contract next) {
		actions.add( new InternalAction(name, sort, next) );
		return this;
	}
	
	@Override
	public String toString() {
		if (actions.size()==1)
			return actions.get(0).toString();
		else
			return StringUtils.join(actions, " (+) ")+")";
	}
	
	@Override
	public Contract deepCopy() {
		return new InternalSum(this);
	}

}
