package com.protose.shared;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class DP implements Serializable{
    
    private File pdfPart;
    private byte[] pdfPartMem;
    private boolean inMemory;

    private String DocName;
    private int posInDoc;

    public DP(File pdfPart, String docName, int pos){
        this.pdfPart = pdfPart;
        inMemory = false;
        this.DocName = docName;
        this.posInDoc = pos;
    }

    public DP(byte[] pdfPart, String docName, int pos){
        this.pdfPartMem = pdfPart;
        inMemory = true;
        this.DocName = docName;
        this.posInDoc = pos;
    }

    public int getPos(){
        return this.posInDoc;
    }

    public String getDocName(){
        return this.DocName;
    }

    public boolean isInMem(){
        return this.inMemory;
    }

    public File getPDFPart(){
        return this.pdfPart;
    }

    public byte[] getPDFinMem(){
        return this.pdfPartMem;
    }

    /**
     * 
     * @return
     */
    public static DP createDecoy(){
        SecureRandom randGen = new SecureRandom();
        byte[] randomData = new byte[1000 + randGen.nextInt(10000)];
        randGen.nextBytes(randomData);
        return new DP(randomData, "decoyProtoSE.sys", 0);//TODO: options
    }

    /**
     * 
     * @return
     */
    public List<String> getWords(){

        List<String> retVal = new ArrayList<String>();

        if(this.isInMem()){
            try (PDDocument PDF = PDDocument.load(this.pdfPartMem);){
                //get a list of words which are in dp
                List<String> wordList = new ArrayList<String>();
                
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(PDF);
                wordList = Arrays.asList(text.split("\\s"));                    

                //pre process the word list
                // List<String> filteredWordList = new ArrayList<String>();
                wordList.stream().distinct().filter(w -> {
                    //remove single digits
                    Pattern pattern = Pattern.compile("[0-9]", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(w.trim());
                    return !matcher.matches();
                }).forEach(w -> {
                    //remove whitespaces
                    w = w.trim();

                    //remove '.' and ',' at the end of the word
                    if(w.endsWith(".")){
                        w = w.replace(".", "");
                    }else if(w.endsWith(",")){
                        w = w.replace(",", "");
                    }

                    if (!w.isBlank()) {
                        retVal.add(w);
                    }            
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }else{
            //TODO: on disk mode
        }

        return retVal;
    }

    @Override
    public boolean equals(Object obj) {
        
        DP other = (DP) obj;

        if(!DocName.equals(other.DocName)) return false;
        if(posInDoc != other.posInDoc) return false;
        return true;
    }

}
