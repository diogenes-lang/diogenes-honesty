package it.unica.co2.examples.ebookstore;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;

public class DishonestSeller extends Participant {

	private static final long serialVersionUID = 1L;
	private static String username = "n.atzei@test.com";
	private static String password = "cavallo";
	
	public DishonestSeller() {
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
				"bookdistrib",
				externalSum()
					.add("confirmdistr", internalSum().add("paydistrib").add("quitdistr"))
					.add("abortdistrib")
		);
		
		
		Session<TST> sessionB = tellAndWait(cB);
		
		Message msg = sessionB.waitForReceive("book");
		
		try {
			
			String chosenBook = msg.getStringValue();
			
			if (isInStock(chosenBook)) { // handled internally
				
				sessionB.sendIfAllowed("confirm");
				
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
				
				Public<TST> pbl = tell(cD);
				Session<TST> sessionD = pbl.waitForSession();
				
				/*
				 * the waitForSession(Public<TST>) above is blocking.
				 * If the session with the distributor never starts, you are culpable in the session that involve
				 * the buyer.
				 */
				
				sessionD.sendIfAllowed("bookdistrib", chosenBook);
				
				try {
					Message msgD = sessionD.waitForReceive(10000, "abortdistrib", "confirmdistr");
					
					switch(msgD.getLabel()) {
					
					case "abortdistrib" :
						logger.log("abort received from the distributor");
						sessionB.sendIfAllowed("abort");						//complete the session that involve the buyer
						break;
						
					case "confirmdistr" : 
						logger.log("confirm received from the distributor");
						
						sessionB.sendIfAllowed("confirm");					//continue the interaction with the buyer
						
						try {
							Message msgB = sessionB.waitForReceive(10000, "quit", "pay");
	
							switch(msgB.getLabel()) {
							
							case "quit" :
								logger.log("quit received");
								sessionD.sendIfAllowed("quitdistr");							//quit the distributor
								break;
								
							case "pay" : 
								logger.log("pay received");
								handlePayment(msgB.getStringValue());			//the buyer sent you the money
								sessionD.sendIfAllowed("paydistrib", bookPrice(chosenBook));	//pay the distributor
								break;
							}
						}
						catch (TimeExpiredException e) {
							//if the buyer not sent 'quit' or 'pay', you are culpable with the distributor
							sessionD.sendIfAllowed("quitdistr");
							
							//and now, if the buyer sent you something? you are culpable in sessionB?
							//no, because the buyer does not expect anything
							
							sessionB.waitForReceive("quit", "pay");		//no timeout
							//maybe this waitForReceive is unnecessary
						}
					}
					
				}
				catch (TimeExpiredException e){
					
					logger.log("no action was received in time");
					
					//if the distributor not sent 'abort' or 'confirm', you are culpable in sessionB
					sessionB.sendIfAllowed("abort");		//this action make you honest in sessionB
					
					//and now, if the distributor sent you something? you are culpable in sessionD!
					processCall(AbortSessionD2.class, sessionD);
					//you are honest
					
				}
			}
			
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
		
	}

	public static void main(String args[]) throws ContractException {
		new DishonestSeller().run();
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


	
	private static class AbortSessionD2 extends CO2Process {
		
		private static final long serialVersionUID = 1L;
		private Session<TST> sessionD;
		
		protected AbortSessionD2(Session<TST> session) {
			super("AbortSessionD2");
			this.sessionD = session;
		}
		
		@Override
		public void run() {
			Message msgD = sessionD.waitForReceive("abortdistrib", "confirmdistr");		//no timeout
			
			switch(msgD.getLabel()) {
			
			case "abortdistrib" :
				break;
			case "confirmdistr" : sessionD.sendIfAllowed("quitdistr");
				break;
			}
		}
	}

}