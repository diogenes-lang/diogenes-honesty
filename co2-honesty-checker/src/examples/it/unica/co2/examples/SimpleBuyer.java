package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.Message;
import co2api.TST;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.process.Participant;


public class SimpleBuyer extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static final String username = "alice@test.com";
	private static final String password = "alice";
	
	public SimpleBuyer() {
		super(username, password);
	}

	
	private Contract contract = 
			internalSum().add(
					"item", //Sort.STRING, 
					externalSum().add("amount", //Sort.INT, 
							internalSum()
							.add("pay", /*Sort.INT,*/ externalSum().add("item"))
							.add("abort")
							)
			);
	
	@Override
	public void run() {
		
		try {
			Session2<TST> session = tellAndWait(contract);
			
			session.sendIfAllowed("item", "01234");
			
			Message msg = session.waitForReceive("amount");
			
			Integer n = Integer.valueOf(msg.getStringValue());
			
			if (n<10) {
				session.sendIfAllowed("pay", n);
				msg = session.waitForReceive("item");
			}
			else {
				session.sendIfAllowed("abort");
			}
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		new SimpleBuyer().run();
	}

}
