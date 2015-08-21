package it.unica.co2.examples.voucher;

import static it.unica.co2.model.ContractFactory.*;

import co2api.Message;
import co2api.TST;
import it.unica.co2.api.Session2;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.process.Participant;

public class VoucherBuyer extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "voucher.buyer@nicola.com";
	private static String password = "voucher.buyer";
	
	public VoucherBuyer() {
		super(username, password);
	}

	@Override
	public void run() {
		
		Contract Cvoucher = externalSum()
				.add("reject", internalSum().add("pay"))
				.add("accept", internalSum().add("voucher"));
		
		Contract CB = internalSum()
				.add("clickpay", internalSum().add("pay"))
				.add("clickvoucher", Cvoucher);
		
		
		Session2<TST> session = tellAndWait(CB);
		
		if (useVoucher()) {
			
			session.send("clickvoucher");

			Message msg = session.waitForReceive("reject", "accept");
			
			switch(msg.getLabel()) {
			
			case "reject":
				session.send("pay");
				break;
				
			case "accept":
				session.send("voucher");
				break;
			}
			
		}
		else {
			session.send("clickpay");
			session.send("pay");
		}
		
		
		
	}

	private boolean useVoucher() {
		return true; //new Random().nextBoolean();
	}
	
	public static void main(String[] args) {
		new VoucherBuyer().run();
	}
}
