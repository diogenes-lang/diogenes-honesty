package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.Message;
import co2api.TST;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;

public class ProcessComposition2Example {

	public static class ProcessA extends CO2Process {

		private static final long serialVersionUID = 1L;

		private final Session2<TST> session;
		
		protected ProcessA(Session2<TST> session) {
			super("ProcessA");
			this.session = session;
		}

		@Override
		public void run() {
			session.waitForReceive("a", "b");
			
			new ProcessA(session).run();
		}
	}
	
	public static class Composed2Process extends Participant {

		private static final long serialVersionUID = 1L;
		
		private static String username = "alice@test.com";
		private static String password = "alice";

		public Composed2Process() {
			super(username, password);
		}

		@Override
		public void run() {
			
			Recursion rec = recursion("x");
			Contract recBody = externalSum().add("a", rec).add("b", rec);
			rec.setContract(recBody);
			
			Contract C = externalSum().add("request", rec);
			
			Session2<TST> session = tellAndWait(C);

			Message msg = null;
			
			msg = session.waitForReceive("request");

			@SuppressWarnings("unused")
			int n;
			try {
				n = Integer.valueOf(msg.getStringValue());
			}
			catch (Throwable e) {
				n=0;
			}
			
			new ProcessA(session).run();
				
//			session.send("end");		//JPF fails
			
		}

	}
	
	
	public static void main(String[] args) throws ContractException {
		new Composed2Process().run();
	}
}
