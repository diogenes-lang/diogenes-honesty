package it.unica.co2.examples.ebookstore;

import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.Participant;
import co2api.ContractException;
import co2api.Message;
import co2api.TST;

public class Distributor extends Participant {
	
	private static final long serialVersionUID = 1L;
	private static String username = "alice@test.com";
	private static String password = "alice";
	
	public Distributor() {
		super(username, password);
	}
	
	@Override
	public void run() {
		
		Contract c = externalSum().add(
				"book",
				internalSum()
					.add("confirm", externalSum().add("pay").add("quit"))
					.add("abort")
		);
		
		Session2<TST> session = tell(c);
		
		Message msg;	
		String isbn;
		

		try {
			msg = session.waitForReceive("book");
			isbn = msg.getStringValue();
			
			if (isPresent(isbn)) {
				session.send("confirm");
				
				msg = session.waitForReceive("pay", "quit");
				
				switch (msg.getLabel()) {
				
				case "pay":	
					logger.log("pay received");
					break;
				
				case "quit": 
					logger.log("quit received");
					break;
				}
			}
			else {
				session.send("abort");
			} 
			
		}
		catch (ContractException e) {

		}
			
	}

	@Override
	protected String getUsername() {
		return username;
	}
	
	public void main(String args[]) throws ContractException {
		new Distributor().run();
	}

	private static boolean isPresent(String isbn) {
	
		return true;
	}


}
