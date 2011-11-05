package net.hauschen.obdtest;

import it.gerdavax.android.bluetooth.BluetoothSocket;
import it.gerdavax.android.bluetooth.LocalBluetoothDevice;
import it.gerdavax.android.bluetooth.LocalBluetoothDeviceListener;
import it.gerdavax.android.bluetooth.RemoteBluetoothDevice;
import it.gerdavax.android.bluetooth.RemoteBluetoothDeviceListener;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class OBDTest extends ListActivity {
	private static final String TAG = "OBDTest";
	private ELMInterface obd;
	private Thread t;
	protected Handler handler = new Handler();
	private int serviceChannel = 1;
	protected LocalBluetoothDevice localBluetoothDevice;
	protected static ProgressDialog dialog;
	protected ArrayList<String> devices;
	private TextView text;

	protected class DeviceAdapter extends BaseAdapter implements LocalBluetoothDeviceListener {

		public int getCount() {
			if (devices != null) {
				return devices.size();
			}
			return 0;
		}

		public Object getItem(int position) {
			return devices.get(position);
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout feedItem = null;

			try {
				if (convertView == null) {
					feedItem = new LinearLayout(OBDTest.this);
					String inflater = Context.LAYOUT_INFLATER_SERVICE;
					LayoutInflater vi = (LayoutInflater) OBDTest.this.getSystemService(inflater);
					vi.inflate(R.layout.item, feedItem, true);
				} else {
					feedItem = (LinearLayout) convertView;
				}

				TextView feedTitle = (TextView) feedItem.findViewById(R.id.address);
				TextView deviceNameAndClass = (TextView) feedItem.findViewById(R.id.name);

				String address = devices.get(position);
				String name = "null";
				String deviceClass = "null";
				try {
					RemoteBluetoothDevice dev;
					dev = localBluetoothDevice.getRemoteBluetoothDevice(address);
					name = dev.getName();
					deviceClass = "" + dev.getDeviceClass();
				} catch (Exception e) {
					e.printStackTrace();
					name = "ERROR";
				}

				if (name != null) {
					deviceClass = name + " - " + deviceClass;
				}

				feedTitle.setText(address);
				deviceNameAndClass.setText(deviceClass);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return feedItem;
		}

		public void scanCompleted(ArrayList<String> devs) {
			devices = devs;
			notifyDataSetChanged();
			hideProgressDialog();
		}

		public void scanStarted() {
			showProgressDialog();
		}

		public void bluetoothDisabled() {
			// TODO Auto-generated method stub
			
		}

		public void bluetoothEnabled() {
			// TODO Auto-generated method stub
			
		}

		public void deviceFound(String arg0) {
			// TODO Auto-generated method stub
			
		}

	}
	
	private class RemoteBluetoothDeviceEventHandler implements RemoteBluetoothDeviceListener {
		RemoteBluetoothDevice device;

		public void paired() {
			// connects to channel 1
			connectTo(device, 1);
		}

		public void pinRequested() {
			Log.d(TAG, "pinRequested()");
			
			// does not work as expected. To be investigated...
			//
			//Intent intent = new Intent("android.bluetooth.intent.action.PAIRING_REQUEST");
			//startActivity(intent);
		}

		public void gotServiceChannel(int serviceID, int channel) {
			serviceChannel = channel;
		}

		public void serviceChannelNotAvailable(int serviceID) {
			// TODO Auto-generated method stub
			
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.test);

		DeviceAdapter adapter = new DeviceAdapter();
		setListAdapter(adapter);

		getListView().setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> adapter, View arg1, int position, long arg3) {

				final String address = devices.get(position);

				try {
					AlertDialog.Builder builder = new AlertDialog.Builder(OBDTest.this);
					builder.setMessage("Do you want to connect to this device?").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							pair(address);
						}
					}).setNegativeButton("No", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
					AlertDialog alert = builder.create();
					alert.show();

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		});

		try {
			localBluetoothDevice = LocalBluetoothDevice.initLocalDevice(this);

			if (localBluetoothDevice.isEnabled()) {
				localBluetoothDevice.setListener(adapter);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = new Dialog(this);

		dialog.setContentView(R.layout.custom_dialog);
		dialog.setTitle("Connected to ELM");

		text = (TextView) dialog.findViewById(R.id.text);
		text.setText("(no data)");

		return dialog;
	}

	@Override
	protected void onDestroy() {
		super.onPause();
		localBluetoothDevice.close();
	}

	private void pair(String address) {
		RemoteBluetoothDevice device = localBluetoothDevice.getRemoteBluetoothDevice(address);
		RemoteBluetoothDeviceEventHandler listener = new RemoteBluetoothDeviceEventHandler();
		listener.device = device;
		device.setListener(listener);
		device.pair();
	}

	
	private void logData(String msg) {
		Message m = new Message();
		Bundle b = new Bundle();
		b.putString("msg", msg);
		m.setData(b);
		m.what = 0;
		handler.sendMessage(m);
	}

	private void setData(String msg) {
		Message m = new Message();
		Bundle b = new Bundle();
		b.putString("msg", msg);
		m.setData(b);
		m.what = 1;
		handler.sendMessage(m);
	}
	
	private void mainLoop(final RemoteBluetoothDevice device)
	{
		try {
			logData("Connecting");
			BluetoothSocket socket = device.openSocket(serviceChannel);
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			
			obd = new ELMInterface(in, out);
	
			logData("Resetting");
			obd.reset();
			logData("Connected to ECU");
			
			int iterations = 1;
			while (true) {
				String data = "Iteration: " + iterations + "\n";
				Vector<ELMParam> params = obd.getParams();
				for (Iterator<ELMParam> i = params.iterator(); i.hasNext();) {
					ELMParam p = i.next();
					data += String.format("%s: %.2f %s\n", 
							p.name, p.getVal(), p.unit);
				}
				setData(data);
				iterations++;
				
				Thread.sleep(250);
			}
	
		} catch (Throwable e) {
			logData("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	
	private void connectTo(final RemoteBluetoothDevice device, final int port) {
		try {
			
			handler = new Handler() {
				private String log = new String("");
				
				@Override
				public void handleMessage(Message msg) {
					if (msg.what == 0) {
						this.log += msg.getData().getString("msg") + "\n";
					} else {
						this.log = msg.getData().getString("msg");
					}
					if (this.log.length() > 500) {
						this.log = this.log.substring(this.log.length() - 500);
					}
					text.setText(this.log);
				}
			};
			
			handler.post(new Runnable() {
				public void run() {
					showDialog(0);
				}
			});
			
			t = new Thread() {
				@Override
				public void run() {
					while (true) {
						mainLoop(device);
						logData("Reconnecting");
						try {
							Thread.sleep(1000);
						} catch (Exception e) { }
					}
				}
			};
			t.start();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}
	}
	
	protected void showProgressDialog() {
		handler.post(new Runnable() {
			public void run() {
				dialog = ProgressDialog.show(OBDTest.this, "", "Scanning Bluetooth devices. Please wait...", true);
			}
		});
	}

	protected void hideProgressDialog() {
		handler.post(new Runnable() {
			public void run() {
				if (dialog != null) {
					dialog.dismiss();
				}
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, 0, Menu.NONE, "Scan").setIcon(android.R.drawable.ic_menu_search);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "selected option " + item.getItemId());
		if (item.getItemId() == 0) {
			try {
				localBluetoothDevice.scan();
			} catch (Exception e) {
			}
		}
		return super.onOptionsItemSelected(item);
	}
}