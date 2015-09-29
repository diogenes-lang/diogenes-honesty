package it.unica.co2.examples;

import static it.unica.co2.api.contract.newapi.ContractFactory.*;
import static it.unica.co2.api.contract.newapi.ContractFactory.*;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.newapi.Contract;
import it.unica.co2.api.process.Participant;

public class HONEST extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "voucher.seller@nicola.com";
	private static String password = "voucher.seller";
	
	public HONEST() {
		super(username, password);
	}

	@Override
	public void run() {
			
		Contract CB = externalSum()
				.add("a", internalSum().add("b"));
		
		Session2<TST> session = tellAndWait(CB);
		
		try {
			session.waitForReceive(3000, "a");
		}
		catch (TimeExpiredException e) {
			session.send("b");
		}
				
	}
	
	public static void main(String[] args) {
		new HONEST().run();
	}
}
