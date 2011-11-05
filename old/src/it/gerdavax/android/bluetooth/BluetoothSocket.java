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

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This interfaces models a RFComm connection to an assigned port to to a remote
 * device. BluetoothSocket instances are obtained invoking public
 * BluetoothSocket openSocket(int port) method on RemoteBluetoothDevice
 * 
 * @author Stefano Sanna - gerdavax@gmail.com - http://www.gerdavax.it
 * 
 */
public interface BluetoothSocket {

	/**
	 * Gets the RemoteBluetoothDevice instance that owns this socket
	 * 
	 * @return the RemoteBluetoothDevice which opened this socket
	 */
	public RemoteBluetoothDevice getRemoteBluetoothDevice();

	/**
	 * Gets the remote port (channel) connected through this socket
	 * 
	 * @return the port to which this socket is connected
	 */
	public int getPort();

	/**
	 * Gets the InputStream
	 * 
	 * @return the InputStream of this socket
	 * @throws Exception
	 */
	public InputStream getInputStream() throws Exception;

	/**
	 * Gets the OutputStream
	 * 
	 * @return
	 * @throws Exception
	 */
	public OutputStream getOutputStream() throws Exception;

	/**
	 * Closes this socket. Input and output stream will be no longer available.
	 * 
	 */
	public void closeSocket();
}
