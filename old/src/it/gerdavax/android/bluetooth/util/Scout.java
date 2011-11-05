package it.gerdavax.android.bluetooth.util;

public class Scout {
	private Scout() {

	}

	public static void exploreClass(String className){
		try {
			Class rfcommSocketClass = Class.forName(className);
			System.out.println("className" + rfcommSocketClass);
			
			ReflectionUtils.printMethods(rfcommSocketClass);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void exploreBluetoothDevice() {
	
	}
	
	public static void scoutRfcommSocket(){
		try {
			Class rfcommSocketClass = Class.forName("android.bluetooth.RfcommSocket");
			System.out.println("android.bluetooth.RfcommSocket?" + rfcommSocketClass);
			
			ReflectionUtils.printMethods(rfcommSocketClass);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void scoutDatabase() {
		try {
			Class databaseClass = Class.forName("android.bluetooth.Database");
			System.out.println("android.bluetooth.Database?" + databaseClass);
			
			ReflectionUtils.printMethods(databaseClass);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
