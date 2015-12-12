package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.TST;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.process.Participant;

public class DISHONEST extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "voucher.buyer@nicola.com";
	private static String password = "voucher.buyer";
	
	public DISHONEST() {
		super(username, password);
	}

	@Override
	public void run() {
			
		Contract CB = internalSum()
				.add("a", externalSum().add("b"));
		
		Session2<TST> session = tellAndWait(CB);
		
		session.sendIfAllowed("b");
				
	}
	
	public static void main(String[] args) {
		new DISHONEST().run();
	}
}
