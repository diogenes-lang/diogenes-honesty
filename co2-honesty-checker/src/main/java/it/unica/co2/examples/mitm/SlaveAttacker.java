package it.unica.co2.examples.mitm;

import static it.unica.co2.api.contract.utils.ContractFactory.def;
import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;

import java.util.Base64;

import co2api.ContractException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.api.process.Participant;
import it.unica.co2.api.process.SkipMethod;

public class SlaveAttacker extends Participant{

	private static final long serialVersionUID = 1L;

	protected SlaveAttacker(String username, String password) {
		super(username, password);
	}
	
	@Override
	public void run() {

		ContractDefinition c = def("c").setContract(
				externalSum()
				.add("pair", Sort.string("plaintext,ciphertext"),
						externalSum().add("range", Sort.string("0-10"),
								internalSum()
								.add("result", Sort.string())
								.add("abort")))
				.add("abort"));
		
		Public<SessionType> pbl = tell(c);
		Session<SessionType> u = pbl.waitForSession();

		try {
		
			Message msg = u.waitForReceive("pair", "abort");
			
			switch (msg.getLabel()) {
			case "abort":
				System.out.println("abort received");
				return;
			}
			
			String pairString = msg.getStringValue();

			msg = u.waitForReceive("range");
			String rangeString = msg.getStringValue();
			
			if (isInputOk(pairString, rangeString)) {
				
				String[] pair = pairString.split(",");
				String plaintext = pair[0];
				String ciphertext = pair[1];
				
				String[] range = rangeString.split("-");
				int min = Integer.valueOf(range[0]);
				int max = Integer.valueOf(range[1]);
			
				if (!isFeasible(max-min)) {
					u.sendIfAllowed("abort");
				}
				else {
					String result = getAllPairs(plaintext, ciphertext, min, max);
					u.sendIfAllowed("result", result);				
				}
			}
			else {
				u.sendIfAllowed("abort");
			}
		}
		catch (ContractException e) {
		}

	}
	
	@SkipMethod
	private boolean isInputOk(String pair, String range) {
		return pair.contains(",") && range.contains("-");
	}
	
	@SkipMethod
	public boolean isFeasible(int n) {
		return n>0 && n<=1024;
	}
	
	@SkipMethod
	private String getAllPairs(String plaintext, String ciphertext, int min, int max) {
		
		StringBuilder sb = new StringBuilder();
		
		for (int i=min; i<=max; i++) {
			
			if (i>min)
				sb.append(",");
			
			String key = AESUtils.getKey(i);
			
//			logger.log("key: "+key);
//			logger.log("plaintext: "+plaintext);
//			logger.log("ciphertext: "+ciphertext);

			sb
			.append(key)
			.append("-")
			.append(AESUtils.encrypt(key, plaintext))
			.append("-")
			.append(AESUtils.decrypt(key, ciphertext));
		}
		
		byte[] encodedResult = Base64.getEncoder().encode(sb.toString().getBytes());
		
		return new String(encodedResult);
//		return sb.toString();
	}
	
	public static void main(String[] args) {
		
		Thread t1;
		
		System.out.println("starting attacker 1");
		t1 = new Thread(new SlaveAttacker("mitm-slave@nicola.com", "mitm-slave"));
		t1.start();
		
		System.out.println("starting attacker 2");
		t1 = new Thread(new SlaveAttacker("mitm-slave@nicola.com", "mitm-slave"));
		t1.start();
		
		System.out.println("starting attacker 3");
		t1 = new Thread(new SlaveAttacker("mitm-slave@nicola.com", "mitm-slave"));
		t1.start();
		
//		String key = "00000000000000000000001001111001";
//		String plain = "Hello world!";
//		String cipher = "iTXjkRbkqwQ0rOU/rIMOZ/Y/hunhWKx3Yy9DwqCwNMk=";
//		
//		assert AESUtils.encrypt(key, plain).equals("Enm+Q4CcF5Pn38wI+fP7Kg==");
//		
//		System.out.println(AESUtils.decrypt(key, cipher));
	}
}