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
 * Signals a general, Bluetooth-related error.
 * 
 * @author Stefano Sanna - gerdavax@gmail.com - http://www.gerdavax.it
 * @since 0.3
 */
public class BluetoothException extends Exception {

	private static final long serialVersionUID = -2281359045593775915L;

	/**
	 * Constructs a new BluetoothException with the specified cause
	 * 
	 * @param t
	 *            the cause (which is saved for later retrieval by the
	 *            Throwable.getCause() method). (A null value is permitted, and
	 *            indicates that the cause is nonexistent or unknown.)
	 */
	public BluetoothException(Throwable t) {
		super(t);
	}

	/**
	 * Constructs an BluetoothException with given error detail message.
	 */
	public BluetoothException(String message) {
		super(message);
	}

	/**
	 * Constructs a new exception with the specified cause and a detail message
	 * of (cause==null ? null : cause.toString()) (which typically contains the
	 * class and detail message of cause).
	 * 
	 * @param message
	 *            the detail message (which is saved for later retrieval by the
	 *            Throwable.getMessage() method).
	 * @param t
	 *            the cause (which is saved for later retrieval by the
	 *            Throwable.getCause() method). (A null value is permitted, and
	 *            indicates that the cause is nonexistent or unknown.)
	 */
	public BluetoothException(String message, Throwable t) {
		super(message, t);
	}

}
