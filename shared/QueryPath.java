package com.protose.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QueryPath implements Serializable{

    //TODO: differentiatie between the two forms
    public int startPos;
    public List<PathAction> actions;
    public List<Integer> positions;

    public QueryPath(){
        actions = new ArrayList<PathAction>();
        positions = new ArrayList<Integer>();
    }
    
    //could be done with direct access
    public void addAction(PathAction action){
        this.actions.add(action);
    }
}