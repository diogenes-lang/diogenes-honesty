package it.unica.co2.examples.insuredsale;

import static it.unica.co2.model.ContractFactory.*;

import co2api.ContractException;
import co2api.Message;
import co2api.Public;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.Participant;


public class InsuredSeller extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "insuredseller@nicola.com";
	private static String password = "insuredseller";
	
	public InsuredSeller() {
		super(username, password);
	}


	
	@Override
	public void run() {
		
		Contract CA = externalSum().add("order", 
				internalSum()
				.add("amount", externalSum().add("pay"))
				.add("abort"));
		
		
		Public<TST> pbl = tell(CA);
		Session2<TST> session = waitForSession(pbl);
		
		Message msg = session.waitForReceive("order");
		
		try {
			Integer n = Integer.parseInt(msg.getStringValue());
			
			if (n<50) {
				new HandlePayment(session, n).run();
			}
			else {
				new Insurance(session, n, this).run();
			}
		}
		catch (Exception | ContractException e) {
			throw new RuntimeException(e);
		}
		
	}

	private static class HandlePayment extends Participant {

		private static final long serialVersionUID = 1L;
		private final Session2<TST> session;
		private final Integer amount;
		
		protected HandlePayment(Session2<TST> session, Integer amount) {
			super(username, password);
			this.session = session;
			this.amount = amount;
		}

		@Override
		public void run() {
			
			if (isServiceAvailable()) {
				session.send("amount", amount);
				Message msg = session.waitForReceive("pay");
				
				try {
					String payment = msg.getStringValue();
					handlePayment(payment);
				}
				catch (ContractException e) {
					throw new RuntimeException(e);
				}
				
			}
			else {
				session.send("abort");
			}
		}
	
		
		
		private boolean isServiceAvailable() {
			return true;
		}
		
		private void handlePayment(String payment) {}
	}
	
	
	private static class Insurance extends Participant {

		private static final long serialVersionUID = 1L;
		private final Session2<TST> session;
		private final Integer amount;
		private final Participant p;
		
		protected Insurance(Session2<TST> session, Integer amount, Participant p) {
			super(username, password);
			this.session = session;
			this.amount = amount;
			this.p = p;
		}

		@Override
		public void run() {
			
			Contract CI = internalSum().add("reqi", externalSum().add("oki").add("aborti"));

			Public<TST> pblI = p.tell(CI);
		
			Session2<TST> sessionI;
			
			try {
				sessionI = p.waitForSession(pblI, 10000);
				
				sessionI.send("reqi", amount);
				
				try {
					Message msg = sessionI.waitForReceive(10000, "oki", "aborti");
					
					switch (msg.getLabel()) {
					
					case "oki":
						new HandlePayment(session, amount);
						break;
						
					case "aborti":
						session.send("abort");
						break;
					}
					
				}
				catch (TimeExpiredException e) {
					
					session.send("abort");
					
					sessionI.waitForReceive("oki", "aborti");
				}
				
			}
			catch (TimeExpiredException e) {
				
				session.send("abort");
				
				sessionI = p.waitForSession(pblI);
				sessionI.send("reqi");
				sessionI.waitForReceive("oki", "aborti");
			}
		
		}
	
	}
	
	public static void main(String[] args) {
		new InsuredSeller().run();
	}
}
