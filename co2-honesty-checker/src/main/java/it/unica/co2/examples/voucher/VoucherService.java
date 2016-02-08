package it.unica.co2.examples.voucher;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.Session;
import co2api.TST;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.process.Participant;

public class VoucherService extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "voucher.service@nicola.com";
	private static String password = "voucher.service";
	
	public VoucherService() {
		super(username, password);
	}

	@Override
	public void run() {
		
		Contract C = internalSum().add("ok").add("no");
		
		Session<TST> session = tellAndWait(C);
		
		
		if (isServiceAvailable()) {
			session.sendIfAllowed("ok");
		}
		else {
			session.sendIfAllowed("no");
		}
		
	}
	
	private boolean isServiceAvailable() {
		return true;//new Random().nextBoolean();
	}

	public static void main(String[] args) {
		new VoucherService().run();
	}
}
