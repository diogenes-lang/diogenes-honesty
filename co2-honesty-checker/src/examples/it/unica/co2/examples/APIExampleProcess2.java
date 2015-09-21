package it.unica.co2.examples;


import static it.unica.co2.model.ContractFactory.*;

import co2api.ContractException;
import co2api.Message;
import co2api.Public;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.CO2Process;
import it.unica.co2.model.process.Participant;

public class APIExampleProcess2 extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "alice@test.com";
	private static String password = "alice";

	public APIExampleProcess2() {
		super(username, password);
	}

	public static void main(String[] args) throws ContractException {
		new APIExampleProcess2().run();
	}
	
	@Override
	public void run() {
		try {
			
			Contract A = 
					internalSum()
					.add("a")
					.add("b", externalSum().add("a").add("b").add("c"))
			;
			
			logger.log("tell");
			
			Public<TST> pbl = tell(A);
			
			try {
				
				Session2<TST> session = waitForSession(pbl, 10000);
				
				logger.log("sending b!");
				session.send("b");
				
				logger.log("receiving message");
				Message msg = session.waitForReceive("a","b","c");
				
				logger.log("received message: "+msg.getLabel()+" "+msg.getStringValue());
				
				switch (msg.getLabel()) {
				
				case "a": 
					logger.log("received a?");
					session.send("a.ok");
					break;
					
				case "b":
					logger.log("received b?");
					session.send("b.ok");
					break;
				}
				
				new ProcessA(session).run();

				logger.log("FINE");
			}
			catch (TimeExpiredException e) {
				Session2<TST> session = waitForSession(pbl);
				session.send("a");
			}
			
		
		}
		catch (ContractException e) {
			e.printStackTrace();
		}
	}
	
	private static class ProcessA extends CO2Process {
		
		private static final long serialVersionUID = 1L;
		private final Session2<TST> session;
		
		protected ProcessA(Session2<TST> session) {
			super("ProcessA");
			this.session = session;
		}

		@Override
		public void run() {
			session.send("a");
			
			parallel(() -> {
				session.send("a1");
			});
			
			parallel(() -> {
				session.send("a2");
			});

			parallel(() -> {
				session.send("a3");
			});
			
			session.send("b");
		}
		
	}
}
