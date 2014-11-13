package com.requea.dysoweb.panel;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


public class CryptUtils {

	private String fCypherType;
	private Cipher f3DESEncryptCipher = null;
	private Cipher f3DESDecryptCipher = null;
	private String fKey = "6j7bE3ghj68cC%&56gFCvhdh";
	public static CryptUtils getInstance(String cypherType) throws Exception {
		return new CryptUtils(cypherType);
	}
	
	public static CryptUtils getInstance(String cypherType, String key) throws Exception {
		return new CryptUtils(cypherType, key);
	}
	
	private CryptUtils(String cypherType) throws Exception {
		if(!"3DES".equals(cypherType)) {
			throw new Exception("not supported cypher");
		}
		fCypherType = cypherType;
	}
	private CryptUtils(String cypherType, String key) throws Exception {
		if(!"3DES".equals(cypherType)) {
			throw new Exception("not supported cypher");
		}
		fCypherType = cypherType;
		if (key != null)
			fKey = key;
	}

	public synchronized String encrypt(String str) throws Exception {
		return encrypt(str, "Base64");
	}

	public synchronized String encrypt(String str, String type) throws Exception {
		if(f3DESEncryptCipher == null) {
			init3DESEncryptCipher();
		}
		
		byte[] cipherbyte = f3DESEncryptCipher.doFinal(str.getBytes());
		// base 32 or 64 encode
		if ("Base32".equals(type))
			return Base32.encode(cipherbyte);
		return Base64.encodeBytes(cipherbyte);
	}

	public synchronized String decrypt(String str) throws Exception {
		return decrypt(str, "Base64");
	}

	public String decrypt(String str, String type) throws Exception {
		if(f3DESDecryptCipher == null) {
			init3DESDecryptCipher();
		}
		synchronized(f3DESDecryptCipher) {
			byte[] encodedData = null;
			if ("Base32".equals(type))
				encodedData = Base32.decode(str);
			else
				encodedData = Base64.decode(str);
		
			String decryptedtxt = new String(f3DESDecryptCipher.doFinal(encodedData));
			return decryptedtxt;
		}
	}

	private synchronized void init3DESEncryptCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		byte [] seed_key = fKey.getBytes();
		
		SecretKeySpec keySpec = new SecretKeySpec(seed_key,"TripleDES");
		Cipher cipher=Cipher.getInstance("TripleDES");
		cipher.init( Cipher.ENCRYPT_MODE, keySpec );
		f3DESEncryptCipher = cipher;
	}

	private synchronized void init3DESDecryptCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		byte [] seed_key = fKey.getBytes();
		
		SecretKeySpec keySpec = new SecretKeySpec(seed_key,"TripleDES");
		Cipher cipher = Cipher.getInstance("TripleDES");
		cipher.init( Cipher.DECRYPT_MODE, keySpec );
		f3DESDecryptCipher = cipher;
	}

	public synchronized Cipher getEncryptCyper() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		if(f3DESEncryptCipher == null) {
			init3DESEncryptCipher();
		}
		return f3DESEncryptCipher;
	}
	
	public synchronized Cipher getDecryptCyper() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		if(f3DESDecryptCipher == null) {
			init3DESDecryptCipher();
		}
		return f3DESDecryptCipher;
	}
	
}
