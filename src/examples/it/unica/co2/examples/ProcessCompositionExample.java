package it.unica.co2.examples;

import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
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
			
			Contract C = externalSum().add("request", internalSum().add("a").add("b"));
			
			Session2<TST> session = tell(C);

			Message msg = session.waitForReceive("request");
			
			int n;
			try {
				n = Integer.valueOf(msg.getStringValue());
			}
			catch (Throwable e) {
				n=0;
			}
			
			if (n>0)
				new ProcessA(session).run();
			else
				new ProcessB(session).run();
		}

		@Override
		protected String getUsername() {
			return username;
		}
	}
	
	
	public static void main(String[] args) throws ContractException {
		new ComposedProcess().run();
	}
}
