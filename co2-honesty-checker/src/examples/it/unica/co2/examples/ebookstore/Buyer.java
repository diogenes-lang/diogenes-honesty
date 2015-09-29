package it.unica.co2.examples.ebookstore;

import static it.unica.co2.api.contract.newapi.ContractFactory.*;
import co2api.ContractException;
import co2api.Message;
import co2api.TST;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.newapi.Contract;
import it.unica.co2.api.process.Participant;

public class Buyer extends Participant {
	
	private static final long serialVersionUID = 1L;
	private static String username = "nicola.a@test.com";
	private static String password = "cicciolina";
	
	public Buyer() {
		super(username, password);
	}

	@Override
	public void run() {
		
		Contract c = internalSum().add(
				"book",
				externalSum()
					.add("confirm", internalSum().add("pay").add("quit"))
					.add("abort")
		);
		
		Session2<TST> session = tellAndWait(c);
		
		Message mB;	
		Integer price;
		String isbn = "8794567347859"; // book id
		Integer desiredPrice = 10;
		
		session.send("book", isbn);
		
		try {
			mB = session.waitForReceive("abort", "confirm");

			switch (mB.getLabel()) {
			
			case "abort":
				logger.log("abort received");
				break;
				
			case "confirm":
				logger.log("confirm received");
				price = Integer.parseInt(mB.getStringValue());
				
				if (price > desiredPrice)
					session.send("quit");
				else {
					session.send("pay", price);
					System.out.println("Session completed: I've buyed the book");
				}
				
			}
		}
		catch (NumberFormatException | ContractException e) {
			logger.log("exception: "+e.getMessage());
		}
		
		logger.log("I'm on duty: "+session.amIOnDuty());
		logger.log("I'm culpable: "+session.amICulpable());
	}
	
	public static void main(String args[]) throws ContractException {
		new Buyer().run();
	}

}
