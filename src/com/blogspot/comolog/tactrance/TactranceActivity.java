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
import de.sciss.net.OSCPacket;
import de.sciss.net.OSCServer;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;

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

		String p = getString(R.string.port);
		final int port = Integer.parseInt(p);

		final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		
		try {
			String localPort = getString(R.string.local_port);
			int lp = Integer.parseInt(localPort);
			mOscServer = OSCServer.newUsing(OSCClient.UDP, lp);
			mOscServer.addOSCListener(new OSCListener() {

				@Override
				public void messageReceived(OSCMessage arg0,
						SocketAddress arg1, long arg2) {
					vibrator.vibrate(50);
				}
				
			});
			mOscServer.start();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
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
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		connect(ps, port);
		
        Button b = (Button)findViewById(R.id.button1);
        b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String s = et.getText().toString();
				if (s != null && !s.equals("")) {
					Editor e = pref.edit();
					e.putString(KEY_ADDRESS, s);
					e.commit();
					connect(s, port);
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return true;
			}
        });
    }
    
    private boolean connect(String s, int port) {
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
				
				OSCMessage msg = new OSCMessage( "/ip", new Object[] {localIpAddr, getString(R.string.local_port)});
				try {
					mOscClient.send(msg);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
				
				return true;
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return false;
    }
    
    private static final String KEY = "Tactrance";
    private static final String KEY_ADDRESS = "Addr";
    private OSCServer mOscServer;
    private OSCClient mOscClient;    
}
