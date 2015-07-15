package it.unica.co2.examples.ebookstore;

import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.Participant;
import co2api.ContractException;
import co2api.Message;
import co2api.TST;
import co2api.TimeExpiredException;

public class Seller extends Participant {

	private static final long serialVersionUID = 1L;
	private static String username = "alice@test.com";
	private static String password = "alice";
	
	public Seller() {
		super(username, password);
	}
	
	@Override
	public void run() {
		
		// the contract to interact with the buyer
		Contract cB = externalSum().add(
				"book",
				internalSum()
					.add("confirm", externalSum().add("pay").add("quit"))
					.add("abort")
		);
		
		// the contract to interact with the distributor
		Contract cD = internalSum().add(
				"book",
				externalSum()
					.add("confirm", internalSum().add("pay").add("quit"))
					.add("abort")
		);
		
		
		Session2<TST> sessionB = tell(cB);
		
		sessionB.waitForReceive("book");
		
		try {
			
			String chosenBook = sessionB.waitForReceive().getStringValue();
			
			if (isInStock(chosenBook)) { // handled internally
				
				sessionB.send("confirm");
				
				Message msgB = sessionB.waitForReceive("quit", "pay");

				switch(msgB.getLabel()) {
				
				case "quit" :
					logger.log("quit received");
					break;
					
				case "pay" : 
					logger.log("pay received");
					handlePayment(msgB.getStringValue());
					break;
				}
			}
			else { // handled with the distributor
				
				try {
				
					Session2<TST> sessionD = tell(cD, 10000);
					
					sessionD.send("book", chosenBook);
					
					try {
						Message msgD = sessionD.waitForReceive(10000, "abort", "confirm");
						
						switch(msgD.getLabel()) {
						
						case "abort" :
							logger.log("abort received");
							sessionB.send("abort");						//complete the session that involve the buyer
							break;
							
						case "confirm" : 
							logger.log("confirm received");
							
							sessionB.send("confirm");					//continue the interaction with the buyer
							
							try {
								Message msgB = sessionB.waitForReceive(10000, "quit", "pay");
		
								switch(msgB.getLabel()) {
								
								case "quit" :
									logger.log("quit received");
									break;
									
								case "pay" : 
									logger.log("pay received");
									handlePayment(msgB.getStringValue());			//the buyer sent you the money
									sessionD.send("pay", bookPrice(chosenBook));	//pay the distributor
									break;
								}
							}
							catch (TimeExpiredException e) {
								//if the buyer not sent 'quit' or 'pay', you are culpable with the distributor
								sessionD.send("quit");
								
								//and now, if the buyer sent you something? you are culpable in sessionB?
								//no, because the buyer does not expect anything
								
								sessionB.waitForReceive("quit", "pay");		//no timeout
								//maybe this waitForReceive is unnecessary
							}
						}
					}
					catch (TimeExpiredException e){
						//if the distributor not sent 'abort' or 'confirm', you are culpable in sessionB
						sessionB.send("abort");		//this action make you honest in sessionB
						
						//and now, if the distributor sent you something? you are culpable in sessionD!
						Message msgD = sessionB.waitForReceive("abort", "confirm");		//no timeout
						
						switch(msgD.getLabel()) {
						
						case "abort" :
							break;
						case "confirm" : sessionD.send("quit");
							break;
						}
						//you are honest
					}
				}
				catch (TimeExpiredException e){
					//if the session is never fused, you are culpable in sessionB
					sessionB.send("abort");		//this action make you honest in sessionB
					
					//and now, if the session is fused?
					
				}
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
	
	public void main(String args[]) throws ContractException {
		new Seller().run();
	}
	
	// ---------------------------- //

	private static boolean isInStock(String tmp) {
		
		return false;
	}
	
	private static Integer bookPrice(String tmp) {
		
		return 10;
	}
	
	private static void handlePayment(String tmp) {
		
		return;
	}


	
	
	
//	private static class HandleInternally extends CO2Process {
//
//		private Session2<TST> session;
//		
//		protected HandleInternally(Session2<TST> session) {
//			super("HandleInternally");
//			this.session = session;
//		}
//
//		@Override
//		public void run() {
//			session.send("confirm");
//			
//			Message msgB = session.waitForReceive("quit", "pay");
//
//			switch(msgB.getLabel()) {
//			
//			case "quit" :
//				logger.log("quit received");
//				break;
//				
//			case "pay" : 
//				logger.log("pay received");
//				try {
//					handlePayment(msgB.getStringValue());
//				}
//				catch (ContractException e) {
//					// TODO Auto-generated catch block
//					throw new RuntimeException(e);
//				}
//				break;
//			}
//		}
//		
//	}
	
}