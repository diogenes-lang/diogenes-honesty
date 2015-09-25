package it.unica.co2.examples.blackjack;

import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;

import static it.unica.co2.api.contract.ContractFactory.*;

import co2api.ContractException;
import co2api.Message;
import co2api.Public;
import co2api.TST;
import co2api.TimeExpiredException;


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
		Recursion playerContract = recursion();
		
		Contract hit = internalSum().add("card", playerContract).add("lose").add("abort");
		Contract end = internalSum().add("win").add("lose").add("abort");
		
		playerContract.setContract(externalSum().add("hit", hit).add("stand", end));
		
		/*
		 * deck service's contract
		 */
		Recursion dealerServiceContract = recursion();
		
		dealerServiceContract.setContract(internalSum().add("next", externalSum().add("card", dealerServiceContract)).add("abort"));

		/*
		 * PROCESS
		 */
		Session2<TST> sessionD = tellAndWait(dealerServiceContract);
		
		Public<TST> pblP = tell(playerContract);
		
		Session2<TST> sessionP;
		
		try {
			sessionP = waitForSession(pblP, 10000);
			new Pplay(sessionP, sessionD, 0).run();
		}
		catch (TimeExpiredException e) {
			sessionD.send("abort");
			
			sessionP = waitForSession(pblP);
			
			//you are culpable in sessionP
			sessionP.waitForReceive("hit", "stand");
			sessionP.send("abort");
			
			//you are honest in all sessions
		}
		
	}

	
	private static class Pplay extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session2<TST> sessionP;
		private final Session2<TST> sessionD;
		private final Integer nP;
		
		protected Pplay(Session2<TST> sessionP, Session2<TST> sessionD, Integer nP) {
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
					sessionD.send("next");
					new Pdeck(sessionP, sessionD, nP).run();
					break;
					
				case "stand":
					logger.log("stand received");
					new Qstand(sessionP, sessionD, nP, 0).run();
					break;
				}
			}
			catch (TimeExpiredException e) {
				sessionD.send("abort");
				
				//you are culpable in sessionP
				sessionP.waitForReceive("hit", "stand");
				sessionP.send("abort");
				
				//you are honest
			}
			
		}

	}
	
	private static class Pdeck extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session2<TST> sessionP;
		private final Session2<TST> sessionD;
		private final Integer nP;
		
		protected Pdeck(Session2<TST> sessionP, Session2<TST> sessionD, Integer nP) {
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
					new Pcard(sessionP, sessionD, nP+n, n).run();
				}
				catch (NumberFormatException | ContractException e) {
					throw new RuntimeException(e);
				}
			}
			catch (TimeExpiredException e) {
				sessionP.send("abort");
				
				sessionD.waitForReceive("card");
				sessionD.send("abort");
			}
			
		}

	}
	
	private static class Qstand extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session2<TST> sessionP;
		private final Session2<TST> sessionD;
		private final Integer nP;
		private final Integer nD;
		
		protected Qstand(Session2<TST> sessionP, Session2<TST> sessionD, Integer nP, Integer nD) {
			super("P");
			this.sessionP = sessionP;
			this.sessionD = sessionD;
			this.nP = nP;
			this.nD = nD;
		}

		@Override
		public void run() {
			
			if (nP<=21) {
				sessionD.send("next");
				new Qdeck(sessionP, sessionD, nP, nD).run();
			}
			else {
				sessionP.send("win");
				sessionD.send("abort");
			}
			
		}

	}
	
	private static class Pcard extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session2<TST> sessionP;
		private final Session2<TST> sessionD;
		private final Integer nP;
		private final Integer n;
		
		protected Pcard(Session2<TST> sessionP, Session2<TST> sessionD, Integer nP, Integer n) {
			super("P");
			this.sessionP = sessionP;
			this.sessionD = sessionD;
			this.nP = nP;
			this.n = n;
		}

		@Override
		public void run() {
			
			if (nP<=21) {
				sessionP.send("card", n);
				new Pplay(sessionP, sessionD, nP).run();
			}
			else {
				sessionP.send("lose");
				sessionD.send("abort");
			}
			
		}

	}
	
	private static class Qdeck extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session2<TST> sessionP;
		private final Session2<TST> sessionD;
		private final Integer nP;
		private final Integer nD;
		
		protected Qdeck(Session2<TST> sessionP, Session2<TST> sessionD, Integer nP, Integer nD) {
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
					new Qcard(sessionP, sessionD, nP, nD+n).run();
				}
				catch (NumberFormatException | ContractException e) {
					throw new RuntimeException(e);
				}
				
			}
			catch (TimeExpiredException e) {
				sessionP.send("abort");
				
				sessionD.waitForReceive("card");
				sessionD.send("abort");
			}
			
		}

	}

	private static class Qcard extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session2<TST> sessionP;
		private final Session2<TST> sessionD;
		private final Integer nP;
		private final Integer nD;
		
		protected Qcard(Session2<TST> sessionP, Session2<TST> sessionD, Integer nP, Integer nD) {
			super("Qcard");
			this.sessionP = sessionP;
			this.sessionD = sessionD;
			this.nP = nP;
			this.nD = nD;
		}

		@Override
		public void run() {
			
			if (nD<nP) {
				new Qstand(sessionP, sessionD, nP, nD).run();
			}
			else {
				sessionP.send("lose");
				sessionD.send("abort");
			}
			
		}

	}
	
	public static void main(String args[]) {
		new Dealer().run();
	}
}
