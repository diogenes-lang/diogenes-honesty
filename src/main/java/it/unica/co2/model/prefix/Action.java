package it.unica.co2.model.prefix;

import it.unica.co2.model.contract.Sort;

public class Action {

	private final String name;
	private final Sort sort;
	private Runnable next;
	
	public Action(String name, Sort sort) {
		this.name = name;
		this.sort = sort;
	}

	public Action next(Runnable next) {
		this.next = next;
		return	 this;
	}
	
	
	
	public Action setNext(Runnable runn) {
		this.next = runn;
		return this;
	}
	

	public String getName() {
		return name;
	}
	
	public Sort getSort() {
		return sort;
	}

	public Runnable getNext() {
		return next;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
