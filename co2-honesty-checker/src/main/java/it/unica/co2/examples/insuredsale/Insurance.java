package it.unica.co2.examples.insuredsale;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import java.io.FileNotFoundException;

import co2api.ContractException;
import co2api.Message;
import co2api.Session;
import co2api.TST;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.process.Participant;
import it.unica.co2.api.process.SkipMethod;
import it.unica.co2.honesty.HonestyChecker;


public class Insurance extends Participant {

	private static final long serialVersionUID = 1L;

	public Insurance() {
		super("insurance@nicola.com", "insurance");
	}

	@Override
	public void run() {

		Contract CI = externalSum().add("req", internalSum().add("ok").add("abort"));
		
		Session<TST> session = tellAndWait(CI);
		
		Message msg = session.waitForReceive("req");
		
		isVoid(1);
		
		Integer amount;
		try {
			amount = Integer.parseInt(msg.getStringValue());
		}
		catch (NumberFormatException | ContractException e) {
			throw new RuntimeException(e);
		}
		
		try {
			if (isInsurable(amount)) {
				session.sendIfAllowed("ok");
			}
			else {
				session.sendIfAllowed("abort");
			}
		}
		catch (Exception e) {
			session.sendIfAllowed("abort");
		}
	}

	@SkipMethod
	private boolean isInsurable(Integer amount) throws RuntimeException, FileNotFoundException {
		return amount < 100;
	}
	
	@SkipMethod
	private void isVoid(Integer amount) {
	}
	
	public static void main(String[] args) {
		HonestyChecker.isHonest(Insurance.class);
	}
}
