/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package faultmodel2;

import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Jingyu Yao
 */
public class Neighbors {
    private Vector3f[][] neighborList;
    
    public Neighbors(FaultLine line){
        neighborList = findNeighbors(line);
    }
    
    //Pairs up the right neighbors of each individual points from
    //the two SubMeshes around a given faultline.
    private Vector3f[][] findNeighbors(FaultLine line){
        
        return new Vector3f[1][1];
    }
    
    //Updates the pairing
    public void updateNeighbors(FaultLine line){
        
    }
}
