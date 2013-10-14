/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * 
 * Shout out goes to Oskar Veerhoek for the OpenGL tutorial
 */
package faultmodel2;

import de.matthiasmann.twl.utils.PNGDecoder;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.util.glu.GLU.gluPerspective;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector3f;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.lwjgl.util.vector.Matrix4f;


/**
 *
 * @author your name
 *
 * A shout out to TheCodingUniverse for the awesome OpenGL tutorials.
 */
public class FaultModel{
    private Mesh mesh;
    private BufferedImage heightMap;
    private BufferedImage faultMap;
    private final int DISPLAY_WIDTH = 800;
    private final int DISPLAY_HEIGHT = 600;
    
    private final int[] WINDOW_DIMENSIONS = {1200, 650};
    private final float[] DEFAULT_CAMERA_POS = {3f, 8.5f,2f};
    private final float[] DEFAULT_CAMERA_ROT = {27.84f, 135.64f};
    private static float[] lightPosition = {200f, -10f, 0f, 1f};
    
    private final float ASPECT_RATIO = (float) WINDOW_DIMENSIONS[0] / (float) WINDOW_DIMENSIONS[1];
    private final EulerCamera camera = new EulerCamera.Builder().setPosition(DEFAULT_CAMERA_POS[0], DEFAULT_CAMERA_POS[1],
            DEFAULT_CAMERA_POS[2]).setRotation(DEFAULT_CAMERA_ROT[0], DEFAULT_CAMERA_ROT[1], 0).setAspectRatio(ASPECT_RATIO).setFieldOfView(60).setFarClippingPane(200).build();
    
    private boolean rain = false;
    private boolean move = false;
    
    private int heightmapDisplayList;
    private Shader shader;
    private int texture;
    
    private int moveSyncCounter = 0;
    private int syncAfterMove;
    
    private int programID;
    
    private final static int rainPerClick = 1000;
    
    public static ArrayList[] rainTrace = new ArrayList[rainPerClick];
    public static int rainTraceNum = 0;
    
    private String helpMessage = "Controls: \n WASD: movement \n shift: go up \n ctrl: go down"
            + "\n mouse: left click: drag \n\tright click: un-drag"
            + "\n P: Change view state \n R: rain \n M: fault movement"
            + "\n N: manual sync \n I: update info"
            + "\n H: help";
    
    private static JFrame frame;
    private static Graphics2D g;
    
