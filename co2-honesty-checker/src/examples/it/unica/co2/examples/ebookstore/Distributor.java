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
	private static String username = "bob@test.com";
	private static String password = "bob";
	
	public Distributor() {
		super(username, password);
	}
	
	@Override
	public void run() {
		
		Contract c = externalSum().add(
				"bookdistrib",
				internalSum()
					.add("confirmdistr", externalSum().add("paydistrib").add("quitdistr"))
					.add("abortdistrib")
		);
		
		Session2<TST> session = tellAndWait(c);
		
		Message msg;	
		String isbn;
		

		try {
			msg = session.waitForReceive("bookdistrib");
			isbn = msg.getStringValue();
			
			if (isPresent(isbn)) {
				session.send("confirmdistr");
				
				msg = session.waitForReceive("paydistrib", "quitdistr");
				
				switch (msg.getLabel()) {
				
				case "paydistrib":	
					logger.log("pay received");
					break;
				
				case "quitdistr": 
					logger.log("quit received");
					break;
				}
			}
			else {
				session.send("abortdistrib");
			} 
			
		}
		catch (ContractException e) {
			logger.log("exception: "+e.getMessage());
		}
		
		logger.log("I'm on duty: "+session.amIOnDuty());
		logger.log("I'm culpable: "+session.amICulpable());
	}

	public static void main(String args[]) throws ContractException {
		new Distributor().run();
	}

	private static boolean isPresent(String isbn) {
	
		return true;
	}


}