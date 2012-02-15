package edu.pu.ao.Micropublisher;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.haggle.Attribute;
import org.haggle.DataObject;
import org.haggle.Handle;
import org.haggle.LaunchCallback;
import org.haggle.Node;

import android.app.Application;
import android.content.Intent;
import android.database.Cursor;
import android.util.Base64;
import android.util.Log;

public class Micropublisher extends Application implements org.haggle.EventHandler {
	public static final String LOG_TAG = "Micropublisher";
	
	public static final int STATUS_OK = 0;
	public static final int STATUS_ERROR = -1;
	public static final int STATUS_REGISTRATION_FAILED = -2;
	public static final int STATUS_SPAWN_DAEMON_FAILED = -3;
	
	static final int PUBLISH_MESSAGE_REQUEST = 0;
	static final int MESSAGE_LENGTH_IN_BYTES = 140;
	
	private MicropublisherView mv = null;
	private org.haggle.Handle hh = null;
	private String publicKeyString = null;
	private String privateKeyString = null;
	private WebDropbox wd = null;
	
	/* private String publicTestKeyString = null;
	private String privateTestKeyString = null; */
	
	@Override
    public void onCreate() {
        super.onCreate();
        
        // start data service
        Intent intent = new Intent(this, UserDataService.class);
        startService(intent);
        Log.d(Micropublisher.LOG_TAG, "Micropublisher:onCreate()");
    }
	
	public Handle getHaggleHandle() {
		return hh;
	}
	
	public MicropublisherView getMv() {
		return mv;
	}
	
	public WebDropbox getWd() {
		return wd;
	}
	
	public void setMicropublisherView(MicropublisherView mv) {
		Log.d(Micropublisher.LOG_TAG, "Micropublisher: Setting mv");
		this.mv = mv;
	}
	
	public void setKeys(String publicKeyString, String privateKeyString) {
		if (keysSet())
			Log.e(Micropublisher.LOG_TAG, "keys already set");
		else {
			this.publicKeyString = publicKeyString;
			this.privateKeyString = privateKeyString;
			
			try {
				// write both keys to files
				FileOutputStream fosPublic = this.openFileOutput("publickey", 0);
				fosPublic.write(publicKeyString.getBytes());
				fosPublic.close();
	
				FileOutputStream fosPrivate = this.openFileOutput("privatekey", 0);
				fosPrivate.write(privateKeyString.getBytes());
				fosPrivate.close();
				Log.d(Micropublisher.LOG_TAG, "writing keys to files, pub: " + publicKeyString);
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(Micropublisher.LOG_TAG, "exception " + e.toString());
			}

			wd = new WebDropbox(this);
		}
	}
	
	public void init() {
		// set keys
		publicKeyString = readContentsFromFile("publickey");
		privateKeyString = readContentsFromFile("privatekey");
		if (publicKeyString != null && privateKeyString != null) {
			Log.d(Micropublisher.LOG_TAG, "set keys from file, pub: " + publicKeyString);
			
			// initialize WebDropbox
			wd = new WebDropbox(this);
		}
	}
	
	public String readContentsFromFile(String filename) {
		try {
			FileInputStream fis = this.openFileInput(filename);
			byte[] buffer = new byte[fis.available()]; // ugh, find a better way!!
			fis.read(buffer);
			fis.close();
			buffer = Base64.decode(buffer,  Base64.DEFAULT);
			return Base64.encodeToString(buffer,  Base64.DEFAULT);
		} catch (FileNotFoundException e) {
			// doesn't exist
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(Micropublisher.LOG_TAG, "problem reading from file " + filename);
			return null;
		}
	}
	
	public boolean keysSet() {
		if (readContentsFromFile("publickey") != null &&
				readContentsFromFile("privatekey") != null)
			return true;
		else
			return false;
	}
	
	public String getPublicKeyString() {
		if (!keysSet())
			Log.e(Micropublisher.LOG_TAG, "keys not set yet! public");
		return publicKeyString;
	}
	
	public String getPrivateKeyString() {
		if (!keysSet())
			Log.e(Micropublisher.LOG_TAG, "keys not set yet! private");
		return privateKeyString;
	}
	
	public boolean receivedMessageBefore(String messageId) {
		boolean received = false;
		MessageDbAdapter mdb = new MessageDbAdapter(mv);
		mdb.open();
		Cursor cursor = mdb.selectEntryByMessageId(messageId);
		cursor.moveToFirst();
		
		if (cursor.getCount() == 1)
			received = true;
		cursor.close();
		mdb.close();
		
		return received;
	}
	
