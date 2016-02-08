package it.unica.co2.examples;



import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractExpiredException;
import co2api.Public;
import co2api.Session;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.api.process.Participant;

public class Retract {
	
	private static String user = "test@co2-plugin.com";
	private static String pass = "test";
	
	
	/*
	 * contracts declaration
	 */
	private static ContractDefinition _tell_contr_0 = def("C");
	
	/*
	 * contracts initialization
	 */
	static {
		_tell_contr_0.setContract(internalSum().add("a", Sort.integer()).add("b", Sort.unit()));
	}
	
	public static class P extends Participant {
		
		private static final long serialVersionUID = 1L;
		
		public P() {
			super(user, pass);
		}
		
		@Override
		public void run() {
			
			Session<TST> sessionY = tellAndWait(internalSum().add("c").add("d"));
			
			Public<TST> pblX = tell(_tell_contr_0, 10000);
			
			try {
				Session<TST> sessionX = pblX.waitForSession(10000);
				
				sessionX.sendIfAllowed("a");
				sessionY.sendIfAllowed("c");
				
			}
			catch(ContractExpiredException e) {
				//retract x
				
				sessionY.sendIfAllowed("d");
			}
			catch (TimeExpiredException e) {
				sessionY.sendIfAllowed("c");
			}
		}
	}
	
	public static void main(String[] args) {
		new P().run();
	}
}
