/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataanalysis;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.FastScatterPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author Jingyu_Yao
 */
public class Dataanalysis {

    static ArrayList<Float> distance = new ArrayList<>();
    static ArrayList<Float> curve = new ArrayList<>();
    static float[][] data;
    static Vector2f faultVector = new Vector2f(0f,128f);
    static float maxCurve = 0, maxDistance = 0;
    static String folder = "set2";
    static String file = "1b1.5r";
    static int size = 500;
    /**
     * @param args the command line arguments
     */
//    public static void main(String[] args) {
//        read();
//        fasterScatterPlot();
//    }
    
    private static void fasterScatterPlot(){
        NumberAxis domain = new NumberAxis("Distance");
        domain.setAutoRange(true);
        NumberAxis range = new NumberAxis("Curve");
        range.setAutoRange(true);
        
        FastScatterPlot plot = new FastScatterPlot(data, domain, range);
        
        JFreeChart chart = new JFreeChart(plot);
        
        try{
            ChartUtilities.saveChartAsPNG(new File(folder + "/" + file + "_plot.png"), chart, size, size);
        }catch(Exception e){
            
        }
    }
    
    private static void read(){
        try{
            FileReader fw = new FileReader(folder + "/" + file + ".txt");
            Scanner s = new Scanner(fw);
            String id; 
            float d,c;
            int x,y;
            while(s.hasNextLine()){
                if(s.next().equalsIgnoreCase("p")){
                    x = s.nextInt();
                    y = s.nextInt();
                    s.next();
                    c = s.nextFloat();
                    d = distanceFromFault(x,y);
                    distance.add(d);
                    curve.add(c);
                    if(c > maxCurve) maxCurve = c;
                    if(d > maxDistance) maxDistance = d;
                }else{
                    if(s.hasNextLine()) s.nextLine();//advance
                }
            }
            
            fw.close();
            s.close();
            
            if(distance.size() == curve.size()){
                data = new float[2][distance.size()];
                for(int index = 0; index < distance.size(); index++){
                    data[0][index] = distance.get(index);
                    data[1][index] = curve.get(index);
//                    System.out.println(data[0][index] + " " + data[1][index]);
                }
                System.out.println(folder + "/" + file + ".txt parsing complete, total points: " + data[0].length);
            }else{
                System.out.println("ERROR: distance and size not equal!");
            }
            
        }catch(Exception e){
            e.printStackTrace();
            System.exit(0);
        }
    }
    
    //these assumes mesh space = actual space
    public static float distanceFromFault(float x, float y){
        Vector2f vec = faultVector;
        
        //point to line distance formula
        return (float)(Math.abs(-vec.x*x + y - vec.y)/Math.sqrt(vec.x*vec.x + 1));
    }
    
    public static float findPerpendicularX(float x, float y){
        Vector2f vec = faultVector;
        float m = 1f / -vec.x;
        Vector2f perpenVec = new Vector2f(m, y - x*m);
        
        if(vec.x == 0f) return x;
        else return (m*x + perpenVec.y - vec.y) / vec.x;
    }
}
