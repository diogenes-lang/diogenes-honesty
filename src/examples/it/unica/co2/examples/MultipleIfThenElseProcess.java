package it.unica.co2.examples;


import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.Participant;
import co2api.ContractException;
import co2api.TST;

public class MultipleIfThenElseProcess extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "alice@test.com";
	private static String password = "alice";

	public MultipleIfThenElseProcess() {
		super(username, password);
	}

	@Override
	protected String getUsername() {
		return username;
	}
	
	public static void main(String[] args) throws ContractException {
		new MultipleIfThenElseProcess().run();
	}
	
	@Override
	public void run() {
		try {
			
			logger.log("START");

			Contract A = internalSum().add("start",
							internalSum()
								.add("then", 
										internalSum()
											.add("then.1", 
													internalSum().add("end")
											)
											.add("else.1", 
													internalSum().add("end")
											)
								)
								.add("else", 
										internalSum().add("end")
								)
						);
			
			Session2<TST> session = tell(A);
			
			session.send("start");
			
			Integer x = new Integer(10);
			Integer y = 10;
			
			if (x>5) {
				session.send("then");
				
				if (y>5) {
					session.send("then.1");
				}
				else {
					session.send("else.1");
				}
				
			}
			else {
				session.send("else");
			}
			
			session.send("end");
			
			logger.log("END");
		
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