    private String fileLoc = "src/faultmodel2/gradient3.jpg";
    //dem10x10.png dem20x20.png dem38x38.png
    //gaussian.jpg gradient3.png test.jpg
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        frame = new JFrame("Info Window");
        frame.setSize(300, 500);
        frame.setVisible(true);
        FaultModel mod = new FaultModel();
    }
    
    public FaultModel(){
        setUp();
        //somehow the precision can only be .1
//        mesh.setMovement(0, new Vector3f(0f,0.01f,0.01f));//sideways
        mesh.setMovement(0, new Vector3f(0f,-0.05f,0.05f));//go out
//        mesh.setMovement(0, new Vector3f(0f,0f,0.05f));////go up
//                mesh.setMovement(0, new Vector3f(0f,-0.05f,0.00f));//go right
//        System.out.println("factor" + 1/mesh.moveFactor.y);
        System.out.println(helpMessage);
        loop();
    }
    
    //Setup the faultmodel including stuff from other class everything
    public void setUp(){
        setDisplay();
        setUpStates();
        setUpMatrices();
        setUpFiles();
        //mesh have to be created before shader
        //since shader use max height of the mesh
        mesh = new Mesh(heightMap,faultMap,1);
//                setUpShader();
        setUpLight();
        glDeleteLists(heightmapDisplayList, 1);
        
        heightmapDisplayList = glGenLists(1);
        
        draw();
    }
    
    private void setDisplay() {
        //Display initialization
        try {
            Display.setDisplayMode(new DisplayMode(DISPLAY_WIDTH, DISPLAY_HEIGHT));
            Display.setFullscreen(false);
            Display.setTitle("FaultModel");
            Display.create();
            
        } catch (LWJGLException e) {
            e.printStackTrace();
            Display.destroy();
        }
    }
    
    private void setUpStates() {
        camera.applyOptimalStates();
        //just slightly bigger than normal dots so its easy
        //to see the dots during rain tracing
        glPointSize(2.1f);
        // Enable the sorting of shapes from far to near
        glEnable(GL_DEPTH_TEST);
        // Set the background to a blue sky colour
        glClearColor(0, 0.75f, 1, 1);
        // Remove the back (bottom) faces of shapes for performance
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }
    
    private void setUpLight(){
        //for lighting
        glShadeModel(GL_SMOOTH);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);
        glLightModel(GL_LIGHT_MODEL_AMBIENT, asFlippedFloatBuffer(new float[]{0f, 0f, 0f, 1f}));
        glLight(GL_LIGHT0, GL_DIFFUSE, asFlippedFloatBuffer(new float[]{0.5f, 0.5f, 0.5f, 1}));
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_DIFFUSE);
    }
    
    private void setUpMatrices() {
        camera.applyPerspectiveMatrix();
    }
    
    private void setUpFiles(){
        try{
            //get the height map
            //String file = JOptionPane.showInputDialog("File path of heightmap(ex: MyDocuments/images/stuff.jpg):");
//            String heightMapFile = "src/faultmodel2/dem10x10.png";//current: Terrain2.bmp, test.jpg
            String heightMapFile = fileLoc;
            heightMap = ImageIO.read(new File(heightMapFile));
            //        String faultMapFile = "src/faultmodel2/heightmap2f.jpg";
            //        faultMap = ImageIO.read(new File(faultMapFile));
            faultMap = new BufferedImage(1,1,1);//just for testing
            
            //create stream
            FileInputStream colorStream = new FileInputStream("src/faultmodel2/lookup_5.png");
            PNGDecoder decoder = new PNGDecoder(colorStream);//use PNG image decoder
            //create a openGL bytebuffer to store PNG data
            ByteBuffer buffer = BufferUtils.createByteBuffer(4 * decoder.getHeight() * decoder.getWidth());
            decoder.decode(buffer, decoder.getWidth() * 4, PNGDecoder.Format.RGBA);//decode it
            buffer.flip();//make it readable to openGL
            
            texture = glGenTextures();//reserver a texture memory slot for openGL to use
            glBindTexture(GL_TEXTURE_2D, texture);//bind this texture to openGL
            glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,decoder.getWidth(),decoder.getHeight(),0,GL_RGBA,GL_UNSIGNED_BYTE,buffer);
            
        }catch(HeadlessException | IOException e) {
            e.printStackTrace();
        }
    }
    
    private void setUpShader(){
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        
        shader = new Shader("src/faultmodel2/shader.vert", "src/faultmodel2/shader.frag");
        programID = shader.getProgram();
        glLinkProgram(programID);
        glValidateProgram(programID);
        glUseProgram(programID);
        
        //Can only change variable after start of program.....
        int loc1 = GL20.glGetUniformLocation(programID, "theMax");
        GL20.glUniform1f(loc1, mesh.max);
    }
    
    private void input() {
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_P) {
                    // Switch between normal mode, point mode, and wire-frame mode.
                    int polygonMode = glGetInteger(GL_POLYGON_MODE);
                    if (polygonMode == GL_LINE) {
                        glPolygonMode(GL_FRONT, GL_FILL);
                    } else if (polygonMode == GL_FILL) {
                        glPolygonMode(GL_FRONT, GL_POINT);
                    } else if (polygonMode == GL_POINT) {
                        glPolygonMode(GL_FRONT, GL_LINE);
                    }
                }
                if (Keyboard.getEventKey() == Keyboard.KEY_R){
                    rain = !rain;
                }
                if (Keyboard.getEventKey() == Keyboard.KEY_M){
                    move = !move;
                }
                if (Keyboard.getEventKey() == Keyboard.KEY_N){
                    mesh.sync();
                }
                if (Keyboard.getEventKey() == Keyboard.KEY_H){
                    JOptionPane.showMessageDialog(null, helpMessage);
                }
                if (Keyboard.getEventKey() == Keyboard.KEY_O){
                    System.out.println(camera.toString());
                }
                if (Keyboard.getEventKey() == Keyboard.KEY_K){
                    camera.setPosition(DEFAULT_CAMERA_POS[0], DEFAULT_CAMERA_POS[1],DEFAULT_CAMERA_POS[2]);
                    camera.setRotation(DEFAULT_CAMERA_ROT[0], DEFAULT_CAMERA_ROT[1], 0);
                }
                if (Keyboard.getEventKey() == Keyboard.KEY_T){
                    for(int i = 0; i < 1000000; i++){
                        mesh.rain();
                    }
                    draw();
                }
                if (Keyboard.getEventKey() == Keyboard.KEY_Y){
                    for(int i = 0; i < 1000; i++){
                        mesh.rainT();
                    }
                    draw();
                }
