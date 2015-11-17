package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.TST;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.process.Participant;


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
			
			Contract C1 = internalSum().add("a", externalSum().add("a"));
			Contract C2 = internalSum().add("b", externalSum().add("a"));
			Contract C3 = internalSum().add("c", externalSum().add("a"));
			
			parallel(()-> {
				Session2<TST> session = tellAndWait(C1);
				session.send("a");
				session.waitForReceive("a","b");
			});
			
			parallel(()-> {
				Session2<TST> session = tellAndWait(C2);
				session.send("b");
				session.waitForReceive("a");
			});
			
			parallel(()-> {
				Session2<TST> session = tellAndWait(C3);
				session.send("c");
				session.waitForReceive("a");
			});
		}

	}
	
	public static void main(String[] args) {
		new ParallelProcess().run();
	}
	
}
