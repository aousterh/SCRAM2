package edu.pu.ao.Micropublisher;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

// TODO: figure out how to escape things properly!!!!

public class MessageDbAdapter {
	// Database fields
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_UUID = "uuid";
	public static final String KEY_MESSAGEID = "messageid";
	public static final String KEY_SIGNATURE = "signature";
	public static final String KEY_NAME = "name";
	public static final String KEY_DISTANCE = "distance";
	public static final String KEY_STATUS = "status";
	public static final String KEY_LOCALID = "localid";
	
	public static final int STATUS_VERIFIED = 0;
	public static final int STATUS_UNVERIFIED = 1;
	public static final int STATUS_FORGERY = 2;
	public static final int STATUS_FROM_ME = 3;
	
	private static final String DATABASE_TABLE = "messages";
	
	private Context context;
	private SQLiteDatabase database;
	private MessageDbHelper dbHelper;
	
	public MessageDbAdapter(Context context) {
		this.context = context;
	}
	
	public MessageDbAdapter open() throws SQLException {
		dbHelper = new MessageDbHelper(context);
		database = dbHelper.getWritableDatabase();
		return this;
	}
	
	public boolean isOpen() {
		return database.isOpen();
	}
	
	public void close() {
		dbHelper.close();
	}
	
	public long createMessageEntry(String message, String uuid, String messageId, String signature,
			String name, int distance, int status, int localId) {
		ContentValues initialValues = createContentValues(message, uuid, messageId, signature,
					name, distance, status, localId);
		return database.insert(DATABASE_TABLE, null, initialValues);
	}
	
	public boolean updateMessageEntry(String message, String uuid, String messageId, String signature,
			String name, int distance, int status, int localId) {
		ContentValues updateValues = createContentValues(message, uuid, messageId, signature,
				name, distance, status, localId);
		messageId = DatabaseUtils.sqlEscapeString(messageId);
		return (database.update(DATABASE_TABLE, updateValues, KEY_MESSAGEID + " = " + messageId, null) > 0);
	}
	
	/* public Cursor selectEntryById(int localId) throws SQLException {
		String query = "SELECT * FROM " + DATABASE_TABLE + " WHERE " + KEY_LOCALID + " = ?";
		Cursor cursor = database.rawQuery(query, new String[] { String.valueOf(localId) });
		return cursor;
	} */
	
	public Cursor selectEntryByMessageId(String messageId) throws SQLException {
		if (messageId == null) // TODO: deal with this better
			return null;
		
		String query = "SELECT * FROM " + DATABASE_TABLE + " WHERE " + KEY_MESSAGEID + " = ?";
	//	messageId = DatabaseUtils.sqlEscapeString(messageId);
		Cursor cursor = database.rawQuery(query, new String[] { messageId });
		return cursor;
	}
	
	public Cursor selectMessagesOrderByLocalId() throws SQLException {
		// TODO: implement better
		String query = "SELECT * FROM " + DATABASE_TABLE + " ORDER BY " + KEY_LOCALID + " DESC";
		Cursor cursor = database.rawQuery(query, null);
		return cursor;
	}
	
	public Cursor selectMessagesByUUID(String uuid) throws SQLException {
		String query = "SELECT * FROM " + DATABASE_TABLE + " WHERE " + KEY_UUID + " = ?";
		//	DO NOT ESCAPE HERE YOU SILLY PERSON.
		Cursor cursor = database.rawQuery(query, new String [] { uuid });
		return cursor;
	}
	
	public int getNextLocalId() throws SQLException {
		String query = "SELECT * FROM " + DATABASE_TABLE + " ORDER BY " + KEY_LOCALID + " DESC";
		Cursor cursor = database.rawQuery(query, null);
		cursor.moveToFirst();
		
		int nextId = 0;
		if (cursor.getCount() > 0)
			nextId = cursor.getInt(cursor.getColumnIndex(KEY_LOCALID)) + 1;
		cursor.close();
		return nextId;
	}
	
	public int getMessageCount() {
		// TODO: learn how to use count
		String query = "SELECT * FROM " + DATABASE_TABLE;
		Cursor cursor = database.rawQuery(query, null);
		int count = cursor.getCount();
		cursor.close();
		return count;
	}
	
	private ContentValues createContentValues(String message, String uuid, String messageId, String signature,
			String name, int distance, int status, int localId) {
		ContentValues contentValues = new ContentValues();
		
	/*	if (name != null)
			name = DatabaseUtils.sqlEscapeString(name);*/
			
		contentValues.put(KEY_MESSAGE, message); // TODO: fix this!!!!!
		contentValues.put(KEY_UUID, uuid);
		contentValues.put(KEY_MESSAGEID, messageId);  // TODO: seriously!!
		contentValues.put(KEY_SIGNATURE, signature);
		contentValues.put(KEY_NAME, name);
		contentValues.put(KEY_DISTANCE, distance);
		contentValues.put(KEY_STATUS, status);
		contentValues.put(KEY_LOCALID, localId);
		return contentValues;
	}
	
}