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
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author Jingyu Yao
 */
public class Mesh {
    public SubMesh[] meshes;
    private Point[] borders;
    public Vector3f[][] masterMesh;//x is height, (y,z) is location
    private Vector3f[][] masterMeshCopy;
    //faults should be ordered from center to top, then center to bottom
    private FaultLine[] faultLines;//Worry about 1 fault for now.. lol
    private Vector2f faultVector;
    float iniMass;
    public ArrayList<Vector3f> rainStrip;
    
    //"null" point is defined with x = -1.0f, all algorithms ignores "null" points
    
    private int meshSize;//total points
    public float max = 0;
    
    public int erodeCounter = 0, diffuseCounter = 0, outBoundCounter = 0, numException = 0;
    public int infLoop = 0, normRain = 0, normErode = 0, cover = 0, fill = 0;
    public float totalMassShift = 0.0f, massTaken = 0.0f, massDrop = 0.0f;
    
    private int sizeX, sizeY;
    
    //Position tracker
    private int posX = -1, posY = -1;
    
    private Random ran = new Random();
    
    private final int infiniteLoopStopper = 200;
    
    private final int numberOfErodePerDiffuse = 10000;//easy to be divided plz
    
    
    //variables provided by Dr.Shaw
    private float wk_fmin = 0.0f, wk_fmax = 0.3f, wk_slp = 2.0f;
    private float wk_carrylength = 20;
    private float wk_a = wk_fmax;
    private float wk_c = (float) ((wk_fmax - wk_fmin) / (wk_fmax - 0.5 * wk_fmin) * wk_slp);
    private float wk_b = wk_c * wk_fmin;
    private float walker = 0;
    
    //diffusion equation
    private float timeStep = 1f;
    private float gridSize = 1f;
    private float D = 10f;
    private float diffuseAmount = (float)(Math.pow(Math.E, (-(gridSize*gridSize))/(4*D*timeStep)) / (4*Math.PI*D*timeStep));
    private float maxDiffuse = 3 * wk_fmax;//for diffuse4, not used
    private float diffuseConstant = 0.1f;//for diffuse 4
    
    public Mesh(BufferedImage heightMap, BufferedImage faultMap, int faults){
        System.out.println(diffuseAmount);
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
        iniMass = totalMass();
        masterMeshCopy = masterMesh;
        //        masterMesh[0][0].x = 200;
        //        masterMesh[215][215].x = 200;
    }
    
    //Load the mesh and all necessary information
    private void load(BufferedImage heightMap, BufferedImage faultMap){
        Random ran = new Random();
        sizeX = heightMap.getWidth();
        sizeY = heightMap.getHeight();
        
        //create masterMesh
        masterMesh = new Vector3f[sizeY][sizeX];
        
        meshSize = (heightMap.getWidth() - 1) * (heightMap.getHeight() - 1);
        Color c;
        float h;
        //add data into the masterMesh
        for (int i = 0; i < heightMap.getWidth(); i++) {
            for (int j = 0; j < heightMap.getHeight(); j++) {
                c = new Color(heightMap.getRGB(i, j));
                h = c.getBlue();
                if(h == 255) h = -1.0f;
                else h = h + ran.nextFloat();//disable for actual map?
                masterMesh[i][j] = new Vector3f(h,i,j);
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
        //        boolean upOrBelow = true;
        //        for(int index = 0; index < meshes.length; index++){
        //            loadSubMeshes(meshes[index], upOrBelow, 0);
        //            upOrBelow = !upOrBelow;
        //        }
        
        //test new method of spliting faults
        faultVector = new Vector2f(1,1);
        
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
                    cur = new Vector2f(faultVector.x * i, faultVector.y * i);
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
    //choose a random value if diffuse is true
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
    
    public void rainT(){
        int i = ran.nextInt(masterMesh.length);
        int j = ran.nextInt(masterMesh[1].length);
        Point oldP = new Point(i,j);
        diffuse();
        diffuseCounter++;
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
    
    
    //Move the submeshes
    public void move(){
        for(int index = 0; index < meshes.length; index++){
            meshes[index].move();
        }
        update();
    }
    
    private float calcDiffuse(float t, float g, float d){
        return (float)(Math.pow(Math.E, (-(g*g))/(4*d*t)) / (4*Math.PI*d*t));
    }
    
    //update the value of each point but does not "reconnect" them
    private void update(){
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
        //        System.out.println(temp.length + " w" + temp[0].length);
        for(int i = 0; i < temp.length; i++){
            for(int j = 0; j < temp[0].length; j++){
                //                                temp[i][j] = new Vector3f(-1f,0f,0f);
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
                }
            }
        }
        
        //FOLLOWING DOES NOT WORK
        //can't just extend the edges by copying the original
        //there will be points w/o any original to copy from
        //left
        //                for(int i = 1; i < temp.length; i++){
        //                    if(temp[i][0].x == -1){
        //                        temp[i][0].x = temp[i][1].x;
        //                    }
        //                }
        //                //right
        //                for(int i = 1; i < temp.length; i++){
        //                    if(temp[i][temp[0].length - 1].x == -1){
        //                        temp[i][temp[0].length - 1].x = temp[i][temp[0].length - 2].x;
        //                    }
        //                }
        //                //bottom
        //                for(int j = 1; j < temp[0].length; j++){
        //                    if(temp[0][j].x == -1){
        //                        temp[0][j].x = temp[1][j].x;
        //                    }
        //                }
        //                //top
        //                for(int j = 1; j < temp[0].length; j++){
        //                    if(temp[temp.length - 1][j].x == -1){
        //                        temp[temp.length - 1][j].x = temp[temp.length - 2][j].x;
        //                    }
        //                }
        
        //now lets fix the borders by checking for -1
        //        for(int i = 1; i < temp.length - 1; i++){
        //            for(int j = 1; j < temp[0].length - 1; j++){
        //                if(temp[i][j].x == -1){
        //                    temp[i][j].x = 100;
        //                }
        //            }
        //        }
        
        
        masterMesh = temp;
        
        //instantiate elements of array
        for(int index = 0; index < meshes.length; index++){
            meshes[index] = new SubMesh(masterMesh.length, masterMesh[0].length, meshes[index].getMovement(), meshes[index].getDisplacement());
        }
        //        System.out.println(meshes[0].mesh.length + " s w" + meshes[0].mesh[0].length);
        
        
        loadSubMeshesV2();
    }
    
    public float totalMass(){
        float sum = 0.0f;
        
        for(int i = 0; i < masterMesh.length; i++){
            for(int j = 0; j < masterMesh[0].length; j++){
                if(masterMesh[i][j].x != -1.0f){
                    sum += masterMesh[i][j].x;
                }
            }
        }
        System.out.println(masterMesh[0][0].x + " " + masterMesh[masterMesh.length-1][0].x
                + " " + masterMesh[0][masterMesh[0].length-1].x+ " " + masterMesh[masterMesh.length-1][masterMesh[0].length-1].x);
        return sum;
    }
    
    public void changeVar(){
        
    }
    
    
    //SCRAPED methods**************************************************
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
    
}
