/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package faultmodel2;

import java.awt.Point;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Jingyu Yao
 */
public class Neighbors {
    public Point[] neighborList;
    private SubMesh mesh;
    
    public Neighbors(SubMesh m){
        mesh = m;
        neighborList = findNeighbors();
    }
    
    //Pairs up the right neighbors of each individual points from
    //the two SubMeshes around a given faultline.
    private Point[] findNeighbors(){
        return new Point[1];
    }
    
    //Updates the pairing
    public void updateNeighbors(){
        
    }
}
