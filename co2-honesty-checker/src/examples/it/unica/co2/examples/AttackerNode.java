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
import co2api.Public;
import co2api.TST;
import it.unica.co2.api.Session2;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.api.process.Participant;
import it.unica.co2.api.process.SkipMethod;
import it.unica.co2.honesty.HonestyChecker;

public class AttackerNode extends Participant{

	private static final long serialVersionUID = 1L;

	protected AttackerNode(String username, String password) {
		super(username, password);
	}
	
	@Override
	public void run() {

		ContractDefinition c = def("c").setContract(
				externalSum().add("pair", Sort.string("plaintext,ciphertext"),
						externalSum().add("range", Sort.string("0-10"),
								internalSum()
								.add("result", Sort.string())
								.add("abort"))));
		
		Public<TST> pbl = tell(c);
		Session2<TST> u = waitForSession(pbl);

		try {
		
			Message msg = u.waitForReceive("pair");
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
					u.send("abort");
				}
				else {
					String result = getAllPairs(plaintext, ciphertext, min, max);
					u.send("result", result);				
				}
			}
			else {
				u.send("abort");
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

		HonestyChecker.isHonest(AttackerNode.class, "", "");
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