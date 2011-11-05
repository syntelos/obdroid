/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/gerdavax/Documents/workspace/AndroidBluetoothLibrarySamples/src/android/bluetooth/IBluetoothDeviceCallback.aidl
 */
package android.bluetooth;

//import java.lang.String;
//import android.os.RemoteException;
import android.os.IBinder;
//import android.os.IInterface;
//import android.os.Binder;
//import android.os.Parcel;

public interface IBluetoothDeviceCallback extends android.os.IInterface {
	/** Local-side IPC implementation stub class. */
	public static abstract class Stub extends android.os.Binder implements android.bluetooth.IBluetoothDeviceCallback {
		private static final java.lang.String DESCRIPTOR = "android.bluetooth.IBluetoothDeviceCallback";

		/** Construct the stub at attach it to the interface. */
		public Stub() {
			this.attachInterface(this, DESCRIPTOR);
		}

		/**
		 * Cast an IBinder object into an IBluetoothDeviceCallback interface,
		 * generating a proxy if needed.
		 */
		public static android.bluetooth.IBluetoothDeviceCallback asInterface(android.os.IBinder obj) {
			if ((obj == null)) {
				return null;
			}
			android.os.IInterface iin = (android.os.IInterface) obj.queryLocalInterface(DESCRIPTOR);
			if (((iin != null) && (iin instanceof android.bluetooth.IBluetoothDeviceCallback))) {
				return ((android.bluetooth.IBluetoothDeviceCallback) iin);
			}
			return new android.bluetooth.IBluetoothDeviceCallback.Stub.Proxy(obj);
		}

		public android.os.IBinder asBinder() {
			return this;
		}

		public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
			switch (code) {
				case INTERFACE_TRANSACTION: {
					reply.writeString(DESCRIPTOR);
					return true;
				}
				case TRANSACTION_onGetRemoteServiceChannelResult: {
					data.enforceInterface(DESCRIPTOR);
					java.lang.String _arg0;
					_arg0 = data.readString();
					int _arg1;
					_arg1 = data.readInt();
					this.onGetRemoteServiceChannelResult(_arg0, _arg1);
					return true;
				}
			}
			return super.onTransact(code, data, reply, flags);
		}

		private static class Proxy implements android.bluetooth.IBluetoothDeviceCallback {
			private android.os.IBinder mRemote;

			Proxy(android.os.IBinder remote) {
				mRemote = remote;
			}

			public android.os.IBinder asBinder() {
				return mRemote;
			}

			public java.lang.String getInterfaceDescriptor() {
				return DESCRIPTOR;
			}

			public void onGetRemoteServiceChannelResult(java.lang.String address, int channel) throws android.os.RemoteException {
				android.os.Parcel _data = android.os.Parcel.obtain();
				try {
					_data.writeInterfaceToken(DESCRIPTOR);
					_data.writeString(address);
					_data.writeInt(channel);
					mRemote.transact(Stub.TRANSACTION_onGetRemoteServiceChannelResult, _data, null, IBinder.FLAG_ONEWAY);
				} finally {
					_data.recycle();
				}
			}
		}

		static final int TRANSACTION_onGetRemoteServiceChannelResult = (IBinder.FIRST_CALL_TRANSACTION + 0);
	}

	public void onGetRemoteServiceChannelResult(java.lang.String address, int channel) throws android.os.RemoteException;
}
