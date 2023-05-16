package com.protose;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.protose.shared.DP;

public class ScrambleBuffer implements Serializable{
    
    private DP[] buffer;
    private int top; //points at first free position
    private int nullfillNumber;
    // private Map localIndex;

    /**
     * 
     * @param size Number of EDPs which fit into the buffer
     * @param nullFactor Factor how much null elements should be mixed in, given in percent 0%-100%
     */
    public ScrambleBuffer(int size, int nullFactor){

        // this.serverStub = serverStub;
        // this.options = options;

        //buffer needs to be devisible by 2 for split
        if(size % 2 == 0){
            this.buffer = new DP[size];
        }else{
            this.buffer = new DP[size + 1];
        }

        // this.nullfillFactorPercent = nullFactor;
        this.nullfillNumber = (int) (buffer.length * ((float) nullFactor/100));

        top = 0;

    }

    /**
     * returns the position where the dp was stored
     * @param in
     * @return
     */
    public int put(DP in){

        buffer[top] = in;
        top++;

        //check if fill is necessary
        if(top + nullfillNumber >= buffer.length){
            fillWithDecoys();
        }

        return top - 1;
    }

    public boolean isFull(){
        return top >= buffer.length;
    }

    /**
     * apply Durstenfeld shuffle on the buffer array
     */
    public void shuffle(){
        Random rand = new Random();
        DP tmp;

        for (int i = buffer.length-1; i > 0 ; i--) {
            int pick = rand.nextInt(i);
            tmp = buffer[pick];
            buffer[pick] = buffer[i];
            buffer[i] = tmp;
        }
    }

    /**
     * 
     * @return
     */
    public List<DP> get(){

        List<DP> retVal = new LinkedList<DP>();

        for (int i = buffer.length / 2; i < buffer.length; i++) {
            retVal.add(buffer[i]);
            buffer[i] = null;
        }

        //reset top pointer
        top = buffer.length / 2;

        return retVal;
    }

    /**
     * 
     * @return
     */
    public List<DP> readAll(){
        return Arrays.asList(buffer);
    }

    /**
     * 
     */
    private void fillWithDecoys(){
        for (int i = top; i < buffer.length; i++) {
            buffer[i] = DP.createDecoy();
        }
        top = buffer.length;
    }

    /**
     * 
     * @param dp
     */
    public void deleteDP(DP dp){
        for (int i = 0; i < buffer.length; i++) {
            if(buffer[i] != null && buffer[i].equals(dp)){
                buffer[i] = DP.createDecoy();
            }
        }
    }

    /**
     * 
     */
    public void clear(){
        for (int i = buffer.length - 1; i >= 0; i--) {
            buffer[i] = null;
        }
        top = 0;
    }

    /**
     * 
     * @return
     */
    public int size(){
        return buffer.length;
    }

    /**
     * 
     * @return
     */
    public int freeSize(){
        return buffer.length - top;
    }
}