/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package ee.forgr.capacitor_updater;

/**
 * Created by Awesometic
 * It's encrypt returns Base64 encoded, and also decrypt for Base64 encoded cipher
 * references: http://stackoverflow.com/questions/12471999/rsa-encryption-decryption-in-android
 */
import android.util.Base64;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;  
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

public class CryptoCipher {

  public static byte[] decryptPrivateRSA(byte[] source, PrivateKey privateKey)
    throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
    OAEPParameterSpec oaepParams = new OAEPParameterSpec(
      "SHA-256",
      "MGF1",
      new MGF1ParameterSpec("SHA-256"),
      PSource.PSpecified.DEFAULT
    );
    cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
    byte[] decryptedBytes = cipher.doFinal(source);
    return decryptedBytes;
  }

  public static byte[] decryptPublicRSA(byte[] source, PublicKey publicKey)
    throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException 
  {
    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    cipher.init(Cipher.DECRYPT_MODE, publicKey);
    byte[] decryptedBytes = cipher.doFinal(source);
    return decryptedBytes;
  }

  public static byte[] decryptAES(byte[] cipherText, SecretKey key, byte[] iv) {
    try {
      IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");
      cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec);
      byte[] decryptedText = cipher.doFinal(cipherText);
      return decryptedText;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static SecretKey byteToSessionKey(byte[] sessionKey) {
    // rebuild key using SecretKeySpec
    SecretKey originalKey = new SecretKeySpec(
      sessionKey,
      0,
      sessionKey.length,
      "AES"
    );
    return originalKey;
  }

  private static PrivateKey readPkcs8PrivateKey(byte[] pkcs8Bytes)
    throws GeneralSecurityException {
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
    try {
      return keyFactory.generatePrivate(keySpec);
    } catch (InvalidKeySpecException e) {
      throw new IllegalArgumentException("Unexpected key format!", e);
    }
  }

  private static PublicKey readX509PublicKey(byte[] x509Bytes)
    throws GeneralSecurityException {
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(x509Bytes);
    try {
      return keyFactory.generatePublic(keySpec);
    } catch (InvalidKeySpecException e) {
      throw new IllegalArgumentException("Unexpected key format!", e);
    }
  }

  private static byte[] joinPrivate(byte[] byteArray1, byte[] byteArray2) {
    byte[] bytes = new byte[byteArray1.length + byteArray2.length];
    System.arraycopy(byteArray1, 0, bytes, 0, byteArray1.length);
    System.arraycopy(
      byteArray2,
      0,
      bytes,
      byteArray1.length,
      byteArray2.length
    );
    return bytes;
  }

  private static PrivateKey readPkcs1PrivateKey(byte[] pkcs1Bytes)
    throws GeneralSecurityException {
    // We can't use Java internal APIs to parse ASN.1 structures, so we build a PKCS#8 key Java can understand
    int pkcs1Length = pkcs1Bytes.length;
    int totalLength = pkcs1Length + 22;
    byte[] pkcs8Header = new byte[] {
      0x30,
      (byte) 0x82,
      (byte) ((totalLength >> 8) & 0xff),
      (byte) (totalLength & 0xff), // Sequence + total length
      0x2,
      0x1,
      0x0, // Integer (0)
      0x30,
      0xD,
      0x6,
      0x9,
      0x2A,
      (byte) 0x86,
      0x48,
      (byte) 0x86,
      (byte) 0xF7,
      0xD,
      0x1,
      0x1,
      0x1,
      0x5,
      0x0, // Sequence: 1.2.840.113549.1.1.1, NULL
      0x4,
      (byte) 0x82,
      (byte) ((pkcs1Length >> 8) & 0xff),
      (byte) (pkcs1Length & 0xff), // Octet string + length
    };
    byte[] pkcs8bytes = joinPrivate(pkcs8Header, pkcs1Bytes);
    return readPkcs8PrivateKey(pkcs8bytes);
  }

  public static PrivateKey stringToPrivateKey(String private_key)
    throws GeneralSecurityException {
    // Base64 decode the result

    String pkcs1Pem = private_key.toString();
    pkcs1Pem = pkcs1Pem.replace("-----BEGIN RSA PRIVATE KEY-----", "");
    pkcs1Pem = pkcs1Pem.replace("-----END RSA PRIVATE KEY-----", "");
    pkcs1Pem = pkcs1Pem.replace("\\n", "");
    pkcs1Pem = pkcs1Pem.replace(" ", "");

    byte[] pkcs1EncodedBytes = Base64.decode(
      pkcs1Pem.getBytes(),
      Base64.DEFAULT
    );
    // extract the private key
    return readPkcs1PrivateKey(pkcs1EncodedBytes);
  }

  public static PublicKey stringToPublicKey(String public_key)
    throws GeneralSecurityException {
    // Base64 decode the result

    String pkcs1Pem = public_key.toString();
    pkcs1Pem = pkcs1Pem.replace("-----BEGIN RSA PUBLIC KEY-----", "");
    pkcs1Pem = pkcs1Pem.replace("-----END RSA PUBLIC KEY-----", "");
    pkcs1Pem = pkcs1Pem.replace("\\n", "");
    pkcs1Pem = pkcs1Pem.replace(" ", "");

    byte[] pkcs1EncodedBytes = Base64.decode(
      pkcs1Pem,
      Base64.DEFAULT
    );

    // extract the public key
    return readPkcs1PublicKey(pkcs1EncodedBytes);
  }

  // since the public key is in pkcs1 format, we have to convert it to x509 format similar
  // to what needs done with the private key converting to pkcs8 format
  // so, the rest of the code below here is adapted from here https://stackoverflow.com/a/54246646
  private static final int SEQUENCE_TAG = 0x30;
  private static final int BIT_STRING_TAG = 0x03;
  private static final byte[] NO_UNUSED_BITS = new byte[] { 0x00 };
  private static final byte[] RSA_ALGORITHM_IDENTIFIER_SEQUENCE =
    {(byte) 0x30, (byte) 0x0d,
            (byte) 0x06, (byte) 0x09, (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xf7, (byte) 0x0d, (byte) 0x01, (byte) 0x01, (byte) 0x01,
            (byte) 0x05, (byte) 0x00};

  private static PublicKey readPkcs1PublicKey(byte[] pkcs1Bytes)
    throws NoSuchAlgorithmException, InvalidKeySpecException, GeneralSecurityException {
    // convert the pkcs1 public key to an x509 favorable format
    byte[] keyBitString = createDEREncoding(BIT_STRING_TAG, joinPublic(NO_UNUSED_BITS, pkcs1Bytes));
    byte[] keyInfoValue = joinPublic(RSA_ALGORITHM_IDENTIFIER_SEQUENCE, keyBitString);
    byte[] keyInfoSequence = createDEREncoding(SEQUENCE_TAG, keyInfoValue);
    return readX509PublicKey(keyInfoSequence);
  }

  private static byte[] joinPublic(byte[] ... bas)
  {
      int len = 0;
      for (int i = 0; i < bas.length; i++)
      {
          len += bas[i].length;
      }

      byte[] buf = new byte[len];
      int off = 0;
      for (int i = 0; i < bas.length; i++)
      {
          System.arraycopy(bas[i], 0, buf, off, bas[i].length);
          off += bas[i].length;
      }

      return buf;
  }

  private static byte[] createDEREncoding(int tag, byte[] value)
  {
      if (tag < 0 || tag >= 0xFF)
      {
          throw new IllegalArgumentException("Currently only single byte tags supported");
      }

      byte[] lengthEncoding = createDERLengthEncoding(value.length);

      int size = 1 + lengthEncoding.length + value.length;
      byte[] derEncodingBuf = new byte[size];

      int off = 0;
      derEncodingBuf[off++] = (byte) tag;
      System.arraycopy(lengthEncoding, 0, derEncodingBuf, off, lengthEncoding.length);
      off += lengthEncoding.length;
      System.arraycopy(value, 0, derEncodingBuf, off, value.length);

      return derEncodingBuf;
  }   

  private static byte[] createDERLengthEncoding(int size)
  {
      if (size <= 0x7F)
      {
          // single byte length encoding
          return new byte[] { (byte) size };
      }
      else if (size <= 0xFF)
      {
          // double byte length encoding
          return new byte[] { (byte) 0x81, (byte) size };
      }
      else if (size <= 0xFFFF)
      {
          // triple byte length encoding
          return new byte[] { (byte) 0x82, (byte) (size >> Byte.SIZE), (byte) size };
      }

      throw new IllegalArgumentException("size too large, only up to 64KiB length encoding supported: " + size);
  }
}
