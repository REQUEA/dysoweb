package com.requea.dysoweb.panel;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.requea.dysoweb.util.xml.XMLUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.w3c.dom.Element;

/*********
 * Class CryptUtils
 * Methods for 3DES, AES, encryption/decryption
 *
 * Asof December 2024 only AES/GCM is used for symetric encryption
 * 3DES is only used for decryption of already stored passwords for backward compatibility
 *
 */
public class CryptUtils {
	private static Log fLog = LogFactory.getLog(CryptUtils.class);
	private String fCypherType;
	private Cipher f3DESEncryptCipher = null;
	private Cipher f3DESDecryptCipher = null;
	private static String f3DESKey = null;
	private static String fAESKey = null;
	private static String fCertPassword = null;

	private File fConfigDir;

	public static CryptUtils getInstance(String cypherType, File configDir) throws Exception {
		return new CryptUtils(cypherType, configDir);
	}

	private CryptUtils(String cypherType, File configDir) throws Exception {
		if(cypherType != null && !"3DES".equals(cypherType) && !"AES".equals(cypherType)) {
			throw new Exception("not supported cypher");
		}
		fCypherType = cypherType;
		fConfigDir = configDir;
	}
	public synchronized String encrypt(String str) throws Exception {
		return encrypt("3DES", str, "Base64");
	}

	public synchronized String encrypt(String cipher, String str, String type) throws Exception {
		if ("AES".equals(cipher)) {
			return encryptAES(str.getBytes(StandardCharsets.UTF_8));
		} else { // default to 3DES
			if (f3DESEncryptCipher == null) {
				init3DESEncryptCipher();
			}

			byte[] cipherbyte = f3DESEncryptCipher.doFinal(str.getBytes());
			// base 32 or 64 encode
			if ("Base32".equals(type))
				return Base32.encode(cipherbyte);
			else
				return Base64.encodeBytes(cipherbyte);
		}
	}

	public synchronized String encrypt(byte[] data, String type) throws Exception {
		return encrypt(fCypherType, data, type);
	}


	public synchronized String encrypt(String cipher, byte[] data, String type) throws Exception {
		if ("AES".equals(cipher)) {
			return encryptAES(data);
		} else {
			if (f3DESEncryptCipher == null) {
				init3DESEncryptCipher();
			}

			byte[] cipherbyte = f3DESEncryptCipher.doFinal(data);
			// base 32 or 64 encode
			if ("Base32".equals(type))
				return Base32.encode(cipherbyte);
			else
				return Base64.encodeBytes(cipherbyte);
		}
	}

