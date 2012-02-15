package edu.pu.ao.Micropublisher;


public class MessageData {
	private MessageDbAdapter mdb;
	private String message;
	private String uuid;	// unique id associated with this message's author
	private String messageId;  // unique id associated with this message
	private String signature;  // computed over message + messageId
	private String name = null;
	private int distance;	// trust distance
	private int status = STATUS_UNVERIFIED;
	private int localId;
	
	public static final int STATUS_VERIFIED = 0;
	public static final int STATUS_UNVERIFIED = 1;
	public static final int STATUS_FORGERY = 2;
	public static final int STATUS_FROM_ME = 3;
	
	public MessageData(MessageDbAdapter mdb, String message, String uuid, String messageId,
			String signature, int localId) {
		this.mdb = mdb;
		this.message = message;
		this.uuid = uuid;
		this.messageId = messageId;
		this.signature = signature;
		this.name = null;
		this.distance = Integer.MAX_VALUE;
		this.status = STATUS_UNVERIFIED;
		this.localId = localId;
	}
	
	public MessageData(MessageDbAdapter mdb, String message, String uuid, String messageId,
			String signature, String name, int distance, int status, int localId) {
		this.mdb = mdb;
		this.message = message;
		this.uuid = uuid;
		this.messageId = messageId;
		this.signature = signature;
		this.name = name;
		this.distance = distance;
		this.status = status;
		this.localId = localId;
	}
	
	public String toString() {
		return "msg: " + message + ", uuid: " + uuid + ", messageId: " + messageId + ", signature: " + signature + 
				", name: " + name + ", distance: " + distance + ", status: " + status + ", localId: " + localId;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public String getSignature() {
		return signature;
	}
	
	public String getMessageId() {
		return messageId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getDistance() {
		return distance;
	}
	
	public void setDistance(int distance) {
		this.distance = distance;
	}
	
	public int getStatus() {
		return status;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
	
	public boolean commitMessageData() {
		if (!mdb.updateMessageEntry(message, uuid, messageId, signature, name, distance, status, localId))
			return (mdb.createMessageEntry(message, uuid, messageId, signature, name, distance, status, localId) != -1);
		return true;
	}
	
}