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
public class SubMesh {
    public Vector3f[][] mesh;//public for easier access/modify
    private Vector3f movement;
    
    //SubMesh's size should be same as masterMesh for convience
    public SubMesh(int x, int y){
        mesh = new Vector3f[x][y];
    }
    
    //Returns the border of this SubMesh to be used in Neighbors class
    public Vector3f[] getBorder(){
        
        return new Vector3f[1];
    }
    
    //Set how this SubMesh is moved
    //the values in the Vector3f represents the magnitude of movement
    public void setMovement(Vector3f move){
        movement = move;
    }
}
