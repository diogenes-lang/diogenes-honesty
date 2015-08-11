package it.unica.co2.examples.insuredsale;

import static it.unica.co2.model.ContractFactory.*;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.Participant;
import co2api.ContractException;
import co2api.Message;
import co2api.TST;


public class Insurance extends Participant {

	private static final long serialVersionUID = 1L;

	public Insurance() {
		super("insurance@nicola.com", "insurance");
	}

	@Override
	public void run() {

		Contract CI = externalSum().add("reqi", internalSum().add("oki").add("aborti"));
		
		Session2<TST> session = tellAndWait(CI);
		
		Message msg = session.waitForReceive("reqi");
		
		Integer amount;
		try {
			amount = Integer.parseInt(msg.getStringValue());
		}
		catch (NumberFormatException | ContractException e) {
			throw new RuntimeException(e);
		}
		
		if (isInsurable(amount)) {
			session.send("oki");
		}
		else {
			session.send("aborti");
		}
	}

	private boolean isInsurable(Integer amount) {
		return amount < 100;
	}
	
	public static void main(String[] args) {
		new Insurance().run();
	}
}
