/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package faultmodel2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;
import java.awt.Image;
import java.awt.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.Iterator;
import java.util.Scanner;
import javax.imageio.*;
import javax.swing.JFrame;

/**
 *
 * @author Jingyu Yao
 */
public class Mesh {
    //"null" point is defined with x = -1.0f, all algorithms ignores "null" points
    public SubMesh[] meshes;//[0]up, [1]down
    private Point[] borders;
    public Vector3f[][] masterMesh;//x is height, (y,z) is location
    private Vector3f[][] masterMeshCopy;
    //faults should be ordered from center to top, then center to bottom
    private FaultLine[] faultLines;//Worry about 1 fault for now.. lol
    public static Vector2f faultVector = new Vector2f(0f,250f);//x=m, y=b no small decimals
    //actual fault m=1.02 b=-220, simulation fault m=0 b=128
    public float iniMass;
    public ArrayList<Vector3f> rainStrip;
    public Vector3f moveFactor = new Vector3f(0f,0f,0f);//combines all the movement vector of the submeshes into one
    private Random ran = new Random();//random generator
    private boolean up = false;//default moving submesh = down
    private int meshSize;//total points
    public float max = 0;//max height on the mesh, used for shader
    private int sizeX, sizeY;//size of masterMesh
    private final int infiniteLoopStopper = 200;
    
    //info variables
    public int erodeCounter = 0, diffuseCounter = 0, outBoundCounter = 0, numException = 0;
    public int infLoop = 0, normRain = 0, normErode = 0, cover = 0, fill = 0;
    public float totalMassShift = 0.0f, massTaken = 0.0f, massDrop = 0.0f;
    
    //Position tracker
    private int posX = -1, posY = -1;
    
    //variables provided by Dr.Shaw
    //delx0 = (wk_a * h + wk_b) / (h + wk_c) * h * (1/distance);//diffusion equation
    private float wk_fmin = 0.0f, wk_fmax = 1f, wk_slp = 2.0f;
    private float wk_carrylength = 20;
    private float wk_a = wk_fmax;
    private float wk_c = (float) ((wk_fmax - wk_fmin) / (wk_fmax - 0.5 * wk_fmin) * wk_slp);
    private float wk_b = wk_c * wk_fmin;
    private float walker = 0;
    
    //diffusion variables
    private float diffuseConstant = 0.1f;//for avgDiffuse, between 0.01 ~ 0.05?
    private final int numberOfErodePerDiffuse = 10000;//ratio of erode:diffuse
    
    private final int numberOfAvalanchePerDiffuse = 1000000;
    private int avalancheCounter = 0;
    
    //flux and channel variables
    public int[][] flux;//how many times a rain passes a point
    private final float fluxMulti = 2f;//3-6 for channel, 47 max for ridge
    private final int channelCutOff = 10;
    private ArrayList<ArrayList<Point>> allChannels;
    private ArrayList<Float> channelCurve;
    ArrayList<Float> channelHeights;
    private float avgCurve = 0f;
    private boolean ridge = false;
    private int colorStage1 = 2, colorStage2 = 3;
    private static int time = 0;
    
    private JFrame frame;
    private String saveFolder = "outputData/set20";
    private int fileCounter = 0;
    
    
    public Mesh(BufferedImage heightMap, BufferedImage faultMap, int faults){
        //        System.out.println(diffuseAmount);
        faultLines = new FaultLine[faults];
        meshes = new SubMesh[faults + 1];//initialize array
        
        load(heightMap, faultMap);
        
        max = masterMesh[0][0].x;
        
        for(int i = 0; i < masterMesh.length; i++){
            for(int j = 0; j < masterMesh[0].length; j++){
                if(masterMesh[i][j].x > max){
                    max = masterMesh[i][j].x;
                }
            }
        }
        //        System.out.println(max);
        iniMass = totalMass();
        masterMeshCopy = masterMesh;
        //        masterMesh[0][0].x = 200;
        //        masterMesh[215][215].x = 200;
        frame = new JFrame("Yoloswag");
        frame.setSize(masterMesh.length,masterMesh[0].length);
        //        frame.setVisible(true);
//        System.out.println(masterMesh[100][100].x);
    }
    
