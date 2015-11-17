package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.TST;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.process.Participant;

public class MultipleIfThenElseProcess extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "alice@test.com";
	private static String password = "alice";

	public MultipleIfThenElseProcess() {
		super(username, password);
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
											.add("then_1", 
													internalSum().add("end")
											)
											.add("else_1", 
													internalSum().add("end")
											)
								)
								.add("else", 
										internalSum().add("end")
								)
						);
			
			Session2<TST> session = tellAndWait(A);
			
			session.send("start");
			
			Integer x = new Integer(10);
			Integer y = 10;
			
			ifThenElse(
				() -> x>5,
				() -> {
					session.send("then");
					
					ifThenElse(
						() -> y>5,
						() -> {
							session.send("then_1");
						},
						() -> {
							session.send("else_1");
						}
					);
				},
				() -> {
					session.send("else");
				}
			);
			
			session.send("end");
			
			logger.log("END");
		
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
