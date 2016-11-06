package it.unica.co2.api.contract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Sum<T extends Action> extends SessionType {

	private static final long serialVersionUID = 1L;

	protected final List<T> actions = new ArrayList<>();
	
	@SafeVarargs
	protected Sum(T... actions) {
		this.actions.addAll(Arrays.asList(actions));
	}
	
	public List<T> getActions() {
		return actions;
	}
	
	public Sum<T> add(String action) {
		return add(action, Sort.unit(), "", null);
	}

	public Sum<T> add(String action, Sort<?> sort) {
		return add(action, sort, "", null);
	}
	
	public Sum<T> add(String action, SessionType next) {
		return add(action, Sort.unit(), "", next);
	}
	
	public Sum<T> add(String action, String guard) {
		return add(action, Sort.unit(), guard, null);
	}
	
	public Sum<T> add(String action, String guard, SessionType next) {
		return add(action, Sort.unit(), guard, next);
	}
	
	public Sum<T> add(String action, Sort<?> sort, String guard) {
		return add(action, sort, guard, null);
	}
	
	public Sum<T> add(String action, Sort<?> sort, SessionType next) {
		return add(action, sort, "", next);
	}
	
	abstract public Sum<T> add(String action, Sort<?> sort, String guard, SessionType next);
	
}