    //Load the mesh and all necessary information
    private void load(BufferedImage heightMap, BufferedImage faultMap){
        if(!FaultModel.text){
            Random ran = new Random();
            sizeX = heightMap.getWidth();
            sizeY = heightMap.getHeight();
            //create masterMesh
            masterMesh = new Vector3f[sizeY][sizeX];
            flux = new int[sizeY][sizeX];
            
            meshSize = (heightMap.getWidth() - 1) * (heightMap.getHeight() - 1);
            Color c;
            float h;
            //add data into the masterMesh
            for (int i = 0; i < sizeX; i++) {
                for (int j = 0; j < sizeY; j++) {
                    c = new Color(heightMap.getRGB(i, j));
                    h = c.getBlue();
                    //                h = c.getBlue() + c.getGreen()*10 + c.getRed()*100;
                    if(h == 255) h = -1.0f;
                    //                if(h == 535500) h = -1.0f;
                    else h = h + ran.nextFloat()*0.5f;//disable for actual map? nope
                    masterMesh[i][j] = new Vector3f(h,i,j);
                }
            }
        }else{
            try(Scanner br = new Scanner(new FileReader(FaultModel.fileLoc))){
                float h;
                float none;
                br.next();
                sizeX = br.nextInt(); br.nextLine();
                //                System.out.println(sizeX);
                br.next();
                sizeY = br.nextInt(); br.nextLine();
                //                System.out.println(sizeY);
                br.nextLine(); br.nextLine();br.nextLine();
                br.next();
                none = br.nextFloat(); br.nextLine();
                //                System.out.println(none);
                //create masterMesh
                masterMesh = new Vector3f[sizeY][sizeX];
                flux = new int[sizeY][sizeX];
                
                for (int i = 0; i < sizeX; i++) {
                    for (int j = 0; j < sizeY; j++) {
                        h = br.nextFloat();
                        if(h == none) h = -1.0f;
                        else h -= 580f;//580 is min height
                        
                        masterMesh[j][i] = new Vector3f(h,j,i);
                    }
                    br.nextLine();
                }
                //                System.out.println(masterMesh.length);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        
        //instantiate elements of array
        for(int index = 0; index < 2; index++){
            meshes[index] = new SubMesh(sizeX,sizeY, new Vector3f(-1f,0f,0f));
        }
        
        //Have to auto-load faults from a source
        //For now, use manual input for testing
        //NOT USED
        faultLines[0] = new FaultLine(513);
        for(int index2 = 0; index2 < faultLines[0].fault.length; index2++){
            faultLines[0].fault[index2] = new Vector2f(index2,index2);
        }
        
        loadSubMeshesV2();
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
    
    //using the faultVector to split the masterMesh, still top-down method
    private void loadSubMeshesV2(){
        Vector2f cur;
        Vector3f p;
        
        //        int upCounter = 0, downCounter = 0;
        for(int j = 0; j < masterMesh.length; j++){
            for(int i = 0; i < masterMesh[0].length; i++){
                if(masterMesh[i][j].x != -1){
                    cur = new Vector2f(i, faultVector.x*i + faultVector.y);
                    p = masterMesh[i][j];
                    if(p.z > cur.y){
                        meshes[0].mesh[i][j] = p;
                        //                        upCounter++;
                    }else{
                        meshes[1].mesh[i][j] = p;
                        //                        downCounter++;
                    }
                }
            }
        }
        //System.out.println(upCounter + " " + downCounter);
        
    }
    
    //Erosion algorithm
    private Point erode(Point p){
        //position of the point being evaluated
        int i = p.x;
        int j = p.y;
        Vector3f vec = masterMesh[i][j];//vector at this point
        float h = vec.x;//current height
        float delx0;//displacement height
        float delx1;//sediment added to walker
        float[] deltaH;//change in height for surrounding
        float greatV;//greatest deltaH
        float distance;//distance between points
        Point nextP;//next point to process
        Vector3f nextVec;//vector at next point
        boolean inBound;
        
        //add value to flux
//        flux[i][j]++;
        
        deltaH = getDeltaH(h,i,j);
        
        greatV = getGreatV(deltaH);
        
        //        if(greatV != -100 && (i + posX != -1 && j + posY != -1 && i + posX != masterMesh[0].length && j + posY != masterMesh.length))
        //            inBound = true;
        //        else
        //            inBound = false;
        
        //all the info have been obtained, now its formula time
        //all negative value = this vector is the highest
        //        if(inBound){
        //if its not the lowest
        if(greatV > 0){
            //setup next point to process/return
            nextP = new Point(i + posX, j + posY);
            //get the next vector to manupilate value
            nextVec = masterMesh[nextP.x][nextP.y];
            distance = (float)Math.sqrt(Math.pow(vec.y - nextVec.y,2) + Math.pow(vec.z - nextVec.z,2) + Math.pow(vec.x - nextVec.x,2));
            
            //time to spice up wk_slp
            float run = (float)Math.sqrt(Math.pow(vec.y - nextVec.y,2) + Math.pow(vec.z - nextVec.z,2));
            float rise = vec.x - nextVec.x;
            wk_slp = Math.abs(rise/run);
            
            wk_c = (float) ((wk_fmax - wk_fmin) / (wk_fmax - 0.5 * wk_fmin) * wk_slp);
            //add appropriate sediment to walker
            delx0 = (wk_a * greatV + wk_b) / (greatV + wk_c) * greatV * (1/distance);
            walker += delx0;
            //remove 1/wk_carrylength amount of sediment from walker to be changed
            //I AM SO DUMB!~!!! CANNOT BELIEVE I MADE CARRYLENGTH AN INTEGER!!!!! FML
            delx1 = walker/wk_carrylength;
            walker -= delx1;
            totalMassShift += delx1;
            //                System.out.println("dis " + distance + " greatV " + greatV + " walker " + walker + " delx0 " + delx0);
            //actually change the height of the vectors
            vec.setX(h - delx0);
            massTaken += delx0;
            nextVec.setX(nextVec.x + delx1);
            massDrop += delx1;
            normErode++;
            //                    System.out.println(taker + " delx " + delx0);
            //move to another point to process
            return nextP;
        }
        else{//else if it is the lowest point
            //setup next point to process/return
            nextP = new Point(i + posX, j + posY);
            //if walker cant complete fill the hole
            if(-greatV > walker){
                vec.setX(h + walker);
                massDrop += walker;
                totalMassShift += walker;
                //                    System.out.println(walker);
                walker = 0;
                cover++;
                return p;
            }
            //dump just a little more than the lowestest point so walker can
            //keep going
            else{
                delx0 = -greatV;
                //                    System.out.println(walker);
                walker -= delx0;
                
                float littleMore = 0.1f * walker;
                
                delx0 += littleMore;
                walker -= littleMore;
                totalMassShift += delx0;
                
                fill++;
                vec.setX(h + delx0);
                massDrop += delx0;
                return nextP;
            }
        }
        
        //        }else{
        //            vec.setX(h + walker);
        //            massDrop += walker;
        //            walker = 0;
        //            outBoundCounter++;
        //            return p;
        //        }
    }
    
    //return the change in height of the surrounding in an array
    private float[] getDeltaH(float h, int i, int j){
        float[] deltaH = new float[8];
        //get dealaH for 8 surrounding locations
        //begin at top left, clock-wise
        posClean();//reset posX and posY to -1
        for(int index = 0; index < deltaH.length; index++){
            try{
                if(masterMesh[i + posX][j + posY].x != -1f){
                    deltaH[index] = h - masterMesh[i + posX][j + posY].x;//negative means higher
                }else{
                    deltaH[index] = -1000f;
                }
            }catch(Exception e){
                deltaH[index] = -1000f;//-100 if point is out of bound
            }
            posNext();//increment posX and posY correctly
        }
        return deltaH;
    }
    
    //find the greatest value in the array
    //maintain posX and posY values
    private float getGreatV(float[] deltaH){
        //find largest deltaH
        //***********This also made it impossible to go out of bound***************
        float greatV = deltaH[0];
        int spot = 0;
        
        for(int index = 1; index < deltaH.length; index++){
            if(deltaH[index] > greatV){
                greatV = deltaH[index];
                spot = index;
            }
        }
        
        
        posClean();
        //get pos to that location
        for(int as = 0; as < spot; as++)
            posNext();
        
        return greatV;
    }
    
    //Use the erode method to erode the mesh
    public void rain(){
        //Used to trace the rain during rendering
        //        rainStrip = new ArrayList<>();//set to null by FaultModel
        int loopCounter = 0;
        rainStrip = new ArrayList<>();
        //random x and y coordinate on the mesh
        int i = ran.nextInt(masterMesh.length);
        int j = ran.nextInt(masterMesh[1].length);
        
        //algorithm ignores -1.0f but if u put a rain drop there, it is still gonna have effect
        if(masterMesh[i][j].x == -1.0f) return;
        
        //initial random point
        Point oldP = new Point(i,j);
        rainStrip.add(masterMesh[i][j]);
        
        if(erodeCounter % numberOfErodePerDiffuse == 0){
            diffuse();
            diffuseCounter++;
        }
//        if(erodeCounter % numberOfAvalanchePerDiffuse == 0){
//            avalanche();
//            avalancheCounter++;
//        }
        
        
        //point after erode
        Point nextPoint = erode(oldP);
        rainStrip.add(masterMesh[nextPoint.x][nextPoint.y]);
        //continues while nextPoint erode is different from oldP
        while(nextPoint.x != oldP.x && nextPoint.y != oldP.y && loopCounter <= infiniteLoopStopper){
            oldP = new Point(nextPoint.x,nextPoint.y);
            nextPoint = erode(oldP);
            rainStrip.add(masterMesh[nextPoint.x][nextPoint.y]);
            loopCounter++;
        }
        //        System.out.println(rainCounter);
        if(loopCounter == infiniteLoopStopper){
            infLoop++;
        }else{
            normRain++;
        }
        //        walker = 0;
        
        erodeCounter++;
        
        FaultModel.rainTrace[FaultModel.rainTraceNum] = rainStrip;
        if(FaultModel.rainTraceNum == FaultModel.rainTrace.length - 1){
            FaultModel.rainTraceNum = 0;
        }else{
            FaultModel.rainTraceNum++;
        }
        //        System.out.println(walker);
    }
    
    //rain method for testing
    public void rainT(){
        flux = new int[sizeY][sizeX];
        Point oldP;
        Point nextPoint;
        Point prevP;
        int loopCounter;
        //actual fake rain
        for(int i = 0; i < masterMesh.length; i++){
            for(int j = 0; j < masterMesh[1].length; j++){
                if(masterMesh[i][j].x != -1.0f){
                    oldP = new Point(i,j);
                    nextPoint = fakeErode(oldP);
                    prevP = oldP;
                    loopCounter = 0;
                    while(!nextPoint.equals(oldP) && loopCounter <= infiniteLoopStopper){
                        loopCounter++;
                        oldP = new Point(nextPoint.x,nextPoint.y);
                        nextPoint = fakeErode(oldP);
                        if(prevP.equals(nextPoint)){
                            break;
                        }
                        else prevP = oldP;
                    }
                    
                    erodeCounter++;
                }
            }
        }
    }
    
    //fake erosion used to test flux
    private Point fakeErode(Point p){
        //position of the point being evaluated
        int i = p.x;
        int j = p.y;
        Vector3f vec = masterMesh[i][j];//vector at this point
        float h = vec.x;//current height
        float[] deltaH;//change in height for surrounding
        float greatV;//greatest deltaH
        Point nextP;//next point to process
        flux[i][j]++;
        deltaH = getDeltaH(h,i,j);
        
        greatV = getGreatV(deltaH);
        
        nextP = new Point(i + posX, j + posY);
        //if its not the lowest
        if(greatV >= 0){
            return nextP;
        }
        else{//else if it is the lowest point
            masterMesh[i][j].x += -greatV;
            return p;
        }
    }
    
    //total avgDiffuse
    public void diffuse(){
        for(int i = 0; i < masterMesh.length - 1; i++){
            for(int j = 0; j < masterMesh[0].length - 1; j++){
                avgDiffuse(i,j);
            }
        }
    }
    
    //avg neighbor height point base
    public void avgDiffuse(int i, int j){
        Vector3f vec = masterMesh[i][j];//vector at this point
        if(vec.x == -1.0f) return; //don run on "null" points
        float h = vec.x;//cur height
        float ht = 0;//avg height of neighbors
        int avaP = 0;//number of neighbors, prevents excessive diffuse on borders
        posClean();
        posNext();
        for(int abc = 0; abc < 4; abc++){
            try{
                if(masterMesh[i + posX][j + posY].x != -1f){
                    ht += masterMesh[i + posX][j + posY].x;
                    avaP++;
                }
            }catch(Exception e){
            }
            if(abc == 1){
                posNext();
            }else{
                posNext();
                posNext();
            }
        }
        float hg = (h - ht/avaP) * diffuseConstant;
        if(hg > 0){
            //            if(hg > maxDiffuse){hg = maxDiffuse;}
            posClean();
            posNext();
            float perNeighbor = (0.25f * (hg));
            //            if(perNeighbor > wk_fmax){perNeighbor = wk_fmax;}
            for(int abc = 0; abc < 4; abc++){
                try{
                    if(masterMesh[i + posX][j + posY].x != -1f){
                        masterMesh[i + posX][j + posY].x += perNeighbor;
                        vec.x -= perNeighbor;
                    }
                }catch(Exception e){
                }
                if(abc == 1){
                    posNext();
                }else{
                    posNext();
                    posNext();
                }
            }
        }
    }
    
    public void avalanche(){
        for(int i = 0; i < masterMesh.length - 1; i++){
            for(int j = 0; j < masterMesh[0].length - 1; j++){
                float curH = masterMesh[i][j].x;
                float min = 10000f;
                float h;
                int minPos = -1;
                posClean();//reset posX and posY to -1
                for(int index = 0; index < 8; index++){
                    try{
                        h = masterMesh[i + posX][j + posY].x;
                        if(h != -1f && h < min){
                            min = h;
                            minPos = index;
                        }
                    }catch(Exception e){
                    }
                    posNext();//increment posX and posY correctly
                }
                
                if(minPos != -1){
                    posClean();
                    for(int dix = 0; dix < minPos; dix++){
                        posNext();
                    }
                    h = masterMesh[i + posX][j + posY].x;
                    float dH = (curH - h) / 2;
                    masterMesh[i][j].x -= dH;
                    masterMesh[i + posX][j + posY].x += dH;
                    
                }
                
            }
        }
    }
    
    //depreciated
    //Move the submeshes
    public void move(){
        for(int index = 0; index < meshes.length; index++){
            meshes[index].move();
        }
        update();
    }
    
    //uplift the submeshes
    public void upLift(){
        for(int index = 0; index < meshes.length; index++){
            meshes[index].upLift();
        }
        update();
        sync();
        time++;
    }
    
    //update the value of each point but does not "reconnect" them
    private void update(){
        //System.arraycopy is boss but we need the conditional
        for(int index = 0; index < meshes.length; index++){
            for(int i = 0; i < meshes[index].mesh.length; i++){
                for(int j = 0; j < meshes[index].mesh[0].length; j++){
                    if(meshes[index].mesh[i][j].x != -1f){
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
    
    //change where the points are located in the masterMesh grid to
    //reflect their actual position
    public void sync(){
        //total movement of the submeshes
        Vector3f totalDisplacement = new Vector3f();
        
        for(int i = 0; i < meshes.length; i++){
            Vector3f.add(totalDisplacement, meshes[i].getDisplacement(), totalDisplacement);
        }
        
        //        System.out.println(totalDisplacement);
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
        
        //current solution is to make the mesh always a square
        //simplification is possible*****
        if(xd > yd)
            yd = xd;
        else
            xd = yd;
        
        Vector3f[][] temp = new Vector3f[sizeY + (int)xd][sizeX + (int)yd];
        int[][] tempFlux = new int[sizeY + (int)xd][sizeX + (int)yd];
        //        System.out.println(temp.length + " w" + temp[0].length);
        for(int i = 0; i < temp.length; i++){
            for(int j = 0; j < temp[0].length; j++){
                temp[i][j] = new Vector3f(-1f,i,j);
            }
        }
        
        boolean xCorrector = false, yCorrector = false;
        //variable to round
        int tx,ty;
        //        float x,y;
        
        for(int i = 0; i < masterMesh.length; i++){
            for(int j = 0; j < masterMesh[0].length; j++){
                if(masterMesh[i][j].x != -1){
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
                    tempFlux[tx][ty] = flux[i][j];
                }
            }
        }
        //replace the masterMesh with the new one
        masterMesh = temp;
        flux = tempFlux;
        
        //instantiate elements of array
        for(int index = 0; index < meshes.length; index++){
            //            meshes[index] = new SubMesh(masterMesh.length, masterMesh[0].length, meshes[index].getMovement(), meshes[index].getDisplacement());
            meshes[index] = new SubMesh(masterMesh.length, masterMesh[0].length, meshes[index].upLiftRate,meshes[index].scaleLength,meshes[index].faultSlipRate, meshes[index].getDisplacement());
        }
        //        System.out.println(meshes[0].mesh.length + " s w" + meshes[0].mesh[0].length);
        
        
        loadSubMeshesV2();
    }
    
    //get the total mass of the mesh, ignore -1.0f
    public float totalMass(){
        float sum = 0.0f;
        
        for(int i = 0; i < masterMesh.length; i++){
            for(int j = 0; j < masterMesh[0].length; j++){
                if(masterMesh[i][j].x != -1.0f){
                    sum += masterMesh[i][j].x;
                }
            }
        }
        //        System.out.println(masterMesh[0][0].x + " " + masterMesh[masterMesh.length-1][0].x
        //                + " " + masterMesh[0][masterMesh[0].length-1].x+ " " + masterMesh[masterMesh.length-1][masterMesh[0].length-1].x);
        return sum;
    }
    
    //called everytime movement vector of a submesh is changed
    private void setMoveFactor(){
        for(int i = 0; i < meshes.length; i++){
            if(meshes[i].getMovement() != null) Vector3f.add(moveFactor, meshes[i].getMovement(), moveFactor);
        }
    }
    
    //set movement of a specific mesh
    public void setMovement(int i, Vector3f v){
        meshes[i].setMovement(v);
        //careful, only allow one moving submesh
        if(i == 0) up = true;
        else up = false;
        setMoveFactor();
    }
    
    public static int getTime(){
        return time;
    }
    
    //get highest fluxes
    //@param multiplier
    public int[][] getDenseFlux(){
        float percentage;
        int avgCounter = 0;
        float avgPercentage = 0;
        float highestFluxPercentage = 0;
        int[][] fluxAnalysis = new int[flux.length][flux[0].length];
        
        
        //find avg percentage rate
        for(int i = 0; i < flux.length; i++){
            for(int j = 0; j < flux[0].length; j++){
                if(flux[i][j] != 0){
                    percentage = (float)(flux[i][j])/erodeCounter;
                    avgPercentage += percentage;
                    avgCounter++;
                    if(percentage > highestFluxPercentage) highestFluxPercentage = percentage;
                }
            }
        }
        
        //find avg percentage rate and initialize self flux by averaging
//        int f=0,c=0;
//        for(int i = 0; i < flux.length; i++){
//            for(int j = 0; j < flux[0].length; j++){
//                if(flux[i][j] != 0){
//                    posClean();
//                    f += flux[i][j];
//                    for(int id = 0; id < 8; id++){
//                        try{
//                            f += flux[i+posX][j+posY];
//                            c++;
//                        }catch(Exception e){}
//                        posNext();
//                    }
//                    if(c != 0)
//                        fluxAnalysis[i][j] = f / c;
//                    percentage = (float)(fluxAnalysis[i][j])/erodeCounter;
//                    avgPercentage += percentage;
//                    avgCounter++;
//                    if(percentage > highestFluxPercentage) highestFluxPercentage = percentage;
//                    f = 0;
//                    c = 0;
//                }
//            }
//        }
        
        avgPercentage = avgPercentage / avgCounter;
//        System.out.println(avgPercentage);
        //1st pass by percentage
        for(int i = 0; i < fluxAnalysis.length; i++){
            for(int j = 0; j < fluxAnalysis[0].length; j++){
                if(flux[i][j] != 0){
//                if(fluxAnalysis[i][j] != 0){
                    percentage = (float)(flux[i][j])/erodeCounter;
//                    percentage = (float)(fluxAnalysis[i][j])/erodeCounter;
                    if(ridge){
                        if(percentage < avgPercentage/fluxMulti){
                            fluxAnalysis[i][j] = flux[i][j];
                        }
//                        if(percentage > avgPercentage/fluxMulti){
//                            fluxAnalysis[i][j] = 0;
//                        }
                    }else{
                        if(percentage > avgPercentage*fluxMulti){
                            fluxAnalysis[i][j] = flux[i][j];
                        }
//                        if(percentage < avgPercentage*fluxMulti){
//                            fluxAnalysis[i][j] = 0;
//                        }
                    }
                }
            }
        }
        
        boolean solo;
        //2nd pass get rid of all the single spots
        for(int i = 0; i < fluxAnalysis.length; i++){
            for(int j = 0; j < fluxAnalysis[0].length; j++){
                solo = true;
                posClean();
                for(int index = 0; index < 8; index++){
                    try{
                        if(fluxAnalysis[i+posX][j+posY] != 0) solo = false;
                    }catch(Exception e){}
                    posNext();
                }
                if(solo == true) fluxAnalysis[i][j] = 0;
            }
        }
        
        return fluxAnalysis;
    }
    
    public void drawImage(){
        BufferedImage img = new BufferedImage(sizeX,sizeY,BufferedImage.TYPE_INT_RGB);
        Color colo;
        int f;
        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeY; j++) {
                if(masterMesh[i][j].x != -1f){
                    f = (int)(masterMesh[i][j].x);
                    colo = new Color(f,f,f);
                    img.setRGB(i, j, colo.getRGB());
                }
            }
        }
        try{
            ImageIO.write(img, "png", new File(saveFolder + "/ini.png"));
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    //draw flux
    public void drawChannel(int[][] f, boolean write){
        if(write){
            BufferedImage img;
//            if(!FaultModel.text)
//                img= FaultModel.heightMap;
//            else{
                img = new BufferedImage(sizeX,sizeY,BufferedImage.TYPE_INT_RGB);
                //need to make our own heightmap if its from data
                Color colo;
                float ff;
                for (int i = 0; i < sizeX; i++) {
                    for (int j = 0; j < sizeY; j++) {
                        if(masterMesh[i][j].x != -1f){
                            ff = masterMesh[i][j].x / max;
                            colo = new Color(ff,ff,ff);
                            img.setRGB(i, j, colo.getRGB());
                        }
                    }
                }
//            }
            Graphics2D g = img.createGraphics();
            g.setColor(Color.red);
            
            //render pass
            for(int i = 0; i < f.length; i++){
                for(int j = 0; j < f[0].length; j++){
                    if(f[i][j] != 0){
                        g.drawLine(i, j, i, j);
                    }
                }
            }
            try{
                ImageIO.write(img, "png", new File(saveFolder + "/" + fileCounter + ".png"));
            }catch(IOException e){
                e.printStackTrace();
            }
        }else{
            Graphics2D g = (Graphics2D)frame.getGraphics();
            g.setColor(Color.red);
            
            //render pass
            for(int i = 0; i < f.length; i++){
                for(int j = 0; j < f[0].length; j++){
                    if(f[i][j] != 0)
                        g.drawLine(i, j, i, j);
                }
            }
        }
    }
    
    //draw color coded channels
    public void drawChannel(ArrayList<ArrayList<Point>> a, boolean write){
        if(write){
            //            BufferedImage img = new BufferedImage(flux.length,flux[0].length, BufferedImage.TYPE_INT_ARGB);
            BufferedImage img;
//            if(!FaultModel.text)
//                img= FaultModel.heightMap;
//            else{
                img = new BufferedImage(sizeX,sizeY,BufferedImage.TYPE_INT_RGB);
                //need to make our own heightmap if its from data
                Color colo;
                float f;
                for (int i = 0; i < sizeX; i++) {
                    for (int j = 0; j < sizeY; j++) {
                        if(masterMesh[i][j].x != -1f){
                            f = masterMesh[i][j].x / max;
                            if(f>1f) f = 1f;
                            if(f<0f) f = 0f;
                            colo = new Color(f,f,f);
                            img.setRGB(i, j, colo.getRGB());
                        }
                    }
                }
//            }
            Graphics2D g = img.createGraphics();
            
            Iterator muahaha; Point plz;
            int x,y;
            
            
            for(int index = 0; index < a.size(); index++){
                //set color of channel
                g.setColor(new Color(ran.nextFloat()*0.7f+0.3f,ran.nextFloat()*0.7f+0.3f,ran.nextFloat()*0.7f+0.3f));
                muahaha = a.get(index).iterator();
                while(muahaha.hasNext()){
                    plz = (Point)muahaha.next();
                    x = plz.x; y = plz.y;
                    g.drawLine(x, y, x, y);
                }
            }
            
            try{
                ImageIO.write(img, "png", new File(saveFolder + "/" + fileCounter + ".png"));
            }catch(IOException e){
                e.printStackTrace();
            }
        }else{
            Graphics2D g = (Graphics2D)frame.getGraphics();
            int c = 0;
            Iterator muahaha; Point plz;
            int x,y;
            
            for(int index = 0; index < a.size(); index++){
                //set color of channel
                if(c == 0){
                    g.setColor(Color.red);
                }else if(c == 1){
                    g.setColor(Color.blue);
                }else if(c == 2){
                    g.setColor(Color.green);
                }else if(c == 4){
                    g.setColor(Color.magenta);
                }
                if(c == 4) c = 0;
                else c++;
                muahaha = a.get(index).iterator();
                while(muahaha.hasNext()){
                    plz = (Point)muahaha.next();
                    x = plz.x; y = plz.y;
                    g.drawLine(x, y, x, y);
                }
            }
        }
    }
    
    //draw color coded curvature data
    public void drawChannel(ArrayList<ArrayList<Point>> a, ArrayList<Float> c, boolean write){
        if(write){
            //            BufferedImage img = new BufferedImage(flux.length,flux[0].length, BufferedImage.TYPE_INT_ARGB);
            BufferedImage img;
//            if(!FaultModel.text)
//                img= FaultModel.heightMap;
//            else{
                img = new BufferedImage(sizeX,sizeY,BufferedImage.TYPE_INT_RGB);
                //need to make our own heightmap if its from data
                Color colo;
                float f;
                for (int i = 0; i < sizeX; i++) {
                    for (int j = 0; j < sizeY; j++) {
                        if(masterMesh[i][j].x != -1f){
                            f = masterMesh[i][j].x / max;
                            if(f>1f) f = 1f;
                            if(f<0f) f = 0f;
                            colo = new Color(f,f,f);
                            img.setRGB(i, j, colo.getRGB());
                        }
                    }
                }
//            }
            
            Graphics2D g = img.createGraphics();
            
            Iterator muahaha; Point plz;
            Iterator curveIter = c.iterator(); float imaboss;
            int x,y;
            
            for(int index = 0; index < a.size(); index++){
                muahaha = a.get(index).iterator();
                while(muahaha.hasNext()){
                    plz = (Point)muahaha.next();
                    imaboss = (float)curveIter.next();
                    // blue < 0 < green <= 1 < red <= 2 < pink
                    if(imaboss < 0)
                        g.setColor(Color.blue);
                    else if(imaboss > 0 && imaboss <= colorStage1)
                        g.setColor(new Color(0,imaboss/colorStage1,0));
                    else if(imaboss > colorStage1 && imaboss <= colorStage2)
                        g.setColor(new Color(imaboss/colorStage2,0,0));
                    else
                        g.setColor(Color.pink);
                    
                    x = plz.x; y = plz.y;
                    g.drawLine(x, y, x, y);
                }
            }
            
            //draw faultline
            g.setColor(Color.red);
            int yd;
            for(int xd = 0; xd < sizeX; xd++){
                yd = (int)(xd*faultVector.x + faultVector.y);
                g.drawLine(xd, yd, xd, yd);
            }
            
            try{
                ImageIO.write(img, "png", new File(saveFolder + "/" + fileCounter + ".png"));
            }catch(IOException e){
                e.printStackTrace();
            }
        }else{
            Graphics2D g = (Graphics2D)frame.getGraphics();
            Iterator muahaha; Point plz;
            int x,y;
            
            for(int index = 0; index < a.size(); index++){
                muahaha = a.get(index).iterator();
                while(muahaha.hasNext()){
                    plz = (Point)muahaha.next();
                    x = plz.x; y = plz.y;
                    g.drawLine(x, y, x, y);
                }
            }
        }
    }
    
    //find channel method, sort and organize dots found in draw method
    public void findChannels(boolean save, boolean draw){
//        System.out.println(masterMesh[100][100].x);
        int[][] dflux = getDenseFlux();
        channelHeights = new ArrayList<>();//arraylist to hold channel heights
        channelCurve = null;//curve and distance to fault
        allChannels = new ArrayList<>();
        int channelLength = 1;//cur length of a channl
        avgCurve = 0f;
        
        ArrayList<Point> curChannel;//maintain the ability to store list of channels
        float curMaxH,curMinH;
        Iterator iter; Point iterP;
        int hx,hy,lx,ly;//coordinates of highest and lowest points
        int nx,ny;
        
        //loop logic: go over every single point
        //if point have dense flux, find all the pts that are connected to it
        //store the "string" of points in an array
        //set the flux to 0 for each points stored so the points don end up in
        //multiple strings
        for(int i = 0; i < dflux.length; i++){
            for(int j = 0; j < dflux[0].length; j++){
                //if logic: find highest and lowest neighbor with flux
                //add each side, gg
                if(dflux[i][j] != 0){
                    dflux[i][j] = 0;//set flux to 0 after we done with it
                    //highest flux recursive way
                    while(findFluxNeighbor(dflux,i,j)){
                        curChannel = new ArrayList<>();
                        curChannel.add(new Point(i,j));//add current pt
                        nx = i; ny = j;
                        //find channel to highest flux
                        while(findFluxNeighbor(dflux,nx,ny)){
                            nx+=posX; ny+=posY;
                            curChannel.add(new Point(nx,ny));
                            dflux[nx][ny] = 0;
                            channelLength++;
                        }
                        //whether the found channel is actually a channel or not
                        //first condition, string of pixels is > channelCutOff
                        if(channelLength > channelCutOff){
                            iter = curChannel.iterator();//iterator of cur channel
                            iterP = (Point)curChannel.get(0);
                            hx = iterP.x; hy = iterP.y;
                            iterP = (Point)curChannel.get(curChannel.size() - 1);
                            lx = iterP.x; ly = iterP.y;
                            //only add if distance between start and finish is > channelCutOff
                            if(Math.sqrt(Math.pow(hx-lx,2) + Math.pow(hy-ly,2)) > channelCutOff/2){
                                //find deltaH
                                curMinH = 10000; curMaxH = 0;
                                while(iter.hasNext()){
                                    iterP = (Point)iter.next();
                                    if(masterMesh[iterP.x][iterP.y].x > curMaxH){
                                        curMaxH = masterMesh[iterP.x][iterP.y].x;
                                        
                                    }
                                    if(masterMesh[iterP.x][iterP.y].x < curMinH){
                                        curMinH = masterMesh[iterP.x][iterP.y].x;
                                        
                                    }
                                }
                                
                                channelHeights.add(curMaxH - curMinH);
                                //add cur channel to total channels
                                allChannels.add(curChannel);
                            }
                        }
                        channelLength = 1;
                    }
                }
            }
        }
        
        //AT LEAST 2 POINTS
        findCurve();
        
        if(save){
            save();
        }
        
        if(draw){
//            drawChannel(allChannels,true);
            drawChannel(allChannels,channelCurve,true);
        }
        fileCounter++;
        System.out.println("done");
    }
    
    //find curve at each point
    private void findCurve(){
        int ptCounter = 0;
        float curCurve;
        channelCurve = new ArrayList<>();
        Iterator chIter = allChannels.iterator();
        Iterator ptIter;
        ArrayList<Point> list;
        Point p1,p2;
        int dir;
        int dx,dy;
        while(chIter.hasNext()){
            list = (ArrayList<Point>)chIter.next();
            ptIter = list.iterator();
            p1 = (Point)ptIter.next();
            
            while(ptIter.hasNext()){
                p2 = (Point)ptIter.next();
                dx = p1.x - p2.x; dy = p1.y - p2.y;
                
                if((dx == 1 && dy == -1) || (dx == -1 && dy == 1))
                    dir = 1;
                else if((dx == 0 && dy == -1) || (dx == 0 && dy == 1))
                    dir = 2;
                else if((dx == -1 && dy == -1) || (dx == 1 && dy == 1))
                    dir = 3;
                else
                    dir = 4;
                channelCurve.add(calcCurve(p1.x,p1.y,dir));
                if(ptIter.hasNext()) p1 = p2;
                else{
                    //process the last poor lonely point ;(
                    dx = p2.x - p1.x; dy = p2.y - p1.y;
                    if((dx == 1 && dy == -1) || (dx == -1 && dy == 1))
                        dir = 1;
                    else if((dx == 0 && dy == -1) || (dx == 0 && dy == 1))
                        dir = 2;
                    else if((dx == -1 && dy == -1) || (dx == 1 && dy == 1))
                        dir = 3;
                    else
                        dir = 4;
                    curCurve = calcCurve(p2.x,p2.y,dir);
                    if(curCurve >= 0){
                        avgCurve += curCurve;
                        ptCounter++;
                    }
                    channelCurve.add(curCurve);
                }
            }
        }
        avgCurve = avgCurve/ptCounter;
//        System.out.println("avgCurve" + avgCurve);
    }
    
    //x,y direction, 1(topleft,downright) 2(top,bot) 3(topright,downleft) 4(left,right)
    private float calcCurve(int i, int j, int dir){
        float curveL = 0f, curveR = 0f;
        boolean lb = false, rb = false;
        int dx,dy;
        if(dir == 1){
            dx = 1; dy = 1;
        }
        else if(dir == 2){
            dx = 1; dy = 0;
        }
        else if(dir == 3){
            dx = -1; dy = 1;
        }
        else{
            dx =0; dy = 1;
        }
        try{
            if(masterMesh[i-dx-dx][j-dy-dy].x == -1.0f || masterMesh[i-dx][j-dy].x == -1.0f)
                lb = true;
            else
                curveL = masterMesh[i-dx-dx][j-dy-dy].x - 2*masterMesh[i-dx][j-dy].x + masterMesh[i][j].x;
        }catch(Exception e){
            lb = true;
        }
        try{
            if(masterMesh[i+dx+dx][j+dy+dy].x == -1.0f || masterMesh[i+dx][j+dy].x == -1.0f)
                rb = true;
            else
                curveR = masterMesh[i+dx+dx][j+dy+dy].x - 2*masterMesh[i+dx][j+dy].x + masterMesh[i][j].x;
        }catch(Exception e){
            rb = true;
        }
        if(!lb && !rb) return (curveL + curveR) / 2f;
        else if(!lb) return curveL;
        else return curveR;
    }
    
    private void save(){
        try{
            float avgHeight = 0;
            float curHeight;
            Iterator muahaha; Point plz;
            Iterator curveHahaha = null;
            if(channelCurve != null)
                curveHahaha = channelCurve.iterator();
            
            int x,y;
            
            FileWriter fw = new FileWriter(saveFolder + "/" + fileCounter + ".txt");
            BufferedWriter w = new BufferedWriter(fw);
            
            //print out param
            w.write("//fileLoc: " + FaultModel.fileLoc); w.newLine();
            w.write("//fluxMulti: " + fluxMulti + " channelCutOff: " + channelCutOff); w.newLine();
            w.write("//wk_fmin: " + wk_fmin + " wk_fmax: " + wk_fmax); w.newLine();
            w.write("//total_mass_shift: " + (totalMass() - iniMass)); w.newLine();
            w.write("//colorstage1: " + colorStage1 + " colorstage2: " + colorStage2); w.newLine();
            
            //first line: # of channel
            w.write("# " + allChannels.size()); w.newLine();
            w.write("avgCurve " + avgCurve);w.newLine();w.newLine();
            for(int index = 0; index < allChannels.size(); index++){
                curHeight = channelHeights.get(index);
                //each new channel begin with its index and height
                w.write("c " + index + " " + curHeight); w.newLine();
                avgHeight += curHeight;
                muahaha = allChannels.get(index).iterator();
                while(muahaha.hasNext()){
                    plz = (Point)muahaha.next();
                    x = plz.x; y = plz.y;
                    //write each point of channel
                    if(curveHahaha == null)
                        w.write("p " + x + " " + y + " " + masterMesh[x][y].x);
                    else
                        w.write("p " + x + " " + y + " " + masterMesh[x][y].x + " " + (float)curveHahaha.next());
                    w.newLine();
                }
                w.newLine();//extra line inbetween channels for readibility
            }
            
            avgHeight = avgHeight / allChannels.size();//calculate avg height
            w.write("avg " + avgHeight);//write avg height
            w.newLine();
            w.write("end");//write end to end
            
            w.close();//close writer
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public boolean empty(int[][] a){
        for(int i = 0; i < a.length; i++){
            for(int j = 0; j < a[0].length; j++){
                if(a[i][j] != 0) return false;
            }
        }
        return true;
    }
    
    //max flux way
    //return whether or not that point exists
    private boolean findFluxNeighbor(int[][] f, int i, int j){
        float h = masterMesh[i][j].x;
        int greatV = 0;
        int spot = -1;
        posClean();
        for(int index = 0; index < 8; index++){
            try{
                if(f[i+posX][j+posY] > greatV){
                    greatV = f[i+posX][j+posY];
                    spot = index;
                }
            }catch(Exception e){}
            posNext();
        }
        if(spot == -1) return false;
        
        posClean();
        //get pos to that location
        for(int as = 0; as < spot; as++)
            posNext();
        return true;
    }
    
    //testing tool
    public void printPointInfo(){
        System.out.println("X Y");
        Scanner s = new Scanner(System.in);
        int x = s.nextInt();
        int y = s.nextInt();
        //        posClean();
        //        for(int index = 0; index < 8; index++){
        //            try{
        //                if(masterMesh[x+posX][y+posY].x != -1.0f){
        //                    System.out.println(posX + " " + posY + " " + masterMesh[x+posX][y+posY].x);
        //                }
        //            }catch(Exception e){}
        //            posNext();
        //        }
        try{
            System.out.println(masterMesh[x-2][y].x + " " + masterMesh[x-1][y].x + " " + masterMesh[x][y].x + " " + masterMesh[x+1][y].x + " " + masterMesh[x+2][y].x);
            float left = masterMesh[x-2][y].x - 2*masterMesh[x-1][y].x + masterMesh[x][y].x;
            System.out.println(left);
            float right = masterMesh[x][y].x - 2*masterMesh[x+1][y].x + masterMesh[x+2][y].x;
            System.out.println(right);
            System.out.println((left + right)/2);
        }catch(Exception e){}
    }
    
    //*************************SCRAPED methods*********************************
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
    
    //diffusion equation NOT USED***************************
    private float timeStep = 1f;
    private float gridSize = 1f;
    private float D = 10f;
    private float diffuseAmount = (float)(Math.pow(Math.E, (-(gridSize*gridSize))/(4*D*timeStep)) / (4*Math.PI*D*timeStep));
    private float maxDiffuse = 3 * wk_fmax;//for diffuse4, not used
    
    //diffuse based on height
    //not very good
    public void diffuse4(Point p){
        int i = p.x;
        int j = p.y;
        Vector3f vec = masterMesh[i][j];//vector at this point
        //        diffuseAmount = calcDiffuse(timeStep, gridSize,D);
        float change = diffuseAmount*masterMesh[i][j].x;
        posClean();
        posNext();
        
        //        if(change > 0.5f) change = 0.5f;
        float perNeighbor = (0.25f * change);//some constant
        for(int abc = 0; abc < 4; abc++){
            try{
                if(masterMesh[i + posX][j + posY].x != -1f){
                    masterMesh[i + posX][j + posY].x += perNeighbor;
                    vec.x -= perNeighbor;
                }
            }catch(Exception e){
            }
            if(abc == 1){
                posNext();
            }else{
                posNext();
                posNext();
            }
        }
        
    }
    
    //least difference of neighbor diffuse
    //weird
    public void diffuse2(Point p){
        int i = p.x;
        int j = p.y;
        Vector3f vec = masterMesh[i][j];//vector at this point
        float h = vec.x;
        float leastPositive = 1000f;//arbitrary large value
        float ht;
        
        posClean();
        posNext();
        for(int abc = 0; abc < 4; abc++){
            try{
                ht = masterMesh[i + posX][j + posY].x;
                if(ht != -1f && h - ht > 0 && h - ht < leastPositive){
                    leastPositive = h - ht;
                }
            }catch(Exception e){
            }
            if(abc == 1){
                posNext();
            }else{
                posNext();
                posNext();
            }
        }
        
        if(leastPositive != 1000f && leastPositive < 3 && leastPositive > 0.3){
            
            posClean();
            posNext();
            float perNeighbor = (0.25f * (leastPositive/2));
            for(int abc = 0; abc < 4; abc++){
                try{
                    if(masterMesh[i + posX][j + posY].x != -1f){
                        masterMesh[i + posX][j + posY].x += perNeighbor;
                        vec.x -= perNeighbor;
                    }
                }catch(Exception e){
                }
                if(abc == 1){
                    posNext();
                }else{
                    posNext();
                    posNext();
                }
            }
        }
    }
    
    //total diffuse-loops over every single point in mesh-zigzag style
    //lmao works so nice, almost TOO nice
    public void diffuse3(){
        diffuseAmount = calcDiffuse(timeStep, gridSize,D);
        for(int i = 0; i < masterMesh.length - 1; i++){
            for(int j = 0; j < masterMesh[0].length - 1; j++){
                singleDiffuse(i,j,diffuseAmount*100);
            }
        }
        timeStep++;
    }
    
    //diffuse a single point, no limit,scales with total height and fickian equation
    private void singleDiffuse(int i, int j, float d){
        //        float change = masterMesh[i][j].x - (0.00776127f - d)* 10 *masterMesh[i][j].x;
        //        float change = masterMesh[i][j].x - d*masterMesh[i][j].x;//legit one
        float change = (0.8f-d)*masterMesh[i][j].x;
        float perNeighbor;
        //                if(change > 10){change = 10;}
        posClean();
        posNext();
        int count = 0;//if its on edge split change in 3 instead of 4
        for(int abc = 0; abc < 4; abc++){
            try{
                if(masterMesh[i + posX][j + posY].x != -1f){
                    count++;
                }
            }catch(Exception e){
            }
            if(abc == 1){//middle is already skipped by default so....
                posNext();
            }else{
                posNext();
                posNext();
            }
        }
        perNeighbor = (change/count);
        
        posClean();
        posNext();
        for(int abc = 0; abc < count; abc++){
            try{
                if(masterMesh[i + posX][j + posY].x != -1f){
                    masterMesh[i + posX][j + posY].x += perNeighbor;
                    masterMesh[i][j].x -= perNeighbor;
                }
            }catch(Exception e){
            }
            if(abc == 1){//middle is already skipped by default so....
                posNext();
            }else{
                posNext();
                posNext();
            }
        }
    }
    
    //diffuse3 in spiral
    //NOT USED
    public void diffuse5(){
        int spiral = 1;
        boolean reverse = false;
        diffuseAmount = calcDiffuse(timeStep, gridSize,D);
        int i = (masterMesh.length - 1) / 2;
        int j = (masterMesh[0].length - 1) / 2;
        
        {//First point
            singleDiffuse(i,j,diffuseAmount);
        }
        //spiral begins
        while(i < masterMesh.length - 1 && j < masterMesh[0].length - 1 && i > 0 && j > 0){
            for(int w = 0; w < spiral; w++){
                if(!reverse)i++;
                else i--;
                singleDiffuse(i,j,diffuseAmount);
            }
            for(int l = 0; l < spiral; l++){
                if(!reverse)j++;
                else j--;
                singleDiffuse(i,j,diffuseAmount);
            }
            reverse = !reverse;
            spiral++;
        }
    }
    
    private float calcDiffuse(float t, float g, float d){
        return (float)(Math.pow(Math.E, (-(g*g))/(4*d*t)) / (4*Math.PI*d*t));
    }
}
