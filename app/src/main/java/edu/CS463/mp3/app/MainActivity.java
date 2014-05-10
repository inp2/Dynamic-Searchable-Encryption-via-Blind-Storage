package edu.CS463.mp3.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
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

    private static final int MIN_BLOCKS = 45;
    private static final int EXPANSION_PARAM = 4;
    private static final int NUM_SMALL_BLOCKS = 2048 * EXPANSION_PARAM;
    private static final int NUM_LARGE_BLOCKS = 81920 * EXPANSION_PARAM;

    private String keyword;
    private Socket client;
    private PrintWriter output;
    private EditText textField;
    private RadioGroup radioMethodGroup;
    private RadioButton radioButton;
    private RadioButton large;
    private Button send;
    private Button exit;
    private byte[] keyprf = new byte[]{-47, -28, -32, 36, -98, 101, 22, -94, 74, -108, -56, -38, -9, 16, 120, 123};
    private byte[] keyfdprf = new byte[]{-79, -51, 113, 101, -48, 62, 89, 14, -33, -25, 56, -37, -120, -40, -72, -58};


    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void Download(String keyword, PrintWriter printWriter) throws Exception
    {
        String documentId = "";

        //For Part-1
        //USE Local Index (cached) and find the documentID from there
        //For Part-2
        //  (a) If index is present in local cache use it to find the documentId
        //Connect to the database
        if (radioButton.getText().equals("Small")) {
            final SqlDatabaseConnectSmall db = new SqlDatabaseConnectSmall(getApplicationContext());
            Cursor c;
            c = db.getSmallDocumentIDs(keyword);

            if (c.getCount() == 0) {
                Log.w("SQLite Query", "No results found");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "No results found", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }

            if (c.moveToFirst()) {
                documentId = c.getString(c.getColumnIndex("document_id"));
            }
        }

        //Let us know what documents we are looking for
        System.out.println("Document Id(s): " + documentId);
//        Toast.makeText(getApplicationContext(), documentId, Toast.LENGTH_SHORT).show();

        DataInputStream reader = new DataInputStream(client.getInputStream());

        if (radioButton.getText().equals("Large")) {
            List<String> indexes = getFileContents(printWriter, "-" + (keyword.hashCode() % 15000), reader);

            BufferedReader bf = new BufferedReader(new StringReader(indexes.get(0)));

            String line;
            StringBuilder docs = new StringBuilder();
            while ((line = bf.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                Log.e("LINE-PART2", line);
                String[] pair = line.split(" ");

                if (pair[0].equals(keyword)) {
                    docs.append(",");
                    docs.append(pair[1]);
                }
            }

            if (docs.length() > 0) {
                documentId = docs.toString().substring(1);      // remove first ,
                Log.e("DOCS-PART2", documentId);
            }
        }

        final List<String> files = getFileContents(printWriter, documentId, reader);

        for (String file : files ) {
            Log.i("FILE RETURNED", file);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Total Emails: " + files.size());
                builder.setItems(files.toArray(new String[files.size()]), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();
                    }
                });
                AlertDialog alert = builder.create();
                alert.getListView().setFastScrollEnabled(true);
                alert.show();
            }
        });

        printWriter.write("BYE");
        reader.close();
    }

    private List<String> getFileContents(PrintWriter printWriter, String documentId, DataInputStream reader) throws IOException {
        List<String> files = new ArrayList<String>();
        for (String document : documentId.split(",")) {

            // get list of blocks for a file
            List<byte[]> blocks = getBlocksOfFile(document, printWriter, reader);

            StringBuilder builder = new StringBuilder();
            boolean firstBlock = true;
            for (byte[] block : blocks) {
                int offset = firstBlock ? 36 : 32;                          // account for sizef header in first block
                builder.append(new String(block, offset, 4096 - offset));
                firstBlock = false;
            }
            files.add(builder.toString());
        }
        return files;
    }

    private List<byte[]> getBlocksOfFile(String document, PrintWriter printWriter, DataInputStream reader) throws IOException {

        Log.i("DOWNLOADING DOCUMENT:", document);
        byte[] hash = hash(document.getBytes());
        byte[] seed = encrypt(keyfdprf, hash, "ECB", -1);
        List<Integer> indexes = generatePseudoRandomSubset(seed, MIN_BLOCKS);
        List<byte[]> blocks = new ArrayList<byte[]>();

        // find first block
        boolean match = false;
        int sizef = 0;
        for (Integer index : indexes) {
            if (!match) {
                byte[] block = retrieveBlock(index, printWriter, reader);
                blocks.add(block);
                // check if block belongs to document (first 32 byte of block)
                for (int i = 0; i < hash.length; i++) {
                    match = true;
                    if (block[i] != hash[i]) {
                        i = hash.length;
                        match = false;
                    }
                }

                if (match) {
                    byte[] arr = {block[hash.length], block[hash.length + 1], block[hash.length + 2], block[hash.length + 3]};
                    ByteBuffer wrapped = ByteBuffer.wrap(arr); // big-endian by default
                    sizef = wrapped.getInt();
                    Log.i("FOUND FIRST BLOCK", "SIZEF " + sizef);
                    break;
                }
            }
        }

        if (sizef == 0) {
            Log.w("SIZEF = 0", "Document not found");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Document not found", Toast.LENGTH_SHORT).show();
                }
            });
        }

        int num_download = EXPANSION_PARAM * sizef;

        // need to download at least MIN_BLOCKS
        if (num_download < MIN_BLOCKS) {
            num_download = MIN_BLOCKS;
        }

        indexes = generatePseudoRandomSubset(seed, num_download);
        blocks.clear();
        for (Integer index : indexes) {
            byte[] block = retrieveBlock(index, printWriter, reader);
            // check if block belongs to document (first 32 byte of block)
            for (int i = 0; i < hash.length; i++) {
                match = true;
                if (block[i] != hash[i]) {
                    i = hash.length;
                    match = false;
                }
            }

            // only add to blocks list if it belongs to file
            if (match) {
                blocks.add(block);
            }
        }

        return blocks;
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
        ByteBuffer wrapped = ByteBuffer.wrap(seed); // big-endian by default
        Random random = new Random(wrapped.getLong());

        int count = 0;
        while (count < n) {
            int num_block = radioButton.getText().equals("Small") ? NUM_SMALL_BLOCKS : NUM_LARGE_BLOCKS;
            int index = random.nextInt(num_block);
            if (!numbers.contains(index)) {
                numbers.add(index);
                count++;
            }
        }
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
            cipher = Cipher.getInstance("AES/" + mode + "/NoPadding");
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

        send = (Button) findViewById(R.id.button);
        radioMethodGroup = (RadioGroup) findViewById(R.id.radioGroup);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Get the keyword from the textfield
                keyword = textField.getText().toString();

                //Reset the text field to blank
                textField.setText("");

                try {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

//                            BufferedReader bf = new BufferedReader(new FileReader("/data/data/edu.CS463.mp3.app"));
                            try {
                                int selectedId = radioMethodGroup.getCheckedRadioButtonId();
                                radioButton = (RadioButton) findViewById(selectedId);

                                client = new Socket(SERVER_IP, SERVER_PORT);
                                output = new PrintWriter(client.getOutputStream(), true);


                                Download(keyword, output);

                                output.flush();
                                output.close();
                                //Closing the connection
                                client.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    System.out.println("CANNOT RUN THREAD" + e.toString());
                }
            }
        });

//        exit = (Button) findViewById(R.id.button2);
//
//        exit.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                try {
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                client = new Socket(SERVER_IP, SERVER_PORT);
//                                output = new PrintWriter(client.getOutputStream(), true);
//                                Toast.makeText(getApplicationContext(), "CLOSING", Toast.LENGTH_SHORT).show();
//                                output.write("BYE");
//                                output.flush();
//                                output.close();
//                                //Closing the connection
//                                client.close();
//                            } catch (Exception e) {
//                                System.out.println("ERROR With Connection: " + e.toString());
//                            }
//                        }
//                    }).start();
//                } catch (Exception e) {
//                    System.out.println("CANNOT RUN THREAD" + e.toString());
//                }
//            }
//        });
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
