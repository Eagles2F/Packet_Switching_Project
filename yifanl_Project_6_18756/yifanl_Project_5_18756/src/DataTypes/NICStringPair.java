package DataTypes;

import NetworkElements.LSRNIC;

public class NICStringPair {
	private LSRNIC nic;
	private String label;
	
	public NICStringPair(LSRNIC n, String l){
		this.nic = n;
		this.label = l;
	}
	
	public LSRNIC getNic() {
		return nic;
	}
	public void setNic(LSRNIC nic) {
		this.nic = nic;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
}
