package com.protose.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class StorePath implements Serializable{
    
    private String pathIdent;
    private List<PathNode> path;

    public StorePath(String ident){
        this.pathIdent = ident;
        this.path = new ArrayList<PathNode>();
    }

    public void addLocal(DP docPart){
        // TODO: check for duplicates?
        PathNode n = new PathNode();
        n.directPointer = docPart;
        n.isInternal = true;
        path.add(n);
    }

    public void addExternal(Integer pos){
        PathNode n = new PathNode();
        n.isInternal = false;
        n.serverPos = pos;
        this.path.add(n);
    }

    public void delete(PathNode toRemove){
        path.remove(toRemove);
    }

    public void fixupLocalToExternal(DP local, int externPos){
        for (PathNode pathNode : path) {
            if( pathNode.directPointer != null &&
                    pathNode.directPointer.equals(local)){
                pathNode.isInternal = false;
                pathNode.directPointer = null;
                pathNode.serverPos = externPos;
                return;
            }
        }
    }

    public List<PathNode> getPath(){
        return this.path;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();

       sb.append("Identifier = " + this.pathIdent);

        return sb.toString();
    }

    /**
     * 
     * @return
     */
    public static PathNode getNewNode(){
        StorePath tmp = new StorePath(null);
        return tmp.new PathNode();
    }


    public class PathNode implements Serializable{
        public boolean isInternal;
        public DP directPointer;
        public int serverPos;

        @Override
        public boolean equals(Object obj) {

            PathNode other = (PathNode) obj;

            if(isInternal && other.isInternal){
                return directPointer.equals(other.directPointer);
            }else if(!isInternal && !other.isInternal){
                return serverPos == other.serverPos;
            }else{
                return false;
            }
        }
    }
    
}