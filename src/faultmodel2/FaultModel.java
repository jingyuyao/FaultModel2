/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package faultmodel2;

import de.matthiasmann.twl.utils.PNGDecoder;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
import javax.swing.JOptionPane;


/**
 *
 * @author your name
 */
public class FaultModel{
    private Mesh mesh;
    private BufferedImage heightMap;
    private BufferedImage faultMap;
    private final int DISPLAY_WIDTH = 800;
    private final int DISPLAY_HEIGHT = 600;
    
    private final int[] WINDOW_DIMENSIONS = {1200, 650};
    private final float ASPECT_RATIO = (float) WINDOW_DIMENSIONS[0] / (float) WINDOW_DIMENSIONS[1];
    private final EulerCamera camera = new EulerCamera.Builder().setPosition(-5.4f, 19.2f,
            33.2f).setRotation(30, 61, 0).setAspectRatio(ASPECT_RATIO).setFieldOfView(60).build();
    
    private boolean rain = false;
    private boolean move = false;
    
    private int heightmapDisplayList;
    private Shader shader;
    private int texture;
    
    private int moveSyncCounter = 0;
    
    private int programID;
    
    private ArrayList[] rainTrace = new ArrayList[10];
    private int rainTraceNum = 0;
    
    private String helpMessage = "Controls: \n WASD: movement \n shift: go up \n ctrl: go down"
            + "\n mouse: left click: drag \n\tright click: un-drag"
            + "\n P: Change view state \n R: rain \n M: fault movement"
            + "\n N: manual sync \n C: erode count "
            + "\n H: help";
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        FaultModel mod = new FaultModel();
    }
    
    public FaultModel(){
        setUp();
        //somehow the precision can only be .1
        mesh.meshes[0].setMovement(new Vector3f(0f,0.01f,0.01f));
        //        mesh.meshes[1].setMovement(new Vector3f(0f,0.05f,0.05f));
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
        setUpShader();
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
    }
    
    private void setUpMatrices() {
        camera.applyPerspectiveMatrix();
    }
    
    private void setUpFiles(){
        try{
            //get the height map
            //String file = JOptionPane.showInputDialog("File path of heightmap(ex: MyDocuments/images/stuff.jpg):");
            String heightMapFile = "src/faultmodel2/test.jpg";//current: Terrain2.bmp, test.jpg
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
                if (Keyboard.getEventKey() == Keyboard.KEY_C){
                    JOptionPane.showMessageDialog(null, "Erode Count: " + mesh.erodeCounter);
                }
                if (Keyboard.getEventKey() == Keyboard.KEY_H){
                    JOptionPane.showMessageDialog(null, helpMessage);
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
        camera.processKeyboard(16, 1);
    }
    
    private void loop(){
        Timer time = new Timer();
        //use timer to run tasks so it does not interfere with render
        time.schedule(new TimerTask() {
            
            @Override
            public void run() {
            }
        }, 300);
        
        while(!Display.isCloseRequested()){
            
            if(rain){
                mesh.rain();
                draw();
                
            }
            if(move){
                
                mesh.move();
                moveSyncCounter++;
                if(moveSyncCounter == 80){//try to keep this as high as possible
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
        
        //close display at end of program
        glUseProgram(0);
        glDeleteProgram(shader.getProgram());
        glBindTexture(GL_TEXTURE_2D, 0);
        Display.destroy();
        System.exit(0);
        
    }
    
    //Renders the model
    public void render(){
        // Clear the pixels on the screen and clear the contents of the depth buffer (3D contents of the scene)
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        // Reset any translations the camera made last frame update
        glLoadIdentity();
        // Apply the camera position and orientation to the scene
        camera.applyTranslations();
        // Render the heightmap using the shaders that are being used
        glCallList(heightmapDisplayList);
        
        if(mesh.rainStrip != null && !mesh.rainStrip.isEmpty()){
            rainTrace[rainTraceNum] = mesh.rainStrip;
            if(rainTraceNum == 9){
                rainTraceNum = 0;
            }else{
                rainTraceNum++;
            }
        }
        
        glUseProgram(0);
        Vector3f v;
        //        glPointSize(4);
        for(int n = 0; n < rainTrace.length; n++){
            if(rainTrace[n] != null){
                glBegin(GL_LINE_STRIP);
                glColor3f(1f,0f,0f);
                for(int i = 0; i < rainTrace[n].size(); i++){
                    v = (Vector3f) rainTrace[n].get(i);
                    
                    glVertex3f(v.z, v.x, v.y);
                }
                glEnd();
            }
        }
        //        glPointSize(2);
        glUseProgram(programID);
        mesh.rainStrip = new ArrayList<>();
    }
    
    //Create a DisplayList from the masterMesh to render
    private void draw(){
        glDeleteLists(heightmapDisplayList, 1);
        
        heightmapDisplayList = glGenLists(1);
        
        glNewList(heightmapDisplayList, GL_COMPILE);
        // Scale back the display list so that its proportions are acceptable.
        glScalef(0.2f, 0.05f, 0.2f);
        // Iterate over the 'strips' of heightmap data.
        Vector3f p;
        for (int z = 0; z < mesh.masterMesh.length - 1; z++) {
            // Render a triangle strip for each 'strip'.
            glBegin(GL_TRIANGLE_STRIP);
            for (int x = 0; x < mesh.masterMesh[z].length; x++) {
                p = mesh.masterMesh[z][x];
                // Take a vertex from the current strip
                glVertex3f(p.z,p.x,p.y);
                
                p = mesh.masterMesh[z+1][x];
                // Take a vertex from the next strip
                glVertex3f(p.z,p.x,p.y);
            }
            glEnd();
        }
        
        glEndList();
    }
    
    //not used atm
    //This controls how much erode and move is called
    public void updateData(int erodeTime, int moveTime){
        mesh.rain();
        //        mesh.move();
        draw();
        
    }
    
    
}
