package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.Session;
import co2api.TST;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.process.Participant;
import it.unica.co2.honesty.HonestyChecker;

public class APIExampleProcess2 extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "alice@nicola.com";
	private static String password = "alice";

	public APIExampleProcess2() {
		super(username, password);
	}

	public static void main(String[] args) throws ContractException {
//		new APIExampleProcess2().run();
		HonestyChecker.isHonest(APIExampleProcess2.class);
	}
	
	@Override
	public void run() {
			
			ContractDefinition A = def("A").setContract(
					internalSum()
					.add("a")
					.add("b", externalSum().add("a").add("b").add("c")))
			;
			
			logger.log("tell");
			
			
			Session<TST> s1 = tellAndWait(A);
			Session<TST> s2 = tellAndWait(A);
			Session<TST> s3 = tellAndWait(A);
			
			processCall(abortAll.class, s1, s2, s3);
	}
	
	public static class abortAll extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session<TST> x;
		private Session<TST> y;
		private Session<TST> z;
		
		public abortAll(Session<TST> x, Session<TST> y, Session<TST> z) {
			super(username, password);
			this.x=x;
			this.y=y;
			this.z=z;
		}
		
		@Override
		public void run() {
			parallel(()->{
				x.sendIfAllowed("pippo");
				processCall(abort.class, x);
			});
	
			parallel(()->{
				y.sendIfAllowed("pippo");
				processCall(abort.class, y);
			});
			
			parallel(()->{
				z.sendIfAllowed("pippo");
				processCall(abort.class, z);
			});
		}
	}
	
	public static class abort extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Session<TST> u;
		
		public abort(Session<TST> u) {
			super(username, password);
			this.u=u;
		}
		
		@Override
		public void run() {
			logger.log("ABORT - entered on run method");
			u.sendIfAllowed("abort");
		}
	}
}
