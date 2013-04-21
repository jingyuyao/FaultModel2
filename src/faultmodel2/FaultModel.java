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
        
    }
    
    public FaultModel(){
        setUp();
    }
    
    //Setup the faultmodel including stuff from other class everything
    public void setUp(){
        try{
        //get the height map
        //String file = JOptionPane.showInputDialog("File path of heightmap(ex: MyDocuments/images/stuff.jpg):");
        String file = "src/faultmodel/test.jpg";//current: Terrain2.bmp, test.jpg
        heightMap = ImageIO.read(new File(file));
        }catch(HeadlessException | IOException e) {
            e.printStackTrace();
        }
    }
    
    //Renders the model
    public void render(){
        
    }
    
    //This controls how much erode and move is called
    public void updateData(int erodeTime, int moveTime){
        
    }
}
