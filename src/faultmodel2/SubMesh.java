/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package faultmodel2;

import java.awt.Point;
import java.util.ArrayList;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Jingyu Yao
 */
public class SubMesh{
    public Vector3f[][] mesh;//public for easier access/modify
    private Vector3f movement;
    private Vector3f displacement = new Vector3f(0f,0f,0f);
    public float upLiftRate,scaleLength, faultSlipRate;
//    private Neighbors neighbors;
    
    //SubMesh's size should be same as masterMesh
    //initial x value is -1 to be indentified as null
    public SubMesh(int x, int y){
        mesh = new Vector3f[x][y];
        for(int i = 0; i < x; i++){
            for(int j = 0; j < x; j++){
                mesh[i][j] =new Vector3f(-1f,i,j);
            }
        }
        movement = new Vector3f(0,0,0);
        upLiftRate = scaleLength = faultSlipRate = 0;
    }
    
    //constructor with movement
    public SubMesh(int x, int y, Vector3f m){
        mesh = new Vector3f[x][y];
        for(int i = 0; i < x; i++){
            for(int j = 0; j < y; j++){
                mesh[i][j] = new Vector3f(-1f,0f,0f);
            }
        }
        setMovement(m);
        upLiftRate = scaleLength = faultSlipRate = 0;
    }
    
    public SubMesh(int x, int y, Vector3f m, Vector3f d){
        mesh = new Vector3f[x][y];
        for(int i = 0; i < x; i++){
            for(int j = 0; j < y; j++){
                mesh[i][j] = new Vector3f(-1f,0f,0f);
            }
        }
        setMovement(m);
        displacement = d;
        upLiftRate = scaleLength = faultSlipRate = 0;
    }
    
    public SubMesh(int x, int y, float a, float l, float v, Vector3f d){
        mesh = new Vector3f[x][y];
        for(int i = 0; i < x; i++){
            for(int j = 0; j < y; j++){
                mesh[i][j] = new Vector3f(-1f,0f,0f);
            }
        }
        upLiftRate = a;
        scaleLength = l;
        faultSlipRate = v;
        displacement = d;
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
    
    public void setUpLift(float a, float l, float v){
        upLiftRate = a;
        scaleLength = l;
        faultSlipRate = v;
    }
    
    //uplift function
    //    Original:
    //    dh/dt = f(x,y) = g(x-vt)q(y)
    //
    //    g(x)= {0             x<x_0
    //           a_0(x-x_0)    x<x_1
    //           a_1(x_1-x)    x_1<x<x_2
    //           0             x>x_2}
    //
    //    q(y)= {0             y<=0
    //           a_2(y_1-y)    y<y_1
    //           0             y>=y_1}
    
    //    x_0 = 0
    //    a_2 = 1
    //    a_0 = a_1 //reduce parameter
    //    x_1 = x_2 = y_1 //reduce parameter
    //    uplift rate: a (=a_0=a_1)
    //    scale length: L(=x_1=x_2=y_1)
    //    fault slip rate: v
    
    //    After Simplification:
    //    dh/dt = f(x,y) = g(x-vt)q(y)
    //    g(x)= {0             x<0
    //           a(L-x)        x<L
    //           a(x_1-x)      L<x<x_2
    //           0             x>2L}
    //
    //    q(y)= {0             y<=0
    //           L-y           y<L
    //           0             y>=L}
    
    //way more complex and accurate uplift function
    public void upLift(){
        float gX = 0, qY = 0;
        float x = 0,y = 0;
        for(int i = 0; i < mesh.length; i++){
            for(int j = 0; j < mesh[0].length; j++){
                
                if(mesh[i][j].x != -1){
                    x = findPerpendicularX(i,j);
                    y = distanceFromFault(i,j);

                    if(x < scaleLength){
                        gX = upLiftRate * (x - faultSlipRate*Mesh.getTime());
                        
                    }else{
                        gX = upLiftRate * (Math.abs(scaleLength - (x - scaleLength)) - faultSlipRate*Mesh.getTime());
                    }
                    
//                    gX = upLiftRate;
                    
                    if(y < scaleLength){
                        qY = scaleLength - y;
                    }else{
                        qY = 0;
                    }
                    
                    mesh[i][j].x += gX * qY;
                    
                    mesh[i][j].y += faultSlipRate;
                    mesh[i][j].z += faultSlipRate*Mesh.faultVector.x;
                    
                }
            }
        }
        displacement.y += faultSlipRate;
        displacement.z += faultSlipRate*Mesh.faultVector.x;
//        System.out.println(displacement);
    }
    
    public float distanceFromFault(int i, int j){
        float x = mesh[i][j].y;
        float y = mesh[i][j].z;
        Vector2f vec = Mesh.faultVector;
        //point to line distance formula
        return (float)(Math.abs(-vec.x*x + y - vec.y)/Math.sqrt(vec.x*vec.x + 1));
    }
    
    public float findPerpendicularX(int i, int j){
        Vector2f vec = Mesh.faultVector;
        
        float x = mesh[i][j].y;
        float y = mesh[i][j].z;
        float m = 1f / -vec.x;
        Vector2f perpenVec = new Vector2f(m, y - x*m);
        
        if(vec.x == 0f) return x;
        else return (m*x + perpenVec.y - vec.y) / vec.x;
    }
    
    public Vector3f getSamplePoint(){
        for(int i = 0; i < mesh.length; i++){
            for(int j = 0; j < mesh[0].length; j++){
                if(mesh[i][j].x != -1.0f)
                    return mesh[i][j];
            }
        }
        return mesh[0][0];
    }
    
    public float getSlipRate(){
        return faultSlipRate;
    }
    
    //depreciated
    //Set how this SubMesh is moved
    //the values in the Vector3f represents the magnitude of movement
    public void setMovement(Vector3f move){
        movement = move;
    }
    
    public Vector3f getMovement(){
        return movement;
    }
    
    public void move(){
        for(int i = 0; i < mesh.length; i++){
            for(int j = 0; j < mesh[0].length; j++){
                if(mesh[i][j].x != -1){
                    Vector3f.add(mesh[i][j], movement, mesh[i][j]);
                }
            }
        }
        Vector3f.add(movement, displacement, displacement);
    }
    
    public Vector3f getDisplacement(){
        return displacement;
    }
}
