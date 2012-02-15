package edu.pu.ao.Micropublisher;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

class MessageAdapter extends BaseAdapter implements ListAdapter {
	private Context mContext;
		
	public MessageAdapter(Context mContext) {
		this.mContext = mContext;
		Log.d(Micropublisher.LOG_TAG, "MessageAdapter contructor");
	}
	
	public int getCount() {
		MessageDbAdapter mdb = new MessageDbAdapter(mContext);
		mdb.open();
		int count = mdb.getMessageCount();
		mdb.close();
		
		if (count == 0)
			return 1;
		else
			return count;
	}

	public Object getItem(int position) {
		MessageDbAdapter mdb = new MessageDbAdapter(mContext);
		mdb.open();
		Cursor cursor = mdb.selectMessagesOrderByLocalId();
		cursor.moveToPosition(position);
		
		String message = cursor.getString(cursor.getColumnIndex(MessageDbAdapter.KEY_MESSAGE));
		String name = cursor.getString(cursor.getColumnIndex(MessageDbAdapter.KEY_NAME));
		int distance = cursor.getInt(cursor.getColumnIndex(MessageDbAdapter.KEY_DISTANCE));
		int status = cursor.getInt(cursor.getColumnIndex(MessageDbAdapter.KEY_STATUS));
	
		MessageData messageData = new MessageData(null, message, null, null, null, name, distance, status, -1);
		mdb.close();
		
		return messageData;
	}

	public long getItemId(int position) {
		return position; // this is silly
	}
	
	// TODO: fix this
/*	public synchronized DataObject deleteMessage(int position) {
		if (position >= dataObjects.size())
			return null;
		
		final DataObject dObj = dataObjects.get(position);
		
		if (dObj == null)
			return null;
		
		dataObjects.remove(position);
		
		if (data.remove(dObj) == null)
			return null;
		
		notifyDataSetChanged();
		
		return dObj;
	}*/
	
	/*public synchronized void updateMessages(DataObject dObj, MessageData messageData) {
		data.put(dObj, messageData);
		dataObjects.add(0, dObj);
		
		notifyDataSetChanged();
	}*/
	
	// assumption: updating other people's data, not yours
	public synchronized void updateMessageData(UserNode userNode) {
		String publicKeyString = null;
		
		Log.e(Micropublisher.LOG_TAG, "updating data for: " + userNode.getName());
		UserDbAdapter udb = new UserDbAdapter(mContext);
		udb.open();
		Cursor cursor = udb.selectEntryById(userNode.getUuid());
		cursor.moveToFirst();
		if (cursor.getCount() == 1)
			publicKeyString = cursor.getString(cursor.getColumnIndex(UserDbAdapter.KEY_PUBLICKEY));
		cursor.close();
		udb.close();
		
		MessageDbAdapter mdb = new MessageDbAdapter(mContext);
		mdb.open();
		cursor = mdb.selectMessagesByUUID(userNode.getUuid());
		cursor.moveToFirst();
		Log.e(Micropublisher.LOG_TAG, "cursor count: " + cursor.getCount());
		while (!cursor.isAfterLast()) {
			String message = cursor.getString(cursor.getColumnIndex(MessageDbAdapter.KEY_MESSAGE));
			String uuid = cursor.getString(cursor.getColumnIndex(MessageDbAdapter.KEY_UUID));
			String messageId = cursor.getString(cursor.getColumnIndex(MessageDbAdapter.KEY_MESSAGEID));
			String signature = cursor.getString(cursor.getColumnIndex(MessageDbAdapter.KEY_SIGNATURE));
			int localId = cursor.getInt(cursor.getColumnIndex(MessageDbAdapter.KEY_LOCALID));
			int status;
			if (Cryptography.verifySignature(publicKeyString, message + uuid + messageId, signature))
				status = MessageData.STATUS_VERIFIED;
			else
				status = MessageData.STATUS_FORGERY;
			
			MessageData messageData = new MessageData(mdb, message, uuid, messageId, signature,
					userNode.getName(), userNode.getDistance(), status, localId);
			boolean updated = messageData.commitMessageData();
			Log.d(Micropublisher.LOG_TAG, "?: " + updated + " name: " + userNode.getName());
			cursor.moveToNext();
		}
		
		cursor.close();
		mdb.close();

		Log.e(Micropublisher.LOG_TAG, "finished updating message data");
		
		notifyDataSetChanged();
	}

    public void refresh() {
    	notifyDataSetChanged();
    }
    
   /* TODO: fix this
    * public synchronized DataObject[] getDataObjects() {
    	return dataObjects.toArray(new DataObject[dataObjects.size()]);
    }
    */
    
	public View getView(int position, View convertView, ViewGroup parent) {    		
		View view = convertView;
		
		MessageDbAdapter mdb = new MessageDbAdapter(mContext);
		mdb.open();
		int count = mdb.getMessageCount();
		mdb.close();
		
		if (count == 0) {
			view = LayoutInflater.from(mContext).inflate(R.layout.empty_message_list_item, parent, false);
			TextView text = (TextView) view.findViewById(R.id.text_item);
			text.setText("none");
			return view;
		}
		
		ViewHolder viewHolder;
		
		if (view == null || view.getTag() == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.message_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.message = (TextView) view.findViewById(R.id.message_item);
            viewHolder.name = (TextView) view.findViewById(R.id.name_item);
            viewHolder.distance = (TextView) view.findViewById(R.id.distance_item);
            view.setTag(viewHolder);
        } else {
        	viewHolder = (ViewHolder) view.getTag();
        }
		
		MessageData messageData = (MessageData) getItem(position);
		
		if (messageData != null) {
			viewHolder.message.setText(messageData.getMessage());
			
			switch (messageData.getStatus()) {
				case MessageData.STATUS_FORGERY:
					viewHolder.distance.setText(null);
					viewHolder.name.setText("forged");
					viewHolder.name.setTextColor(Color.RED);
					break;
				case MessageData.STATUS_FROM_ME:
					viewHolder.distance.setText(null);
					viewHolder.name.setText("me");
					viewHolder.name.setTextColor(Color.CYAN);
					break;
				case MessageData.STATUS_UNVERIFIED:
					viewHolder.distance.setText(null);
					viewHolder.name.setText("unverified");
					viewHolder.name.setTextColor(Color.YELLOW);
					break;
				case MessageData.STATUS_VERIFIED:
					String name = messageData.getName();
					if (name != null)
						viewHolder.name.setText(name);
					else
						viewHolder.name.setText("unknown");
					viewHolder.distance.setText(String.valueOf(messageData.getDistance()));
					viewHolder.name.setTextColor(Color.GREEN);
					break;
			}
		}
		
		return view;
	}
	
	static class ViewHolder {
		private TextView message;
		private TextView name;
		private TextView distance;
	}
	
}
