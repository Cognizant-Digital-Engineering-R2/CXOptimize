/*
 * Copyright 2014 - 2018 Cognizant Technology Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cognizant.pace.CXOptimize.UI


import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter
import java.security.NoSuchAlgorithmException


class EncryptionUtils
{
    static String getEncryptedPassword(def password,String secretKey) throws NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, NoSuchAlgorithmException, InvalidKeyException
    {
        return encrypt(password.toString(),secretKey)
    }

    static String getDecryptedPassword(String encPassword,String secretKey) throws NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException, InvalidAlgorithmParameterException
    {
        return decrypt(encPassword,secretKey)
    }

    static String getEncryptedSecretPharse(String secPhrase,String secretKey) throws NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, NoSuchAlgorithmException, InvalidKeyException
    {
        return encrypt(secPhrase,secretKey)
    }

    static String getDecryptedSecretPharse(String encSecPhrase,String secretKey) throws NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException, InvalidAlgorithmParameterException
    {
        return decrypt(encSecPhrase,secretKey)
    }

    static String encrypt(String plaintext,String key)throws NoSuchPaddingException, IllegalBlockSizeException,BadPaddingException, IOException, NoSuchAlgorithmException, InvalidKeyException
    {
        String ALGORITMO = "AES/CBC/PKCS5Padding"
        String CODIFICACION = "UTF-8"
        byte[] raw = DatatypeConverter.parseHexBinary(key)
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES")
        Cipher cipher = Cipher.getInstance(ALGORITMO)
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
        byte[] cipherText = cipher.doFinal(plaintext.getBytes(CODIFICACION))
        byte[] iv = cipher.getIV()
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        outputStream.write(iv)
        outputStream.write(cipherText)
        byte[] finalData = outputStream.toByteArray()
        String encodedFinalData = DatatypeConverter.printBase64Binary(finalData)
        return encodedFinalData
    }

    static String decrypt(String encodedInitialData,String key)throws IllegalBlockSizeException,BadPaddingException, UnsupportedEncodingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException
    {
        String ALGORITMO = "AES/CBC/PKCS5Padding"
        String CODIFICACION = "UTF-8"
        byte[] encryptedData = DatatypeConverter.parseBase64Binary(encodedInitialData)
        byte[] raw = DatatypeConverter.parseHexBinary(key)
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES")
        Cipher cipher = Cipher.getInstance(ALGORITMO)
        byte[] iv = Arrays.copyOfRange(encryptedData, 0, 16)
        byte[] cipherText = Arrays.copyOfRange(encryptedData, 16, encryptedData.length)
        IvParameterSpec iv_specs = new IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv_specs)
        byte[] plainTextBytes = cipher.doFinal(cipherText)
        String plainText = new String(plainTextBytes)
        return plainText
    }

}
