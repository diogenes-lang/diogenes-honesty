package it.unica.co2.examples;


import static it.unica.co2.model.ContractFactory.*;
import static it.unica.co2.util.Facilities.*;
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
			
			_switch(
					msg.getLabel(),
					
					_case("a", () -> {
						logger.log("received a?");
					}),
					
					_case("b", () -> {
						logger.log("received b?");
					})
					,
					
					_case("c", () -> {
						logger.log("received c?");
					})
			);
			
		
			logger.log("FINE");
		
		}
		catch (ContractException e) {
			e.printStackTrace();
		}
	}
}