	public void addToFriends(String publicKey, int distance) {
		if (distance == 1 && publicKey != null) {
			String uuid = Cryptography.getUuidFromPublicKey(publicKey);
			
			// add uuid as an interest
			Attribute attr = new Attribute("Message", uuid, 3);
			hh.registerInterest(attr);
			
		}
	}
	
	public int initHaggle() {
		if (hh != null)
			return STATUS_OK;
		
		/* Start up Haggle */
		int status = Handle.getDaemonStatus();
		
		if (status == Handle.HAGGLE_DAEMON_NOT_RUNNING || status == Handle.HAGGLE_DAEMON_CRASHED) {
			Log.d(Micropublisher.LOG_TAG, "Trying to spawn Haggle daemon");

			if (!Handle.spawnDaemon(new LaunchCallback() {
				
				public int callback(long milliseconds) {

					Log.d(Micropublisher.LOG_TAG, "Spawning milliseconds..." + milliseconds);

					if (milliseconds == 10000) {
						Log.d(Micropublisher.LOG_TAG, "Spawning failed, giving up");

						return -1;
					}
					return 0;
				}
			})) {
				Log.d(Micropublisher.LOG_TAG, "Spawning failed...");
				return STATUS_SPAWN_DAEMON_FAILED;
			}
		}
		long pid = Handle.getDaemonPid();

		Log.d(Micropublisher.LOG_TAG, "Haggle daemon pid is " + pid);

		/* Try to get a handle to a running Haggle instance */
		int tries = 1;
		while (tries > 0) {
			try {
				hh = new Handle("Micropublisher");

			} catch (Handle.RegistrationFailedException e) {
				Log.e(Micropublisher.LOG_TAG, "Registration failed : " + e.getMessage());

				if (e.getError() == Handle.HAGGLE_BUSY_ERROR) {
					Handle.unregister("Micropublisher");
					continue;
				} else if (--tries > 0) 
					continue;

				Log.e(Micropublisher.LOG_TAG, "Registration failed, giving up");
				return STATUS_REGISTRATION_FAILED;
			}
			break;
		}

		/* Register for events */
		hh.registerEventInterest(EVENT_NEIGHBOR_UPDATE, this);
		hh.registerEventInterest(EVENT_NEW_DATAOBJECT, this);
		hh.registerEventInterest(EVENT_INTEREST_LIST_UPDATE, this);
		hh.registerEventInterest(EVENT_HAGGLE_SHUTDOWN, this);
		
		hh.eventLoopRunAsync(this);
		
		Attribute attr = new Attribute("Message", "news", 1);
		hh.registerInterest(attr);
		
		return STATUS_OK;
	}
	

	//@Override
	public void onNeighborUpdate(Node[] neighbors) {
		Log.d(Micropublisher.LOG_TAG, "Got neighbor update, thread id=" + Thread.currentThread().getId());
		// TODO Auto-generated method stub
	}

	public void shutdownHaggle() {
		hh.shutdown();
	}
	
	//@Override
	public void onNewDataObject(DataObject dObj) {
		if (dObj.getAttribute("Message", 0) != null) {
			Log.d(Micropublisher.LOG_TAG, "received message from Haggle, filepath: " + dObj.getFilePath() +
					", time: " + System.currentTimeMillis());
		}
		
		mv.runOnUiThread(mv.new DataUpdater(dObj));
	}

	//@Override
	public void onInterestListUpdate(Attribute[] interests) {
		Log.d(Micropublisher.LOG_TAG, "Setting interests (size=" + interests.length + ")");
		// TODO Auto-generated method stub
		for (int i = 0; i < interests.length; i++)
			Log.d(Micropublisher.LOG_TAG, "interest: " + interests[i].getName());
	}

	//@Override
	public void onShutdown(int reason) {
		Log.d(Micropublisher.LOG_TAG, "Shutdown event, reason=" + reason);
		if (hh != null) {
			hh.dispose();
			hh = null;
		} else {
			Log.d(Micropublisher.LOG_TAG, "Shutdown: handle is null!");
		}
	}

	//@Override
	public void onEventLoopStart() {
		Log.d(Micropublisher.LOG_TAG, "Event loop started.");
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void onEventLoopStop() {
		Log.d(Micropublisher.LOG_TAG, "Event loop stopped.");
		// TODO Auto-generated method stub
		
	}

	// why is only this method synchronized?
	public void finiHaggle() {
		if (hh != null) {
			hh.eventLoopStop();
			hh.dispose();
			hh = null;
		}
	}
	
}