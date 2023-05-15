package com.protose;

public class SearchStructure {
    
    private int width;
    private int depth;

    private Node[][] array;
    private int headW;
    private int headD;
    private int head;

    public SearchStructure(int width, int depth){
        headW = 0; headD = 0;
        this.width = width;
        array = new Node[width][depth];
    }

    public Node getNode(int position){
       return null;//TODO
    }

    public int getHead(){
        return this.head;
    }


    /**
     * Returns the Node at the given position
     * @param w
     * @param d
     * @return
     */
    public Node getNode(int w, int d){
        return array[w][d];
    }


    /**
     * stores the given node at the given position
     * if something is already stored at this position the data 
     * is overridden
     * if the given position is not reachable an error is thrown
     * @param toAdd
     * @param w
     * @param d
     */
    public void addNode(Node toAdd, int w, int d){

        if(w > width || d > depth){
            //TODO resize
        }


        Node cur = array[w][d];
        if(cur != null){
            //TODO delete entry in P db
        }

        //update head
        head++;
        if(w > headW || d > headD){
            headW = w;
            headD = d;
        }

        //write node
        array[w][d] = toAdd;
    }
}