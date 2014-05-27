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
public class AddMoneyMessage extends CoinMessage {


  /**
   * contains the message which generated the money adding.
   * this is useful for nodes which will receive the AddMoneyMessage
   * being able to verify that the add money is correct
   */
  private CoinMessage cm;
  
  /**
   * contains the amount of money to add to the account
   */
  private int amount;
  
  /**
   * the node Id is going to be updated
   */
  private Id updated;
  
  private Object result;
  
  public static final short TYPE = 12;
  
  public static final long serialVersionUID = CoinMessage.SERIAL_BASE + (long) TYPE;
  

  /**
   * Create an AddMoneyMessage
   */
  public AddMoneyMessage (int id, NodeHandle source, Id dest, CoinMessage cm, int amount, Id updated) { 
    super(id, source, dest);
    
    this.updated = updated;
    this.cm = cm;
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
  
  public Id getUpdated() {
    return updated;
  }
  
  public void setResponse(Object o) {
    super.setResponse();
    this.result = o;
  }
  
  public Object getResponse() {
    return result;
  }

  public CoinMessage getCoinMessage() {
    return cm;
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
    return "[AddMoneyMessage source: " + getSource() + " dest: " + dest + "]";
  }

}
