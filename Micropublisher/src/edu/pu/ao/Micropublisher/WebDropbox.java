package edu.pu.ao.Micropublisher;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import org.haggle.DataObject;

import android.os.Environment;
import android.util.Log;

public class WebDropbox {
	public static final String LOG_TAG = "WebDropbox";
	
	private static final int BUFFER_SIZE = 1024;
	private static final String IP_ADDRESS = "128.112.7.57";
	private static final int PORT = 5840;
	private final long MIN_INTERVAL = 10*1000;     // 10 seconds in millis;
	//private final long MAX_INTERVAL = 20*60*1000;  // 20 minutes in millis
	
	private Micropublisher mp;
	private String uuid;
	private ArrayList<String> files = new ArrayList<String>();
	private SendingThread sendThread;
	private ReceivingThread receiveThread;
	
	public WebDropbox(Micropublisher mp) {
		this.mp = mp;
		uuid = Cryptography.getUuidFromPublicKey(mp.getPublicKeyString());
		
		sendThread = new SendingThread(MIN_INTERVAL);
		if (!sendThread.isAlive()) {
			sendThread.start();
		}
		receiveThread = new ReceivingThread(MIN_INTERVAL);
		if (!receiveThread.isAlive()) {
			receiveThread.start();
		}
		Log.d(WebDropbox.LOG_TAG, "created WebDropbox");
	}

	public void addFile(String contentFilepath) {
		files.add(contentFilepath);
		Log.d(WebDropbox.LOG_TAG, "filepath to add to wd: " + contentFilepath);
	}

	public void cancelThreads() {
		sendThread.cancel();
		receiveThread.cancel();
	}
	
	private class ReceivingThread extends Thread {
			private long interval;
			private boolean toRun = true;
			private String lastReceivedMessageId = "0";
			
			private final String REQUEST_INDICATOR = "DATA REQUEST:::";
			private final String UP_TO_DATE_INDICATOR = "UP TO DATE:::";
			
			public ReceivingThread(long interval) {
				this.interval = interval;
			}

			public void run() {
				while (toRun) {
					try {

					//	Log.e(Micropublisher.LOG_TAG, "pull interval: " + interval);
						sleep(interval);
					}
					catch (Exception e) {
						toRun = false;
					}
					receiveData();
				}
			}

			private void receiveData() {
				boolean upToDate = false;
				boolean ableToConnect = true;
				
				while (!upToDate && ableToConnect) {
					Log.d(WebDropbox.LOG_TAG, "sending requests for data");
					try {
						InetAddress addr = InetAddress.getAllByName(IP_ADDRESS)[0];
						Log.d(WebDropbox.LOG_TAG, "address: " + addr);
						Socket sock = new Socket(IP_ADDRESS, PORT);
						Log.d(WebDropbox.LOG_TAG, String.valueOf(sock.getLocalPort()));
						
						// create streams
						DataOutputStream outStream = new DataOutputStream(sock.getOutputStream());
						DataInputStream inStream = new DataInputStream(sock.getInputStream());
						
						// write request to server
						String content = REQUEST_INDICATOR + lastReceivedMessageId;
						outStream.write(content.getBytes());
						Log.d(WebDropbox.LOG_TAG, "sent request to server: " + content);
						
						// create file to read data into
						File dir = new File(Environment.getExternalStorageDirectory() + "/Micropublisher");
						dir.mkdirs();
						String filename = "micropublisher-" + System.currentTimeMillis();
						String filepath = dir + "/" + filename;
						File generatedFile = new File(filepath);
						FileOutputStream fos = new FileOutputStream(generatedFile);
						
						// read data from socket and write into file
						byte[] buffer = new byte[BUFFER_SIZE];
						int read = 0;
						String firstData = null;
						while ((read = inStream.read(buffer)) == buffer.length) {
							fos.write(buffer);
							if (firstData == null)
								firstData = new String(buffer);
							buffer = new byte[BUFFER_SIZE];
						}
						if (read != -1) {
							fos.write(buffer, 0, read);
							if (firstData == null)
								firstData = new String(buffer, 0, read);
						}
						outStream.close();
						inStream.close();
						fos.close();
						sock.close();
						
						if (firstData != null && !firstData.contains(UP_TO_DATE_INDICATOR)) {
							Log.d(Micropublisher.LOG_TAG + "A", "received message from WebDropbox, time: " + System.currentTimeMillis());
							
							// parse file data
							ContentParser cp = new ContentParser(filepath);
							Log.d(Micropublisher.LOG_TAG, "received msg: " + cp.getText("message"));
							String messageId = cp.getText("messageId");
							lastReceivedMessageId = messageId;
							
							if (!mp.receivedMessageBefore(messageId)) {
								Log.d(Micropublisher.LOG_TAG + "B", "from wd, new: " + cp.getText("messageId"));
								// Add to Haggle
								DataObject dObj = new DataObject(filepath);
						        dObj.addAttribute("Message", "news");
						        dObj.addAttribute("Message", uuid);  // add UUID as an attribute
						        mp.getHaggleHandle().publishDataObject(dObj);
						        dObj.dispose();
								
							} else {
								Log.d(Micropublisher.LOG_TAG + "C", "from wd, seen before: " + cp.getText("messageId"));
							}
						} else {
							Log.e(Micropublisher.LOG_TAG, "server timestamp: " + firstData);
							Log.e(Micropublisher.LOG_TAG, "my timestamp: " + System.currentTimeMillis());
							upToDate = true;
						}
					} catch (ConnectException e) {
						ableToConnect = false;
						Log.d(WebDropbox.LOG_TAG, "pulling data: unable to connect", e);
					} catch (Exception e) {
						Log.e(WebDropbox.LOG_TAG, "error in receive thread", e);
					}
				}
			/*	if (!ableToConnect)
					interval = Math.min(interval * 2, MAX_INTERVAL);  // exponential backoff
				else
					interval = MIN_INTERVAL; */
			}

