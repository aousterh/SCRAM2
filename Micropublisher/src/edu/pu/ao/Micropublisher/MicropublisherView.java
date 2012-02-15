package edu.pu.ao.Micropublisher;

import org.haggle.DataObject;
import org.haggle.DataObject.DataObjectException;
import org.haggle.Handle;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

public class MicropublisherView extends Activity {
	public static final int MENU_PUBLISH  = 1;
	public static final int MENU_DELETE_ALL = 2;
	public static final int MENU_MANAGE_KEYS = 3;
	
	private MessageAdapter messageAdpt = null;
	private Micropublisher mp = null;
	private boolean shouldRegisterWithHaggle = true;
	private Context context = this;
	private UserDbAdapter db;
	private MessageDbAdapter messageDb;
	
	/** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Micropublisher.LOG_TAG, "MicropublisherView:onCreate()");
        setContentView(R.layout.main);
        
        mp = (Micropublisher) getApplication();
        mp.setMicropublisherView(this);
        mp.init();
        
        ListView messageList = (ListView) findViewById(R.id.message_list);
        Log.d(Micropublisher.LOG_TAG, "messageList: " + messageList);
        messageAdpt = new MessageAdapter(this);
        messageList.setAdapter(messageAdpt);
        registerForContextMenu(messageList);
        
	}
	
	@Override
    public void onRestart() {
    	super.onRestart();
    	Log.d(Micropublisher.LOG_TAG, "MicropublisherView:onRestart()");
    }
	
	@Override
	public void onStart() {
		super.onStart();
		
		Log.d(Micropublisher.LOG_TAG, "MicropublisherView:OnStart()");
		 if (shouldRegisterWithHaggle) {
			 int ret = mp.initHaggle();
			 
			 if (ret != Micropublisher.STATUS_OK) {
				 Log.d(Micropublisher.LOG_TAG, "Micropublisher could not start Haggle daemon.");
			 } else {
				 Log.d(Micropublisher.LOG_TAG, "Registration with Haggle successful");
			 }
		 }
	}
	
	@Override
    protected void onResume() {
    	super.onResume();
    	Log.d(Micropublisher.LOG_TAG, "MicropublisherView:onResume()");
	}
	
	@Override
	protected void onPause() {
    	super.onPause();
    	Log.d(Micropublisher.LOG_TAG, "MicropublisherView:onPause()");

 	}
	
    @Override
    protected void onStop() {
    	super.onStop();
    	Log.d(Micropublisher.LOG_TAG, "MicropublisherView:onStop()");
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if (db != null && db.isOpen())
    		db.close();
    	if (messageDb != null && messageDb.isOpen())
    		messageDb.close();
    	Log.d(Micropublisher.LOG_TAG, "MicropublisherView:onDestroy()");
    }
/*	
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	super.onSaveInstanceState(savedInstanceState);
    	
    	savedInstanceState.putInt("messageId", messageId);
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	
        // restore state
        if (savedInstanceState == null) {
        	Log.e(Micropublisher.LOG_TAG, "no instance state!");
        	messageId = 0;
        } else {
        	Log.e(Micropublisher.LOG_TAG, "yes instance state!");
        	messageId = savedInstanceState.getInt("messageId");
        }
    }*/
    
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_PUBLISH, 0, R.string.menu_publish).setIcon(android.R.drawable.ic_menu_edit);
     //   menu.add(0, MENU_DELETE_ALL, 0, R.string.menu_delete_all).setIcon(android.R.drawable.ic_menu_delete);
        menu.add(0, MENU_MANAGE_KEYS, 0, R.string.menu_manage_keys).setIcon(android.R.drawable.ic_lock_lock);
        
        return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	final Intent intent = new Intent();
    	
    	switch (item.getItemId()) {
	    	case MENU_PUBLISH: {
	    		if (mp.keysSet()) {
		    		intent.setClass(getApplicationContext(), PublishView.class);
		        	this.startActivityForResult(intent, Micropublisher.PUBLISH_MESSAGE_REQUEST);
		        	return true;
	    		} else {
	    			Toast newToast = Toast.makeText(this, "don't have keys yet", Toast.LENGTH_SHORT);
	    			newToast.show();
	    		}
	    		break;
    		}
	    /*	case MENU_DELETE_ALL: {
	    		Handle haggleHandle = mp.getHaggleHandle();
	    		DataObject dObj = messageAdpt.deleteMessage(0);
	    		while (dObj != null) {
	    			haggleHandle.deleteDataObject(dObj);
	    			dObj.dispose();
	    			dObj = messageAdpt.deleteMessage(0);
	    		}
				Log.d(Micropublisher.LOG_TAG, "Deleted all messages");
				return true;
			}*/
	    	case MENU_MANAGE_KEYS: {
	    		/*intent.setComponent(new ComponentName("edu.pu.ao.MiniApp", "edu.pu.ao.MiniApp.MiniAppActivity"));
	    		startActivity(intent);*/
	    		intent.setComponent(new ComponentName("edu.pu.mf.iw.ProjectOne", "edu.pu.mf.iw.ProjectOne.MainScreen"));
	    		startActivity(intent);
	    		return true;
	    	}
    	}
    	return false;
    }
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
	    	case KeyEvent.KEYCODE_BACK:
	    	case KeyEvent.KEYCODE_HOME:
	    		Log.d(Micropublisher.LOG_TAG,"Key back, exit application and deregister with Haggle");
	    		mp.finiHaggle();
	    		shouldRegisterWithHaggle = true;
	    		this.finish();
	    		break;
	    	}
	    	
			return super.onKeyDown(keyCode, event);
		}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch(requestCode) {
			case Micropublisher.PUBLISH_MESSAGE_REQUEST:
				onPublishMessageResult(resultCode, data);
				break;
		}
	}
	/*
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		Log.d(Micropublisher.LOG_TAG, "onCreateContextMenu");
		if (messageAdpt.getDataObjects().length != 0) {
			menu.add("Delete");
			menu.add("Cancel");
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Log.d(Micropublisher.LOG_TAG, "onContextItemSelected" + item.getTitle() + "target view id=" + info.targetView.getId());
		
		if (messageAdpt.getDataObjects().length == 0)
			return true;
		
		if (item.getTitle() == "Delete") {
			DataObject dObj = messageAdpt.deleteMessage(info.position);
			mp.getHaggleHandle().deleteDataObject(dObj);
			dObj.dispose();
			Log.d(Micropublisher.LOG_TAG, "Disposed of data object");
		}
		return true;
	}*/
	
	private void onPublishMessageResult(int resultCode, Intent data) {
		String message = data.getStringExtra("message");
		Log.d(Micropublisher.LOG_TAG, "message is: " + message);
		
		try {
			
			String uuid = Cryptography.getUuidFromPublicKey(mp.getPublicKeyString());
			String messageId = uuid + System.currentTimeMillis();
			
			// signature is computed over the message, uuid, and messageId
			String signature = Cryptography.generateSignature(mp.getPrivateKeyString(), message + uuid + messageId);

			String contentFilepath = ContentParser.createFile(message, uuid, signature, messageId);
			if (contentFilepath != null) {
				// add to Haggle
				DataObject dObj = new DataObject(contentFilepath);
		        dObj.addAttribute("Message", "news");
		        dObj.addAttribute("Message", uuid);  // add UUID as an attribute
		        Handle hh = mp.getHaggleHandle();
		        if (hh != null)
		        	hh.publishDataObject(dObj);
		        else
		        	Log.e(Micropublisher.LOG_TAG, "uh oh, null!");
		        Log.d(Micropublisher.LOG_TAG + "G", "published data object: " + messageId + ", time: " + System.currentTimeMillis());
		        dObj.dispose();
		        
		        //add to WebDropbox
		        WebDropbox wd = mp.getWd();
		        if (wd != null)
		        	wd.addFile(contentFilepath);
			}
		//	}
		} catch (DataObjectException e) {
			Log.d(Micropublisher.LOG_TAG, "Could not create data object");
		}
	}
	
	public class DataUpdater implements Runnable {
		int type;
		DataObject dObj = null;
		UserNode userNode = null;
		
		public final int EVENT_NEW_USER_DATA = 4;
		
		public DataUpdater(DataObject dObj) {
			this.type = org.haggle.EventHandler.EVENT_NEW_DATAOBJECT;
			this.dObj = dObj;
		}
		
		public DataUpdater(UserNode userNode) {
			this.type = EVENT_NEW_USER_DATA;
			this.userNode = userNode;	
		}
		
		public void run() {
			Log.d(Micropublisher.LOG_TAG, "Running data updater, type: " + type);
			switch(type) {
			case org.haggle.EventHandler.EVENT_NEW_DATAOBJECT:
				handleEventNewDataObject(dObj);
				messageAdpt.refresh();	
				break;
			case EVENT_NEW_USER_DATA:
				messageAdpt.updateMessageData(userNode);
				break;
			}
			Log.d(Micropublisher.LOG_TAG, "data updater done");
		}
		
		public void handleEventNewDataObject(DataObject dObj) {
			String pubKeyString = null;
			Log.d(Micropublisher.LOG_TAG + "D", "new data object from Haggle, time: " + System.currentTimeMillis());
			MessageData messageData = null;
			
			// parse data object's content
			ContentParser cp = new ContentParser(dObj.getFilePath());
			String message = cp.getText("message");
			String signature = cp.getText("signature");
			String uuid = cp.getText("uuid");
			String messageId = cp.getText("messageId");
			
			if (mp.receivedMessageBefore(messageId)) {
				Log.d(Micropublisher.LOG_TAG + "E", "from haggle, seen before: " + messageId);
				return;
			} else
				Log.d(Micropublisher.LOG_TAG + "F", "from haggle, new: " + messageId);
			
			MessageDbAdapter mdb = new MessageDbAdapter(context);
			mdb.open();
			int nextLocalId = mdb.getNextLocalId();
			messageData = new MessageData(mdb, message, uuid, messageId, signature, nextLocalId);
			
			// retrieve the corresponding public key
	        UserDbAdapter db = new UserDbAdapter(context);
	        db.open();
	        Cursor cursor = db.selectEntryById(uuid);
	        cursor.moveToFirst();
	        String name = null;
	        if (mp.keysSet() &&
	        		uuid.equals(Cryptography.getUuidFromPublicKey(mp.getPublicKeyString()))) {
	        	// my uuid, verify signature
	        	
	        	if (Cryptography.verifySignature(mp.getPublicKeyString(), message + uuid + messageId, signature)) {
		        	messageData.setStatus(MessageData.STATUS_FROM_ME);
		        	messageData.setDistance(0);
	        	} else {
					messageData.setStatus(MessageData.STATUS_FORGERY);
				}  	
	        } else if (cursor.getCount() == 1) {
	        	// trusted node's uuid, verify signature
	        	
	        	pubKeyString = cursor.getString(cursor.getColumnIndex(UserDbAdapter.KEY_PUBLICKEY));
	        	if (Cryptography.verifySignature(pubKeyString, message + uuid + messageId, signature)) {
	        		name = cursor.getString(cursor.getColumnIndex(UserDbAdapter.KEY_NAME));
	        		int distance = cursor.getInt(cursor.getColumnIndex(UserDbAdapter.KEY_DISTANCE));
	        		messageData.setDistance(distance);
					messageData.setName(name);
					messageData.setStatus(MessageData.STATUS_VERIFIED);
				} else {
					messageData.setStatus(MessageData.STATUS_FORGERY);
				}  
	        } else {
	        	// unknown uuid
	        	messageData.setStatus(MessageData.STATUS_UNVERIFIED);
	        }
	        cursor.close();
			db.close();
			
			// save messageData to the database
			Log.d(Micropublisher.LOG_TAG, "commiting messageData: " + messageData + " fp: " + dObj.getFilePath());
			messageData.commitMessageData();
			mdb.close();
			
			return;
		}
	}
	
}