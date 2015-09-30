package it.unica.co2.examples.insuredsale;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.Message;
import co2api.TST;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.process.Participant;


public class IBuyer extends Participant {

	private static final long serialVersionUID = 1L;

	public IBuyer() {
		super("buyer@nicola.com", "buyer");
	}

	@Override
	public void run() {
		Contract CA = internalSum().add("order", 
				externalSum()
				.add("amount", internalSum().add("pay"))
				.add("abort"));
		
		
		Integer orderAmount = 60;
		
		Session2<TST> session = tellAndWait(CA);
		
		session.send("order", orderAmount);
		
		Message msg = session.waitForReceive("amount", "abort");
		
		switch(msg.getLabel()) {
		case "amount":
			session.send("pay", "1234-0000-1234-0000");
			break;
			
		case "abort":
			break;
		}
		
	}

	public static void main (String[] args) {
		new IBuyer().run();
	}
}
