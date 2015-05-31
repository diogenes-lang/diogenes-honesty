package it.unica.co2.model.contract;

public class InternalAction extends Action {

	public InternalAction(String name, Sort sort) {
		super(name, sort, Type.INTERNAL);
	}

	public InternalAction(String name, Sort sort, Contract next) {
		super(name, sort, Type.INTERNAL, next);
	}

}
