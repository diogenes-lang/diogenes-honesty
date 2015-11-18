package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

import co2api.ContractException;
import co2api.Message;
import co2api.TST;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.InputPatterns;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.api.process.Participant;
import it.unica.co2.util.Xeger;

public class AttackerNode extends Participant{

	private static final long serialVersionUID = 1L;

	protected AttackerNode(String username, String password) {
		super(username, password);
	}
	
	@Override
	public void run() {

		@InputPatterns(value={
			"pair="+InputPatterns.base64+","+InputPatterns.base64,
			"range="+InputPatterns.intNumber+"-"+InputPatterns.intNumber
		})
		ContractDefinition c = def("c").setContract(
				externalSum().add("pair", Sort.STRING,
						externalSum().add("range", Sort.STRING,
								internalSum()
								.add("result", Sort.STRING))
								.add("abort")));
		
		Session2<TST> u = tellAndWait(c);
		
		try {
		
			Message msg = u.waitForReceive("pair");
			String pairString = msg.getStringValue();

			msg = u.waitForReceive("range");
			String rangeString = msg.getStringValue();
			
			if (isInputOk(pairString, rangeString)) {	// concretely evaluated by JPF
				
				String[] pair = pairString.split(",");
				String plaintext = pair[0];
				String ciphertext = pair[1];
				
				
				String[] range = rangeString.split("-");
				int min = Integer.valueOf(range[0]);
				int max = Integer.valueOf(range[1]);
			
				ifThenElse(
					() -> !isFeasible(max-min),
					() -> {
						u.send("abort");
					},
					() -> {
						String result = getAllPairs(plaintext, ciphertext, min, max);
						u.send("result", result);				
					}
				);
			}
			else {
				u.send("abort");
			}

			
		}
		catch (ContractException e) {
//			
//			parallel(()->{
//				u.send("abort");
//			});
//			
//			parallel(()->{
//				u.send("result");
//			});
		}

	}
	
	
	
	private boolean isInputOk(String pair, String range) {
		return pair.contains(",") && range.contains("-");
	}
	
	private boolean isFeasible(int n) {
		return n>0 && n<=1024;
	}
	
	private String getAllPairs(String plaintext, String ciphertext, int min, int max) {
		
		StringBuilder sb = new StringBuilder();
		
		for (int i=min; i<=max; i++) {
			if (i>min)
				sb.append("");
			
			String key = AESUtils.getKey(i);
			
			sb
			.append(key)
			.append("-")
			.append(AESUtils.encrypt(key, plaintext))
			.append("-")
			.append(AESUtils.decrypt(key, ciphertext));
		}
		
		byte[] encodedResult = Base64.getEncoder().encode(sb.toString().getBytes());
		
		return new String(encodedResult);
		
	}
	
	public static void main(String[] args) {
		String pattern = "[a-z0-9A-Z]{22}==-[0-9A-Za-z]{22}==";
		
		System.out.println(Xeger.generate(pattern));
		
//		HonestyChecker.isHonest(AttackerNode.class, "", "");
		System.out.println(AESUtils.encrypt(AESUtils.getKey(0), "1"));
		System.out.println(AESUtils.encrypt(AESUtils.getKey(0), "0"));
		System.out.println(AESUtils.encrypt(AESUtils.getKey(0), "a"));
	}
}

class AESUtils {
	
	static final Decoder base64Decoder = Base64.getDecoder();
	static final Encoder base64Encoder = Base64.getEncoder();
	
	public static String getKey(int n) {
		return StringUtils.leftPad(Integer.toBinaryString(n), 32, '0');
	}
	
    public static String encrypt(String key, String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec("0123456789abcdef".getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            
            String encryptedString = base64Encoder.encodeToString(encrypted);

            return encryptedString;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static String decrypt(String key, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec("0123456789abcdef".getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(base64Decoder.decode(encrypted));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

}