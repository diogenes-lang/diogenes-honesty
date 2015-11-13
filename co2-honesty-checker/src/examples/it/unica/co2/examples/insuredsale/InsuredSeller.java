package it.unica.co2.examples.insuredsale;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.Message;
import co2api.Public;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.process.Participant;


public class InsuredSeller extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "insuredseller1@nicola.com";
	private static String password = "insuredseller1";
	
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
				processCall(HandlePayment.class, session, n);
			}
			else {
				processCall(HandleInsurance.class, session, n);
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
	
	
	private static class HandleInsurance extends Participant {

		private static final long serialVersionUID = 1L;
		private final Session2<TST> session;
		private final Integer amount;
		
		protected HandleInsurance(Session2<TST> session, Integer amount) {
			super(username, password);
			this.session = session;
			this.amount = amount;
		}

		@Override
		public void run() {
			
			Contract CI = internalSum().add("reqi", externalSum().add("oki").add("aborti"));

			Public<TST> pblI = tell(CI);
		
			Session2<TST> sessionI;
			
			try {
				sessionI = waitForSession(pblI, 10000);
				
				sessionI.send("reqi", amount);
				
				try {
					Message msg = sessionI.waitForReceive(10000, "oki", "aborti");
					
					switch (msg.getLabel()) {
					
					case "oki":
						processCall(HandlePayment.class, session, amount);
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
				
				sessionI = waitForSession(pblI);
				sessionI.send("reqi");
				sessionI.waitForReceive("oki", "aborti");
			}
		
		}
	
	}
	
	public static void main(String[] args) {
		new InsuredSeller().run();
	}
}
