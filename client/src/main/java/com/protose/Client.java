package com.protose;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import javax.crypto.SealedObject;

import org.apache.commons.lang3.SerializationUtils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import com.protose.shared.IRemoteServer;
import com.protose.shared.Crypto;
import com.protose.shared.DB;
import com.protose.shared.DP;
import com.protose.shared.OptionsManager;
import com.protose.shared.QueryPath;
import com.protose.shared.StorePath;
import com.protose.shared.OptionsManager.OPTIONIDENT;
import com.protose.shared.StorePath.PathNode;


public class Client {
    
    //logger
    private Logger logger;
    private FileHandler loggerFileHandler;

    //optionsmanager
    private OptionsManager optionsManager;

    //Document index
    private DB<String,StorePath> D;

    //Client components
    private BiMap<String, String> wordMap;
    private List<PathNode> freeList;
    private Crypto crypto; 
    private ScrambleBuffer scrambleBuffer;
    private DocumentSplitter documentSplitter;
    private PathGenerator pathGenerator;
    private Map<String,StorePath> queryCash;

    //RMI serverObject
    private IRemoteServer serverStub;

    //next free position on server datastructure
    private int searchStructureHead;
    //width of the searchStructure
    private int searchStructureWidth;

    //user interface
    private GUI gui;

