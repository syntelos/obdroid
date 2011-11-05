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
 * This interface define common methods and Bluetooth constants for local and
 * remote Bluetooth devices.
 * 
 * @author Stefano Sanna - gerdavax@gmail.com - http://www.gerdavax.it
 * @since 0.2
 */
public interface BluetoothDevice {

	/**
	 * 
	 * @author Stefano Sanna - gerdavax@gmail.com
	 * @since 0.2
	 */
	public static final class BluetoothProfiles {
		/**
		 * UUID for Serial Port Profile (SPP)
		 */
		public static final int UUID_SERIAL_PORT_PROFILE = 0x1101;

		/**
		 * Dial-up Networking - LAN access via PPP
		 */
		public static final int UUID_DUN_PPP_PROFILE = 0x1102;
		// Dial-up Networking
		public static final int UUID_DUNP_PROFILE = 0x1103;
		public static final int UUID_OBEX_OBJECT_PUSH_PROFILE = 0x1105;
		public static final int UUID_OBEX_FTP_PROFILE = 0x1106;
		public static final int UUID_CORDLESS_TELEPHONY_PROFILE = 0x1109;
		public static final int UUID_AV_REMOTE_CONTROLLER_PROFILE = 0x110E;
		public static final int UUID_FAX_PROFILE = 0x1111;
		public static final int UUID_GA_HEADSET_PROFILE = 0x1108;
		public static final int UUID_GA_HEADSET_AUDIO_GATEWAY_PROFILE = 0x1112;
		public static final int UUID_HANDSFREE_PROFILE = 0x111E;
		public static final int UUID_HANDSFREE_AUDIO_GATEWAY_PROFILE = 0x111F;
		public static final int UUID_BASIC_PRINTING_PROFILE = 0x1122;
		public static final int UUID_BASIC_PRINTING_STATUS_PROFILE = 0x1123;
		public static final int UUID_HARDCOPY_CABLE_REPLACEMENT_PROFILE = 0x1125;
		public static final int UUID_HARDCOPY_CABLE_REPLACEMENT_PRINT_PROFILE = 0x1126;
		public static final int UUID_HARDCOPY_CABLE_REPLACEMENT_SCAN_PROFILE = 0x1127;
		public static final int UUID_HUMAN_DEVICE_INTERFACE_PROFILE = 0x1124;
	}

	public static final class BluetoothClasses {
		public static final int SERVICE_MAJOR_CLASS_INFORMATION = 0x800000;
		public static final int SERVICE_MAJOR_CLASS_TELEPHONY = 0x400000;
		public static final int SERVICE_MAJOR_CLASS_AUDIO = 0x200000;
		public static final int SERVICE_MAJOR_CLASS_OBEX = 0x100000;
		public static final int SERVICE_MAJOR_CLASS_CAPTURE = 0x080000;
		public static final int SERVICE_MAJOR_CLASS_RENDER = 0x040000;
		public static final int SERVICE_MAJOR_CLASS_NETWORK = 0x020000;
		public static final int SERVICE_MAJOR_CLASS_POSITION = 0x010000;

		public static final int DEVICE_MAJOR_COMPUTER = 256;
		public static final int DEVICE_MAJOR_PHONE = 512;
		public static final int DEVICE_MAJOR_LAN = 768;
		public static final int DEVICE_MAJOR_AV = 1024;
		public static final int DEVICE_MAJOR_PERIPHERAL = 1280;
		public static final int DEVICE_MAJOR_IMAGING = 1536;
		public static final int DEVICE_MAJOR_UNCLASSIFIED = 7936;

		public static final int DEVICE_MINOR_COMPUTER_MISC = 0;
		public static final int DEVICE_MINOR_COMPUTER_DESKTOP = 4;
		public static final int DEVICE_MINOR_COMPUTER_SERVER = 8;
		public static final int DEVICE_MINOR_COMPUTER_LAPTOP = 12;
		public static final int DEVICE_MINOR_COMPUTER_SUBLAPTOP = 16;
		public static final int DEVICE_MINOR_COMPUTER_PDA = 20;
		public static final int DEVICE_MINOR_COMPUTER_WATCH_SIZE = 24;
		public static final int DEVICE_MINOR_PHONE_MISC = 0;
		public static final int DEVICE_MINOR_PHONE_CELLULAR = 4;
		public static final int DEVICE_MINOR_PHONE_CORDLESS = 8;
		public static final int DEVICE_MINOR_PHONE_SMARTPHONE = 12;
		public static final int DEVICE_MINOR_PHONE_MODEM = 16;
	}

	public static final class BluetoothProtocols {
		public static final int PROTOCOL_SDP = 0x0001;
		public static final int PROTOCOL_UDP = 0x0002;
		public static final int PROTOCOL_RFCOMM = 0x0003;
		public static final int PROTOCOL_TCP = 0x0004;
		public static final int PROTOCOL_TCS_BIN = 0x0005;
		public static final int PROTOCOL_OBEX = 0x0008;
		public static final int PROTOCOL_IP = 0x0009;
		public static final int PROTOCOL_FTP = 0x000a;
		public static final int PROTOCOL_HTTP = 0x000c;
		public static final int PROTOCOL_BNEP = 0x000f;
		public static final int PROTOCOL_L2CAP = 0x0100;
	}

	/**
	 * Gets the friendly name associated to this Bluetooth device. For remote
	 * devices, friendly name could be not immediately retrieved by the
	 * Bluetooth service when the remote device is discovered
	 * 
	 * @return the friendly name of the device
	 * @since 0.2
	 */
	public String getName() throws BluetoothException;

	/**
	 * Gets the BD Address of this device.
	 * 
	 * @return the BD Address
	 * @since 0.2
	 */
	public String getAddress() throws BluetoothException;

}
