/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package faultmodel2;

import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Jingyu Yao
 */
public class Mesh {
    private SubMesh[] meshes;
    private Vector3f[][] masterMesh;
    private FaultLine[] faultLines;//Worry about 1 fault for now.. lol
    private int meshSize;//total points
    
    public Mesh(BufferedImage heightMap, BufferedImage faultMap, int faults){
        faultLines = new FaultLine[faults];
        load(heightMap, faultMap);
    }
    
    //Load the mesh and all necessary information
    private void load(BufferedImage heightMap, BufferedImage faultMap){
        //create masterMesh
        masterMesh = new Vector3f[heightMap.getWidth()][heightMap.getHeight()];
        meshSize = (heightMap.getWidth() - 1) * (heightMap.getHeight() - 1);
        Color c;
        //add data into the masterMesh
        for (int i = 0; i < heightMap.getWidth(); i++) {
            for (int j = 0; j < heightMap.getHeight(); j++) {
                c = new Color(heightMap.getRGB(i, j));
                masterMesh[i][j] = new Vector3f(c.getBlue(),i,j);
            }
        }
        
        //Have to auto-load faults from a source
        //For now, use manual input for testing
        faultLines[0] = new FaultLine(513);
        for(int index2 = 0; index2 < faultLines[0].fault.length; index2++){
            faultLines[0].fault[index2] = new Vector2f(index2,index2);
        }
        
        //Now load the SubMeshes using top-down method
        
    }
    
    //Erosion algorithm
    public void erode(){
        
    }
    
    //Use the erode method to erode the mesh
    public void rain(){
        
    }
    
    //Move the submeshes
    public void move(){
        
    }
    
    //Connect the SubMeshes and update masterMesh data
    public Vector3f[][] connectMeshes(){
        
        return new Vector3f[1][1];
    }
}
