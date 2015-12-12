package it.unica.co2.examples.voucher;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import co2api.Message;
import co2api.Public;
import co2api.TST;
import co2api.TimeExpiredException;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;

public class VoucherSeller extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "voucher.seller@nicola.com";
	private static String password = "voucher.seller";
	
	public VoucherSeller() {
		super(username, password);
	}

	@Override
	public void run() {
		
		Contract Cvoucher = internalSum()
				.add("reject", externalSum().add("pay"))
				.add("accept", externalSum().add("voucher"));
		
		Contract CB = externalSum()
				.add("clickpay", externalSum().add("pay"))
				.add("clickvoucher", Cvoucher);
		
		Contract CV = externalSum().add("ok").add("no");
		
		
		
		Session2<TST> sessionB = tellAndWait(CB);
		
		Message msg = sessionB.waitForReceive("clickpay", "clickvoucher");
		
		switch(msg.getLabel()) {
		
		case "clickpay":
			sessionB.waitForReceive("pay");
			break;
			
		case "clickvoucher":
			Public<TST> pblV = tell(CV);
			
			try {
				Session2<TST> sessionV = waitForSession(pblV, 10000);
				processCall(Q.class,sessionB, sessionV);
			}
			catch (TimeExpiredException e) {
				
				parallel(()->{
					sessionB.sendIfAllowed("reject");
					sessionB.waitForReceive("pay");
				});
				
				Session2<TST> sessionV = waitForSession(pblV);
				sessionV.waitForReceive("ok","no");
			}
			
			break;
		}
		
	}

	public static class Q extends CO2Process {

		private static final long serialVersionUID = 1L;
		private Session2<TST> sessionB;
		private Session2<TST> sessionV;
		
		protected Q(Session2<TST> sessionB, Session2<TST> sessionV) {
			super("Q");
			this.sessionB = sessionB;
			this.sessionV = sessionV;
		}

		@Override
		public void run() {
			
			Message msg;
			try {
				msg = sessionV.waitForReceive(10000, "no", "ok");

				switch(msg.getLabel()) {
				
				case "ok":
					sessionB.sendIfAllowed("accept");
					sessionB.waitForReceive("voucher");
					break;
					
				case "no":
					sessionB.sendIfAllowed("reject");
					sessionB.waitForReceive("pay");
					break;
				}
			}
			catch (TimeExpiredException e) {
				
				parallel(()->{
					sessionB.sendIfAllowed("reject");
					sessionB.waitForReceive("pay");
				});
				
				sessionV.waitForReceive("ok", "no");
			}
			
			
		}
	}
	
	
	
	public static void main(String[] args) {
		new VoucherSeller().run();
	}
}
