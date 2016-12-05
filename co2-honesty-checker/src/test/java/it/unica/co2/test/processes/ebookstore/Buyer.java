package it.unica.co2.test.processes.ebookstore;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;

import co2api.ContractException;
import co2api.Message;
import co2api.Session;
import it.unica.co2.api.contract.SessionType;
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
		
		SessionType c = internalSum().add(
				"book",
				externalSum()
					.add("confirm", internalSum().add("pay").add("quit"))
					.add("abort")
		);
		
		Session<SessionType> session = tellAndWait(c);
		
		Message mB;	
		Integer price;
		String isbn = "8794567347859"; // book id
		Integer desiredPrice = 10;
		
		session.sendIfAllowed("book", isbn);
		
		try {
			mB = session.waitForReceive("abort", "confirm");

			switch (mB.getLabel()) {
			
			case "abort":
				System.out.println("abort received");
				break;
				
			case "confirm":
				System.out.println("confirm received");
				price = Integer.parseInt(mB.getStringValue());
				
				if (price > desiredPrice)
					session.sendIfAllowed("quit");
				else {
					session.sendIfAllowed("pay", price);
					System.out.println("Session completed: I've buyed the book");
				}
				
			}
		}
		catch (NumberFormatException | ContractException e) {
			System.out.println("exception: "+e.getMessage());
		}
		
	}
	
	public static void main(String args[]) throws ContractException {
		new Buyer().run();
	}

}
