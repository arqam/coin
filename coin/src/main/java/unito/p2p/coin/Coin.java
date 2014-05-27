/**
 * 
 */
package unito.p2p.coin;

import rice.Continuation;
import rice.p2p.commonapi.NodeHandle;

import unito.p2p.coin.messaging.FundingMessage;

/**
 * @(#) Coin.java This interface is exported by all instances of Coin. An
 * instance of Coin provides a distributed accounting service. 
 * 
 * @author Fabio Varesano
 *
 */
public interface Coin {
  
  /**
   * Send amount of money to the given NodeHandle and return the
   * result to the given continuation. The result of this method
   * will be an acknowledgement of the completed result. 
   * 
   * @param nh the node wich will receive endpoint money
   * @param amount the amount of money to send
   * @param command Command to be performed when the result is received
   */
  public void doCashFlow(NodeHandle nh, int amount, Continuation command);
  
  
  /**
   * Return the balance of the given NodeHandle to the Continuation
   *  
   * @param nh the NodeHandle to get its balance
   * @param command the continuation which will receive the result
   */
  public void balanceRequest(NodeHandle nh, Continuation command);
  
  
  /**
   * Add money to a node account. This operation is controlled by a central
   * authority which converts the amount of money into system virtual money.
   * The given Fundingessage will be signed by the central authority.
   * 
   * @param fm the authority signed FundingMessage
   * @param command the continuation which will receive the result
   */
  public void funding(FundingMessage fm, Continuation command);
  
  
  /**
   * Used to trasform back system virtual money into real money.
   * A withdrawal message is sent to the account root then routed to
   * all the account holders which update the system account.
   * This will return back to the node who posses the account a list of
   * signed acknowledgment that he will then submit to the central authority
   * which then will add amount to the real account of the node
   * 
   * @param amount the amount of money to convert back
   * @param command the Continuation which will recieve the result
   */
  public void withdrawal(int amount, Continuation command);

}