			public void cancel() {
				toRun = false;
				interrupt();
			}

		}
	
	private class SendingThread extends Thread {
		private long interval = -1;
		private boolean toRun = true;

		public SendingThread(long interval) {
			this.interval = interval;
		}

		public void run() {
			while (toRun) {
				try {

				//	Log.e(Micropublisher.LOG_TAG, "push interval: " + interval);
					sleep(interval);
				}
				catch (Exception e) {
					toRun = false;
				}
				if (!files.isEmpty())
					sendData();
			}
		}

		private void sendData() {
			boolean ableToConnect = true;
			
			Log.d(WebDropbox.LOG_TAG, "sending data");
			while (!files.isEmpty() && ableToConnect) {
				try {
					InetAddress addr = InetAddress.getAllByName(IP_ADDRESS)[0];
					Log.d(WebDropbox.LOG_TAG, "address: " + addr);
					Socket sock = new Socket(IP_ADDRESS, PORT);
					Log.d(WebDropbox.LOG_TAG, String.valueOf(sock.getLocalPort()));
					
					// write contents to server
					OutputStream outStream = sock.getOutputStream();
					String filepath = files.remove(0);
					FileInputStream fis = new FileInputStream(filepath);
					byte[] buffer = new byte[BUFFER_SIZE];
					int read = 0;
					while ((read = fis.read(buffer)) == buffer.length) {
						outStream.write(buffer);
					}
					outStream.write(buffer, 0, read);
					outStream.close();
					fis.close();
					sock.close(); // NEW
					Log.d(WebDropbox.LOG_TAG, "wrote one file to server: " + filepath);
					} catch (FileNotFoundException e) {
						Log.d(WebDropbox.LOG_TAG, "file no longer exists");
					}  catch (ConnectException e) {
						ableToConnect = false;
						Log.d(WebDropbox.LOG_TAG, "pushing data: unable to connect", e);
					} catch (Exception e) {
						Log.e(WebDropbox.LOG_TAG, "error in send thread", e);
					}
			}	
		
		/*	if (!ableToConnect)
				interval = Math.min(interval * 2, MAX_INTERVAL);  // exponential backoff
			else
				interval = MIN_INTERVAL;*/

			
		}

		public void cancel() {
			toRun = false;
			interrupt();
		}

	}
}