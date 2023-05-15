package com.protose;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.protose.shared.*;

/**
 * Hello world!
 *
 */
public class Client {
    public static void main( String[] args ){
        System.out.println( "Hello from Client" );
        Shared testObj = new Shared(10);

        JFrame f=new JFrame();//creating instance of JFrame  
          
        JButton b=new JButton("click");//creating instance of JButton  
        b.setBounds(130,100,100, 40);//x axis, y axis, width, height  
                
        f.add(b);//adding button in JFrame  
                
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setSize(400,500);//400 width and 500 height  
        f.setLayout(null);//using no layout managers  
        f.setVisible(true);//making the frame visible  
    }
}
