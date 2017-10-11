package srt.inz.avspot;


import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

 @SuppressLint("NewApi") public class BTchat extends Activity {
	
	//private static final String TAG = "BTchat";
	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 2;
	private static final int REQUEST_ENABLE_BT = 3;

	// Name of the connected device
	private String mConnectedDeviceName = null;

	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BTChatService mChatService = null;
	
	Button brq,bvstop;
	
	 TTsmanager ttsManager = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (D)
			Log.e("BTchat", "+++ ON CREATE +++");
        
		// Set up the window layout
		setContentView(R.layout.btmain);
			
		bvstop=(Button)findViewById(R.id.bt_vstop);
		
		ttsManager = new TTsmanager();
        ttsManager.init(this);
        

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}    
        
    }
	
		public void	voice_spkstp(View view)
		{
			TTS_stop();		
		}
		 
	@Override
	public void onStart() {
		super.onStart();
		
		if (D)
			Log.e("BTchat", "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null)
				setupChat();
		}
	}
	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e("BTchat", "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BTChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
	}
	private void setupChat() {
		Log.d("BTchat", "setupChat()");

		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BTChatService(this, mHandler);

		// Initialize the buffer for outgoing messages
	//-----------------------------------------------	mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e("BTchat", "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e("BTchat", "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		//super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e("BTchat", "--- ON DESTROY ---");	
	}

	private void ensureDiscoverable() {
		if (D)
			Log.d("BTchat", "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	public void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BTChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);

		}
	}

	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		
		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i("BTchat", "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BTChatService.STATE_CONNECTED:
					setStatus(getString(R.string.title_connected_to,
							mConnectedDeviceName));
					
					break;
				case BTChatService.STATE_CONNECTING:
					setStatus(R.string.title_connecting);
					break;
				case BTChatService.STATE_LISTEN:
				case BTChatService.STATE_NONE:
					setStatus(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				
				break;
			case MESSAGE_READ:
				
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
							
				Toast.makeText(getBaseContext(), readMessage.trim(), Toast.LENGTH_SHORT).show();
				
				if(readMessage.contains("a"))
				{					
					speak("Vizhinjam : Vizhinjam International Seaport is a port under construction by the Arabian Sea at Trivandrum in India."
							+"The total project expenditure is pegged at 6595 crores over three phases and is proposed to follow the" 
							+"landlord port model with a view to catering to passenger, container and other clean cargo."
							+"Vizhinjam International Seaport Limited (VISL) is a special purpose government company (fully owned by Government of Kerala) that would act as an implementing agency for the development of a"
							+"greenfield port - Vizhinjam International Deepwater Multi purpose Seaport- at Vizhinjam in Thiruvananthapuram, capital city of Kerala.");				
				}
				
				if(readMessage.contains("b"))
				{			
					speak("Thiruvananthapuram, formerly known as Trivandrum, is the capital and the largest city of the Indian state of Kerala."
				+ "It is located on the west coast of India near the extreme south of the mainland. Referred to by Mahatma Gandhi as the Evergreen city of India,it is classified as a Tier-II city by the Government of India.");
		
				}
				if(readMessage.contains("c"))
				{						
					speak("Kollam or Quilon , formerly Desinganadu, is an old seaport and city on the Laccadive Sea coast of Kerala, India. The city is on the banks Ashtamudi Lake."
					+"Kollam has had a strong commercial reputation since the days of the Phoenicians and Romans");						
				}
				if(readMessage.contains("d"))
				{						
					speak("Kollam or Quilon , formerly Desinganadu, is an old seaport and city on the Laccadive Sea coast of Kerala, India. The city is on the banks Ashtamudi Lake."
					+"Kollam has had a strong commercial reputation since the days of the Phoenicians and Romans");						
				}
				
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};
	
	protected void speak(String ss) {
		
		// TODO Auto-generated method stub
		 String toSpeak = ss;
         //Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
        // txt.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
		 ttsManager.initQueue(toSpeak);
		 
    }
	protected void TTS_stop()
	{
		ttsManager.shutDown();
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d("BTchat", "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d("BTchat", "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	private void connectDevice(Intent data) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				BTDeviceList.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService.connect(device);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.btoption_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, BTDeviceList.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}    
}
