package net.hauschen.obdtest;

abstract public class ELMParam {

	public int mode;
	public int pid;
	public String name;
	public String unit;
	protected ELMInterface intf;
	public boolean hasCmd = false;
	
	protected float lastVal = (float)0.0;
	
	public ELMParam(String name, String unit, ELMInterface intf) {
		this.name = name;
		this.unit = unit;
		this.intf = intf;
	}
	
	public ELMParam(String name, String unit, int mode, int pid) {
		this.mode = mode;
		this.pid = pid;
		this.name = name;
		this.unit = unit;
		this.hasCmd = true;
	}
	
	public void parseMsg(int msg[], int msgLen) {}
	
	public float getVal() {
		return lastVal;
	}
	
	public String getCmd() {
		if (hasCmd) {
			return String.format("%02x%02x", this.mode, this.pid);
		} else {
			return null;
		}
	}
}