	private String encryptAES(byte[] data) throws
			NoSuchPaddingException,
			NoSuchAlgorithmException,
			InvalidAlgorithmParameterException,
			InvalidKeyException,
			BadPaddingException,
			IllegalBlockSizeException {

		// generate random iv
		SecureRandom random = new SecureRandom();
		byte[] iv = new byte[12];
		random.nextBytes(iv);

		if (fAESKey == null)
			loadKeys();
		// init aes cipher
		byte[] key = fAESKey.getBytes();
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));

		// encrypt
		byte[] cipherText = cipher.doFinal(data);

		// build whole message (iv length + iv + encrypted text)
		ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + cipherText.length);
		byteBuffer.putInt(iv.length);
		byteBuffer.put(iv);
		byteBuffer.put(cipherText);
		byte[] cipherMessage = byteBuffer.array();

		// base64 encoding
		return Base64.encodeBytes(cipherMessage);
	}

	public synchronized String decrypt(String str) throws Exception {
		return decrypt("3DES", str, "Base64");
	}

	public String decrypt(String cipher, String str, String type) throws Exception {
		if ("AES".equals(cipher)) {
			return decryptAES(str);
		} else {
			if(f3DESDecryptCipher == null) {
				init3DESDecryptCipher();
			}
			synchronized(f3DESDecryptCipher) {
				byte[] encodedData;
				if ("Base32".equals(type))
					encodedData = Base32.decode(str);
				else
					encodedData = Base64.decode(str);

				return new String(f3DESDecryptCipher.doFinal(encodedData));
			}
		}
	}

	private String decryptAES(String str) throws
			InvalidAlgorithmParameterException,
			InvalidKeyException,
			NoSuchPaddingException,
			NoSuchAlgorithmException,
			BadPaddingException,
			IllegalBlockSizeException {
		// decode and deconstruct the message
		byte[] decoded = Base64.decode(str);
		ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
		int ivLength = byteBuffer.getInt();
		byte[] iv = new byte[ivLength];
		byteBuffer.get(iv);
		byte[] cipherText = new byte[byteBuffer.remaining()];
		byteBuffer.get(cipherText);

		if (fAESKey == null)
			loadKeys();
		// init cipher
		byte[] key = fAESKey.getBytes();
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));

		// decrypt
		byte[] plainText = cipher.doFinal(cipherText);

		return new String(plainText, StandardCharsets.UTF_8);
	}

	private synchronized void init3DESEncryptCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		if (f3DESKey == null)
			loadKeys();
		byte [] seed_key = f3DESKey.getBytes();

		SecretKeySpec keySpec = new SecretKeySpec(seed_key,"TripleDES");
		Cipher cipher=Cipher.getInstance("TripleDES");
		cipher.init( Cipher.ENCRYPT_MODE, keySpec );
		f3DESEncryptCipher = cipher;
	}

	public String getCertPassword() {
		if (fCertPassword == null) {
			loadKeys();
		}
		if (fCertPassword != null) {
			try {
				return decryptAES(fCertPassword);
			} catch (Exception e) {
			}
		}
		return fCertPassword;
	}
	private void loadKeys() {
		File fileConfig = new File(fConfigDir, "server.xml");

		if(fileConfig.exists()) {
			Element elConfig = null;
			try {
				elConfig = XMLUtils.parse(new FileInputStream(fileConfig)).getDocumentElement();
			} catch (Exception e) {
				fLog.error("Error parsing server.xml", e);
			}
			if (elConfig!= null) {
				f3DESKey = XMLUtils.getChildText(elConfig, "TripleDESKey");
				fAESKey = XMLUtils.getChildText(elConfig, "AESKey");
				fCertPassword = XMLUtils.getChildText(elConfig, "CertPassword");
			}
			boolean save = false;
			if (f3DESKey == null) {
				f3DESKey = RandomStringUtils.random(24, true, true);
				save=true;
			}
			if (fAESKey == null) {
				fAESKey = RandomStringUtils.random(16, true, true);
				save=true;
			}
			if (save) {
				setChildValue(elConfig, "TripleDESKey", f3DESKey);
				setChildValue(elConfig, "AESKey", fAESKey);
				String str = XMLUtils.DocumentToString(elConfig.getOwnerDocument(), true);
				FileWriter fw;
				try {
					fw = new FileWriter(fileConfig);
					fw.write(str);
					fw.close();
				} catch (IOException e) {
					fLog.error("Error saving server.xml", e);
				}

			}
		}


	}

	private void setChildValue(Element elConfig, String name, String value) {
		Element el = XMLUtils.getChild(elConfig, name);
		if(el == null) {
			el = XMLUtils.addElement(elConfig, name);
		}
		XMLUtils.setText(el, value);
	}

	private synchronized void init3DESDecryptCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		if (f3DESKey == null)
			loadKeys();
		byte [] seed_key = f3DESKey.getBytes();

		SecretKeySpec keySpec = new SecretKeySpec(seed_key,"TripleDES");
		Cipher cipher = Cipher.getInstance("TripleDES");
		cipher.init( Cipher.DECRYPT_MODE, keySpec );
		f3DESDecryptCipher = cipher;
	}

	public synchronized Cipher getEncryptCipher() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		if(f3DESEncryptCipher == null) {
			init3DESEncryptCipher();
		}
		return f3DESEncryptCipher;
	}

	public synchronized Cipher getDecryptCipher() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		if(f3DESDecryptCipher == null) {
			init3DESDecryptCipher();
		}
		return f3DESDecryptCipher;
	}


	/**
	 * Build a hash
	 * @param str
	 * @return
	 * @throws RegistryException
	 */
}
