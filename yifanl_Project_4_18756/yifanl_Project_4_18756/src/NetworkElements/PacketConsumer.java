/**
 * @author Andrew Fox
 */
package NetworkElements;

import DataTypes.*;

public interface PacketConsumer {
	public void addNIC(NIC nic);
	public void addNIC(NIC nic,int side);
	public void sendFromBuffer();
}
