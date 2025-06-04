/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataanalysis;

import java.awt.Color;
import java.awt.Paint;
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
public class Dataanalysis2 {

    static ArrayList<Double> distance = new ArrayList<>();
    static ArrayList<Double> curve = new ArrayList<>();
    static ArrayList<Double> distance2 = new ArrayList<>();//no fault side
    static ArrayList<Double> curve2 = new ArrayList<>();
    static double[][] data;
    static double[][] data2;
//    static double[] faultVector = {1.02d,-220d};//0f,128f-simula || actual fault m=0.8 b=-168
    static double[] faultVector = {0d,250d};
    static double maxCurve = 0, maxDistance = 0;
    static String folder = "set20";
    static int file = 6;
    static int size = 500;
    static boolean raw = false;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        File f = new File(folder + "/graphs");
        f.mkdir();
        for(int index = 0; index <= file; index++){
            read(index);
            plot(index);
        }
//        read(file);
//        plot(file);
    }
    
    private static void plot(int i){
        
        //calculate the normalized data
        XYSeries normData = new XYSeries("Uplifted side");
        double[] normalized = new double[(int)maxDistance+1];
        int[] counter = new int[(int)maxDistance+1];
        int curSpot;
        for(int index = 0; index < data[0].length; index++){
            curSpot = (int)data[0][index];
            normalized[curSpot] += data[1][index];
            counter[curSpot]++;
        }
        for(int index = 0; index < normalized.length;index++){
            normData.add(index,(normalized[index] / counter[index]));
        }
        
        //calculate the normalized data for no uplift side
        XYSeries normData2 = new XYSeries("No uplife side");
        double[] normalized2 = new double[(int)maxDistance+1];
        int[] counter2 = new int[(int)maxDistance+1];
        int curSpot2;
        for(int index = 0; index < data2[0].length; index++){
            curSpot2 = (int)data2[0][index];
            normalized2[curSpot2] += data2[1][index];
            counter2[curSpot2]++;
        }
        for(int index = 0; index < normalized2.length;index++){
            normData2.add(index,(normalized2[index] / counter2[index]));
        }
        
        //set axis
        NumberAxis domain = new NumberAxis("Distance");
        domain.setAutoRange(true);
        NumberAxis range = new NumberAxis("Curvature");
        range.setAutoRange(true);
        
        //set scatter plot renderer
        XYLineAndShapeRenderer lineRender = new XYLineAndShapeRenderer();
        //set line plot renderer
        XYLineAndShapeRenderer line2Render = new XYLineAndShapeRenderer();
        //create plot
        XYPlot plot = new XYPlot();
        
        //set axis
        plot.setDomainAxis(domain);
        plot.setRangeAxis(range);
        
//        if(raw){
        //render scatter plot
        plot.setDataset(1, new XYSeriesCollection(normData));
        plot.setRenderer(1,lineRender);
//        }else{
        //render line as annotation
        plot.setDataset(2, new XYSeriesCollection(normData2));
        plot.setRenderer(2,line2Render);
//        }
        
        JFreeChart chart = new JFreeChart(plot);
        try{
            String name;
//            if(raw) name = folder + "/" + file + "_raw.png";
            name = folder + "/graphs/" + i + "_both_100.png";
            ChartUtilities.saveChartAsPNG(new File(name), chart, size, size);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    private static void read(int i){
        try{
            FileReader fw = new FileReader(folder + "/" + i + ".txt");
            Scanner s = new Scanner(fw);
            double d,c;
            int x,y;
            double faultY;
            while(s.hasNextLine()){
                if(s.next().equalsIgnoreCase("p")){
                    x = s.nextInt();
                    y = s.nextInt();
                    s.next();
                    c = s.nextDouble();
                    d = distanceFromFault(x,y);
                    faultY = x*faultVector[0] + faultVector[1];
                    if(d < 100){
                        if(y < faultY){
                            distance.add(d);
                            curve.add(c);
                        }else{
                            distance2.add(d);
                            curve2.add(c);
                        }
                        if(c > maxCurve) maxCurve = c;
                        if(d > maxDistance) maxDistance = d;
                    }
                }else{
                    if(s.hasNextLine()) s.nextLine();//advance
                }
            }
            
            fw.close();
            s.close();
            
            if(distance.size() == curve.size()){
                data = new double[2][distance.size()];
                for(int index = 0; index < distance.size(); index++){
                    data[0][index] = distance.get(index);
                    data[1][index] = curve.get(index);
                }
                System.out.println("uplift side total points: " + data[0].length);
            }else{
                System.out.println("ERROR: distance and size not equal!");
            }
            
            if(distance2.size() == curve2.size()){
                data2 = new double[2][distance2.size()];
                for(int index = 0; index < distance2.size(); index++){
                    data2[0][index] = distance2.get(index);
                    data2[1][index] = curve2.get(index);
                }
                System.out.println("no uplift side total points: " + data2[0].length);
            }else{
                System.out.println("ERROR: distance and size not equal!");
            }
            
        }catch(Exception e){
            e.printStackTrace();
            System.exit(0);
        }
    }
    
    //these assumes mesh space = actual space
    public static double distanceFromFault(double x, double y){
//        Vector2f vec = faultVector;
        
        //point to line distance formula
        return (double)(Math.abs(-faultVector[0]*x + y - faultVector[1])/Math.sqrt(faultVector[0]*faultVector[0] + 1));
    }
    
    public static double findPerpendicularX(double x, double y){
//        Vector2f vec = faultVector;
        double m = 1f / -faultVector[0];
        
//        Vector2f perpenVec = new Vector2f(m, y - x*m);
        double[] perpenVec = {m, y - x*m};
        
        
        if(faultVector[0] == 0f) return x;
        else return (m*x + perpenVec[1] - faultVector[1]) / faultVector[0];
    }
}
