/************************************************************************
  			unito/p2p/coin/messaging/CashFlowMessage.java - Copyright fabio

Here you can write a license for your code, some comments or any other
information you want to have in your generated code. To to this simply
configure the "headings" directory in uml to point to a directory
where you have your heading files.

or you can just replace the contents of this file with your own.
If you want to do this, this file is located at

/opt/kde/share/apps/umbrello/headings/heading.java

-->Code Generators searches for heading files based on the file extension
   i.e. it will look for a file name ending in ".h" to include in C++ header
   files, and for a file name ending in ".java" to include in all generated
   java code.
   If you name the file "heading.<extension>", Code Generator will always
   choose this file even if there are other files with the same extension in the
   directory. If you name the file something else, it must be the only one with that
   extension in the directory to guarantee that Code Generator will choose it.

you can use variables in your heading files which are replaced at generation
time. possible variables are : author, date, time, filename and filepath.
just write %variable_name%

This file was generated on %date% at %time%
The original location of this file is /home/fabio/uml-generated-code/unito/p2p/coin/messaging/CashFlowMessage.java
**************************************************************************/

package unito.p2p.coin.messaging;
import java.io.IOException;

import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.util.rawserialization.JavaSerializer;



/**
 * Class CashFlowMessage
 */
public class CashFlowMessage extends CoinMessage {

  public static final short TYPE = 2;
  
  public static final long serialVersionUID = CoinMessage.SERIAL_BASE + (long) TYPE;

  private int amount;
  
  
  public CashFlowMessage(int id, NodeHandle source, NodeHandle dest, int amount) { 
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

  /**
   * Set the value of amount
   * @param newVar the new value of amount
   */
  private void setAmount ( int newAmount ) {
    amount = newAmount;
  }

  /**
   * Get the value of amount
   * @return the value of amount
   */
  public int getAmount ( ) {
    return amount;
  }
  
  public void setResponse() {
    super.setResponse();
  }

  

  /**
   * Get the value of dest
   * @return the value of dest
   */
  public NodeHandle getDirectDest() {
    return super.getDirectDest();
  }
  
  
  public String toString() {
    return "[CashFlowMessage from " + super.getSource() + " to " + getDirectDest() + " of " + Integer.toString(amount) + "]";
  }
  
  
  /**
   * Serialize the given message
   *
   * @param buf DESCRIBE THE PARAMETER
   * @exception IOException DESCRIBE THE EXCEPTION
   */
  public void serialize(OutputBuffer buf) throws IOException {   
    JavaSerializer.serialize(this, buf);
  }




}
