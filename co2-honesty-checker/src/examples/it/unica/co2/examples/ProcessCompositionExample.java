package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.Message;
import co2api.Session;
import co2api.TST;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.process.Participant;

public class ProcessCompositionExample {

	private static String username = "alice@test.com";
	private static String password = "alice";

	public static class ProcessA extends Participant {

		private static final long serialVersionUID = 1L;

		private final Session<TST> session;
		
		protected ProcessA(Session<TST> session) {
			super(username, password);
			this.session = session;
		}

		@Override
		public void run() {
			session.sendIfAllowed("a");
			
			processCall(ProcessA.class, session);
		}
	}
	
	public static class ProcessB extends Participant {

		private static final long serialVersionUID = 1L;

		private final Session<TST> session;
		
		protected ProcessB(Session<TST> session) {
			super(username, password);
			this.session = session;
		}

		@Override
		public void run() {
			session.sendIfAllowed("b");
		}
	}
	
	public static class ComposedProcess extends Participant {

		private static final long serialVersionUID = 1L;
		
		public ComposedProcess() {
			super(username, password);
		}

		@Override
		public void run() {
			
			Recursion rec = recursion("x");
			rec.setContract(internalSum().add("a", recRef(rec)));
			Contract C = externalSum().add("request", internalSum().add("a", rec).add("b"));
			
			Session<TST> session = tellAndWait(C);

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
				processCall(ProcessA.class, session);
			else
				processCall(ProcessB.class, session);
				
//			session.sendIfAllowed("end");		//JPF fails
			
		}

	}
	
	
	public static void main(String[] args) throws ContractException {
		new ComposedProcess().run();
	}
}
