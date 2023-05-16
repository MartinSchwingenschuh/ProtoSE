package com.protose;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;

import com.protose.shared.DP;

/**
 * Document splitter
 */
public class DocumentSplitter {
    
    // private Crypto crypto;

    public DocumentSplitter(){

    }


    /**
     * 
     * @param toSplit
     * @return
     */
    public List<DP> splitDocument(File toSplit){

        List<DP> retVal = new ArrayList<DP>();

        try(PDDocument document = PDDocument.load(toSplit);){
    
            // Splitter Class
            Splitter splitting = new Splitter();
    
            // Splitting the pages into multiple PDFs
            List<PDDocument> pages = splitting.split(document);

            // Using a iterator to Traverse all pages
            Iterator<PDDocument> iteration = pages.listIterator();

            // Saving each page as an individual document
            int partPos = 0;
            while (iteration.hasNext()) {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                PDDocument pd = iteration.next();
                pd.save(outStream);
                byte[] arr = outStream.toByteArray();
                //TODO: add on disk mode

                retVal.add(new DP(arr, toSplit.getName(), partPos));
                partPos++;

                //close pdd document
                pd.close();
            }

            // document.close();

        }catch(Exception e){
            e.printStackTrace();
            System.out.println("ERROR IN DOCUMENT SPLITTER");
        }

        return retVal;
    }


    /**
     * 
     * @param input
     * @param docName
     * @return
     */
    public ByteArrayOutputStream combineDocument(List<DP> input, String docName){
        
        if(input.size() == 0) return null;
        
        ByteArrayOutputStream retVal = new ByteArrayOutputStream();

        //Instantiating PDFMergerUtility class
        PDFMergerUtility PDFmerger = new PDFMergerUtility();
        
        //Setting the destination file
        // PDFmerger.setDestinationFileName(docName);
        PDFmerger.setDestinationStream(retVal);
        // PDDocument root = new PDDocument();
        
        try{
            //adding the source files
            for (DP part : input) {
                
                if(part.isInMem()){
                    ByteArrayInputStream inStream = new ByteArrayInputStream(part.getPDFinMem());
                    PDFmerger.addSource(inStream);
                }else{
                    PDFmerger.addSource(part.getPDFPart());
                }                
            }            
                
            //Merging the documents
            PDFmerger.mergeDocuments(null);

        }catch(Exception e){
            e.printStackTrace();
        }
        
        return retVal;
    }
}