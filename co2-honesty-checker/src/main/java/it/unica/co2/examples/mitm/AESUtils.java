package it.unica.co2.examples.mitm;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

import it.unica.co2.api.process.SkipMethod;

public class AESUtils {
	
	static final Decoder base64Decoder = Base64.getDecoder();
	static final Encoder base64Encoder = Base64.getEncoder();
	
	public static String getKey(int n) {
		return StringUtils.leftPad(Integer.toBinaryString(n), 32, '0');
	}
	
	@SkipMethod
    public static String encrypt(String key, String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec("0123456789abcdef".getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            
            String encryptedString = base64Encoder.encodeToString(encrypted);

            return encryptedString;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

	@SkipMethod
    public static String decrypt(String key, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec("0123456789abcdef".getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
          
            byte[] original;
			try {
				original = cipher.doFinal(base64Decoder.decode(encrypted));
			}
			catch (BadPaddingException e) {
				return null;
			}
			
			String plaintext = new String(original);
			
			if (Charset.forName("US-ASCII").newEncoder().canEncode(plaintext)) {
				return plaintext;
			}
			else {
				return null;
			}
			
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

}