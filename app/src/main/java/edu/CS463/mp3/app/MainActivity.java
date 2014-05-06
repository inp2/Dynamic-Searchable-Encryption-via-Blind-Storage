package edu.CS463.mp3.app;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends Activity {
    //Set you VM's server IP here
    public static final String SERVER_IP = "172.22.150.61";
    //Set you VM's server port here: make sure the port number
    //Is sufficiently high for your program to listen on that number
    public static final int SERVER_PORT = 8888;
    public static final String LOOKUP_CMD = "LOOKUP";
    public HashMap<Integer, ArrayList> SmallMap = new HashMap<Integer, ArrayList>();
    private String keyword;
    private Socket client;
    private PrintWriter output;
    private EditText textField;
    private RadioGroup radioMethodGroup;
    private RadioButton radioButton;
    private RadioButton large;
    private Button send;


    public void Lookup(String keyword, PrintWriter printWriter, RadioButton button)
    {
        String documentId = "";

        if (button.getText().equals("Small")) {
            //For Part-1
            //USE Local Index (cached) and find the documentID from there
            //Connect to the database
            final SqlDatabaseConnectSmall db = new SqlDatabaseConnectSmall(getApplicationContext());
            Cursor c;
            c = db.getSmallDocumentIDs(keyword);

            if (c.moveToFirst()) {
                documentId = c.getString(c.getColumnIndex("document_id"));
            }
            //c.moveToFirst();
            //if(!c.isAfterLast()) {
            //Get the output and show to the user
            Toast.makeText(getApplicationContext(), documentId, Toast.LENGTH_SHORT).show();
            System.out.println("Document Id(s): " + documentId);
            //Write the message to output stream
            printWriter.write("LOOKUP " + documentId);
            // }
            //else
            //{
            Toast.makeText(getApplicationContext(), keyword + " does not exist", Toast.LENGTH_SHORT).show();
            System.out.println(keyword + " does not exist");
            //}
        } else {
            //For Part-2
            //  (a) If index is present in local cache use it to find the documentId

            final SqlDatabaseConnectLarge db = new SqlDatabaseConnectLarge(getApplicationContext());
            Cursor c;
            c = db.getLargeDocumentIDs(keyword);

            if (c.moveToFirst()) {
                documentId = c.getString(c.getColumnIndex("document_id"));
            }
            System.out.println("Document Id(s): " + documentId);
            //Get the output and show to the user
            Toast.makeText(getApplicationContext(), documentId, Toast.LENGTH_SHORT).show();
            //Write the message to output stream
            printWriter.write("LOOKUP " + documentId);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Reference to the text field
        textField = (EditText) findViewById(R.id.editText);
        addListenerOnButton();
    }

    public void addListenerOnButton() {
        radioMethodGroup = (RadioGroup) findViewById(R.id.radioGroup);

        //Reference to the send button
        send = (Button) findViewById(R.id.button);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Get the keyword from the textfield
                keyword = textField.getText().toString();

                //Reset the text field to blank
                textField.setText("");

                //Get selected radio button from radioGroup
                int selectedId = radioMethodGroup.getCheckedRadioButtonId();
                //Find the radiobutton from radio group
                radioButton = (RadioButton) findViewById(selectedId);

                try
                {
                    //Connect to server
                    client = new Socket(SERVER_IP, SERVER_PORT);
                    output = new PrintWriter(client.getOutputStream(), true);
                    Lookup(keyword, output, radioButton);
                    output.flush();
                    output.close();
                    //Closing the connection
                    client.close();
                } catch (UnknownHostException e)
                {
                    e.printStackTrace();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
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
