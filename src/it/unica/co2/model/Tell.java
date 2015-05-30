package it.unica.co2.model;

import it.unica.co2.model.contract.Contract;





public class Tell implements Prefix {

	private final int timeout;
	
	private final String sessionName;
	private final Contract contract;
	
	public Tell(String session, Contract contract) {
		this(session, contract, 10_000);
	}
	
	public Tell(String session, Contract contract, int timeout) {
		this.sessionName = session;
		this.contract = contract;
		this.timeout = timeout;
	}

	@Override
	public void run() {
		System.out.println("\t>> tell "+sessionName+" ("+contract+") [timeout="+timeout+"msec]");
	}
	
	@Override
	public String toString() {
		return "tell "+sessionName+" ("+contract+")";
	}
}
