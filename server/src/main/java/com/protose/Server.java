package com.protose;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.crypto.SealedObject;
// import org.apache.logging.log4j.LogManager;
// import org.apache.logging.log4j.Logger;

import org.apache.commons.lang3.SerializationUtils;

import com.protose.shared.DB;
import com.protose.shared.OptionsManager;
import com.protose.shared.QueryPath;
import com.protose.shared.OptionsManager.OPTIONIDENT;
import com.protose.shared.IRemoteServer;

public class Server extends UnicastRemoteObject implements IRemoteServer{
    
    //search structure
    private int[][] P; //[w][d]
    private int width;
    private int depth;
    private int head;

    //path index
    //mapping: hash -> encrypted path
    private DB<byte[],SealedObject> I;
    
    //edp storage
    //mapping hash -> edp
    private DB<Integer,SealedObject> S;

    private OptionsManager optionsManager;
    private Logger logger;
    private FileHandler loggerFileHandler;

    private SecureRandom randomGen;

    Server() throws RemoteException{

        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        //handle server options
        File optionsFile = new File("./etc/ServerOptions.options");//./Server/etc/ServerOptions.options
        optionsManager = new OptionsManager(optionsFile);

        //Init Logger
        logger = Logger.getLogger("ProtoSE:ServerLog");
        String logPath = optionsManager.getOption(
            OPTIONIDENT.SERVER_LOG_PATH).concat(
                optionsManager.getOption(OPTIONIDENT.SERVER_LOG_NAME)
        );
        try {  
            loggerFileHandler = new FileHandler(logPath);  
            logger.addHandler(loggerFileHandler);
            SimpleFormatter formatter = new SimpleFormatter();  
            loggerFileHandler.setFormatter(formatter);
        } catch (Exception e) {  
            e.printStackTrace();  
        }

        //create / connect data base
        S = new DB<Integer, SealedObject>(
            optionsManager.getOption(OPTIONIDENT.SERVER_S_PATH), 
            optionsManager.getOption(OPTIONIDENT.SERVER_S_NAME)
        );

        //create / connect database for word index
        I = new DB<byte[], SealedObject>(
            optionsManager.getOption(OPTIONIDENT.SERVER_I_PATH),
            optionsManager.getOption(OPTIONIDENT.SERVER_I_NAME)
        );

        //create a new search structure
        width = Integer.parseInt(optionsManager.getOption(OPTIONIDENT.SERVER_P_WIDTH));
        depth = Integer.parseInt(optionsManager.getOption(OPTIONIDENT.SERVER_P_DEPTH));
        P = new int[width][depth];

        //create the RMI registry
        try{
            Registry registry = LocateRegistry.createRegistry(
                Integer.parseInt(optionsManager.getOption(OPTIONIDENT.SERVER_PORT))
            );
            registry.bind(optionsManager.getOption(OPTIONIDENT.RMI_REGISTRY_NAME), this);    
        }catch(Exception e){
            e.printStackTrace();
        }

        randomGen = new SecureRandom();

        logger.info("server ready");
    }

    @Override
    public Set<SealedObject> access(QueryPath p) throws RemoteException {
        Set<SealedObject> retVal = new HashSet<SealedObject>();

        for (Integer pos : p.positions) {
            int posW = pos%width;
            int posD = pos/width;//TODO: check this
            int ident = P[posW][posD];

            SealedObject edp = S.find(ident);
            retVal.add(edp);
        }

        return retVal;
    }

    @Override
    public void storeEDP(SealedObject edp, int position) throws RemoteException {
        System.out.println("storing edp: " + edp.toString() + "at pos: "+position);
        
        //error check: pos must never be bigger than head -> not allowed
        if(position > head){
            System.out.println("[ERROR]");
            logger.log(Level.SEVERE, "storeEDP position is larger than head");
            //TODO: think of a proper error handling
        }
        
        int ident = edp.hashCode();

        int posW = position%width;
        int posD = position/width;

        if(P[posW][posD] > 0){
            //there is data to be handled
            //TODO: implement behaviour
        }

        //store the ident at the correct position
        P[posW][posD] = ident;      
        
        //store the edp in the database 
        S.save(ident, edp);

        //update head
        if(position == head){ head++; }

        logger.log(Level.INFO, "EDP stored at: " + position);
    }

    @Override
	public SealedObject getPath(byte[] hash) throws RemoteException {
        SealedObject path = I.find(hash);
        if(path == null) System.out.println("requested path not found"); //TODO:delete this
        I.delete(hash);
        logger.log(Level.INFO, "path accessed and deleted: " + hash);
        return path;
	}

    @Override
    public void storePath(byte[] hash, SealedObject path) throws RemoteException {
        I.save(hash, path);
        logger.log(Level.INFO, "path stored: " + hash );
    }	

    @Override
    public void reduce() throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reduce'");
    }

    @Override
    public int getSearchStructureHead() throws RemoteException {
        return this.head;
    }

    @Override
    public int getSearchStructureWidth() throws RemoteException{
        return this.width;
    }

    public void shutDown(){

        //create storage directory if missing
        String tmpDir = optionsManager.getOption(OPTIONIDENT.SERVER_TMP_DIR);
        File localStorageDir = new File(tmpDir);
        localStorageDir.mkdirs();

        //store search structure
        File wordMapFile = new File(tmpDir.concat("/P.sys"));
        try(OutputStream outputStream = new FileOutputStream(wordMapFile)) {
            outputStream.write(SerializationUtils.serialize((Serializable) P));
        }catch(Exception e){
            e.printStackTrace();
        }

        //store head
        File headFile = new File(tmpDir.concat("/head.sys"));
        try(OutputStream outputStream = new FileOutputStream(headFile)) {
            outputStream.write(SerializationUtils.serialize((Serializable) head));
        }catch(Exception e){
            e.printStackTrace();
        }

        //shut down rmi service
        try{
            Naming.unbind(optionsManager.getOption(OPTIONIDENT.RMI_REGISTRY_NAME));
            UnicastRemoteObject.unexportObject(this, true);
        }catch(Exception e){/* do nothing */}

        loggerFileHandler.close();
        System.out.println("INFO: server shut down");
    }

    public static void main(String[] args) { 
        try {
            Server server = new Server();
            
            //TODO: rmi service runns concurrently
            // we need to control shutdown here


        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}