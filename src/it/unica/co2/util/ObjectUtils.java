package it.unica.co2.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

public class ObjectUtils {
	
	static final Decoder base64Decoder = Base64.getDecoder();
	static final Encoder base64Encoder = Base64.getEncoder();

	public static String serializeObjectToString(Object object) throws IOException {
		
		try (
				ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(arrayOutputStream);
				) {
			objectOutputStream.writeObject(object);
			objectOutputStream.flush();
			return new String(base64Encoder.encode(arrayOutputStream.toByteArray()));
		}
	}

	public static Object deserializeObjectFromString(String objectString) throws IOException, ClassNotFoundException {
		
		try (
				ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(base64Decoder.decode(objectString));
				ObjectInputStream objectInputStream = new ObjectInputStream(arrayInputStream)
				) {
			return objectInputStream.readObject();
		}
	}

	public static <T> T deserializeObjectFromString(String objectString, Class<T> clazz) throws ClassNotFoundException, IOException {
		return clazz.cast( deserializeObjectFromString(objectString) );
	}
}