//                if (Keyboard.getEventKey() == Keyboard.KEY_G){
////                    lightPosition = new float[]{camera.x(), camera.y(), camera.z(), 1};
//                }
//                if (Keyboard.getEventKey() == Keyboard.KEY_F){
//                    mesh.diffuse3();
//                    draw();
//                }
                //                if (Keyboard.getEventKey() == Keyboard.KEY_U){
                //                    cleanUp();
                //                }
                if (Keyboard.getEventKey() == Keyboard.KEY_I){
                    updateInfo();
                }
            }
            
        }
        if (Mouse.isButtonDown(0)) {
            Mouse.setGrabbed(true);
        } else if (Mouse.isButtonDown(1)) {
            Mouse.setGrabbed(false);
        }
        if (Mouse.isGrabbed()) {
            camera.processMouse(1, 80, -80);
        }
        camera.processKeyboard(16, 3);
        glLight(GL_LIGHT0, GL_POSITION, asFlippedFloatBuffer(lightPosition));
    }
    
    private void loop(){
        //        Timer time = new Timer();
        //        //use timer to run tasks so it does not interfere with render
        //        time.schedule(new TimerTask() {
        //
        //            @Override
        //            public void run() {
        //
        //            }
        //        }, 300);
        
        while(!Display.isCloseRequested()){
            
            if(rain){
                for(int i = 0; i < rainPerClick; i++){
                    mesh.rain();
                }
                draw();
            }
            if(move){
                
                mesh.move();
                moveSyncCounter++;
                if(Math.abs(mesh.moveFactor.y) > Math.abs(mesh.moveFactor.z)){
                    syncAfterMove = (int)(1/Math.abs(mesh.moveFactor.y));
                }else{
                    syncAfterMove = (int)(1/Math.abs(mesh.moveFactor.z));
                }
                if(moveSyncCounter == syncAfterMove){//try to keep this as high as possible
                    mesh.sync();          //it "eats" the mesh if its too low
                    moveSyncCounter = 0;
                }
                draw();
            }
            render();
            
            input();
            Display.update();
            Display.sync(60);
        }
        
        cleanUp();
    }
    
    private void cleanUp(){
        //close display at end of program
//                glUseProgram(0);
//                glDeleteProgram(shader.getProgram());
        glDeleteLists(heightmapDisplayList, 1);
        glBindTexture(GL_TEXTURE_2D, 0);
        Display.destroy();
        System.exit(0);
    }
    
    //Renders the model
    public void render(){
        //update the list
        //        draw();
        // Clear the pixels on the screen and clear the contents of the depth buffer (3D contents of the scene)
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        // Reset any translations the camera made last frame update
        glLoadIdentity();
        // Apply the camera position and orientation to the scene
        camera.applyTranslations();
        // Render the heightmap using the shaders that are being used
        glCallList(heightmapDisplayList);
        
        
//                glUseProgram(0);
        Vector3f v;
        //        glPointSize(4);
        for(int n = 0; n < rainTrace.length; n++){
            if(rainTrace[n] != null){
                glBegin(GL_LINE_STRIP);
                glColor3f(1f,0f,0f);
                for(int i = 0; i < rainTrace[n].size(); i++){
                    v = (Vector3f) rainTrace[n].get(i);
                    
                    glVertex3f(v.z, v.x + 0.01f, v.y);
                }
                glEnd();
            }
        }
        
//                glPointSize(2);
//                glUseProgram(programID);
        //        mesh.rainStrip = new ArrayList<>();
    }
    
    //outdated draw method
    private void drawT(){
        glDeleteLists(heightmapDisplayList, 1);
        
        heightmapDisplayList = glGenLists(1);
        
        glNewList(heightmapDisplayList, GL_COMPILE);
        // Scale back the display list so that its proportions are acceptable.
        glScalef(0.2f, 0.05f, 0.2f);
        glNormal3f(0,-1,0);
        // Iterate over the 'strips' of heightmap data.
        Vector3f p;
                glColor3f(0.4f, 0.27f, 0.17f);
        for (int z = 0; z < mesh.masterMesh.length - 1; z++) {
            // Render a triangle strip for each 'strip'.
            glBegin(GL_TRIANGLE_STRIP);
            for (int x = 0; x < mesh.masterMesh[z].length; x++) {
                p = mesh.masterMesh[z][x];
                // Take a vertex from the current strip
                vx(p);
                p = mesh.masterMesh[z+1][x];
                // Take a vertex from the next strip
                vx(p);
            }
            glEnd();
        }
        glEndList();
    }
    
    //Create a DisplayList from the masterMesh to render
    private void draw(){
        glDeleteLists(heightmapDisplayList, 1);
        
        heightmapDisplayList = glGenLists(1);
        
        glNewList(heightmapDisplayList, GL_COMPILE);
        // Scale back the display list so that its proportions are acceptable.
        glScalef(0.2f, 0.05f, 0.2f);
        glNormal3f(0,-1,0);
        // Iterate over the 'strips' of heightmap data.
        Vector3f p1,p2,p3,p4;
                glColor3f(0.4f, 0.27f, 0.17f);
        for (int z = 0; z < mesh.masterMesh.length - 1; z++) {
            // Render a triangle strip for each 'strip'.
            glBegin(GL_TRIANGLES);
            for (int x = 0; x < mesh.masterMesh[z].length - 1; x++) {
                if(mesh.masterMesh[z][x].x != -1.0f){
                    p1 = mesh.masterMesh[z][x];
                    p2 = mesh.masterMesh[z+1][x];
                    p3 = mesh.masterMesh[z+1][x+1];
                    p4 = mesh.masterMesh[z][x+1];
                    
                    if(p2.x < p3.x && p2.x != -1.0f){
                        if(p2.x != -1.0f && p4.x != -1.0f){
                            vx(p1);
                            vx(p2);
                            vx(p4);
                        }
                        if(p2.x != -1.0f && p3.x != -1.0f && p4.x != -1.0f){
                            vx(p2);
                            vx(p3);
                            vx(p4);
                        }
                    }else if(p3.x != -1.0f){
                        if(p2.x != -1.0f && p3.x != -1.0f){
                            vx(p1);
                            vx(p2);
                            vx(p3);
                        }
                        if(p3.x != -1.0f && p4.x != -1.0f){
                            vx(p1);
                            vx(p3);
                            vx(p4);
                        }
                    }
                }
            }
            glEnd();
        }
        glEndList();
        
//        updateInfo();
    }
    
    //draw a single point
    //NOTE: mesh(x,y,z) --> openGL(z,x,y)
    private void vx(Vector3f p){
        glVertex3f(p.z,p.x,p.y);
    }
    
    //updates the information window
    private void updateInfo(){
        g = (Graphics2D)frame.getGraphics();
        g.clearRect(0, 0, 300, 500);
        g.drawString("Erode Count: " + mesh.erodeCounter + " Diffuse Count: " + mesh.diffuseCounter, 20, 50);
        g.drawString("Change in mass: " + (mesh.totalMass() - mesh.iniMass), 20, 70);
        g.drawString("InfLoop: " + mesh.infLoop + " normRain: " + mesh.normRain, 20, 90);
        g.drawString("normErode: " + mesh.normErode, 20, 110);
        g.drawString("cover: " + mesh.cover, 20, 130);
        g.drawString("fill hole & keep going: " + mesh.fill, 20, 150);
        g.drawString("Average mass shift: " + (mesh.totalMassShift / (mesh.normErode + mesh.cover + mesh.fill)), 20, 170);
        g.drawString("MassTaken: " + mesh.massTaken + " MassDrop: " + mesh.massDrop, 20, 190);
        g.drawString("OutBound: " + mesh.outBoundCounter, 20, 210);
        g.drawString("NumException: " + mesh.numException, 20, 230);
    }
    
    //not used atm
    //This controls how much erode and move is called
    public void updateData(int erodeTime, int moveTime){
        mesh.rain();
        //        mesh.move();
        draw();
        
    }
    
    /**
     * @param values the float values that are to be turned into a FloatBuffer
     * @return a FloatBuffer readable to OpenGL (not to you!) containing values
     */
    private static FloatBuffer asFlippedFloatBuffer(float... values) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(values.length);
        buffer.put(values);
        buffer.flip();
        return buffer;
    }
}
