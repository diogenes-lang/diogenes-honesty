package it.unica.co2;

import static it.unica.co2.model.CO2Factory.*;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.prefix.Variable;
import it.unica.co2.model.process.Process;

public class ExampleProcess {
	
	
	public static void main (String[] args) throws Exception {
		
		System.out.println("---------Example1---------");
		new Example1().run();
		
		System.out.println("---------Example2---------");
		new Example2().run();

	}
	
	
	
	private static class Example1 extends Process {

		@Override
		public void run() {
			Contract contract = internalSum(
					internalAction("a"), 
					internalAction("b")
					);
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
	
	private static class Example2 extends Process {

		@Override
		public void run() {
			Contract contract = null;
			/*
			 * process {
			 * 		(x) tell ( c? .(a! (+) b!) ).
			 * 			do x c? n.
			 * 			if n>0
			 * 			then do x a!
			 * 			else do x b!
			 * }
			 */
			
			tell("x", contract);
			
			Variable n = new Variable();
			doReceive("x", "c", n);
			
			ifThenElse(
//					() -> n.getValue()!=null, 
					doSendProcess("x", "a"), 
					doSendProcess("x", "b")
			);
			
		}
		
	}
}
