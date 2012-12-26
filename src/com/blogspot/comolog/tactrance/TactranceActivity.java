/*******************************************************************************
 * Copyright 2012 Akihiro Komori
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.blogspot.comolog.tactrance;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
import android.widget.TextView;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

public class TactranceActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final SharedPreferences pref = getSharedPreferences(KEY,
				Activity.MODE_PRIVATE);
		String targetAddress = pref.getString(KEY_ADDRESS, "");

		final EditText et = (EditText) findViewById(R.id.editText1);
		et.setText(targetAddress);

		final TextView tv = (TextView) findViewById(R.id.textViewIp);
		tv.setText(getIpAddress());

		mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

		// String outp = getString(R.string.outgoing_port);
		// mOutgoingPortDefault = Integer.parseInt(outp);

		// String inp = getString(R.string.incomming_port);
		// mIncommingPortDefault = Integer.parseInt(inp);

		// mIncommingPort = mIncommingPortDefault;
		// mOutgoingPort = mOutgoingPortDefault;
		mIncommingPort = mOutgoingPort = IO_PORT;

		// OSC In
		try {
			mOscIn = new OSCPortIn(mIncommingPort);
			mOscIn.addListener(OSC_ADDRESS_TOUCH, mOscListener);
		} catch (IOException e1) {
			if (BuildConfig.DEBUG)
				e1.printStackTrace();
		}

		// OSC Out
		try {
			if (!targetAddress.equals("")) {
				mOscOut = new OSCPortOut(InetAddress.getByName(targetAddress),
						mOutgoingPort);

				if (BuildConfig.DEBUG)
					Log.v(TAG, "mOscOut started");
			}
		} catch (IOException e1) {
			if (BuildConfig.DEBUG)
				e1.printStackTrace();
		}

		Button b = (Button) findViewById(R.id.button1);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String targetAddress = et.getText().toString();
				if (targetAddress != null && !targetAddress.equals("")) {
					Editor e = pref.edit();
					e.putString(KEY_ADDRESS, targetAddress);
					e.commit();

					try {
						if (!targetAddress.equals("")) {
							mOscOut = new OSCPortOut(InetAddress
									.getByName(targetAddress), mOutgoingPort);

							if (BuildConfig.DEBUG)
								Log.v(TAG, "mOscOut started");
						}
					} catch (IOException e1) {
						if (BuildConfig.DEBUG)
							e1.printStackTrace();
					}
				}
			}
		});

		View v = findViewById(R.id.view1);
		v.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (mOscOut == null)
						return false;

					try {
						if (mOscOut != null) {
							ArrayList<Object> args = new ArrayList<Object>();
							args.add("");
							OSCMessage m = new OSCMessage(OSC_ADDRESS_TOUCH,
									args);
							mOscOut.send(m);
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				return true;
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();

		// OSC Resume
		if (mOscIn != null)
			mOscIn.startListening();

	}

	@Override
	protected void onPause() {
		super.onPause();

		// OSC Pause
		if (mOscIn != null)
			mOscIn.stopListening();
	}

	@Override
	protected void onStop() {

		// OSC Close
		if (mOscIn != null)
			mOscIn.close();
		super.onStop();
	}

	// @Override
	// protected void onResume() {
	// TextView tv = (TextView) findViewById(R.id.textViewIp);
	//
	// if (checkWifiState()) {
	// tv.setText(getIpAddress());
	// if (mOscServer == null)
	// mOscServer = createServer(mIncommingPort);
	// } else
	// tv.setText(R.string.wifi_is_off);
	//
	// super.onResume();
	// }

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

		return wifiManager.isWifiEnabled();
	}

	// private OSCServer createServer(int port) {
	//
	// OSCServer s = null;
	// try {
	// s = OSCServer.newUsing(OSCClient.UDP, port);
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// if (s == null)
	// return null;
	//
	// try {
	// s.addOSCListener(mOscListener);
	// s.start();
	// } catch (IOException e1) {
	// if (BuildConfig.DEBUG)
	// e1.printStackTrace();
	// s.dispose();
	// s = null;
	// }
	// return s;
	// }
	//
	// private OSCClient createClient(String s, int port, boolean isTellingPort)
	// {
	// if (!checkWifiState()) {
	// try {
	// Toast.makeText(this, R.string.ckeck_wifi_state,
	// Toast.LENGTH_SHORT).show();
	// } catch (Resources.NotFoundException e) {
	// if (BuildConfig.DEBUG)
	// e.printStackTrace();
	// }
	// return null;
	// }
	//
	// OSCClient c;
	// try {
	// c = OSCClient.newUsing(OSCClient.UDP);
	// } catch (IOException e1) {
	// if (BuildConfig.DEBUG)
	// e1.printStackTrace();
	// return null;
	// }
	//
	// c.setTarget(new InetSocketAddress(s, port));
	// try {
	// c.start();
	// String localIpAddr = getIpAddress();
	// if (isTellingPort) {
	// OSCMessage msg = new OSCMessage("/ip", new Object[] {
	// localIpAddr, String.valueOf(mIncommingPort) });
	// try {
	// c.send(msg);
	// } catch (IOException e) {
	// if (BuildConfig.DEBUG)
	// e.printStackTrace();
	// c.dispose();
	// c = null;
	// }
	// }
	// } catch (IOException e1) {
	// if (BuildConfig.DEBUG)
	// e1.printStackTrace();
	// c.dispose();
	// c = null;
	// }
	// return c;
	// }
	//
	// @Override
	// protected void onDestroy() {
	// if (mOscClient != null)
	// mOscClient.dispose();
	// if (mOscServer != null)
	// mOscServer.dispose();
	// super.onDestroy();
	// }

	private String getIpAddress() {
		if (!checkWifiState())
			return "";
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		String ret = ((ipAddress >> 0) & 0xFF) + "."
				+ ((ipAddress >> 8) & 0xFF) + "." + ((ipAddress >> 16) & 0xFF)
				+ "." + ((ipAddress >> 24) & 0xFF);
		return ret;
	}

	private static final String KEY = "Tactrance";
	private static final String KEY_ADDRESS = "Addr";

	private OSCPortOut mOscOut;
	private OSCPortIn mOscIn;

	// private int mIncommingPortDefault;
	// private int mOutgoingPortDefault;
	private int mIncommingPort;
	private int mOutgoingPort;

	private Vibrator mVibrator;

	private static final String OSC_ADDRESS_TOUCH = "/touch";

	private static final int IO_PORT = 7770;

	private static final String TAG = "TactranceActivity";

	private OSCListener mOscListener = new OSCListener() {

		@Override
		public void acceptMessage(Date time, OSCMessage message) {
			if (mVibrator != null) {
				if (message.getAddress().equals(OSC_ADDRESS_TOUCH))
					mVibrator.vibrate(50);
			}
		}
	};
}
