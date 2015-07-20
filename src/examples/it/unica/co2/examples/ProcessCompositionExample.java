package it.unica.co2.examples;

import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.Recursion;
import it.unica.co2.model.process.CO2Process;
import it.unica.co2.model.process.Participant;
import co2api.ContractException;
import co2api.Message;
import co2api.TST;

public class ProcessCompositionExample {

	public static class ProcessA extends CO2Process {

		private static final long serialVersionUID = 1L;

		private final Session2<TST> session;
		
		protected ProcessA(Session2<TST> session) {
			super("ProcessA");
			this.session = session;
		}

		@Override
		public void run() {
			session.send("a");
			
			new ProcessA(session).run();
		}
	}
	
	public static class ProcessB extends CO2Process {

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
	
	public static class ComposedProcess extends Participant {

		private static final long serialVersionUID = 1L;
		
		private static String username = "alice@test.com";
		private static String password = "alice";

		public ComposedProcess() {
			super(username, password);
		}

		@Override
		public void run() {
			
			Recursion rec = recursion().setContract(internalSum().add("a"));
			Contract C = externalSum().add("request", internalSum().add("a", rec).add("b"));
			
			Session2<TST> session = tellAndWait(C);

			Message msg = null;
			
			msg = session.waitForReceive("request");

			int n;
			try {
				n = Integer.valueOf(msg.getStringValue());
			}
			catch (Throwable e) {
				n=0;
			}
			
			if (n>=0)
				new ProcessA(session).run();
			else
				new ProcessB(session).run();
				
//			session.send("end");		//JPF fails
			
		}

	}
	
	
	public static void main(String[] args) throws ContractException {
		new ComposedProcess().run();
	}
}
