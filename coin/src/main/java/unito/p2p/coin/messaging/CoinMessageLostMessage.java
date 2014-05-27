package unito.p2p.coin.messaging;

import java.io.IOException;

import rice.*;
import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.p2p.commonapi.*;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.util.rawserialization.JavaSerializer;
import unito.p2p.coin.*;


/**
 * @(#) MessageLostMessage.java This class represents a reminder to Past that an
 * outstanding message exists, and that the waiting continuation should be
 * informed if the message is lost.
 */
public class CoinMessageLostMessage extends CoinMessage {

  /**
   * the id the message was sent to
   */
  protected Id id;

  /**
   * the hint the message was sent to
   */
  protected NodeHandle hint;

  /**
   * the message
   */
  protected Message message;
  
  public static final short TYPE = 1;

  public static final long serialVersionUID = CoinMessage.SERIAL_BASE + (long) TYPE;

  /**
   * Constructor which takes a unique integer Id and the local id
   *
   * @param uid The unique id
   * @param local The local nodehandle
   * @param id DESCRIBE THE PARAMETER
   * @param message DESCRIBE THE PARAMETER
   * @param hint DESCRIBE THE PARAMETER
   */
  public CoinMessageLostMessage(int uid, NodeHandle local, Id id, Message message, NodeHandle hint) {
    super(uid, local, local.getId());

    setResponse();
    this.hint = hint;
    this.message = message;
    this.id = id;
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
   * Method by which this message is supposed to return it's response - in this
   * case, it lets the continuation know that a the message was lost via the
   * receiveException method.
   *
   * @param c The continuation to return the reponse to.
   * @param env DESCRIBE THE PARAMETER
   * @param instance DESCRIBE THE PARAMETER
   */
  public void returnResponse(Continuation c, Environment env, String instance) {
    Logger logger = env.getLogManager().getLogger(getClass(), instance);
    Exception e = new CoinException("Outgoing message '" + message + "' to " + id + "/" + hint + " was lost - please try again.");
    if (logger.level <= Logger.WARNING) {
      logger.logException("ERROR: Outgoing PAST message " + message + " with UID " + getUID() + " was lost", e);
    }
    c.receiveException(e);
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
   * Returns a string representation of this message
   *
   * @return A string representing this message
   */
  public String toString() {
    return "[CoinMessageLostMessage]";
  }
}
