package it.unica.co2.examples.ebookstore;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.Message;
import co2api.Session;
import co2api.TST;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.process.Participant;

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
		
		Session<TST> session = tellAndWait(c);
		
		Message msg;	
		String isbn;
		

		try {
			msg = session.waitForReceive("bookdistrib");
			isbn = msg.getStringValue();
			
			if (isPresent(isbn)) {
				session.sendIfAllowed("confirmdistr");
				
				msg = session.waitForReceive("paydistrib", "quitdistr");
				
				switch (msg.getLabel()) {
				
				case "paydistrib":	
					System.out.println("pay received");
					break;
				
				case "quitdistr": 
					System.out.println("quit received");
					break;
				}
			}
			else {
				session.sendIfAllowed("abortdistrib");
			} 
			
		}
		catch (ContractException e) {
			System.out.println("exception: "+e.getMessage());
		}
		
	}

	public static void main(String args[]) throws ContractException {
		new Distributor().run();
	}

	private static boolean isPresent(String isbn) {
	
		return true;
	}


}
