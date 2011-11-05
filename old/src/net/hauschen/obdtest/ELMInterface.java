package net.hauschen.obdtest;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Vector;

import android.util.Log;

public class ELMInterface {
	private static final String TAG = "ELMInterface";
	private static final Integer ELMMAXBUF = 512;
	private InputStream _in;
	private OutputStream _out;
	private Integer _lock = new Integer(0);
	public String elmid = "Unknown";
	public String vid = "Unknown";
	
	private Vector<ELMParam> params = new Vector<ELMParam>();
	
	ELMInterface(InputStream in, OutputStream out) throws Exception {
		_in = in;
		_out = out;

		params.add(
				new ELMParam("RPM", "rpm", 0x01, 0x0c) {
					@Override
					public void parseMsg(int msg[], int msgLen) {
						this.lastVal = (float)((msg[2] << 8 | msg[3]) / 4.0);
					}
				}
			);
		params.add(
				new ELMParam("Speed", "km/h", 0x01, 0x0d) {
					@Override
					public void parseMsg(int msg[], int msgLen) {
						this.lastVal = (float)msg[2];
					}
				}
			);
		params.add(
				new ELMParam("Miles per Gallon", "mpg", this) {
					@Override
					public float getVal() {
						double vss = this.intf.getParam(0x01, 0x0d).getVal();
						double maf = this.intf.getParam(0x01, 0x10).getVal();
						return (float)((14.7 * 6.17 * 4.54 * vss * 0.621371) / (3600 * maf / 100.0));
					}
				}
			);
		params.add(
				new ELMParam("Engine Load", "%", 0x01, 0x04) {
					@Override
					public void parseMsg(int msg[], int msgLen) {
						this.lastVal = (float)msg[2] * 100 / 255;
					}
				}
			);
		params.add(
				new ELMParam("MAF rate", "g/s", 0x01, 0x10) {
					@Override
					public void parseMsg(int msg[], int msgLen) {
						this.lastVal = (float)((msg[2] << 8 | msg[3]) / 4.0);
					}
				}
			);
		params.add(
				new ELMParam("Timing advance", "g/s", 0x01, 0x0e) {
					@Override
					public void parseMsg(int msg[], int msgLen) {
						this.lastVal = ((float)msg[2] / (float)2.0) - 64;
					}
				}
			);
		params.add(
				new ELMParam("Temperature", "¡C", 0x01, 0x05) {
					@Override
					public void parseMsg(int msg[], int msgLen) {
						this.lastVal = (float)msg[2] - 40;
					}
				}
			);
		/*
		params.add(
				new ELMParam("Fuel Pressure", "kPa", 0x01, 0x0a) {
					@Override
					public void parseMsg(int msg[], int msgLen) {
						this.lastVal = (float)msg[2] * 3;
					}
				}
			);
			*/
		params.add(
				new ELMParam("Throttle", "%", 0x01, 0x11) {
					@Override
					public void parseMsg(int msg[], int msgLen) {
						this.lastVal = (float)msg[2] * 100 / 255;
					}
				}
			);
		params.add(
				new ELMParam("Run Time", "seconds", 0x01, 0x1f) {
					@Override
					public void parseMsg(int msg[], int msgLen) {
						this.lastVal = (float)(msg[2] << 8 | msg[3]);
					}
				}
			);

	}
		
	private void flush() throws Exception {
		_out.flush();
		while (_in.available() > 0) {
			_in.read();
		}
	}

	public static String toHexString(String s)
	{
		return toHexString(s.toCharArray());
	}
	
	public static String toHexString(char[]bytes)
	{
	    StringBuilder sb = new StringBuilder(bytes.length*2);
	    for(char b: bytes) {
	    	if (Character.isLetter(b) || Character.isDigit(b)) {
	    		sb.append(b);
	    	} else if (b == '\r') {
	    		sb.append("\\r");
	    	} else if (b == '\n') {
	    		sb.append("\\n");
	    	} else if (b == ' ') {
	    		sb.append(' ');
	    	} else {	    		
	    	    sb.append("\\" + Integer.toHexString(b+0x800).substring(1));
	    	}
	    }
	    return sb.toString();
	}
	
