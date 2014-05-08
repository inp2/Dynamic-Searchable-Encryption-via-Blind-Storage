package edu.CS463.mp3.app;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends Activity {
    //Set you VM's server IP here
    public static final String SERVER_IP = "172.22.150.61";
    //Set you VM's server port here: make sure the port number
    //Is sufficiently high for your program to listen on that number
    public static final int SERVER_PORT = 8888;
    public static final String LOOKUP_CMD = "LOOKUP";
    private static final String REPLY = "REPLY ";
    private static final int NUM_SMALL_BLOCKS = 2048 * 4;
    private static final int NUM_BIG_BLOCKS = 2048 * 4;
    private static final int MIN_BLOCKS = 45;
    public HashMap<Integer, ArrayList> SmallMap = new HashMap<Integer, ArrayList>();
    private String keyword;
    private Socket client;
    private PrintWriter output;
    private EditText textField;
    private RadioGroup radioMethodGroup;
    private RadioButton radioButton;
    private RadioButton large;
    private Button send;
    private byte[] keyprf = new byte[]{-47, -28, -32, 36, -98, 101, 22, -94, 74, -108, -56, -38, -9, 16, 120, 123};
    private byte[] keyfdprf = new byte[]{-79, -51, 113, 101, -48, 62, 89, 14, -33, -25, 56, -37, -120, -40, -72, -58};


    public void Lookup(String keyword, PrintWriter printWriter, RadioButton button) throws Exception
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

            // TODO : handle case where document_id not found
            DataInputStream reader = new DataInputStream(client.getInputStream());

            for (String document : documentId.split(",")) {
                // 1 - get list of blocks
                // 2 - for each block, download block
                // 3 - aggregate each block and add to list of Strings
                List<String> block_ids = getBlockIds(document, printWriter, reader);
                break;
            }

            printWriter.write("BYE");
            reader.close();

//            c.moveToFirst();
//            if(!c.isAfterLast()) {
//            //Get the output and show to the user
//            Toast.makeText(getApplicationContext(), documentId, Toast.LENGTH_SHORT).show();
//            System.out.println("Document Id(s): " + documentId);
//            //Write the message to output stream
//            printWriter.write("LOOKUP " + documentId);
//            printWriter.write("DOWNLD 1");
            // }
            //else
            //{
//            Toast.makeText(getApplicationContext(), keyword + " does not exist", Toast.LENGTH_SHORT).show();
//            System.out.println(keyword + " does not exist");
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

    private List<String> getBlockIds(String document, PrintWriter printWriter, DataInputStream reader) throws IOException {

        Log.e("-----", document);
        byte[] hash = hash(document.getBytes());
        byte[] seed = encrypt(keyfdprf, hash, "ECB", -1);
        List<Integer> indexes = generatePseudoRandomSubset(seed, MIN_BLOCKS);

        boolean match = false;
        for (Integer index : indexes) {

            if (!match) {
                byte[] block = retrieveBlock(index, printWriter, reader);
                // check if block belongs to document (first 32 byte of block)
                for (int i = 0; i < hash.length; i++) {
                    match = true;
                    if (block[i] != hash[i]) {
                        Log.e("-----mismtath", "" + block[i] + "      " + hash[i]);
                        i = hash.length;
                        match = false;
                    }
                }

                if (match) {
                    byte[] arr = {block[hash.length], block[hash.length + 1], block[hash.length + 2], block[hash.length + 3]};
                    ByteBuffer wrapped = ByteBuffer.wrap(arr); // big-endian by default
                    int number = wrapped.getInt();
                    Log.e("------", "SIZEF   " + number);
                    break;
                }
            }
        }

        // retrieve blocks indexed

        return null;
    }

    private byte[] retrieveBlock(int blockID, PrintWriter printWriter, DataInputStream reader) throws IOException {

        printWriter.println("DOWNLD " + blockID);
        Log.i("--------", "DOWNLOAD " + blockID);

        byte[] buffer = new byte[4096];
        reader.readFully(buffer, 0, 4096);
        return decrypt(keyprf, buffer, "CTR", blockID);
    }

    /**
     * Generates a list of n unique numbers within the range of blocks.length
     * using SHA1PRNG with the seed provided.
     *
     * @param seed
     * @param n
     */
    private List<Integer> generatePseudoRandomSubset(byte[] seed, int n) {
        List<Integer> numbers = new ArrayList<Integer>();
//        try{
//            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        ByteBuffer wrapped = ByteBuffer.wrap(seed); // big-endian by default
//                    int number = wrapped.getInt();
        Random random = new Random(wrapped.getLong());
//            Log.w("algo-----", random.());

        int count = 0;
        while (count < n) {
            int index = random.nextInt(NUM_SMALL_BLOCKS);  // TODO : this needs to be size of
            if (!numbers.contains(index)) {
                numbers.add(index);
                count++;
            }
        }

//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }

        return numbers;
    }

    /**
     * Returns byte[] of SHA-256 hash on input
     *
     * @param input
     * @return
     */
    private byte[] hash(byte[] input) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return digest.digest(input);
    }

    /**
     * Encrypts specified input using key.
     *
     * @param key   key to encrypt with
     * @param input input to encrypt
     * @param mode  either "CBC" or "ECB"
     * @param index used for IV for "CBC" mode. Can be ignored if using "ECB"
     * @return encrypted output NOTE: output may be longer than input due to padding
     */
    private byte[] encrypt(byte[] key, byte[] input, String mode, int index) {

        Cipher cipher;
        byte[] output = null;

        try {
            cipher = Cipher.getInstance("AES/" + mode + "/PKCS5Padding");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            if (mode.equals("CBC") || mode.equals("CTR")) {
                // create 16 byte IV based on block index
                byte[] iv = ByteBuffer.allocate(16).putInt(index).array();
                AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            } else {
                // no need for IV with ECB mode
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            }
            output = cipher.doFinal(input);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output;
    }

    private byte[] decrypt(byte[] key, byte[] input, String mode, int index) {

        Cipher cipher;
        byte[] output = null;

        try {
            cipher = Cipher.getInstance("AES/" + mode + "/PKCS5Padding");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            if (mode.equals("CBC") || mode.equals("CTR")) {
                // create 16 byte IV based on block index
                byte[] iv = ByteBuffer.allocate(16).putInt(index).array();
                AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            } else {
                // no need for IV with ECB mode
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
            }
            output = cipher.doFinal(input);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output;
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

                // Get selected radio button from radioGroup
                int selectedId = radioMethodGroup.getCheckedRadioButtonId();
                // Find the radio button from radio group
                radioButton = (RadioButton) findViewById(selectedId);

                try {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                client = new Socket(SERVER_IP, SERVER_PORT);
                                output = new PrintWriter(client.getOutputStream(), true);

                                Lookup(keyword, output, radioButton);

                                output.flush();
                                output.close();
                                //Closing the connection
                                client.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    //Connect to server

                } catch (Exception e) {
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