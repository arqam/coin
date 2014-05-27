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
 * Class AddMoneyMessage.
 * This message will be routed by accountRoot node while it
 * receive a message which need the account to be decremented
 * (eg: Cash Flow being source)
 */
public class FundingMessage extends CoinMessage {
  
  /**
   * contains the amount of money to add to the account
   */
  private int amount;
  
  private Object result;
  
  public static final short TYPE = 13;
  
  public static final long serialVersionUID = CoinMessage.SERIAL_BASE + (long) TYPE;
  

  /**
   * Create an AddMoneyMessage
   */
  public FundingMessage (int id, NodeHandle source, Id dest, int amount) { 
    super(id, source, dest);
    
    this.amount = amount;
  };
  
  /**
   * Raw Serialization **************************************
   *
   * @return The Type value
   */
  public short getType() {
    return TYPE;
  }
  
  public void setResponse(Object o) {
    super.setResponse();
    this.result = o;
  }
  
  public Object getResponse() {
    return result;
  }
  
  public int getAmount() {
    return amount;
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
    return "[FundingMessage source: " + getSource() + "]";
  }

}