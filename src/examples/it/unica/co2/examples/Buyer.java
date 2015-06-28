package it.unica.co2.examples;

import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.Sort;
import it.unica.co2.model.process.Participant;
import co2api.ContractException;
import co2api.Message;
import co2api.TST;


public class Buyer extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static final String username = "alice@test.com";
	private static final String password = "alice";
	
	public Buyer() {
		super(username, password);
	}

	
	private Contract contract = 
			internalSum().add(
					"item", Sort.STRING, 
					externalSum().add("amount", Sort.INT, 
							internalSum()
							.add("pay", Sort.INT, externalSum().add("item"))
							.add("abort")
							)
			);
	
	@Override
	public void run() {
		
		try {
			Session2<TST> session = tell(contract);
			
			session.send("item", "01234");
			
			Message msg = session.waitForReceive("amount");
			
			Integer n = Integer.valueOf(msg.getStringValue());
			
			if (n<10) {
				session.send("pay", n);
				msg = session.waitForReceive("item");
			}
			else {
				session.send("abort");
			}
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected String getUsername() {
		return username;
	}
	
	public static void main(String[] args) {
		new Buyer().run();
	}

}
