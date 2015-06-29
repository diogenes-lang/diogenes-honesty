package it.unica.co2.examples;


import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.Participant;
import co2api.ContractException;
import co2api.Message;
import co2api.TST;

public class APIExampleProcess extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "alice@test.com";
	private static String password = "alice";

	public APIExampleProcess() {
		super(username, password);
	}

	@Override
	protected String getUsername() {
		return username;
	}
	
	public static void main(String[] args) throws ContractException {
		new APIExampleProcess().run();
	}
	
	@Override
	public void run() {
		try {
			
			Contract A = 
					internalSum()
					.add("a")
					.add("b", externalSum().add("a").add("b").add("c"))
			;
			
			logger.log("tell");
			Session2<TST> session = tell(A);
			
			logger.log("sending a!");
			session.send("b");
			
			logger.log("receiving message");
			Message msg = session.waitForReceive("a","b"/*,"c"*/);
			
			logger.log("received message: "+msg.getLabel()+" "+msg.getStringValue());
			
			switch (msg.getLabel()) {
			
			case "a": 
				logger.log("received a?");
				session.send("a.ok");
				break;
			
			case "b":
				logger.log("received b?");
				session.send("b.ok");
				break;
			}
			
			session.send("end");
			logger.log("FINE");
		
		}
		catch (ContractException e) {
			e.printStackTrace();
		}
	}
}
