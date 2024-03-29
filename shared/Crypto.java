package com.protose.shared;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
    
    //symmetric key
    private SecretKey symmetricKey;
    private IvParameterSpec IV_privKeyEncr;
    private String ivPath;
    static final String DEFAULT_IV_PATH = "iv.key";

    //asymmetric key pair
    private KeyPair keyPair;
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
   
    private String privateKeyPath;
    static final String DEFAULT_PRIVATEKEY_PATH = "private.key";
   
    private String publicKeyPath;
    static final String DEFAULT_PUBLICKEY_PATH = "public.key";

    //HMAC
    private Mac mac;

    private SecureRandom randomGen;

    //Keystore params
    private String keyStorePath;
    static final String DEFAULT_KEYSTORE_PATH = "keystore.key";
    private String keyStorePW;
    static final String DEFAULT_KEYSTORE_PW = "password";
    static final String KEYSTORE_SECRET_ID = "PROTOSE:SECRET";

    //symmetric encryption algorithm
    public static final String DEFAULT_SYM_ALGORITHM = "AES/CBC/PKCS5Padding";
    

    /**
     * 
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public Crypto(String keyPath, String keyStorePW) throws NoSuchAlgorithmException, InvalidKeyException{
        
        //set given values or go back to default values
        if(keyPath != null){
            this.keyStorePath   = keyPath.concat(DEFAULT_KEYSTORE_PATH);
            this.privateKeyPath = keyPath.concat(DEFAULT_PRIVATEKEY_PATH);
            this.publicKeyPath  = keyPath.concat(DEFAULT_PUBLICKEY_PATH);
            this.ivPath         = keyPath.concat(DEFAULT_IV_PATH);
        }else{
            this.keyStorePath = DEFAULT_KEYSTORE_PATH;
            this.privateKeyPath = DEFAULT_PRIVATEKEY_PATH;
            this.publicKeyPath = DEFAULT_PUBLICKEY_PATH;
            this.ivPath = DEFAULT_IV_PATH;
        }

        if(keyStorePW != null){
            this.keyStorePW = keyStorePW;
        }else{
            this.keyStorePW = DEFAULT_KEYSTORE_PW;
        }

        //retrieve stored data
        try {
            loadKeys();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //generate symmetric key
        if(symmetricKey == null){
            System.out.println("[INFO] generating new symmetric key");
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            symmetricKey = keyGen.generateKey();
        }
        
        //generate iv
        if(IV_privKeyEncr == null){
            System.out.println("[INFO] generating new iv");
            IV_privKeyEncr = generateIv();
        }

        //generate asymetric key pair
        if(privateKey == null && publicKey == null){
            System.out.println("[INFO] generating new key pair");
            KeyPairGenerator RSAGenerator = KeyPairGenerator.getInstance("RSA");
            this.keyPair = RSAGenerator.generateKeyPair();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        }

        //init HMAC
        mac = Mac.getInstance("HmacSHA256");
        mac.init(symmetricKey);
        
        //random generator
        randomGen  = new SecureRandom();

        //store keys on disk
        try {
            storeKeys();
        } catch (KeyStoreException | CertificateException | IOException e) {
            e.printStackTrace();
        }
    }

    /*
     *************************************************************************************************
     * GETTER/SETTER
     *************************************************************************************************
    */

    /**
     * @return the public key from the asymmetric key pair
     */
    public RSAPublicKey getPublicKey(){
        return this.publicKey;
    }

    /**
     * @return the private key from the asymmetric key pair
     */
    public RSAPrivateKey getPrivateKey(){
        return this.privateKey;
    }

    /**
     * @return the secret symmetric key
     */
    public SecretKey getSecretKey(){
        return this.symmetricKey;
    }

    /**
     * 
     * @return the IV from the symmetric encryption
     */
    public IvParameterSpec getIV(){
        return this.IV_privKeyEncr;
    }

    /*
     *************************************************************************************************
     * RANDOM FUNCTIONS
     *************************************************************************************************
    */  
    /**
     * returns a random number between 0 and to
     * @param to
     * @return
     */
    public int getRandomInt(int to){
        return randomGen.nextInt(to);
    }

    /*
     *************************************************************************************************
     * KEY STORAGE
     *************************************************************************************************
    */

    /**
     * @throws KeyStoreException
     * @throws IOException
     * @throws FileNotFoundException
     * 
     */
    private void createKeyStore() throws Exception{

        //Creating the KeyStore object
        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        keyStore.load(null, keyStorePW.toCharArray());        

        //write keystore to disk
        try (FileOutputStream fos = new FileOutputStream(keyStorePath)) {
            keyStore.store(fos, keyStorePW.toCharArray());
        }
    }


    /**
     * 
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    public void storeKeys() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException{

        //Creating the KeyStore object
        KeyStore keyStore = KeyStore.getInstance("JCEKS");

        //Check if the keystore file is present and if not create a new one
        File keyStoreFile = new File(keyStorePath);
        if(!keyStoreFile.exists()){
            try {
                createKeyStore();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }    
        
        //load the key store
        java.io.FileInputStream fis = new FileInputStream(keyStorePath);
        keyStore.load(fis, keyStorePW.toCharArray());

        //Creating the KeyStore.ProtectionParameter object
        KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(keyStorePW.toCharArray());        

        //store the symmetric secret key
        if(symmetricKey != null){
            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(symmetricKey);
            keyStore.setEntry(KEYSTORE_SECRET_ID, secretKeyEntry, protectionParam);
        }

        //store private key in cipher-text using the symmetric key
        if(privateKey != null){
            try {
                //encrypt key
                SealedObject encryptedKey = encryptObject(DEFAULT_SYM_ALGORITHM, privateKey, symmetricKey, IV_privKeyEncr);
                
                //write iv to disk
                File ivFile = new File(ivPath);
                FileOutputStream ivFOS = new FileOutputStream(ivFile);
                ivFOS.write(IV_privKeyEncr.getIV());
                ivFOS.close();

                //write encrypted key to file
                File KeyFile = new File(privateKeyPath);
                FileOutputStream FOS = new FileOutputStream(KeyFile);
                ObjectOutputStream privateOOS = new ObjectOutputStream(FOS);
                privateOOS.writeObject(encryptedKey);
                privateOOS.flush();
                privateOOS.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
        }

        //store public key in clear text
        if(publicKey != null){
            File publicKeyFile = new File(publicKeyPath);
            FileOutputStream publicFOS = new FileOutputStream(publicKeyFile);
            ObjectOutputStream publicOOS = new ObjectOutputStream(publicFOS);
            publicOOS.writeObject(publicKey);
            publicOOS.flush();
            publicOOS.close();
        }

        //save the key store to the keystore file
        java.io.FileOutputStream fos = new FileOutputStream(keyStorePath);
        keyStore.store(fos, keyStorePW.toCharArray());

        System.out.println("[INFO] keys stored on disk");
    }

    /**
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableEntryException
     * 
     */
    public void loadKeys() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableEntryException{

        //Creating the KeyStore object
        KeyStore keyStore = KeyStore.getInstance("JCEKS");

        //check if the keystore exists, if not there is no 
        //symmetric key to be retrieved and the function can 
        //just return
        File keyStoreFile = new File(keyStorePath);
        if(!keyStoreFile.exists()) return;

        //Loading the KeyStore object
        try{
            FileInputStream fis = new FileInputStream(keyStorePath);
            keyStore.load(fis, DEFAULT_KEYSTORE_PW.toCharArray());            

            //Creating the KeyStore.ProtectionParameter object
            KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(keyStorePW.toCharArray());

            //retrieve symmetric key
            KeyStore.SecretKeyEntry secretKeyEnt = (KeyStore.SecretKeyEntry)keyStore.getEntry(KEYSTORE_SECRET_ID, protectionParam);
            symmetricKey = secretKeyEnt.getSecretKey(); 
        }catch(Exception e){
            e.printStackTrace();
        }

        //retrieve iv
        try {
            File ivFile = new File(ivPath);
            FileInputStream ivFIS = new FileInputStream(ivFile);
            byte[] ivByte = ivFIS.readAllBytes();            
            IV_privKeyEncr = new IvParameterSpec(ivByte);   
            ivFIS.close();     
        } catch (Exception e) {
            e.printStackTrace();
        }

        //retrieve private key
        try{
            File privateKeyFile = new File(privateKeyPath);
            FileInputStream privateFIS = new FileInputStream(privateKeyFile);
            ObjectInputStream privateOIS = new ObjectInputStream(privateFIS);
            SealedObject readObj = (SealedObject) privateOIS.readObject();
            privateOIS.close();
            privateKey = (RSAPrivateKey) decryptObject(DEFAULT_SYM_ALGORITHM, readObj, symmetricKey, IV_privKeyEncr);
        } catch(Exception e){
            e.printStackTrace();
        }

        //retrieve public key
        try {
            File publicKeyFile = new File(publicKeyPath);
            FileInputStream publicFIS = new FileInputStream(publicKeyFile);
            ObjectInputStream publicOIS = new ObjectInputStream(publicFIS);
            publicKey = (RSAPublicKey) publicOIS.readObject();
            publicOIS.close(); 
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    /*
     *************************************************************************************************
     * RANDOM
     *************************************************************************************************
    */
    /**
     * 
     * @param input
     */
    public void shuffleArray(Object[] input){
        
        Object tmp;

        for (int i = input.length-1; i > 0 ; i--) {
            int pick = randomGen.nextInt(i);
            tmp = input[pick];
            input[pick] = input[i];
            input[i] = tmp;
        }
    }

    /**
     * 
     * @param <T>
     * @param input
     */
    public <T> void shuffleList(List<T> input){
        T tmp;

        for (int i = input.size()-1; i > 0 ; i--) {
            int pick = randomGen.nextInt(i);
            tmp = input.get(pick);
            input.set(pick, input.get(i));
            input.set(i, tmp);
        }
    }


    /*
     *************************************************************************************************
     * HASHING
     *************************************************************************************************
    */

    /**
     * 
     * @param input
     * @return
     * @throws InvalidKeyException
     */
    static public BigInteger hmac(BigInteger input, Key key){
        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            // mac.init(symmetricKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new BigInteger(mac.doFinal(input.toByteArray()));
    }

    /**
     * 
     * @param key
     * @param input
     * @return
     */
    public static BigInteger hmac(BigInteger key, BigInteger input){
        
        SecretKeySpec k = new SecretKeySpec(key.toByteArray(), "AES");
        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(k);
            // mac.init(k);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new BigInteger(mac.doFinal(input.toByteArray()));
    }

    /**
     * hashes the given input String into a byte[] uses the 
     * symmetric key stored in the crypto object
     * @param input
     * @return
     */
    public byte[] hmac(String input){
        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA256");
            // mac.init(key);
            mac.init(symmetricKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mac.doFinal(input.getBytes());
    }


    /*
     *************************************************************************************************
     * ASYMMETRIC ENCRYPTION
     *************************************************************************************************
    */

    /**
     * 
     * @return
     */
    public BigInteger getRandomST(){
        return new BigInteger(2048, randomGen);
    }

    /**
     * 
     * @param input
     * @return
     */
    public static BigInteger tdp(BigInteger input, RSAPublicKey key){

        if(key == null){ return null; }
        
        BigInteger m = key.getModulus();
        BigInteger e = key.getPublicExponent();

        return input.modPow(e, m);
    }

    /**
     * 
     * @param input
     * @return
     */
    public static BigInteger itdp(BigInteger input, RSAPrivateKey key){

        if(key == null){ return null; }

        BigInteger m = key.getModulus();
        BigInteger d = key.getPrivateExponent();

        return input.modPow(d, m);
    }

    /**
     * 
     * @param privateKey
     * @param publicKey
     */
    public void setRSAKeys(RSAPrivateKey privateKey, RSAPublicKey publicKey){
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    /*
     *************************************************************************************************
     * SYMMETRIC ENCRYPTION
     *************************************************************************************************
    */
    public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(n);
        SecretKey key = keyGenerator.generateKey();
        return key;
    }

    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    public static String encrypt(String algorithm, String input, SecretKey key,
        IvParameterSpec iv) throws Exception {
    
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder()
            .encodeToString(cipherText);
    }

    public static String decrypt(String algorithm, String cipherText, SecretKey key,
        IvParameterSpec iv) throws Exception {
    
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] plainText = cipher.doFinal(Base64.getDecoder()
            .decode(cipherText));
        return new String(plainText);
    }

    public static SealedObject encryptObject(String algorithm, Serializable object,
        SecretKey key, IvParameterSpec iv) throws Exception {
    
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        SealedObject sealedObject = new SealedObject(object, cipher);
        return sealedObject;
    }

    /**
     * encrypts a object using the set options in the crypto object
     * @param object
     * @return
     */
    public SealedObject encrypt(Serializable object){
        SealedObject sealedObject = null;
        
        try {
            
            Cipher cipher = Cipher.getInstance(Crypto.DEFAULT_SYM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, this.symmetricKey, this.IV_privKeyEncr);
            sealedObject = new SealedObject(object, cipher);

        }catch (Exception e) {
            e.printStackTrace();
        }
        return sealedObject;
    }

    public static Serializable decryptObject(String algorithm, SealedObject sealedObject,
        SecretKey key, IvParameterSpec iv) throws Exception {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        Serializable unsealObject = (Serializable) sealedObject.getObject(cipher);
        return unsealObject;
    }

    public DP decryptEDP(SealedObject edp){
        DP dp = null;
        try{
            Cipher cipher = Cipher.getInstance(Crypto.DEFAULT_SYM_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, this.symmetricKey, this.IV_privKeyEncr);
            dp = (DP) edp.getObject(cipher);
        }catch(Exception e){
            e.printStackTrace();
        }
        return dp;
    }

    /**
     * decrypts a Path
     * @param EP
     * @return
     */
    public StorePath decryptPath(SealedObject EP){
        StorePath P = null;
        try{
            Cipher cipher = Cipher.getInstance(Crypto.DEFAULT_SYM_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, this.symmetricKey, this.IV_privKeyEncr);
            P = (StorePath) EP.getObject(cipher);
        }catch(Exception e){
            e.printStackTrace();
        }
        return P;
    }
}