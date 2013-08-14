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
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import java.awt.Point;
import java.util.Random;

/**
 *
 * @author Jingyu Yao
 */
public class Mesh {
    public SubMesh[] meshes;
    private Point[] borders;
    public Vector3f[][] masterMesh;//x is height, (y,z) is location
    //faults should be ordered from center to top, then center to bottom
    private FaultLine[] faultLines;//Worry about 1 fault for now.. lol
    private int meshSize;//total points
    public float max = 0;
    
    private int sizeX, sizeY;
    
    //Position tracker
    private int posX = -1, posY = -1;
    
    private Random ran = new Random();
    
    //variables provided by Dr.Shaw
    private float wk_fmin = 0.0f, wk_fmax = 0.3f, wk_slp = 2.0f;
    private int wk_carrylength = 2;
    private float wk_a = wk_fmax;
    private float wk_c = (float) ((wk_fmax - wk_fmin) / (wk_fmax - 0.5 * wk_fmin) * wk_slp);
    private float wk_b = wk_c * wk_fmin;
    private float walker = 0;
    
    
    public Mesh(BufferedImage heightMap, BufferedImage faultMap, int faults){
        faultLines = new FaultLine[faults];
        meshes = new SubMesh[faults + 1];//initialize array
        
        //instantiate elements of array
        for(int index = 0; index < faults + 1; index++){
            meshes[index] = new SubMesh(heightMap.getWidth(),heightMap.getHeight(), new Vector3f(0f,0f,0f));
        }
        
        load(heightMap, faultMap);
        //testing
        //                for(int i = 0; i < heightMap.getWidth(); i++){
        //                    for(int j = 0; j < heightMap.getHeight(); j++){
        //                        System.out.print(masterMesh[i][j].x + " ");
        //                    }
        //                    System.out.println();
        //                }
        
        max = masterMesh[0][0].x;
        
        for(int i = 0; i < masterMesh.length; i++){
            for(int j = 0; j < masterMesh[0].length; j++){
                if(masterMesh[i][j].x > max){
                    max = masterMesh[i][j].x;
                }
            }
        }
    }
    
    //Load the mesh and all necessary information
    private void load(BufferedImage heightMap, BufferedImage faultMap){
        sizeX = heightMap.getWidth();
        sizeY = heightMap.getHeight();
        
        //create masterMesh
        masterMesh = new Vector3f[sizeY][sizeX];
        
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
        //only consider a single faultline
        boolean upOrBelow = true;
        for(int index = 0; index < meshes.length; index++){
            loadSubMeshes(meshes[index], upOrBelow, 0);
            upOrBelow = !upOrBelow;
        }
        
    }
    
    //loadSubMeshes
    //Current method only deal with a single faultline
    //m: mesh to change, up: above faultline or below fault line, line: distinguishing faultline
    private void loadSubMeshes(SubMesh m, boolean up, int line){
        if(up){
            for(int j = 0; j < masterMesh.length; j++){
                for(int i = 0; i < masterMesh[0].length; i++){
                    if(i <= faultLines[line].fault[j].y){
                        m.mesh[i][j] = masterMesh[i][j];
                    }
                }
            }
        }else{
            for(int j = 0; j < masterMesh.length; j++){
                for(int i = 0; i < masterMesh[0].length; i++){
                    if(i > faultLines[line].fault[j].y){
                        m.mesh[i][j] = masterMesh[i][j];
                    }
                }
            }
        }
    }
    
