package it.unica.co2.model.prefix;


public class DoSend implements Prefix {

	private final String session;
	private final String action;
	private final String value;
	
	public DoSend(String session, String contract) {
		this(session, contract, null);
	}
	
	public DoSend(String session, String action, String value) {
		super();
		this.session = session;
		this.action = action;
		this.value=value;
	}

	@Override
	public void run() {
		System.out.println("\t>> do "+session+" "+action+"! "+(value!=null? value:""));
	}
	
	@Override
	public String toString() {
		return "do "+session+" "+action+"! "+(value!=null? value:"");
	}
}
