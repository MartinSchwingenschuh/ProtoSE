package com.protose.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

import javax.crypto.SealedObject;

import com.protose.shared.QueryPath;


public interface IRemoteServer extends Remote{
    
    /**
     * returns the EDPs specified by the path
     * @param p
     * @return
     * @throws RemoteException
     */
    public Set<SealedObject> access(QueryPath p) throws RemoteException;
    
    /**
     * Stores the given edp at the given position
     * If the position is already populated the old 
     * data is overridden with the new and the 
     * old edp is deleted
     * @param edp
     * @param position
     * @throws RemoteException
     */
    public void storeEDP(SealedObject edp, int position) throws RemoteException;


    /**
     * Returns the path mapped by the given hash
     * this operation deletes the path on the server
     * @return
     * @throws RemoteException
     */
    public SealedObject getPath(byte[] hash) throws RemoteException;

    /**
     * 
     * @throws RemoteException
     */
    public void storePath(byte[] hash, SealedObject path) throws RemoteException;


    /**
     * TODO: what needs to happen when reducing
     * @throws RemoteException
     */
    public void reduce() throws RemoteException;

    /**
     * 
     * @return
     * @throws RemoteException
     */
    public int getSearchStructureHead() throws RemoteException;

    /**
     * 
     * @return
     * @throws RemoteException
     */
    public int getSearchStructureWidth() throws RemoteException;

}
