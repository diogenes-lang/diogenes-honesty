package it.unica.co2.examples;

import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.CO2Process;
import it.unica.co2.model.process.Participant;
import co2api.TST;


public class ParallelProcessExample {

	
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
		}
		
	}
	
	private static class ProcessB extends CO2Process {
		
		private static final long serialVersionUID = 1L;
		private final Session2<TST> session;
		
		protected ProcessB(Session2<TST> session) {
			super("ProcessB");
			this.session = session;
		}

		@Override
		public void run() {
			session.send("b");
		}
		
	}
	
	public static class ParallelProcess extends Participant {
		
		private static final long serialVersionUID = 1L;
		
		private static String username = "alice@test.com";
		private static String password = "alice";
		
		public ParallelProcess() {
			super(username, password);
		}

		@Override
		public void run() {
			
			Contract C = internalSum().add("a").add("b");
			
			Session2<TST> session = tellAndWait(C);
			
			Thread tA = new Thread(new ProcessA(session));
			Thread tB = new Thread(new ProcessB(session));
			
			tA.start();
			tB.start();
		}

		@Override
		protected String getUsername() {
			return username;
		}
		
	}
	
	public static void main(String[] args) {
		new ParallelProcess().run();
	}
	
}
