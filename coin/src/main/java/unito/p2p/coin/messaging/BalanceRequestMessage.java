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
public class BalanceRequestMessage extends CoinMessage {

  
  private rice.p2p.commonapi.NodeHandle requester;
  private rice.p2p.commonapi.NodeHandle queried;
  
  private Object result;
  
  public static final short TYPE = 7;
  
  public static final long serialVersionUID = CoinMessage.SERIAL_BASE + (long) TYPE;
  

  public BalanceRequestMessage (int id, NodeHandle source, Id dest, NodeHandle requester) { 
    super(id, source, dest);
    
    this.queried = source;
    this.requester = requester;
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

  /**
   * Get the value of requester
   * @return the value of requester
   */
  public NodeHandle getRequester() {
    return requester;
  }
  
  /**
   * Get the value of requester
   * @return the value of requester
   */
  public NodeHandle getQueried() {
    return queried;
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
    return "[BalanceRequestMessage requester: " + requester + "]";
  }

}
