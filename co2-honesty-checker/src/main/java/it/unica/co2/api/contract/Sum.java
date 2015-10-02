package it.unica.co2.api.contract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Sum<T extends Action> extends Contract {

	private static int CLASS_ID = 0;
	protected final int ID=CLASS_ID++;

	private static final long serialVersionUID = 1L;

	protected final List<T> actions = new ArrayList<>();;
	
	@SafeVarargs
	protected Sum(T... actions) {
		this.actions.addAll(Arrays.asList(actions));
	}
	
	public List<T> getActions() {
		return actions;
	}
	
	public Sum<T> add(String action) {
		return add(action, Sort.UNIT);
	}

	public Sum<T> add(String action, Sort sort) {
		return add(action, sort, null);
	}
	
	public Sum<T> add(String action, Contract next) {
		return add(action, Sort.UNIT, next);
	}
	
	abstract public Sum<T> add(String action, Sort sort, Contract next);
	
}
