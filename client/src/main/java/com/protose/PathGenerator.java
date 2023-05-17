package com.protose;

import java.security.SecureRandom;
import java.util.List;

import com.protose.shared.Crypto;
import com.protose.shared.OptionsManager;
import com.protose.shared.QueryPath;
import com.protose.shared.StorePath;
import com.protose.shared.OptionsManager.OPTIONIDENT;
import com.protose.shared.StorePath.PathNode;

public class PathGenerator {

    private Crypto crypto;
    // private int hideDistance;
    private int serverWidth;
    private OptionsManager options;

    public PathGenerator(Crypto crypto, OptionsManager options, int serverWidth){
        this.crypto = crypto;
        // this.hideDistance = hideDistance;
        this.serverWidth = serverWidth;
        this.options = options;
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
        int configuredHideDistance = Integer.parseInt(options.getOption(OPTIONIDENT.CLIENT_PATHGEN_HIDEDISTANCE));

        //hide first and last node only if configured in options
        if(options.getOption(OPTIONIDENT.CLIENT_PATHGEN_FLHIDING).equals("true")){
            PathNode n0 = storePath.new PathNode();
            int d0 = (serverHead >= configuredHideDistance) ? (configuredHideDistance): serverHead;
            n0.serverPos = crypto.getRandomInt(d0);
            n0.isInternal = false;
            p.add(0, n0);
    
            PathNode nn = storePath.new PathNode();
            int d1 = (serverHead >= configuredHideDistance) ? (configuredHideDistance): serverHead;
            nn.serverPos = crypto.getRandomInt(d1);
            nn.isInternal = false;
            p.add(p.size(), nn);
        }

        // //expand the path between the nodes
        qp.startPos = p.get(0).serverPos;

        //mode selection
        if(options.getOption(OPTIONIDENT.CLIENT_PATHGEN_MODE).equals("PATHACC")){
            //path access mode
            //TODO: impl this?
        }else if(options.getOption(OPTIONIDENT.CLIENT_PATHGEN_MODE).equals("RANDACC")){
            //random access mode

            //add the required nodes
            for (PathNode pathNode : p) {
                //only process external nodes
                //and only if distinct
                if(!pathNode.isInternal && !qp.positions.contains(pathNode.serverPos)){ 
                    qp.positions.add(pathNode.serverPos); 
                }
            }

            //add additional random nodes
            int numberofRounds = 10;
            SecureRandom rand = new SecureRandom();
            for (int i = 0; i < numberofRounds; i++) {
                int randNumber = rand.nextInt(serverHead);
                if(!qp.positions.contains(randNumber)){
                    qp.positions.add(randNumber);
                }
            }
        }

        return qp;
    }
}