    Client(){

        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        //Init Optionsmanager
        File optionsFile = new File("./etc/ClientOptions.options");//./etc/ClientOptions.options
        optionsManager = new OptionsManager(optionsFile);

        //Init Logging
        logger = Logger.getLogger("ClientLog");  

        try {  
            loggerFileHandler = new FileHandler(optionsManager.getOption(OPTIONIDENT.CLIENT_LOG_FILE));
            logger.addHandler(loggerFileHandler);
            SimpleFormatter formatter = new SimpleFormatter();  
            loggerFileHandler.setFormatter(formatter);
        } catch (Exception e) {   
            e.printStackTrace();  
        }

        logger.log(Level.INFO, "Client started");

        //init data bases
        D = new DB<String,StorePath>(
            optionsManager.getOption(OPTIONIDENT.CLIENT_D_PATH), 
            optionsManager.getOption(OPTIONIDENT.CLIENT_D_NAME)
        );
        
        String tmpDir = optionsManager.getOption(OPTIONIDENT.CLIENT_TMP_DIR);

        //create folders if missing
        File tmpDocs = new File(tmpDir + "/docs");
        tmpDocs.mkdirs();

        //init word map
        File wordMapFile = new File(tmpDir.concat("/wordMap.sys"));
        try(InputStream inputStream = new FileInputStream(wordMapFile)){
            wordMap = SerializationUtils.deserialize(inputStream);
        }catch(Exception e){
            logger.log(Level.INFO, "could not recover wordMap, generating a new empty map");
            wordMap = HashBiMap.create();
        }      

        //init free list
        File freeListFile = new File(tmpDir.concat("/List.sys"));
        try(InputStream inputStream = new FileInputStream(freeListFile)){
            freeList = SerializationUtils.deserialize(inputStream);
        }catch(Exception e){
            logger.log(Level.INFO, "could not recover freeList, generating a new empty list");
            freeList = new ArrayList<PathNode>();
        }              

        //init query cash
        File queryCashFile = new File(tmpDir.concat("/queryCash.sys"));
        try(InputStream inputStream = new FileInputStream(queryCashFile)){
            queryCash = SerializationUtils.deserialize(inputStream);
        }catch(Exception e){
            logger.log(Level.INFO, "could not recover queryCash, generating a new empty cash");
            queryCash = new HashMap<String,StorePath>(
                Integer.parseInt(optionsManager.getOption(OPTIONIDENT.CLIENT_QUERYCASH_SIZE))
            );
        }            

        //init crypto functions
        try {
            crypto = new Crypto(
                optionsManager.getOption(OPTIONIDENT.CLIENT_KEYS_DIR).concat("/"),
                "password"); //TODO: handle dialog with user to get password
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "Invalid key for crypto functions");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE,"No such algorithm in crypto functions, check crypto object");
        }   
        
        //Connect to RMI server infrastructure
        try {
            Registry registry = LocateRegistry.getRegistry(
                optionsManager.getOption(OPTIONIDENT.SERVER_HOST), 
                Integer.parseInt(
                    optionsManager.getOption(OPTIONIDENT.SERVER_PORT))
            );
            serverStub = (IRemoteServer) registry.lookup(
                optionsManager.getOption(OPTIONIDENT.RMI_REGISTRY_NAME)
            );
            // serverStub.setPublicKey(crypto.getPublicKey());
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());//TODO
            e.printStackTrace();
        }

        //get informations from server
        try {
            searchStructureHead = serverStub.getSearchStructureHead();
        } catch (RemoteException e) {
            //TODO: add prper logging and error handling
            e.printStackTrace();
        }

        //search structure information
        try {
            searchStructureWidth = serverStub.getSearchStructureWidth();
        } catch (RemoteException e) {
            //TODO: add prper logging and error handling
            //TODO: if we have the value from the optionsfile compare 
            // and detect difference
            e.printStackTrace();
        }

        //init PathGenerator
        pathGenerator = new PathGenerator(
            this.crypto,
            optionsManager,
            searchStructureWidth
        );

        //scramble Buffer
        File scrambleBufferFile = new File(optionsManager.getOption(OPTIONIDENT.CLIENT_TMP_DIR).concat("/scrambleBuffer.sys"));
        try(InputStream inputStream = new FileInputStream(scrambleBufferFile)){
            scrambleBuffer = SerializationUtils.deserialize(inputStream);
        }catch(Exception e){
            logger.log(Level.INFO, "could not recover scrambleBuffer, generating a new empty buffer");
            scrambleBuffer = new ScrambleBuffer(
                Integer.parseInt(optionsManager.getOption(OPTIONIDENT.CLIENT_SCRAMBLEBUFFER_SIZE)), 
                Integer.parseInt(optionsManager.getOption(OPTIONIDENT.CLIENT_SCRAMBLEBUFFER_NULLFACTOR))
            );
        }   

        //Document Splitter
        documentSplitter = new DocumentSplitter(); 
        
        //user interface
        gui = new GUI(this);
    }

    public void shutDown(){
        
        //TODO: use a path from options manager for all paths
        //create the folder if missing
        File localStorageDir = new File("./tmp");
        localStorageDir.mkdirs();

        //store word map
        File wordMapFile = new File("./tmp/wordMap.sys");
        try(OutputStream outputStream = new FileOutputStream(wordMapFile)) {
            outputStream.write(SerializationUtils.serialize((Serializable) wordMap));
        }catch(Exception e){
            e.printStackTrace();
        }

        //store query cash
        File queryCashFile = new File("./tmp/queryCash.sys");
        try(OutputStream outputStream = new FileOutputStream(queryCashFile)) {
            outputStream.write(SerializationUtils.serialize((Serializable) queryCash));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //store freelist
        File freeListFile = new File("./tmp/freeList.sys");
        try(OutputStream outputStream = new FileOutputStream(freeListFile)) {
            outputStream.write(SerializationUtils.serialize((Serializable) freeList));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //store scrambleBuffer
        File scrambleBufferFile = new File("./tmp/scrambleBuffer.sys");
        try(OutputStream outputStream = new FileOutputStream(scrambleBufferFile)) {
            outputStream.write(SerializationUtils.serialize((Serializable) scrambleBuffer));
        } catch (Exception e) {
            e.printStackTrace();
        }

        loggerFileHandler.close();
    }  

    public OptionsManager getOptions(){
        return this.optionsManager;
    }

    public Crypto geCrypto(){
        return this.crypto;
    }
    
    /**
     * 
     * @param file
     */
    public void addDocument(File file){        

        logger.log(Level.INFO, "processing document " + file.getName());

        //split document into dps
        List<DP> dpList = documentSplitter.splitDocument(file);
        // Map<String,StorePath> pathMap = new HashMap<String,StorePath>();

        //create document store path and store it locally
        StorePath docPath = new StorePath(file.getName());
        for (DP dp : dpList) {
            docPath.addLocal(dp);
        }
        this.D.save(file.getName(), docPath);

        //get all distinct words from dp
        //and corresponding paths from server
        for (DP dp : dpList) {
            dp.getWords().stream().distinct().forEach(w -> {
                
                //map word
                String wm = wordMap.get(w);
                if(wm == null){ wm = wordMap.inverse().get(w); }
                if(wm == null){ wm = w; }

                //path either on server or not in system -> new path necessary
                if(!queryCash.containsKey(wm)){
                    //check server
                    SealedObject ep = null;
                    try {
                        ep = serverStub.getPath(crypto.hmac(wm));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    if(ep != null){
                        //store path locally
                        queryCash.put(wm, crypto.decryptPath(ep));
                    }else{
                        //new path
                        queryCash.put(wm, new StorePath(wm));
                    }
                }
                
                //path is in cash, add the dp to it
                queryCash.get(wm).addLocal(dp);
            });
        }

        /*
         * queryCash contains all the words with the corresponding 
         * path objects
        */

        //put dps into scramble buffer
        for (DP dp : dpList) {

            //if the scramble buffer is full we empty half of it
            if(scrambleBuffer.isFull()){

                //apply shuffle
                scrambleBuffer.shuffle();

                //process the upper half of the buffer 
                scrambleBuffer.get().stream().forEach(part -> {

                    //handle decoy elements, just store in freelist and server
                    if(part.getDocName().equals("decoyProtoSE.sys")){
                        int position = searchStructureHead;
                        searchStructureHead++;

                        //add external position to free list
                        PathNode pn = StorePath.getNewNode();
                        pn.serverPos = position;
                        freeList.add(pn);

                        //store decoy at server
                        try {
                            serverStub.storeEDP(crypto.encrypt(part), position);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        return;
                    }

                    //calculate position from head and free list
                    //TODO: always empty free list before using new node?
                    int position;
                    if(freeList.size() > 0){
                        PathNode freePosition = freeList.remove(crypto.getRandomInt(freeList.size()));
                        position = freePosition.serverPos;
                    }else{
                        position = searchStructureHead;
                        searchStructureHead++;
                    }                    

                    //fixup of paths
                    //fixup of document store paths
                    StorePath tmpDocPath = D.find(part.getDocName());
                    tmpDocPath.fixupLocalToExternal(part, position);
                    this.D.save(part.getDocName(), tmpDocPath);

                    //fix up of word store paths
                    part.getWords().stream().distinct().forEach(w -> {
                        StorePath p = null;

                        //check if the word is mapped
                        String wm = wordMap.get(w);
                        if(wm == null){ wm = wordMap.inverse().get(w); }
                        if(wm == null){ wm = w; }
                        
                        //check if path is stored locally
                        if(queryCash.containsKey(wm)){                      
                            p = queryCash.get(wm);
                        }else{
                            //if not it has to be stored on server
                            try {
                                byte[] wordHash = crypto.hmac(wm);
                                SealedObject ep = serverStub.getPath(wordHash);
                                p = crypto.decryptPath(ep);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        
                        //error case there is no path found in system
                        if(p == null){
                            logger.log(Level.SEVERE, "no path found for fixup");
                            return;
                        }
                        
                        p.fixupLocalToExternal(part, position);
                        queryCash.put(wm, p);
                    });

                    //encrypt dp
                    SealedObject edp = crypto.encrypt(part);

                    //store edp on server
                    try {
                        serverStub.storeEDP(edp, position);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });           
            }else{
                //if enough space in scramble buffer just add the dp
                scrambleBuffer.put(dp);                
            }            
        }

        //store document path locally
        this.D.save(file.getName(), docPath);
        logger.log(Level.INFO, file.getName() + "added to database D");

        //empty the cash
        emptyQueryCash();
    }

    /**
     * 
     * @param fileName
     */
    public void deleteDocument(String fileName){

        //null guard
        if(fileName == null){ return; }

        //get the store path of the document
        StorePath p = D.find(fileName);

        //error handling
        if(p == null){
            logger.log(Level.SEVERE, "no document path found");
            return;
        }

        //process each node separately
        for (PathNode pathNode : p.getPath()) {
            
            if(pathNode.isInternal){
                //internal node

                //fix the word path for each word in dp
                for (String w : pathNode.directPointer.getWords()) {
                    
                    //check the word mapping
                    String usedWord = wordMap.get(w);
                    if(usedWord == null){ wordMap.inverse().get(w); }
                    if(usedWord == null){ usedWord = w;}

                    StorePath wp = null;
                    if(queryCash.containsKey(usedWord)){
                        //cashed locally
                        wp = queryCash.get(usedWord);
                    }else{
                        //on server
                        try {
                            SealedObject ewp = serverStub.getPath(crypto.hmac(usedWord));
                            wp = crypto.decryptPath(ewp);
                        } catch (RemoteException e1) {
                            e1.printStackTrace();
                        }
                    }

                    if(wp == null){
                        //error case
                        continue;
                    }

                    //update path
                    wp.delete(pathNode);
                    queryCash.put(usedWord, wp);  
                }

                //delete the locally stored object
                scrambleBuffer.deleteDP(pathNode.directPointer);

            }else{
                //external node

                //generate server query
                StorePath tmp = new StorePath(null);
                tmp.addExternal(pathNode.serverPos);
                QueryPath query = pathGenerator.generateQuery(tmp, searchStructureHead);

                //query server
                List<DP> dps = new LinkedList<DP>();
                try {
                    Set<SealedObject> edps = serverStub.access(query);
                    
                    for (SealedObject edp : edps) {
                        DP ret = crypto.decryptEDP(edp);
                        if(ret.getDocName().equals(fileName)){ dps.add(ret); }
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                //fix the word path for each word in dp
                for (DP dp : dps) {
                    for (String w : dp.getWords()) {

                        //check the word mapping
                        String usedWord = wordMap.get(w);
                        if(usedWord == null){ wordMap.inverse().get(w); }
                        if(usedWord == null){ usedWord = w;}

                        StorePath wp = null;
                        if(queryCash.containsKey(usedWord)){
                            //cashed locally
                            wp = queryCash.get(usedWord);
                        }else{
                            //on server
                            try {
                                SealedObject ewp = serverStub.getPath(crypto.hmac(usedWord));
                                wp = crypto.decryptPath(ewp);
                            } catch (RemoteException e1) {
                                e1.printStackTrace();
                            }
                        }

                        if(wp == null){
                            //error case
                            continue;
                        }

                        //update path
                        wp.delete(pathNode);
                        queryCash.put(usedWord, wp);
                    }
                }

                //add node to freelist
                freeList.add(pathNode);            
            }
        }

        //delete local store path
        D.delete(fileName);        
    }
 
    /**
     * 
     * @param docname
     * @return
     */
    public ByteArrayOutputStream searchDocument(String docname){

        //search in local storage
        StorePath p = D.find(docname);
        if(p == null) return null;

        //generate a query from the store path
        QueryPath query = pathGenerator.generateQuery(p, searchStructureHead);

        List<DP> DPs = new LinkedList<DP>();

        if(query != null){
            //get query result set from server
            Set<SealedObject> EDPs = null;
            try {
                EDPs = serverStub.access(query);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            //decrypt query result set
            for (SealedObject edp : EDPs) {
                DPs.add(crypto.decryptEDP(edp));
            }

            //filter for true result set
            List<DP> tmp = new LinkedList<DP>();
            for (DP dp : DPs) {
                if(dp.getDocName().equals(docname)) tmp.add(dp);
            }
            DPs = tmp;
        }

        //combine with local stored document parts
        for (PathNode n : p.getPath()) {
            if(n.isInternal){
                DPs.add(n.directPointer);
            }    
        }

        //sort the document parts with the internal page ordering
        DPs.sort(new Comparator<DP>() {

            @Override
            public int compare(DP o1, DP o2) {
                return o1.getPos() - o2.getPos();
            }
            
        });

        //filter for duplicates
        List<DP> filteredList = new LinkedList<DP>();
        for (DP dp : DPs) {
            if(!filteredList.contains(dp)){ filteredList.add(dp); }
        }

        //concat them into one document
        return documentSplitter.combineDocument(filteredList, docname);
    }    

    /**
     * 
     * @return a list of document names stored in the document store D
     */
    public List<String> getDocumentNames(){
        List<byte[]> keysByte = D.getAllKeys();
        List<String> retVal = new LinkedList<String>();
        for (byte[] key : keysByte) {
            retVal.add(SerializationUtils.deserialize(key));
        }
        return retVal;
    }

       /**
     * 
     * @param searchWord
     * @return
     */
    public List<DP> searchWord(String searchWord){

        //check if there is a word mapping for the search word
        String usedWord = wordMap.get(searchWord);
        if(usedWord == null){ wordMap.inverse().get(searchWord); }
        if(usedWord == null){ usedWord = searchWord; }

        StorePath storePath = null;

        //check if the store path is in local cash
        if(queryCash.containsKey(usedWord)){
            storePath = queryCash.get(usedWord);
        }else{
            //get store path from server
            try {
                SealedObject encrStorePath = serverStub.getPath(crypto.hmac(usedWord));
                
                //if nothing was found at server the searchword is not in system
                if(encrStorePath == null){
                     return new LinkedList<>(); 
                }
                
                storePath = crypto.decryptPath(encrStorePath);
                //cash the store path which we use for the query
                queryCash.put(usedWord, storePath);
            } catch (RemoteException e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, "error in searchWord did not get a valid storePath from server");
                return new LinkedList<>();
            }
        }        

        //generate query path
        QueryPath query = pathGenerator.generateQuery(storePath, searchStructureHead);

        //query server
        List<DP> DPSet = new LinkedList<DP>();
        try {
            Set<SealedObject> edps = serverStub.access(query);
            for (SealedObject sealedObject : edps) {
                DPSet.add(crypto.decryptEDP(sealedObject));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }        

        //filter result set for search word to get the true result set
        //TODO: think of better (faster) filtering maybe with the path data
        DPSet = DPSet.stream().filter(dp -> {
                return dp.getWords().contains(searchWord);
            }).collect(Collectors.toList());

        //add the local document parts
        for (PathNode node : storePath.getPath()) {
            if(node.isInternal){ DPSet.add(node.directPointer); }
        }

        //empty cash
        emptyQueryCash();

        return DPSet;
    }


    /**
     * store the paths from the cash at server 
     * only do it if query cash is larger than the min size
     */
    public void emptyQueryCash(){
        //TODO:make option for query cash hold back size
        if(queryCash.size() >= 10){

            // load all paths from word map
            for (Map.Entry<String,String> entry : wordMap.entrySet()) {
                if(!queryCash.containsKey(entry.getKey())){
                    try {
                        SealedObject ep = serverStub.getPath(crypto.hmac(entry.getKey()));
                        StorePath p = crypto.decryptPath(ep);
                        queryCash.put(entry.getKey(), p);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                if(!queryCash.containsKey(entry.getValue())){
                    try {
                        SealedObject ep = serverStub.getPath(crypto.hmac(entry.getValue()));
                        StorePath p = crypto.decryptPath(ep);
                        queryCash.put(entry.getValue(), p);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }


            //query cash contains all the paths we need
            
            //reverse the mapping
            for (String w : wordMap.keySet()) {
                
                //get the mapped word
                String wm = wordMap.get(w);
                if(wm == null){ wm = wordMap.inverse().get(w); }
                
                //get both paths from query cash
                StorePath wp = queryCash.remove(w);
                StorePath wmp = queryCash.remove(wm);

                //store the paths crossed out in query cash
                queryCash.put(w, wmp);
                queryCash.put(wm, wp);
            }

            //reset word map -> no mapping at this point
            wordMap.clear();
            
            //temporarily save the elements which should be kept
            //calculate the number of elements kept in query cash
            //TODO: make option for this number
            int keep = ((queryCash.size() - 10) % 2 == 0) ? (10) : (11);
            Map<String, StorePath> keptEntries = new HashMap<String,StorePath>();
            Iterator<String> queryCashIterator = queryCash.keySet().iterator();
            for (int i = 0; i < keep; i++) {
                if(queryCashIterator.hasNext()){
                    String cur = queryCashIterator.next();
                    keptEntries.put(
                        cur,
                        queryCash.get(cur)    
                    );
                }
            }

            //delete the stored entries from queryCash
            for (String w : keptEntries.keySet()) {
                queryCash.remove(w);
            }

            //create a new mapping for the remaining entries in query cash
            Map<String, StorePath> tmpQueryCash = new HashMap<String,StorePath>();          
            //get the whole queryCash keyset
            String words[] = queryCash.keySet().toArray(new String[0]);
            
            //randomize array
            crypto.shuffleArray(words);
 
            for (int i = 0; i < words.length; i+=2) {
                tmpQueryCash.put(words[i], queryCash.get(words[i+1]));
                tmpQueryCash.put(words[i+1], queryCash.get(words[i]));

                wordMap.put(words[i], words[i+1]);
            }

            //overwrite the queryCash with the newly mapped one
            queryCash = tmpQueryCash;
            
            // store the queryCash entries on server
            for (Map.Entry<String, StorePath> e : queryCash.entrySet()) {
                try {
                    serverStub.storePath(
                        crypto.hmac(e.getKey()), 
                        crypto.encrypt(e.getValue())
                    );
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            }
            queryCash.clear();

            // restore the entries temporarily saved
            queryCash = keptEntries;
        }
    }


    /**
     * resets the system and deletes all the stored data
     */
    public void purge(){

        //delete the databases
        D.clear();

        //reset the in memory objects
        wordMap.clear();
        freeList.clear();
        scrambleBuffer.clear();
        queryCash.clear();

        //send purge to server
        try {
            serverStub.purge();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client(); 
    }
}