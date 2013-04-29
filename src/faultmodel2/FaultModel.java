/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package faultmodel2;

import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;


/**
 *
 * @author your name
 */
public class FaultModel {
    private Mesh mesh;
    private BufferedImage heightMap;
    private BufferedImage faultMap;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        FaultModel mod = new FaultModel();
    }
    
    public FaultModel(){
        setUp();
    }
    
    //Setup the faultmodel including stuff from other class everything
    public void setUp(){
        try{
        //get the height map
        //String file = JOptionPane.showInputDialog("File path of heightmap(ex: MyDocuments/images/stuff.jpg):");
        String heightMapFile = "src/faultmodel2/test.jpg";//current: Terrain2.bmp, test.jpg
        heightMap = ImageIO.read(new File(heightMapFile));
//        String faultMapFile = "src/faultmodel2/heightmap2f.jpg";
//        faultMap = ImageIO.read(new File(faultMapFile));
        faultMap = new BufferedImage(1,1,1);//just for testing
        
        }catch(HeadlessException | IOException e) {
            e.printStackTrace();
        }
        mesh = new Mesh(heightMap,faultMap,1);
    }
    
    //Renders the model
    public void render(){
        
    }
    
    //This controls how much erode and move is called
    public void updateData(int erodeTime, int moveTime){
        
    }
}
