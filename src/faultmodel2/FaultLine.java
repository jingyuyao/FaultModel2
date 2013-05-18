/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package faultmodel2;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Jingyu Yao
 */
public class FaultLine {
    public Vector2f[] fault;//public for easier access/modify
//    private Neighbors neighbors;
//    private SubMesh m1, m2;
    
    public FaultLine(int length){
        fault = new Vector2f[length];
//        findMeshes();
//        neighbors = new Neighbors(this);
    }
    
//    //Set m1 and m2 which are the two SubMesh this faultline is seperating
//    private void findMeshes(){
//        
//    }
    
}
