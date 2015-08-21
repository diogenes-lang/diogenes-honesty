package it.unica.co2.examples;

import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.CO2Process;
import it.unica.co2.model.process.Participant;
import co2api.TST;


public class ParallelProcessExample {

	public static class ParallelProcess extends Participant {
		
		private static final long serialVersionUID = 1L;
		
		private static String username = "bob@test.com";
		private static String password = "bob";
		
		public ParallelProcess() {
			super(username, password);
		}

		@Override
		public void run() {
			
			Contract C1 = externalSum().add("a").add("b").add("c");
			Contract C = internalSum().add("a", C1).add("b",C1).add("c",C1);
			
			Session2<TST> session = tellAndWait(C);
			
			
			Runnable rbl = () -> {
				session.waitForReceive("a","b","c");
			};
			
			
			parallel(new ProcessA(session,rbl));
			
			parallel(new ProcessB(session,rbl));
			
			parallel(()-> {
				session.send("c");
				rbl.run();
			});
		}

	}
	
	private static class ProcessA extends CO2Process {
		
		private static final long serialVersionUID = 1L;
		private final Session2<TST> session;
		private final Runnable rbl;
		
		protected ProcessA(Session2<TST> session, Runnable rbl) {
			super("ProcessA");
			this.session = session;
			this.rbl = rbl;
		}

		@Override
		public void run() {
			session.send("a");
			rbl.run();
		}
		
	}
	
	private static class ProcessB extends CO2Process {
		
		private static final long serialVersionUID = 1L;
		private final Session2<TST> session;
		private final Runnable rbl;
		
		protected ProcessB(Session2<TST> session, Runnable rbl) {
			super("ProcessB");
			this.session = session;
			this.rbl = rbl;
		}

		@Override
		public void run() {
			session.send("b");
			rbl.run();
		}
		
	}
	
	public static void main(String[] args) {
		new ParallelProcess().run();
	}
	
}
