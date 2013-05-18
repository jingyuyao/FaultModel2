/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package faultmodel2;

import java.awt.Point;
import java.util.ArrayList;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Jingyu Yao
 */
public class SubMesh {
    public Vector3f[][] mesh;//public for easier access/modify
    private Vector3f movement;
    private Neighbors neighbors;
    
    //SubMesh's size should be same as masterMesh
    //initial x value is -1 to be indentified as null
    public SubMesh(int x, int y){
        mesh = new Vector3f[x][y];
        for(int i = 0; i < x; i++){
            for(int j = 0; j < x; j++){
                mesh[i][j] =new Vector3f(-1f,i,j);
            }
        }
        movement = new Vector3f();
    }
    
    //constructor with movement
    public SubMesh(int x, int y, Vector3f m){
        mesh = new Vector3f[x][y];
        for(int i = 0; i < x; i++){
            for(int j = 0; j < y; j++){
                mesh[i][j] =new Vector3f(-1f,0f,0f);
            }
        }
        setMovement(m);
    }
    
    //Returns the border of this SubMesh to be used in Neighbors class
    public Point[] getNeighbors(){
        ArrayList<Point> temp = new ArrayList<Point>();
        for(int i = 0; i < mesh[0].length; i++){
            for(int j = 0; j < mesh.length; j++){
                
            }
        }
        
        return new Point[1];
    }
    
    public Point getMidPoint(){
        return new Point((int)mesh.length/2, (int)mesh[0].length/2);
    }
    
    //Set how this SubMesh is moved
    //the values in the Vector3f represents the magnitude of movement
    public void setMovement(Vector3f move){
        movement = move;
    }
    
    public void move(){
        for(int i = 0; i < mesh.length; i++){
            for(int j = 0; j < mesh[0].length; j++){
                if(mesh[i][j].x != -1){
                    Vector3f.add(mesh[i][j], movement, mesh[i][j]);
                }
            }
        }
    }
}
