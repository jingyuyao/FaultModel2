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
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Jingyu Yao
 */
public class Mesh {
    private SubMesh[] meshes;
    private Vector3f[][] masterMesh;
    private FaultLine[] faultLines;
    private int meshSize;//total points
    
    public Mesh(int x, int y, int faults){
        masterMesh = new Vector3f[x][y];
        faultLines = new FaultLine[faults];
    }
    
    //Load the mesh and all necessary information
    public void load(BufferedImage heightMap){
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
