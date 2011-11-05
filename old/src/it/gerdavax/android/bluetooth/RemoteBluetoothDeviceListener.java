/*
 * Copyright (C) 2009 Stefano Sanna
 * 
 * gerdavax@gmail.com - http://www.gerdavax.it
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.gerdavax.android.bluetooth;

/**
 * Interface definition for a callback to be invoked when an event happens on a
 * RemoteBluetoothDevice.
 * 
 * @author Stefano Sanna - gerdavax@gmail.com - http://www.gerdavax.it
 * 
 */
public interface RemoteBluetoothDeviceListener {

	/**
	 * Called when the RemoteBluetoothDevice has been paired (and
	 * BluetoothSockets can be opened)
	 */
	public void paired();

	/**
	 * Called when the remote requires to send the security PIN. The PIN could
	 * be provided by the user opening the window in the notification area or
	 * programmatically using the <code>setPin(int)</code> on the
	 * RemoteBluetoothDevice instance.
	 * 
	 */
	public void pinRequested();

	/**
	 * Called when requested service is supported by the device and channel
	 * number has been retrieved
	 * 
	 * @param serviceID
	 * @param channel
	 * @since 0.2
	 */
	public void gotServiceChannel(int serviceID, int channel);

	/**
	 * Called when requested service is not supported by this device
	 * 
	 * @param serviceID
	 * @since 0.2
	 */
	public void serviceChannelNotAvailable(int serviceID);
}
