package it.unica.co2.examples.mitm;

import static it.unica.co2.api.contract.utils.ContractFactory.def;
import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import co2api.ContractException;
import co2api.ContractExpiredException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;
import it.unica.co2.api.process.SkipMethod;

public class MasterAttacker extends Participant{

	private static final long serialVersionUID = 1L;

	private List<String> pairs = Collections.synchronizedList(new ArrayList<String>());
	
	protected MasterAttacker(String username, String password) {
		super(username, password);
	}
	
	@Override
	public void run() {

		ContractDefinition c = def("c").setContract(
				internalSum()
				.add("pair", Sort.string(),
						internalSum().add("range", Sort.string(),
								externalSum()
								.add("result", Sort.string())
								.add("abort")))
				.add("abort"));
		
		
		
		try {
			Public<SessionType> pblx = tell(c, 10_000);
			Session<SessionType> x = pblx.waitForSession();

			try {
				Public<SessionType> pbly = tell(c, 10_000);
				Session<SessionType> y = pbly.waitForSession();
				
				try {
					Public<SessionType> pblz = tell(c, 10_000);
					Session<SessionType> z = pblz.waitForSession();
					//all session are fused
					
					processCall(Attack.class, pairs, x, y, z);
				}
				catch (ContractExpiredException e) {
					x.sendIfAllowed("abort");
					y.sendIfAllowed("abort");
				}
			}
			catch (ContractExpiredException e) {
				x.sendIfAllowed("abort");
			}
		}
		catch (ContractExpiredException e) {

		}
	}
	
	public static class Attack extends CO2Process {

		private static final long serialVersionUID = 1L;

		private List<String> pairs;
		private final Session<SessionType> x;
		private final Session<SessionType> y;
		private final Session<SessionType> z;

		public Attack(List<String> pairs, Session<SessionType> x, Session<SessionType> y, Session<SessionType> z) {
			this.pairs = pairs;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		@Override
		public void run() {
			
			String key1 = AESUtils.getKey(1234);		//the key we want to crack (not known by the attacker)
			String key2 = AESUtils.getKey(42);			//the key we want to crack (not known by the attacker)
			String plaintext = "Hello world!";
			String ciphertext = AESUtils.encrypt(key2, AESUtils.encrypt(key1, plaintext));
			
			String pair = plaintext + "," + ciphertext;
			
			System.out.println("pair to decrypt: "+pair);
			
			String rangex = "0-500";
			String rangey = "500-1000";
			String rangez = "1000-1500";

			Thread t1 = parallel(()->{
				processCall(CallToSlave.class, pairs, x, pair, rangex);
			});

			Thread t2 = parallel(()->{
				processCall(CallToSlave.class, pairs, y, pair, rangey);
			});

			Thread t3 = parallel(()->{
				processCall(CallToSlave.class, pairs, z, pair, rangez);
			});

			try {
				t1.join();
				t2.join();
				t3.join();
			}
			catch (InterruptedException e) {}
			
			System.out.println("all slaves are terminated");
			
			Pair<String,String> keys = findKeys(pairs);
			checkKeys(plaintext, ciphertext, keys);
		}
		
		@SkipMethod
		private void checkKeys(String plaintext, String ciphertext, Pair<String, String> keys) {
			
			if (keys==null) {
				System.out.println("key not found! Check your implementation");
			}
			else {
				System.out.println("k1: "+ keys.getLeft());
				System.out.println("k2: "+ keys.getRight());
				
				System.out.println("plaintext: "+plaintext);
				System.out.println("ciphertext: "+ciphertext);
				System.out.println("plaintext (computed): "+AESUtils.decrypt(keys.getLeft(), AESUtils.decrypt(keys.getRight(), ciphertext)));
				System.out.println("ciphertext (computed): "+AESUtils.encrypt(keys.getRight(), AESUtils.encrypt(keys.getLeft(), plaintext)));				
			}
		}
		
		@SkipMethod
		private Pair<String, String> findKeys(List<String> pairs) {
			System.out.println("pairs.size(): "+pairs.size());
			
			Decoder decoder = Base64.getDecoder();
			
			Map<String, String> encPlaintextColl = new HashMap<>();
			Map<String, String> decCiphertextColl = new HashMap<>();
			
			for (String p: pairs) {
				p = new String(decoder.decode(p));
//				logger.log("p: "+p);
				
				for (String tripleS : p.split(",")) {
					String[] triple = tripleS.split("-");

					encPlaintextColl.put(triple[1], triple[0]);
					decCiphertextColl.put(triple[2], triple[0]);
				}
			}
			
			for (String encPlain : encPlaintextColl.keySet()) {
				
				if (decCiphertextColl.containsKey(encPlain)) {
					return new ImmutablePair<String, String>(
							encPlaintextColl.get(encPlain),
							decCiphertextColl.get(encPlain));
				}
			}
			
			return null;
		}
	}
	
	
	public static class CallToSlave extends CO2Process {

		private static final long serialVersionUID = 1L;

		private List<String> pairs;
		private final Session<SessionType> x;
		private final String pair;
		private final String range;
		
		public CallToSlave(List<String> pairs, Session<SessionType> x, String pair, String range) {
			super();
			this.pairs = pairs;
			this.x = x;
			this.pair = pair;
			this.range = range;
		}

		@Override
		public void run() {
			x.sendIfAllowed("pair", pair);
			x.sendIfAllowed("range", range);
			
			Message msg = x.waitForReceive("result", "abort");
			
			switch (msg.getLabel()) {
			case "result":
				try {
					String receivedPairs = msg.getStringValue();
					pairs.add(receivedPairs);
				}
				catch (ContractException e) {
					throw new RuntimeException(e);
				}
			}
			
		}
		
	}	
	
	public static void main(String[] args) {
		new MasterAttacker("mitm-master@nicola.com", "mitm-master").run();
	}
}