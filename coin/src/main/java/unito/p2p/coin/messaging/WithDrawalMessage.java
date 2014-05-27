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
 * Class WithDrawalMessage
 */
public class WithDrawalMessage extends CoinMessage {

  //
  // Fields
  //

  private int amount;
  private Object result;
  
  public static final short TYPE = 4;
  
  public static final long serialVersionUID = CoinMessage.SERIAL_BASE + (long) TYPE;
  
  //
  // Constructors
  //
  public WithDrawalMessage (int id, NodeHandle source, Id dest, int amount) { 
    super(id, source, dest);
    
    this.amount = amount;

  }


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
    return "[WithDrawalMessage source: " + getSource() + "]";
  }
}