    //Erosion algorithm
    public Point erode(Point p){
        //position of the point being evaluated
        int i = p.x;
        int j = p.y;
        Vector3f vec = masterMesh[i][j];//vector at this point
        float h = vec.x;//current height
        float delx0;//displacement height
        float[] deltaH = new float[8];//change in height for surrounding
        float greatV;//greatest deltaH
        float distance;//distance between points
        Point nextP;//next point to process
        Vector3f nextVec;//vector at next point
        
        //get dealaH for 8 surrounding locations
        //begin at top left, clock-wise
        posClean();//reset posX and posY to -1
        for(int index = 0; index < deltaH.length; index++){
            try{
                deltaH[index] = h - masterMesh[i + posX][j + posY].x;
            }catch(Exception e){
                deltaH[index] = -1;//-1 if point is out of bound
            }
            posNext();//increment posX and posY correctly
        }
        
        //find largest deltaH & get pos to that location
        posClean();
        greatV = deltaH[0];
        for(int index = 1; index < deltaH.length; index++){
            if(deltaH[index] > greatV){
                greatV = deltaH[index];
                posNext();
            }
        }
        
        //all the info have been obtained, now its formula time
        //all negative value = this vector is the highest
        if(greatV > 0 && i + posX != -1 && j + posY != -1 && i + posX != masterMesh[0].length && j + posY != masterMesh.length){
            //setup next point to process/return
            nextP = new Point(i + posX, j + posY);
            //get the next vector to manupilate value
            nextVec = masterMesh[nextP.x][nextP.y];
            distance = (float)Math.sqrt(Math.pow(vec.y - nextVec.y,2) + Math.pow(vec.y - nextVec.z,2));
            
            //add appropriate sediment to walker
            walker += (wk_a * greatV + wk_b) / (greatV + wk_c) * greatV * (1/distance);
            
            //removent 1/wk_carrylength amount of sediment from walker to be changed
            delx0 = (1/wk_carrylength) * walker;
            walker -= delx0;
            
            //actually change the height of the vectors
            vec.setX(h - delx0);
            nextVec.setX(nextVec.x + delx0);
            
            //move to another point to process
            return nextP;
        }
        else{
//            System.out.println("dumped:" + walker);
            //since this is the lowest point
            //dump all sediment in walker here
            vec.setX(h + walker);
            walker = 0;
            
            //return self
            return p;
        }
//        else if(i + posX != -1 && j + posY != -1 && i + posX != masterMesh[0].length && j + posY != masterMesh.length){
//            //            System.out.println("dumped:" + walker);
//            
//            //setup next point to process/return
//            nextP = new Point(i + posX, j + posY);
//            //dump just a little more than the lowestest point so walker can
//            //keep going
//            if(Math.abs(greatV) > walker){
//                vec.setX(h + walker);
//                walker = 0;
//                return p;
//            }else{
//                delx0 = Math.abs(greatV) + 0.1f*walker;
//                
//                vec.setX(h + delx0);
//                walker -= delx0;
//                
//                return nextP;
//            }
//        }
//        walker = 0;
//        return p;
    }
    
    //Use the erode method to erode the mesh
    public void rain(){
        //random x and y coordinate on the mesh
        int i = ran.nextInt(masterMesh.length);
        int j = ran.nextInt(masterMesh[1].length);
        
        //initial random point
        Point oldP = new Point(i,j);
        //point after erode
        Point nextPoint = erode(oldP);
        
        //continues while nextPoint erode is different from oldP
        while(nextPoint.x != oldP.x && nextPoint.y != oldP.y){
            oldP = new Point(nextPoint.x,nextPoint.y);
            nextPoint = erode(oldP);
        }
    }
    
    //Move the submeshes
    public void move(){
        for(int index = 0; index < meshes.length; index++){
            meshes[index].move();
        }
        update();
    }
    
    //update the value of each point but does not "reconnect" them
    public void update(){
        //System.arraycopy is boss but we need the conditional
        for(int index = 0; index < meshes.length; index++){
            for(int i = 0; i < meshes[index].mesh.length; i++){
                for(int j = 0; j < meshes[index].mesh[0].length; j++){
                    if(meshes[index].mesh[i][j].x != -1){
                        masterMesh[i][j] = meshes[index].mesh[i][j];
                    }
                }
            }
        }
    }
    
    //helper methods
    private void posClean(){ posX = -1; posY = -1;}
    
