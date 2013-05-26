/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package faultmodel2;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import static org.lwjgl.opengl.GL11.*;  
import static org.lwjgl.opengl.GL20.*;
/**
 *
 * @author your name
 */
public class Shader {
    int shaderProgram;
    
    public Shader(String vertex, String fragment){
        shaderProgram = glCreateProgram();
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        int fragShader = glCreateShader(GL_FRAGMENT_SHADER);
        
        StringBuilder vertexShaderSource = new StringBuilder();
        StringBuilder fragShaderSource = new StringBuilder();
        
        try{
            BufferedReader reader = new BufferedReader(new FileReader(vertex));
            String line;
            
            while((line = reader.readLine()) != null){
                vertexShaderSource.append(line).append('\n');
            }
            
            reader.close();
        }catch(IOException e){
            System.err.println("Vertex shader fail load");
        }
        
        try{
            BufferedReader reader = new BufferedReader(new FileReader(fragment));
            String line;
            
            while((line = reader.readLine()) != null){
                fragShaderSource.append(line).append('\n');
            }
            
            reader.close();
        }catch(IOException e){
            System.err.println("Fragment shader fail load");
        }
        
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);
        if(glGetShader(vertexShader, GL_COMPILE_STATUS) == GL_FALSE){
            System.err.println("Vertex shader don compile");
        }
        
        glShaderSource(fragShader, fragShaderSource);
        glCompileShader(fragShader);
        if(glGetShader(fragShader, GL_COMPILE_STATUS) == GL_FALSE){
            System.err.println("Vertex shader don compile");
        }
        
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragShader);
        
    }
    
    public int getProgram() {return shaderProgram;}
}
