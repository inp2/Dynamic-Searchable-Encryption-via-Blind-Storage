package edu.CS463.mp3.app;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends Activity
{
    //Set you VM's server IP here
    public static final String SERVER_IP = "172.22.150.61";
    //Set you VM's server port here: make sure the port number
    //Is sufficiently high for your program to listen on that number
    public static final int SERVER_PORT = 8888;
    public static final String LOOKUP_CMD = "LOOKUP";
    public HashMap<Integer, ArrayList> SmallMap = new HashMap<Integer, ArrayList>();
    private Socket client;
    private PrintWriter output;
    private EditText textField;
    private Button send;
    private Button exit;
    private String message;
    private String text;

    public static void Lookup(String keyword, PrintWriter printWriter)
    {
        String documentId="";
        //For Part-1
        //USE Local Index (cached) and find the documentID from there

        //For Part-2
        //  (a) If index is present in local cache use it to find the documentId
        // (otherwise) use the download functionality to get the appropriate
        // documentId from the server
        //and use them to download the files: this would be done by implementing
        // SSE.Search functionality

        //Write the message to output stream
        printWriter.write("LOOKUP " + documentId);
        //Get the output and show to the user
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Reference to the text field
        textField = (EditText)findViewById(R.id.editText);
        //Reference to the send button
        send = (Button)findViewById(R.id.button);

       send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             //Get the keyword from the textfield
             String keyword = textField.getText().toString();
             //Connect to the database
                final SqlDatabaseConnect db = new SqlDatabaseConnect(getApplicationContext());
                Cursor c;
                c = db.getDocumentIDs(keyword);
                //Toast.makeText(getApplicationContext(), c.toString(), Toast.LENGTH_LONG);

                if (c.moveToFirst()) {
                    text = c.getString(c.getColumnIndex("document_id"));
                }

                System.out.println(text);
             //Reset the text field to blank
             textField.setText("");
                try
                {
                    //Connect to server
                    client = new Socket(SERVER_IP, SERVER_PORT);
                    output = new PrintWriter(client.getOutputStream(), true);
                    Lookup(keyword, output);
                    output.flush();
                    output.close();
                    //Closing the connection
                    client.close();
                }
                catch (UnknownHostException e)
                {
                    e.printStackTrace();
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
        //Reference to the send button
        exit = (Button)findViewById(R.id.button2);

        //Button press event listener
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),
                        "Closing", Toast.LENGTH_LONG).show();
                System.exit(0);
            }

        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