	private String readResponse() throws Exception
	{
		byte[] buf = new byte[ELMMAXBUF];
		String response = "";

		while (true) {
			int bytesRead = this._in.read(buf);
			String strBuf = new String(buf, 0, bytesRead);
			response += strBuf;
			
			if (strBuf.startsWith("-1")) {
				throw new Exception(
					"Timed out. Received [" + toHexString(response) + "]");
			}
			
			/*
			 * On initial connection, we can get off by one with our responses.
			 * Ignore any response with "ELM"
			 */
			if (strBuf.contains(">")) {
				break;
			}
			//Log.i(TAG, "Read so far: " + toHexString(response));
		}
		
		Log.i(TAG, "Received: " + toHexString(response));
		response = response.replace("SEARCHING...", "");
		response = response.trim();

		if (response.contains("?")) {
			throw new Exception("Did not understand");
		}

		if (response.contains("BUS BUSY")) {
			throw new Exception("Bus busy");
		}

		if (response.contains("FB ERROR")) {
			throw new Exception("Feedback error");
		}

		if (response.contains("DATA ERROR")) {
			throw new Exception("Data error");
		}

		if (response.contains("NO DATA")) {
			throw new Exception("No data");
		}

		if (response.contains("UNABLE TO CONNECT")) {
			throw new Exception("Unable to connect to ECU");
		}
		
		return response;		
	}
	
	private void sendCmd(String cmd) throws Exception {
		String s = cmd.trim() + "\r";
		this._out.write(s.getBytes());
	}
	
	public String doRawCmd(String cmd) throws Exception {
		Log.i(TAG, "doing " + cmd);
		synchronized(this._lock) {
			sendCmd(cmd);
			try {
				return readResponse().replace(">", "");
			} catch (Exception e) {
				String cause = e.getMessage();
				if (cause == null) cause = "Unknown problem";
				throw new Exception(cause + " while executing [" + cmd + "]");
			}
		}
	}
		
	public String doCmd(String cmd, String desc) throws Exception {
		Log.i(TAG, "doing " + cmd);
		synchronized(this._lock) {
			sendCmd(cmd);
			String response;
			try {
				response = readResponse();
				if (response.contains("ELM")) {
					Log.i(TAG, "Early response, trying again");
					sendCmd(cmd);
					response = readResponse();
				}
			} catch (Exception e) {
				String cause = e.getMessage();
				if (cause == null) cause = "Unknown problem";
				throw new Exception(cause + "while executing [" + cmd + "]");
			}
			if (!response.contains("OK")) {
				throw new Exception("Unable to " + desc + " [" + toHexString(response) + "]");
			}
			return response.replaceAll("[\r\n>]", " ");
		}
	}
			
	public void reset() throws Exception {
		this.elmid = doRawCmd("atz");
		Thread.sleep(200);
		flush();
		
		doCmd("ate0", "disable echo");
		doCmd("atl0", "disable linefeed");
		doCmd("ath0", "disable headers");
		
		this.vid = doRawCmd("010d");
		Log.i(TAG, "id: " + this.vid);
		
		/*
		 * Determine the fastest working timeout value
		 */
		String[] timeouts = {"0A", "14", "1E", "28", "32"};
		for (String timeout: timeouts) {
			try {
				doCmd("atst" + timeout, "set timeout");
				
				for (int i = 0; i < 3; i++) {
					doRawCmd("0100");
				}
				
				Log.i(TAG, "timeout set to " + timeout);
				return;
				
			} catch (Exception e) { 
				Log.i(TAG, "timeout " + timeout + "failed, trying next");
			}
		}
		throw new Exception("Unable to set any timeout");
	}
	
	public ELMParam getParam(int mode, int pid) {
		for (Iterator<ELMParam> i = this.params.iterator(); i.hasNext();) {
			ELMParam p = i.next();
			if (p.mode == mode && p.pid == pid) {
				return p;
			}
		}
		return null;
	}
	
	private void parseRawResults(String results) throws Exception {
		int[]msg = new int[5];
		String lines[] = results.split("[\\r\\n]+");
		for (String line: lines) {
			line = line.replace(" ", "");
			int numBytes = 0;
			for (int i = 0; i < line.length(); i+=2) {
				numBytes++;
				try {
					msg[i / 2] = Integer.parseInt(line.substring(i, i + 2), 16) & 0xff;
				} catch (Exception e) {
					msg[i / 2] = 0;
				}
			}
			if ((msg[0] & 0x40) != 0) {
				int mode = msg[0] & ~0x40;
				int pid = msg[1];
			    ELMParam p = getParam(mode, pid);
			    p.parseMsg(msg, numBytes);
			}
		}
	}
	
	public Vector<ELMParam> getParams() throws Exception {
		for (Iterator<ELMParam> i = this.params.iterator(); i.hasNext();) {
			ELMParam p = i.next();
			try {
				if (p.hasCmd) {
				String results = doRawCmd(p.getCmd());
					if (results != null) {
						Log.i(TAG, String.format("%s: %s [%s]",
								p.name, p.getCmd(), toHexString(results)));
						//String results = "41 0C 1A F8\r\r41 0C 1A F8\r\r";
						parseRawResults(results);
					}
				}
			} catch (Exception e) {
			}
		}
		
		return this.params;
	}
}
