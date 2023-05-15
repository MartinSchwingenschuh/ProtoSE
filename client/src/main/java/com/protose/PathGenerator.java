package com.protose;

import java.util.List;

import com.protose.shared.Crypto;
import com.protose.shared.PathAction;
import com.protose.shared.QueryPath;
import com.protose.shared.StorePath;
import com.protose.shared.StorePath.PathNode;

public class PathGenerator {

    private Crypto crypto;
    private int hideDistance;
    private int serverWidth;

    //TODO: what do i need here?
    public PathGenerator(Crypto crypto, int hideDistance, int serverWidth){
        this.crypto = crypto;
        this.hideDistance = hideDistance;
        this.serverWidth = serverWidth;
    }


    /**
     * 
     * @param storePath
     * @param serverHead
     * @return
     */
    public QueryPath generateQuery(StorePath storePath, int serverHead) {

        //if the server head is 0 there is no valid path to generate
        if(serverHead == 0){ return null; }
        
        QueryPath qp = new QueryPath();
        List<PathNode> p = storePath.getPath();

        // randomize entries
        crypto.shuffleList(p);

        //hide first and last node by adding fake nodes
        // int real0 = p.get(0).serverPos;
        // int realn = p.get(p.size()).serverPos;

        PathNode n0 = storePath.new PathNode();
        n0.serverPos = crypto.getRandomInt(serverHead);//TODO: implement custom distance
        n0.isInternal = false;
        p.add(0, n0);

        PathNode nn = storePath.new PathNode();
        nn.serverPos = crypto.getRandomInt(serverHead);//TODO: implement custom distance
        nn.isInternal = false;
        p.add(p.size(), nn);

        // //expand the path between the nodes
        qp.startPos = p.get(0).serverPos;

        // qp.positions.add(qp.startPos);
        //TODO: this is just the access pattern which needs to be 
        //hidden
        for (PathNode pathNode : p) {
            //only process external nodes
            //and only if distinct
            if(!pathNode.isInternal && !qp.positions.contains(pathNode.serverPos)){ 
                qp.positions.add(pathNode.serverPos); 
            }
        }

        //TODO: think of a proper algorithm for the expansion
        // for (int i = 0; i < p.size()-1; i++) {
        //     PathNode p0 = p.get(i);
        //     PathNode p1 = p.get(i+1);

        //     int d0 = p0.serverPos / serverWidth;
        //     int w0 = p0.serverPos % serverWidth;

        //     int d1 = p1.serverPos / serverWidth;
        //     int w1 = p1.serverPos % serverWidth;

        //     //left right
        //     int deltaHorizontal = Math.abs(w1-w0);
        //     if(d1-d0 < 0){
        //         //left
        //         for (int j = 0; j < deltaHorizontal; j++) {
        //             qp.actions.add(PathAction.LEFT);
        //         }
        //     }else{
        //         //right
        //         for (int j = 0; j < deltaHorizontal; j++) {
        //             qp.actions.add(PathAction.RIGHT);
        //         }
        //     }            

        //     //up down
        //     int deltaVertical = Math.abs(d1-d0);
        //     if(d1-d0 < 0){
        //         //up
        //         for (int j = 0; j < deltaVertical; j++) {
        //             qp.actions.add(PathAction.UP);
        //         }
        //     }else{
        //         //down
        //         for (int j = 0; j < deltaVertical; j++) {
        //             qp.actions.add(PathAction.DOWN);
        //         }
        //     }     

        // }

        return qp;
    }


}
