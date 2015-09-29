package it.unica.co2.examples;

import static it.unica.co2.api.contract.newapi.ContractFactory.*;

import co2api.ContractException;
import co2api.Public;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.newapi.Contract;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;
import it.unica.co2.honesty.HonestyChecker;

public class APIExampleProcess extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "alice@test.com";
	private static String password = "alice";

	public APIExampleProcess() {
		super(username, password);
	}

	public static void main(String[] args) throws ContractException {
		HonestyChecker.isHonest(APIExampleProcess.class);
//		new APIExampleProcess().run();
	}
	
	@Override
	public void run() {
		
			
			Contract C = 
					externalSum()
					.add("a")
					.add("b")
			;
			
			Contract D = 
					internalSum()
//					.add("a", internalSum().add("a"))
					.add("hello")
			;
			
			Session2<TST> sessionC = tellAndWait(C);
			
			
			Public<TST> pblD = tell(D);
			
			try {
				Session2<TST> sessionD = waitForSession(pblD, 10_000);
				sessionD.send("hello");
				
				sessionC.waitForReceive("a", "b");
			}
			catch (TimeExpiredException e1) {
				
				parallel(()->{
					Session2<TST> sessionD1 = waitForSession(pblD);
					sessionD1.send("hello");
				});
				
				parallel(()->{
					sessionC.waitForReceive("a", "b");
				});
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
