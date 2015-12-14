package it.unica.co2.examples.blackjack;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;


public class Dealer extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static final String username = "alice@test.com";
	private static final String password = "alice";
	
	public Dealer() {
		super(username, password);
	}

	
	
	
	@Override
	public void run() {

		/*
		 * player's contract
		 */
		Recursion playerContract = recursion("x");
		
		Contract hit = internalSum().add("card", recRef(playerContract)).add("lose").add("abort");
		Contract end = internalSum().add("win").add("lose").add("abort");
		
		playerContract.setContract(externalSum().add("hit", hit).add("stand", end));
		
		/*
		 * deck service's contract
		 */
		Recursion dealerServiceContract = recursion("y");
		
		dealerServiceContract.setContract(internalSum().add("next", externalSum().add("card", recRef(dealerServiceContract))).add("abort"));

		/*
		 * PROCESS
		 */
		Session<TST> sessionD = tellAndWait(dealerServiceContract);
		
		Public<TST> pblP = tell(playerContract);
		
		Session<TST> sessionP;
		
		try {
			sessionP = pblP.waitForSession(10000);
			processCall(Pplay.class, sessionP, sessionD, 0);
		}
		catch (TimeExpiredException e) {
			sessionD.sendIfAllowed("abort");
			
			sessionP = pblP.waitForSession();
			
			//you are culpable in sessionP
			sessionP.waitForReceive("hit", "stand");
			sessionP.sendIfAllowed("abort");
			
			//you are honest in all sessions
		}
		
	}

	
	private static class Pplay extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session<TST> sessionP;
		private final Session<TST> sessionD;
		private final Integer nP;
		
		protected Pplay(Session<TST> sessionP, Session<TST> sessionD, Integer nP) {
			super("Pplay");
			this.sessionP = sessionP;
			this.sessionD = sessionD;
			this.nP = nP;
		}

		@Override
		public void run() {
			
			Message msg;
			
			try {
				msg = sessionP.waitForReceive(10000, "hit", "stand");
				
				switch (msg.getLabel()) {
				
				case "hit":
					logger.log("hit received");
					sessionD.sendIfAllowed("next");
					processCall(Pdeck.class, sessionP, sessionD, nP);
					break;
					
				case "stand":
					logger.log("stand received");
					processCall(Qstand.class, sessionP, sessionD, nP, 0);
					break;
				}
			}
			catch (TimeExpiredException e) {
				sessionD.sendIfAllowed("abort");
				
				//you are culpable in sessionP
				sessionP.waitForReceive("hit", "stand");
				sessionP.sendIfAllowed("abort");
				
				//you are honest
			}
			
		}

	}
	
	private static class Pdeck extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session<TST> sessionP;
		private final Session<TST> sessionD;
		private final Integer nP;
		
		protected Pdeck(Session<TST> sessionP, Session<TST> sessionD, Integer nP) {
			super("Pdeck");
			this.sessionP = sessionP;
			this.sessionD = sessionD;
			this.nP = nP;
		}

		@Override
		public void run() {
			
			try {
				Message msg = sessionD.waitForReceive(10000, "card");
				
				try {
					Integer n = Integer.parseInt(msg.getStringValue());
					
					logger.log("received card "+n);
					processCall(Pcard.class, sessionP, sessionD, nP+n, n);
				}
				catch (NumberFormatException | ContractException e) {
					throw new RuntimeException(e);
				}
			}
			catch (TimeExpiredException e) {
				sessionP.sendIfAllowed("abort");
				
				sessionD.waitForReceive("card");
				sessionD.sendIfAllowed("abort");
			}
			
		}

	}
	
	private static class Qstand extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session<TST> sessionP;
		private final Session<TST> sessionD;
		private final Integer nP;
		private final Integer nD;
		
		protected Qstand(Session<TST> sessionP, Session<TST> sessionD, Integer nP, Integer nD) {
			super("P");
			this.sessionP = sessionP;
			this.sessionD = sessionD;
			this.nP = nP;
			this.nD = nD;
		}

		@Override
		public void run() {
			
			if (nP<=21) {
				sessionD.sendIfAllowed("next");
				processCall(Qdeck.class, sessionP, sessionD, nP, nD);
			}
			else {
				sessionP.sendIfAllowed("win");
				sessionD.sendIfAllowed("abort");
			}
			
		}

	}
	
	private static class Pcard extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session<TST> sessionP;
		private final Session<TST> sessionD;
		private final Integer nP;
		private final Integer n;
		
		protected Pcard(Session<TST> sessionP, Session<TST> sessionD, Integer nP, Integer n) {
			super("P");
			this.sessionP = sessionP;
			this.sessionD = sessionD;
			this.nP = nP;
			this.n = n;
		}

		@Override
		public void run() {
			
			if (nP<=21) {
				sessionP.sendIfAllowed("card", n);
				processCall(Pplay.class, sessionP, sessionD, nP);
			}
			else {
				sessionP.sendIfAllowed("lose");
				sessionD.sendIfAllowed("abort");
			}
			
		}

	}
	
	private static class Qdeck extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session<TST> sessionP;
		private final Session<TST> sessionD;
		private final Integer nP;
		private final Integer nD;
		
		protected Qdeck(Session<TST> sessionP, Session<TST> sessionD, Integer nP, Integer nD) {
			super("Qdeck");
			this.sessionP = sessionP;
			this.sessionD = sessionD;
			this.nP = nP;
			this.nD = nD;
		}

		@Override
		public void run() {
			
			try {
				Message msg = sessionD.waitForReceive(10000, "card");
				
				Integer n;
				try {
					n = Integer.parseInt(msg.getStringValue());

					logger.log("received card "+n);
					processCall(Qcard.class, sessionP, sessionD, nP, nD+n);
				}
				catch (NumberFormatException | ContractException e) {
					throw new RuntimeException(e);
				}
				
			}
			catch (TimeExpiredException e) {
				sessionP.sendIfAllowed("abort");
				
				sessionD.waitForReceive("card");
				sessionD.sendIfAllowed("abort");
			}
			
		}

	}

	private static class Qcard extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session<TST> sessionP;
		private final Session<TST> sessionD;
		private final Integer nP;
		private final Integer nD;
		
		protected Qcard(Session<TST> sessionP, Session<TST> sessionD, Integer nP, Integer nD) {
			super("Qcard");
			this.sessionP = sessionP;
			this.sessionD = sessionD;
			this.nP = nP;
			this.nD = nD;
		}

		@Override
		public void run() {
			
			if (nD<nP) {
				processCall(Qstand.class, sessionP, sessionD, nP, nD);
			}
			else {
				sessionP.sendIfAllowed("lose");
				sessionD.sendIfAllowed("abort");
			}
			
		}

	}
	
	public static void main(String args[]) {
		new Dealer().run();
	}
}
