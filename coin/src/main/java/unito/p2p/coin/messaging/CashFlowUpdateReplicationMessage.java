

package unito.p2p.coin.messaging;

import java.io.IOException;

import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.util.rawserialization.JavaSerializer;



/**
 * Class CashFlowUpdateReplicationMessage
 */
public class CashFlowUpdateReplicationMessage extends CoinMessage {


  private CashFlowMessage cfm;
  
  private CashFlowUpdateMessage cfum;
  
  private NodeHandle accountRoot;
  
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
   * Create a CashFlowUpdateReplicationMessage
   * 
   * @param id unique identifier
   * @param cfm the CashFlowMessage which generated this CashFlowUpdate
   * @param the accountRoot which generated the CashFlowUpdateReplicationMessage
   */
  public CashFlowUpdateReplicationMessage(int id, CashFlowUpdateMessage cfum, NodeHandle accountRoot) { 
    super(id, cfum.getCashFlowMessage().getSource(), cfum.getCashFlowMessage().directDest.getId());
    
    this.accountRoot = accountRoot;
    this.cfm = cfum.getCashFlowMessage();
    this.cfum = cfum;
    this.replication = false;
  }
  
  public NodeHandle getAccountRoot() {
    return accountRoot;
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
   * Return the CashFlowUpdateMessage which generated this CashFlowUpdate
   * @return the CashFlowUpdateMessage
   */
  public CashFlowUpdateMessage getCashFlowUpdateMessage() {
    return cfum;
  }
  
  
  /**
   * Standard toString method
   */
  public String toString() {
    return "[CashFlowUpdateReplicationMessage from " + super.getSource() + " to " + getDirectDest() + " of " + cfm + "]";
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