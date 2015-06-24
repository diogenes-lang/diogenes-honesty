package it.unica.co2;

import static it.unica.co2.model.ContractFactory.*;
import static it.unica.co2.util.Facilities.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.Participant;
import co2api.ContractException;
import co2api.Message;
import co2api.TST;

public class APIExampleProcess extends Participant {

	private static String username = "alice@test.com";
	private static String password = "alice";

	protected APIExampleProcess() {
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
					.add(internalAction("a"))
					.add(
							internalAction("b").next(
									externalSum().add("a").add("b").add("c")
							)
					);
			
			logger.log("tell");
			Session2<TST> session = tell(A);
			
			logger.log("sending a!");
			session.send("a");
			
			logger.log("receiving message");
			Message msg = session.waitForReceive("a","b"/*,"c"*/);
			
			logger.log("received message: "+msg.getLabel()+" "+msg.getStringValue());
			
			_switch(
					msg.getLabel(),
					
					_case("a", () -> {
						logger.log("received a?");
						session.send("pippo_a");
					}),
					
					_case("b", () -> {
						logger.log("received b?");
						session.send("pippo_b");
					})//,
					
//					_case("c", () -> {
//						logger.log("received c?");
//						session.send("pippo_c");
//					})
			);
			
			logger.log("IF THEN ELSE");
			
			int x = 42;
			
			if (x>0) {
				session.send("then_1");
				
				if (x>0) {
					session.send("then_1_1");
				}
				else {
					session.send("else_1_1");
				}
				
			}
			else {
				session.send("else_1");
			}
			
			session.send("do_end");
			
			logger.log("FINE");
		
		}
		catch (ContractException e) {
			e.printStackTrace();
		}
	}
}