    //clever position tracker for the loop =)
    private void posNext(){
        if(posX == 1){
            posY++;
            posX = -1;
        }else{
            posX++;
        }
        //skip the midpoint which is current location
        if(posX == 0 && posY == 0){
            posX = 1;
        }
    }
    
    //SCRAPED
    //Connect the SubMeshes and update masterMesh data
    public void connectMeshes(){
        for(int index = 0; index < meshes.length; index++){
            int x = (int)meshes[index].mesh[1][1].x - 1;
            int y = (int)meshes[index].mesh[1][1].y - 1;
            if(x == 0 || y == 0){
                return;//don waste resources
            }
            moveSub(meshes[index], new Point(x,y));
        }
    }
    
    //SCRAPED
    private void moveSub(SubMesh m, Point p){
        SubMesh temp = new SubMesh(m.mesh[0].length, m.mesh.length);
        for(int i = 0; i < m.mesh[0].length; i++){
            for(int j = 0; j < m.mesh.length; j++){
                try{
                    temp.mesh[i+p.x][j+p.y] = m.mesh[i][j];
                }catch(Exception e){
                    
                }
            }
        }
    }
    
    //change where the points are located in the masterMesh grid to
    //reflect their actual position
    public void sync(){
        Vector3f totalDisplacement = new Vector3f();
        
        for(int i = 0; i < meshes.length; i++){
            Vector3f.add(totalDisplacement, meshes[i].getDisplacement(), totalDisplacement);
        }
//        System.out.println(meshes[0].getDisplacement());
        //if the displacement is 3.5+ it is 4
        int xd,yd;//displacement in abs value
        float x = Math.abs(totalDisplacement.y), y = Math.abs(totalDisplacement.z);
        
        if(x - (int)x >= 0.5f){
            xd = (int)x + 1;
        }
        else{
            xd = (int)x;
        }
        
        if(y - (int)y >= 0.5f){
            yd = (int)y + 1;
        }
        else{
            yd = (int)y;
        }
//        System.out.println(xd + " " + yd);
        
        Vector3f[][] temp = new Vector3f[sizeY + (int)xd][sizeX + (int)yd];
        System.out.println(temp.length + " w" + temp[0].length);
        for(int i = 0; i < temp.length; i++){
            for(int j = 0; j < temp[0].length; j++){
                temp[i][j] = new Vector3f(-1f,0f,0f);
            }
        }
        
        boolean xCorrector = false, yCorrector = false;
        //variable to round
        int tx,ty;
//        float x,y;
        
        for(int i = 0; i < masterMesh.length; i++){
            for(int j = 0; j < masterMesh[0].length; j++){
                if(masterMesh[i][j].y < 0)
                    xCorrector = true;
                if(masterMesh[i][j].z < 0)
                    yCorrector = true;
                
                x = masterMesh[i][j].y;
                if(xCorrector)
                    x += xd;//add abs displacement value to it dont go out of bound
                y = masterMesh[i][j].z;
                if(yCorrector)
                    y += yd;
                
                if(x - (int)x >= 0.5f)
                    tx = (int)x + 1;
                else
                    tx = (int)x;
                
                if(y - (int)y >= 0.5f)
                    ty = (int)y + 1;
                else
                    ty = (int)y;
                
                temp[tx][ty] = masterMesh[i][j];
            }
        }
        masterMesh = temp;
        
        //instantiate elements of array
        for(int index = 0; index < meshes.length; index++){
            meshes[index] = new SubMesh(masterMesh.length, masterMesh[0].length, meshes[index].getMovement(), meshes[index].getDisplacement());
        }
        System.out.println(meshes[0].mesh.length + " s w" + meshes[0].mesh[0].length);
        
        //Now load the SubMeshes using top-down method
        //only consider a single faultline
        boolean upOrBelow = true;
        for(int index = 0; index < meshes.length; index++){
            loadSubMeshes(meshes[index], upOrBelow, 0);
            upOrBelow = !upOrBelow;
        }
        
    }
}
