

package unito.p2p.coin.messaging;

import java.io.IOException;

import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.util.rawserialization.JavaSerializer;



/**
 * Class CashFlowMessageUpdate
 */
public class CashFlowConfirmMessage extends CoinMessage {


  private CashFlowMessage cfm;
  
  public static final short TYPE = 4;
  
  public static final long serialVersionUID = CoinMessage.SERIAL_BASE + (long) TYPE;
  
  private boolean replication;
  
  /**
   * Raw Serialization **************************************
   *
   * @return The Type value
   */
  public short getType() {
    return TYPE;
  }
  
  /**
   * Create a CashFlowUpdateMessage
   * 
   * @param id unique identifier
   * @param cfm the CashFlowMessage which generated this CashFlowUpdate
   */
  public CashFlowConfirmMessage(int id, CashFlowMessage cfm) { 
    super(id, cfm.getSource(), cfm.directDest.getId());

    this.cfm = cfm;
    this.replication = false;
  }
  
  /**
   * Set the CashFlowUpdateMessage as a replication request.
   * The application call this method when using the message
   * to propagate the CashFlow to the account holders leafset
   */
  public void setReplication() {
    this.replication = true;
  }
  
  /**
   * Used to understad if this CashFlowUpdateMessage has been sent
   * as a replication request
   * 
   * @return a boolean representing replication state
   */
  public boolean isReplication() {
    return replication;
  }
  
  /**
   * Return the CashFlowMessage which generated this CashFlowUpdate
   * @return the CashFlowMessage
   */
  public CashFlowMessage getCashFlowMessage() {
    return cfm;
  }
  
  /**
   * Standard toString method
   */
  public String toString() {
    return "[CashFlowConfirmMessage from " + super.getSource() + " to " + getDirectDest() + " of " + cfm + "]";
  }
  
  /**
   * Serialize the given message
   *
   * @param buf DESCRIBE THE PARAMETER
   * @exception IOException DESCRIBE THE EXCEPTION
   */
  public void serialize(OutputBuffer buf) throws IOException {
    // use the java serializer for semplicity
    JavaSerializer.serialize(this, buf);
  }




}