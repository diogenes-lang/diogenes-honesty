package it.unica.co2.model.contract;

public abstract class Action {

	public enum Sort { 
		UNIT, 
		INT,
		STRING
	}
	
	private final String name;
	private final Sort sort;
	private final Contract next;
	
	public Action(String name, Sort sort) {
		this(name, sort, null);
	}
	
	public Action(String name, Sort sort, Contract next) {
		this.name = name;
		this.sort = sort;
		this.next = next;
	}

	public String getName() {
		return name;
	}

	public Sort getType() {
		return sort;
	}

	public Contract getNext() {
		return next;
	}

	@Override
	public String toString() {
		return name + (this instanceof InternalAction?"!":"?") + (sort!=Sort.UNIT? " :"+sort: "") + (next!=null? "."+next.toString(): "") ;
	}
}
