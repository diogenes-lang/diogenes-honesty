package it.unica.co2.examples;



import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.honesty.HonestyChecker;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ContractWrapper;
import it.unica.co2.model.contract.Recursion;
import it.unica.co2.model.contract.Sort;
import it.unica.co2.model.process.CO2Process;
import it.unica.co2.model.process.Participant;
import co2api.ContractException;
import co2api.Message;
import co2api.Public;
import co2api.TST;
import co2api.TimeExpiredException;

/*
 * auto-generated by co2-plugin
 * creation date: 13-08-2015 18:01:56
 */

@SuppressWarnings("unused")
public class test {
	
	private static String username = "test@co2-plugin.com";
	private static String password = "test";
	
	
	/*
	 * contracts declaration
	 */
	private static ContractWrapper C = wrapper();
	private static ContractWrapper C1 = wrapper();
	
	/*
	 * contracts initialization
	 */
	static {
		C.setContract(externalSum().add("a", Sort.INT).add("b", Sort.INT));
		C1.setContract(internalSum().add("a", Sort.INT).add("b", Sort.INT));
	}
	
	public static class P extends Participant {
		
		private static final long serialVersionUID = 1L;
		
		public P() {
			super(username, password);
		}
		
		@Override
		public void run() {
			Session2<TST> y;
			
			Public<TST> _pbl_y_C = tell(C);
			
			y = waitForSession(_pbl_y_C);
			
			Message _msg_y_0 = y.waitForReceive("a","b");
				
			switch (_msg_y_0.getLabel()) {
				
				case "a":
					int n_0;
					try {
						n_0 = Integer.parseInt(_msg_y_0.getStringValue());
					}
					catch (NumberFormatException | ContractException e) {
						throw new RuntimeException(e);
					}
					break;
				case "b":
					int n_1;
					try {
						n_1 = Integer.parseInt(_msg_y_0.getStringValue());
					}
					catch (NumberFormatException | ContractException e) {
						throw new RuntimeException(e);
					}
					new P1(n_1).run();
					break;
				
				default:
					throw new IllegalStateException("You should not be here");
			}
			
		}
	}
	
	private static class P1 extends Participant {
		
		private static final long serialVersionUID = 1L;
		private Integer a;
		
		public P1(Integer a) {
			super(username, password);
			this.a=a;
		}
		
		@Override
		public void run() {
			boolean n;
			Session2<TST> x;
			
			Public<TST> _pbl_x_C = tell(C);
			
			x = waitForSession(_pbl_x_C);
			
			if (((a+(11*2))>10)) {
				Message _msg_x_1 = x.waitForReceive("a");
					
				switch (_msg_x_1.getLabel()) {
					
					case "a":
						int n1_0;
						try {
							n1_0 = Integer.parseInt(_msg_x_1.getStringValue());
						}
						catch (NumberFormatException | ContractException e) {
							throw new RuntimeException(e);
						}
						break;
					
					default:
						throw new IllegalStateException("You should not be here");
				}
				
			}
			else {
			}
		}
	}
	
	public static void main(String[] args) {
		HonestyChecker.isHonest(P.class);
	}
}
