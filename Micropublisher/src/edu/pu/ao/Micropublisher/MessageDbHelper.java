package edu.pu.ao.Micropublisher;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class MessageDbHelper extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "messages";
	private static final String DATABASE_CREATE = "CREATE TABLE " + DATABASE_NAME  + 
			"(_id integer primary key autoincrement, message text, uuid text, messageid text, signature text, name text, distance text, status integer, localid integer);";
	private static final String DATABASE_UPGRADE = "DROP TABLE IF EXISTS " + DATABASE_NAME;
	
	MessageDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
	//	db.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
		db.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(DATABASE_UPGRADE);
		onCreate(db);
	}
	
	public String getDbName() {
		return DATABASE_NAME;
	}
}