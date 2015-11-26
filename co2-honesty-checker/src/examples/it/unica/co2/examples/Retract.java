package it.unica.co2.examples;



import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractExpiredException;
import co2api.Public;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.api.process.Participant;

public class Retract {
	
	private static String username = "test@co2-plugin.com";
	private static String password = "test";
	
	
	/*
	 * contracts declaration
	 */
	private static ContractDefinition _tell_contr_0 = def("_tell_contr_0");
	
	/*
	 * contracts initialization
	 */
	static {
		_tell_contr_0.setContract(internalSum().add("a", Sort.integer()).add("b", Sort.unit()));
	}
	
	public static class P extends Participant {
		
		private static final long serialVersionUID = 1L;
		
		public P() {
			super(username, password);
		}
		
		@Override
		public void run() {
			
			Session2<TST> sessionY = tellAndWait(internalSum().add("c").add("d"));
			
			Public<TST> pblX = tell(_tell_contr_0, 10000);
			
			try {
				Session2<TST> sessionX = waitForSession(pblX, 10000);
				
				sessionX.send("a");
				sessionY.send("c");
				
			}
			catch(ContractExpiredException e) {
				//retract x
				
				sessionY.send("d");
			}
			catch (TimeExpiredException e) {
				sessionY.send("c");
			}
		}
	}
	
	public static void main(String[] args) {
		new P().run();
	}
}
