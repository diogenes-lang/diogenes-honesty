package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.ContractExpiredException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.contract.Contract;
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
//		new APIExampleProcess("").run();
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
			
			
			Public<TST> pblI = tell(C, 5000);
			
			Session<TST> session;
			
			try {
				
				session = pblI.waitForSession();
				session.sendIfAllowed("req", 0);
				
				try {
					Message msg = session.waitForReceive(10000, "ok", "no");
					
					switch (msg.getLabel()) {
					
					case "ok":
						break;
						
					case "no":
						break;
					}
					
				}
				catch (TimeExpiredException e) {
					
					session.sendIfAllowed("abort");
					
//					try {
						session.waitForReceive("ok", "no");
//					}
//					catch (ContractViolationException e1) {
//					}
				}
				
			}
			catch (ContractExpiredException e) {
			}
			
	}
	
}
