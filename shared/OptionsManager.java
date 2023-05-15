package com.protose.shared;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class OptionsManager{

    public enum OPTIONIDENT {
        
        //networking options
        RMI_REGISTRY_NAME, 
        SERVER_HOST, 
        SERVER_PORT,

        //server specific options
        SERVER_S_NAME,
        SERVER_S_PATH,
        SERVER_I_NAME,
        SERVER_I_PATH,
        SERVER_LOG_NAME,
        SERVER_LOG_PATH,
        SERVER_P_WIDTH,
        SERVER_P_DEPTH,
        SERVER_TMP_DIR,

        //client specific options
        CLIENT_LOG_FILE,
        CLIENT_D_NAME,
        CLIENT_D_PATH,
        CLIENT_TMP_DIR,
        CLIENT_KEYS_DIR,
        CLIENT_PATHGEN_HIDEDISTANCE,
        CLIENT_SCRAMBLEBUFFER_SIZE,
        CLIENT_SCRAMBLEBUFFER_NULLFACTOR,
        CLIENT_QUERYCASH_SIZE
    } 

    private static final Map<OPTIONIDENT,Pattern> compiledPatternMap;
    private static final Map<OPTIONIDENT,String> defaultValues;
    private Map<OPTIONIDENT,String> optionsMap;

    static {

        Map<OPTIONIDENT,Pattern> staticPatternMap = new HashMap<OPTIONIDENT,Pattern>();
        Map<OPTIONIDENT,String> staticDefaultValueMap = new HashMap<OPTIONIDENT,String>();

        staticPatternMap.put(
            OPTIONIDENT.RMI_REGISTRY_NAME, Pattern.compile("RMI_REGISTRY_NAME .+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.RMI_REGISTRY_NAME, "ProtoSE:RMI_SERVER_STUB");

        staticPatternMap.put(
            OPTIONIDENT.SERVER_HOST, Pattern.compile("SERVER_HOST ([0-9]{1,3}\\.){3}[0-9]{1,3}",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.SERVER_HOST, "localhost");

        staticPatternMap.put(
            OPTIONIDENT.SERVER_PORT, Pattern.compile("SERVER_PORT [0-9]+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.SERVER_PORT, "localhost");

        staticPatternMap.put(
            OPTIONIDENT.SERVER_S_NAME, Pattern.compile("SERVER_S_NAME \\.+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.SERVER_S_NAME, "S_DocumentStorage");

        staticPatternMap.put(
            OPTIONIDENT.SERVER_S_PATH, Pattern.compile("SERVER_S_PATH \\.+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.SERVER_S_PATH, "./srv/");

        staticPatternMap.put(
            OPTIONIDENT.SERVER_I_NAME, Pattern.compile("SERVER_I_NAME \\.+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.SERVER_I_NAME, "I_WordIndex");

        staticPatternMap.put(
            OPTIONIDENT.SERVER_I_PATH, Pattern.compile("SERVER_I_PATH \\.+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.SERVER_I_PATH, "./srv/");

        staticPatternMap.put(
            OPTIONIDENT.SERVER_LOG_NAME, Pattern.compile("SERVER_LOG_NAME \\.+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.SERVER_LOG_NAME, "protoSE.log");

        staticPatternMap.put(
            OPTIONIDENT.SERVER_LOG_PATH, Pattern.compile("SERVER_LOG_PATH \\.+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.SERVER_LOG_PATH, "./logs/");

        staticPatternMap.put(
            OPTIONIDENT.SERVER_P_WIDTH, Pattern.compile("SERVER_P_WIDTH \\d+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.SERVER_P_WIDTH, "1000");

        staticPatternMap.put(
            OPTIONIDENT.SERVER_P_DEPTH, Pattern.compile("SERVER_P_DEPTH \\d+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.SERVER_P_DEPTH, "1000");

        staticPatternMap.put(
            OPTIONIDENT.SERVER_TMP_DIR, Pattern.compile("SERVER_TMP_DIR \\.+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.SERVER_TMP_DIR, "./tmp");

        /*----------------------------------------------------------------- */

        staticPatternMap.put(
            OPTIONIDENT.CLIENT_LOG_FILE, Pattern.compile("CLIENT_LOG_FILE \\.+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.CLIENT_LOG_FILE, "./logs/protoSE.log");

        staticPatternMap.put(
            OPTIONIDENT.CLIENT_D_NAME, Pattern.compile("CLIENT_D_NAME \\.+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.CLIENT_D_NAME, "D_document_index");

        staticPatternMap.put(
            OPTIONIDENT.CLIENT_D_PATH, Pattern.compile("CLIENT_D_PATH \\.+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.CLIENT_D_PATH, "./srv");

        staticPatternMap.put(
            OPTIONIDENT.CLIENT_TMP_DIR, Pattern.compile("CLIENT_TMP_DIR \\.+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.CLIENT_TMP_DIR, "./tmp");

        staticPatternMap.put(
            OPTIONIDENT.CLIENT_KEYS_DIR, Pattern.compile("CLIENT_KEYS_DIR \\.+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.CLIENT_KEYS_DIR, "./keys");

        staticPatternMap.put(
            OPTIONIDENT.CLIENT_PATHGEN_HIDEDISTANCE, Pattern.compile("CLIENT_PATHGEN_HIDEDISTANCE \\d+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.CLIENT_PATHGEN_HIDEDISTANCE, "10");

        staticPatternMap.put(
            OPTIONIDENT.CLIENT_SCRAMBLEBUFFER_SIZE, Pattern.compile("CLIENT_SCRAMBLEBUFFER_SIZE \\d+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.CLIENT_SCRAMBLEBUFFER_SIZE, "10");

        staticPatternMap.put(
            OPTIONIDENT.CLIENT_SCRAMBLEBUFFER_NULLFACTOR, Pattern.compile("CLIENT_SCRAMBLEBUFFER_NULLFACTOR \\d+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.CLIENT_SCRAMBLEBUFFER_NULLFACTOR, "20");

        staticPatternMap.put(
            OPTIONIDENT.CLIENT_QUERYCASH_SIZE, Pattern.compile("CLIENT_QUERYCASH_SIZE \\d+",
            Pattern.CASE_INSENSITIVE)
        );
        staticDefaultValueMap.put(OPTIONIDENT.CLIENT_QUERYCASH_SIZE, "10");

        compiledPatternMap  = Collections.unmodifiableMap(staticPatternMap);
        defaultValues = Collections.unmodifiableMap(staticDefaultValueMap);
    }

    public OptionsManager(File optionsFile){

        FileInputStream fis = null;
        
        try {
            fis = new FileInputStream(optionsFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(fis));

        List<String> optionLines = new ArrayList<String>();

        br.lines().filter(line -> {
            if(line.startsWith("#") || line.isBlank()){
                return false;
            }else{
                return true;
            }
        }).forEach(line -> {
            optionLines.add(line);
        });

        //create new map
        optionsMap = new HashMap<OPTIONIDENT,String>();

        //copy the values of the pattern map into the options map
        compiledPatternMap.entrySet().forEach(e -> {
            optionsMap.put(e.getKey(), null);
        });

        //go through each option line and match it against all patterns
        for (String optionLine : optionLines) {           

            //match against pattern and set options map
            compiledPatternMap.entrySet().forEach(e ->{
                if(e.getValue().matcher(optionLine).matches()){
                    String[] parts = optionLine.split(" ");
                    optionsMap.put(e.getKey(), parts[1].trim());
                }
            });

            //set the default values for options where no value was provided
            optionsMap.entrySet().forEach(e -> {
                if(e.getValue() == null){
                    optionsMap.put(e.getKey(), defaultValues.get(e.getKey()));
                }
            });
        }
    }

    /**
     * 
     * @param optionKey
     * @return
     */
    public String getOption(OPTIONIDENT optionKey){
        return optionsMap.get(optionKey);
    }

}
