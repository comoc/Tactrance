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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.GestureDetector;
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

		mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

		mHandler = new Handler();

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

		mView = findViewById(R.id.view1);
		mView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				float width = mView.getWidth();
				float height = mView.getHeight();
				int action = event.getAction();
				if (action == MotionEvent.ACTION_DOWN
						|| action == MotionEvent.ACTION_MOVE
						|| action == MotionEvent.ACTION_UP) {
					if (mOscOut == null)
						return false;

					try {
						if (mOscOut != null) {
							ArrayList<Object> args = new ArrayList<Object>();
							args.add(event.getX() / width);
							args.add(event.getY() / height);
							OSCMessage m = new OSCMessage(OSC_ADDRESS_TOUCH,
									args);
							mOscOut.send(m);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

				}

				mGestureDetector.onTouchEvent(event);

				return true;
			}
		});

		mAccels = new float[3];
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
			mSensor = mSensorManager
					.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}

		mGestureDetector = new GestureDetector(this, mOnGestureListener);
	}

	@Override
	protected void onResume() {
		super.onResume();

		final TextView tv = (TextView) findViewById(R.id.textViewIp);
		tv.setText(getIpAddress());

		mSensorManager.registerListener(mSensorEventListener, mSensor,
				SensorManager.SENSOR_DELAY_NORMAL);

		// OSC Resume
		if (mOscIn != null)
			mOscIn.startListening();

	}

	@Override
	protected void onPause() {
		super.onPause();

		mSensorManager.unregisterListener(mSensorEventListener, mSensor);

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

	private View mView;

	private static final String OSC_ADDRESS_TOUCH = "/touch";
	private static final String OSC_ADDRESS_ACCEL = "/accel";
	private static final String OSC_ADDRESS_SINGLE_TAP = "/singleTap";
	private static final String OSC_ADDRESS_DOUBLE_TAP = "/doubleTap";
	private static final String OSC_ADDRESS_LONG_PRESS = "/longPress";
	private static final String OSC_ADDRESS_FLING = "/fling";
	private static final String OSC_ADDRESS_SCROLL = "/scroll";

	private static final int IO_PORT = 7777;

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

	private SensorManager mSensorManager;
	private Sensor mSensor;
	private SensorEventListener mSensorEventListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor == mSensor) {

				mAccels[0] = event.values[0];
				mAccels[1] = event.values[1];
				mAccels[2] = event.values[2];
				mHandler.post(mRunnable);
			}
		}
	};

	private Runnable mRunnable = new Runnable() {

		@Override
		public void run() {
			// synchronized (TactranceActivity.this) {
			// try {
			// if (mOscOut != null) {
			// ArrayList<Object> args = new ArrayList<Object>();
			// args.add(mAccels[0]);
			// args.add(mAccels[1]);
			// args.add(mAccels[2]);
			// OSCMessage m = new OSCMessage(OSC_ADDRESS_ACCEL, args);
			// mOscOut.send(m);
			// }
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			// }
		}
	};

	private Handler mHandler;
	private float[] mAccels;

	private GestureDetector mGestureDetector;
	private GestureDetector.SimpleOnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {

		@Override
		public boolean onDoubleTap(MotionEvent event) {
			// Toast.makeText(TactranceActivity.this, "onDoubleTap",
			// Toast.LENGTH_SHORT).show();

			synchronized (TactranceActivity.this) {
				try {
					if (mOscOut != null) {
						float width = mView.getWidth();
						float height = mView.getHeight();
						ArrayList<Object> args = new ArrayList<Object>();
						args.add(event.getX() / width);
						args.add(event.getY() / height);
						OSCMessage m = new OSCMessage(OSC_ADDRESS_DOUBLE_TAP,
								args);
						mOscOut.send(m);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return super.onDoubleTap(event);
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent event) {
			// Toast.makeText(TactranceActivity.this, "onDoubleTapEvent",
			// Toast.LENGTH_SHORT).show();
			return super.onDoubleTapEvent(event);
		}

		@Override
		public boolean onDown(MotionEvent event) {
			// Toast.makeText(TactranceActivity.this, "onDown",
			// Toast.LENGTH_SHORT)
			// .show();
			return super.onDown(event);
		}

		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2,
				float velocityX, float velocityY) {
			// Toast.makeText(TactranceActivity.this, "onFling",
			// Toast.LENGTH_SHORT).show();

			synchronized (TactranceActivity.this) {
				try {
					if (mOscOut != null) {
						ArrayList<Object> args = new ArrayList<Object>();
						args.add(velocityX);
						args.add(velocityY);
						OSCMessage m = new OSCMessage(OSC_ADDRESS_FLING, args);
						mOscOut.send(m);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return super.onFling(event1, event2, velocityX, velocityY);
		}

		@Override
		public void onLongPress(MotionEvent event) {
			// Toast.makeText(TactranceActivity.this, "onLongPress",
			// Toast.LENGTH_SHORT).show();

			synchronized (TactranceActivity.this) {
				try {
					if (mOscOut != null) {
						float width = mView.getWidth();
						float height = mView.getHeight();

						ArrayList<Object> args = new ArrayList<Object>();
						args.add(event.getX() / width);
						args.add(event.getY() / height);
						OSCMessage m = new OSCMessage(OSC_ADDRESS_LONG_PRESS,
								args);
						mOscOut.send(m);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			super.onLongPress(event);
		}

		@Override
		public boolean onScroll(MotionEvent event1, MotionEvent event2,
				float distanceX, float distanceY) {
			// Toast.makeText(TactranceActivity.this, "onScroll",
			// Toast.LENGTH_SHORT).show();

			synchronized (TactranceActivity.this) {
				try {
					if (mOscOut != null) {
						float width = mView.getWidth();
						float height = mView.getHeight();

						ArrayList<Object> args = new ArrayList<Object>();
						args.add(event1.getX() / width);
						args.add(event1.getY() / height);
						args.add(event2.getX() / width);
						args.add(event2.getY() / height);
						float mwh = width > height ? width : height;
						args.add(distanceX / mwh);
						args.add(distanceY / mwh);
						args.add(event2.getPointerCount());

						OSCMessage m = new OSCMessage(OSC_ADDRESS_SCROLL, args);
						mOscOut.send(m);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return super.onScroll(event1, event2, distanceX, distanceY);
		}

		@Override
		public void onShowPress(MotionEvent event) {
			// Toast.makeText(TactranceActivity.this, "onShowPress",
			// Toast.LENGTH_SHORT).show();
			super.onShowPress(event);
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent event) {
			// Toast.makeText(TactranceActivity.this, "onSingleTapConfirmed",
			// Toast.LENGTH_SHORT).show();

			synchronized (TactranceActivity.this) {
				try {
					if (mOscOut != null) {
						float width = mView.getWidth();
						float height = mView.getHeight();

						ArrayList<Object> args = new ArrayList<Object>();
						args.add(event.getX() / width);
						args.add(event.getY() / height);
						OSCMessage m = new OSCMessage(OSC_ADDRESS_SINGLE_TAP,
								args);
						mOscOut.send(m);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return super.onSingleTapConfirmed(event);
		}

		@Override
		public boolean onSingleTapUp(MotionEvent event) {
			// Toast.makeText(TactranceActivity.this, "onSingleTapUp",
			// Toast.LENGTH_SHORT).show();
			return super.onSingleTapUp(event);
		}
	};
}
