package unito.p2p.coin.messaging;
import java.io.IOException;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.commonapi.rawserialization.RawMessage;
import rice.Continuation;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.Id;
import unito.p2p.coin.security.Signable;



/**
 * Class CoinMessage
 */
abstract public class CoinMessage implements RawMessage, Signable  {

  protected int id;
  protected rice.p2p.commonapi.NodeHandle source;
  protected rice.p2p.commonapi.Id dest;
  protected rice.p2p.commonapi.NodeHandle directDest;
  protected String mac;
  protected boolean response;
  
  public static final long SERIAL_BASE = 35160;
  
  /**
   * Create a messege to be sent to the root node of the given id
   * @param source
   * @param dest
   */
  public CoinMessage (int id, NodeHandle source, Id dest) {
    this.id = id;
    this.source = source;
    this.dest = dest;
  };
  
  /**
   * Create a message to be sent directly to a given node
   * @param source
   * @param dest
   */
  public CoinMessage (int id, NodeHandle source, NodeHandle directDest) {
    this.id = id;
    this.source = source;
    this.directDest = directDest;
    this.dest = directDest.getId();
  };
  

  /**
   * Set the value of id
   * @param newVar the new value of id
   */
  protected void setId ( int newVar ) {
    id = newVar;
  }

  /**
   * Get the value of id
   * @return the value of id
   */
  protected int getUId ( ) {
    return id;
  }

  /**
   * Set the value of source
   * @param newVar the new value of source
   */
  protected void setSource ( rice.p2p.commonapi.NodeHandle newVar ) {
    source = newVar;
  }


  /**
   * Set the value of dest
   * @param newVar the new value of dest
   */
  protected void setDest ( rice.p2p.commonapi.Id newVar ) {
    dest = newVar;
  }

  /**
   * Get the value of dest
   * @return the value of dest
   */
  protected rice.p2p.commonapi.Id getDest ( ) {
    return null;
  }
  
  
  /**
   * Get the directDest
   * @return the direct destination for this message
   */
  protected NodeHandle getDirectDest() {
    return directDest;
  }

  /**
   * Set the value of mac
   * @param newVar the new value of mac
   */
  protected void setMac ( String newVar ) {
    mac = newVar;
  }

  /**
   * Get the value of mac
   * @return the value of mac
   */
  protected String getMac ( ) {
    return mac;
  }

  //
  // Other methods
  //

  /**
   * @return       int
   */
  public int getUID(  )
  {
    return id;
  }


  /**
   * @return       int
   */
  public int getPriority(  )
  {
    return 0;
  }


  /**
   * @return       rice.p2p.commonapi.NodeHandle
   */
  public rice.p2p.commonapi.NodeHandle getSource(  )
  {
    return source;
  }


  /**
   * @return       rice.p2p.commonapi.Id
   */
  public rice.p2p.commonapi.Id getDestination(  )
  {
    return null;
  }


  /**
   * @return       boolean
   */
  public boolean isResponse(  )
  {
    return response;
  }


  /**
   */
  public void setResponse(  )
  {
    response = true;
  }


  /**
   * @return       String
   */
  protected String calculateMac(  )
  {
    return "";
  }
  
  public void serialize(OutputBuffer buf) throws IOException {
    //rice.p2p.util.rawserialization.JavaSerializer.serialize(this, buf);
    throw new IllegalStateException();
  } 


}
