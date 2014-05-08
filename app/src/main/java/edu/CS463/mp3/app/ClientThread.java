package edu.CS463.mp3.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Nkechinyere on 5/7/14.
 */
public class ClientThread implements Runnable {

    @Override
    public void run() {
        try {
            //Connect to server
            System.out.println("CONNECTING");
            Socket client = new Socket("172.22.150.61", 8888);

            System.out.println("SENDING");
            PrintWriter output = new PrintWriter(client.getOutputStream(), true);
            BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
            Lookup(keyword, output, radioButton);
            System.out.println("FILE" + input.readLine());
            output.flush();
            System.out.println("FLUSH CONNECTION");


            output.close();
            System.out.println("CLOSE CONNECTION");
            //Closing the connection
            client.close();
        } catch (UnknownHostException e) {
            System.out.println("Error " + e.toString());
        } catch (IOException e) {
            System.out.println("Error " + e.toString());
        }
    }
});