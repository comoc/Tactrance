/*******************************************************************************
 * Copyright (c) 2012 Akihiro Komori.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Akihiro Komori - initial API and implementation
 ******************************************************************************/
package com.blogspot.comolog.tactrance;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import de.sciss.net.OSCClient;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import de.sciss.net.OSCServer;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class TactranceActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final SharedPreferences pref = getSharedPreferences(KEY, Activity.MODE_PRIVATE);
        String ps = pref.getString(KEY_ADDRESS, "");
        
		final EditText et = (EditText)findViewById(R.id.editText1);
		et.setText(ps);

		String outp = getString(R.string.outgoing_port);
		mOutgoingPort = Integer.parseInt(outp);
		
		String inp = getString(R.string.incomming_port);
		mIncommingPort = Integer.parseInt(inp);

		final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
				
		try {
			mOscServer = OSCServer.newUsing(OSCClient.UDP, mIncommingPort);
			mOscServer.addOSCListener(new OSCListener() {

				@Override
				public void messageReceived(OSCMessage arg0,
						SocketAddress arg1, long arg2) {
					vibrator.vibrate(50);
				}
				
			});
			mOscServer.start();
		} catch (IOException e1) {
        	if (BuildConfig.DEBUG)
        		e1.printStackTrace();
		}
		
		try {
			mOscClient = OSCClient.newUsing( OSCClient.UDP );
//			mOscClient.addOSCListener(new OSCListener() {
//
//				@Override
//				public void messageReceived(OSCMessage arg0,
//						SocketAddress arg1, long arg2) {
//				}
//			});
		} catch (IOException e1) {
        	if (BuildConfig.DEBUG)
        		e1.printStackTrace();
		}
		
		connect(ps, mOutgoingPort);
		
        Button b = (Button)findViewById(R.id.button1);
        b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String s = et.getText().toString();
				if (s != null && !s.equals("")) {
					Editor e = pref.edit();
					e.putString(KEY_ADDRESS, s);
					e.commit();
					connect(s, mOutgoingPort);
				}
			}
        	
        });
        
        View v = findViewById(R.id.view1);
        v.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					OSCMessage msg = new OSCMessage( "/touch", new Object[] {  });
					try {
						mOscClient.send(msg);
					} catch (IOException e) {
			        	if (BuildConfig.DEBUG)
			        		e.printStackTrace();
					}
				}
				return true;
			}
        });
    }
    
    private boolean checkWifiState() {
    	WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    	int wifiState = wifiManager.getWifiState();
    	switch (wifiState) {
    	case WifiManager.WIFI_STATE_DISABLING:
    	    Log.v("WifiState", "WIFI_STATE_DISABLING");
    	    break;
    	case WifiManager.WIFI_STATE_DISABLED:
    	    Log.v("WifiState", "WIFI_STATE_DISABLED");
    	    break;
    	case WifiManager.WIFI_STATE_ENABLING:
    	    Log.v("WifiState", "WIFI_STATE_ENABLING");
    	    break;
    	case WifiManager.WIFI_STATE_ENABLED:
    	    Log.v("WifiState", "WIFI_STATE_ENABLED");
    	    break;
    	case WifiManager.WIFI_STATE_UNKNOWN:
    	    Log.v("WifiState", "WIFI_STATE_UNKNOWN");
    	    break;
    	}
    	
    	return wifiState == WifiManager.WIFI_STATE_ENABLED;
    }
    
    private boolean connect(String s, int port) {
		if (!checkWifiState()) {
			try {
				Toast.makeText(this, R.string.ckeck_wifi_state, Toast.LENGTH_SHORT).show();
			} catch (Resources.NotFoundException e) {
				if (BuildConfig.DEBUG)
					e.printStackTrace();
			}
		}
    	if (mOscClient != null) {
			mOscClient.setTarget(new InetSocketAddress(s, port));
			try {
				if (mOscClient.isConnected())
					mOscClient.stop();
				mOscClient.start();
				
		        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		        int ipAddress = wifiInfo.getIpAddress();
		        String localIpAddr =
		            ((ipAddress >> 0) & 0xFF) + "." +
		            ((ipAddress >> 8) & 0xFF) + "." +
		            ((ipAddress >> 16) & 0xFF) + "." +
		            ((ipAddress >> 24) & 0xFF);
				
				OSCMessage msg = new OSCMessage( "/ip", new Object[] {localIpAddr, String.valueOf(mIncommingPort)});
				try {
					mOscClient.send(msg);
				} catch (IOException e) {
		        	if (BuildConfig.DEBUG)
		        		e.printStackTrace();
				}				
				
				return true;
			} catch (IOException e1) {
	        	if (BuildConfig.DEBUG)
	        		e1.printStackTrace();
			}
		}
		return false;
    }

	@Override
	protected void onDestroy() {
		//-------
		//-------
		super.onDestroy();
	}

    private static final String KEY = "Tactrance";
    private static final String KEY_ADDRESS = "Addr";
    private OSCServer mOscServer;
    private OSCClient mOscClient;
    
    private int mIncommingPort;
    private int mOutgoingPort;
}
