package it.unica.co2.examples.ebookstore;

import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.Participant;
import co2api.ContractException;
import co2api.Message;
import co2api.TST;

public class Buyer extends Participant {
	
	private static final long serialVersionUID = 1L;
	private static String username = "alice@test.com";
	private static String password = "alice";
	
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
		catch (NumberFormatException | ContractException tee) {

		}

	}

	@Override
	protected String getUsername() {
		return username;
	}
	
	public void main(String args[]) throws ContractException {
		new Buyer().run();
	}

}
