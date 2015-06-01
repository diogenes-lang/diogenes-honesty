package it.unica.co2;

import static it.unica.co2.model.CO2Factory.*;
import it.unica.co2.model.Process;
import it.unica.co2.model.contract.Contract;

public class ExampleProcess extends Process {
	
	private Contract contract;
	
	public ExampleProcess() {
		
		contract = internalSum(
				internalAction("a"), 
				internalAction("b")
				);
	}

	@Override
	public void run() {
		
		/*
		 * process {
		 * 		(x) tell x (a! (+) b!) . (t. do x a!) + do x b!. do x a? 
		 * }
		 */
		tell("x", contract);
		sum(
				sumOperand(tauPrefix(), doSendProcess("x", "a")),
				sumOperand(
						doSendPrefix("x", "b"),
						doReceiveProcess("x", "a")
				)
		);
	}
}
