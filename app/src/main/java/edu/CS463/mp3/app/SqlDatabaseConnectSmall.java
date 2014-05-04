package edu.CS463.mp3.app;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.sql.SQLException;


/**
 * Created by Nkechinyere on 5/3/14.
 */
public class SqlDatabaseConnectSmall extends SQLiteOpenHelper {
    //The Android's default system path of your application database.
    private static String DB_PATH = "/data/data/edu.CS463.mp3.app/databases/";
    private static String DB_NAME = "small_map.db";
    private final Context myContext;
    private SQLiteDatabase myDataBase;
    private SQLiteDatabase myData;


    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     *
     * @param context
     */
    public SqlDatabaseConnectSmall(Context context) {
        super(context, DB_NAME, null, 1);
        this.myContext = context;
    }

    public void openDataBase() throws SQLException {
        //Open the database
        String myPath = DB_PATH + DB_NAME;
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    }

    @Override
    public synchronized void close() {
        if (myDataBase != null)
            myDataBase.close();
        super.close();
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }


    public Cursor getSmallDocumentIDs(String keyword) {
        String myPath = DB_PATH + DB_NAME;

        //  myData = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
        File file = new File("/data/data/edu.CS463.mp3.app/databases/small_map.db");
        myData = SQLiteDatabase.openOrCreateDatabase(file, null);

        Cursor cur;
        cur = myData.rawQuery("select document_id from small_map where keyword='" + keyword + "'", null);
        cur.moveToFirst();
        myData.close();
        return cur;
    }
}
