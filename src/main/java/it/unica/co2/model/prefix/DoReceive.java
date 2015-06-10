package it.unica.co2.model.prefix;



public class DoReceive implements Prefix {

	private final String session;
	private final String action;
	private final Variable receivedValue;
	
	public DoReceive(String session, String action) {
		this(session, action, null);
	}
	
	public DoReceive(String session, String action, Variable receivedValue) {
		this.session = session;
		this.action = action;
		this.receivedValue = receivedValue;
	}

	@Override
	public void run() {
		System.out.println("\t>> do "+session+" "+action+"?");
		
		if (receivedValue!=null)
			receivedValue.setValue("received!");
		
	}

	@Override
	public String toString() {
		return "do "+session+" "+action+"? ";
	}

}
