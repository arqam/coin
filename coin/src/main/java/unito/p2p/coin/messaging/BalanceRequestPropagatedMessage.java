package unito.p2p.coin.messaging;

import java.io.IOException;

import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.Message;
import rice.Continuation;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.util.rawserialization.JavaSerializer;
import unito.p2p.coin.security.Signable;


/**
 * Class BalanceRequestMessage
 */
public class BalanceRequestPropagatedMessage extends CoinMessage {

  
  private BalanceRequestMessage brm;
  
  private int value;
  
  private NodeHandle accountRoot;
  
  public static final short TYPE = 7;
  
  public static final long serialVersionUID = CoinMessage.SERIAL_BASE + (long) TYPE;
  

  public BalanceRequestPropagatedMessage(int id, BalanceRequestMessage brm, NodeHandle accountRoot) { 
    super(id, null, (Id) null);
    
    this.brm = brm;
    this.accountRoot = accountRoot;
  };
  
  /**
   * Set the message to be a response containing a value.
   * This is called by accountholders when they reply to a BalanceRequest
   * to indicate the account value they have in memory
   * 
   * @param value the value to send as response
   */
  public void setResponse(int value) {
    this.value = value;
    
    super.setResponse();
  }
  
  public int getValue() {
    return value;
  }
  
  public NodeHandle getAccountRoot() {
    return accountRoot;
  }
  
  public BalanceRequestMessage getBalanceRequestMessage() {
    return brm;
  }
  
  /**
   * Raw Serialization **************************************
   *
   * @return The Type value
   */
  public short getType() {
    return TYPE;
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
  
  /**
   * Standard toString method
   */
  public String toString() {
    return "[BalanceRequestPropagatedMessage balancerequestmessage: " + brm + "]";
  }

}

