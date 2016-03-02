package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.ContractException;
import co2api.Public;
import co2api.Session;
import co2api.SessionI;
import co2api.TST;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.process.Participant;
import it.unica.co2.honesty.HonestyChecker;

public class QuickTest extends Participant {

	private static final long serialVersionUID = 1L;
	
	public QuickTest() {
		super("alice@test.com", "alice");
	}

	public static void main(String[] args) throws ContractException {
		HonestyChecker.isHonest(QuickTest.class);
		
		System.out.println(new Session<>(null, null) instanceof SessionI);
	}
	
	@Override
	public void run() {
		
			ContractDefinition c = def("c").setContract(
					internalSum()
					.add("a")
					.add("b")
				);
			
			
			Public<TST> pbl = tell(c);
			
			parallel(()-> {
				pbl.sendIfAllowed("a");
			});

			parallel(()-> {
				pbl.sendIfAllowed("b");
			});

	}
	
}
