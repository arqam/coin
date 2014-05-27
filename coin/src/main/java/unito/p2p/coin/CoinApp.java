
package unito.p2p.coin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.WeakHashMap;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Map.Entry;


import rice.Continuation.MultiContinuation;
import rice.Continuation.NamedContinuation;
import rice.Continuation.SimpleContinuation;
import rice.Continuation.StandardContinuation;
import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.CancellableTask;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.NodeHandleSet;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.commonapi.appsocket.AppSocket;
import rice.p2p.commonapi.appsocket.AppSocketReceiver;
import rice.p2p.util.MathUtils;
import rice.p2p.util.rawserialization.SimpleInputBuffer;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;
import rice.pastry.messaging.JavaSerializedDeserializer;

import rice.Continuation;

import unito.p2p.coin.messaging.*;

/**
 * Implementation of the Coin Interface 
 * 
 * @author Fabio Varesano
 */
public class CoinApp implements Coin, Application {
  
  /**
   * the number of milliseconds to wait before declaring a message lost
   */
  public final int MESSAGE_TIMEOUT;
  
  /**
   * The Endpoint represents the underlieing node.
   */
  protected Endpoint endpoint;
  
  /**
   * The unique ids used by the messages sent across the wire
   */
  private int id;
  
  /**
   * the hashtable of outstanding messages
   */
  private Hashtable<Integer, Continuation> outstanding;

  /**
   * the hashtable of outstanding timer tasks
   */
  private Hashtable<Integer, CancellableTask> timers;
  
  /**
   * The application environment
   */
  protected Environment environment;
  
  /**
   * A logger to use freepastry logging subsystem
   */
  protected Logger logger;
  
  /**
   * AppSocket -> ByteBuffer[] Used for receiving the objects.
   */
  WeakHashMap pendingSocketTransactions = new WeakHashMap();
  
  
  /**
   * Holds the accounts informations
   */
  private Hashtable<Id, CoinAccount> accounts;
  
  /**
   * the replication Factor for Coin
   */
  protected int replicationFactor = 10;
  
  /**
   * the percentage of successful replica cash flow updates in order to declare success
   */
  public final double SUCCESSFUL_CASHFLOW_THRESHOLD = 0.5;

  /**
   * the percentage of successful replica balance request responses in order to declare success
   */
  public final double SUCCESSFUL_BALANCE_THRESHOLD = 0.8;
  
  /**
   * Create a CoinApp instance over the node
   * 
   * @param node the underlieing node.
   * @param instance The unique instance name of this Coin
   */
  public CoinApp(Node node, String instance) {
    this.environment = node.getEnvironment();
    logger = environment.getLogManager().getLogger(getClass(), instance);
    
    MESSAGE_TIMEOUT = 30000;
    
    this.endpoint = node.buildEndpoint(this, instance);
    
    this.outstanding = new Hashtable<Integer, Continuation>();
    this.timers = new Hashtable<Integer, CancellableTask>();
    
    this.accounts = new Hashtable<Id, CoinAccount>();
    
    this.id = Integer.MIN_VALUE;
    
    // TODO: do not use java serialization :-)
    // this can make coin implementable using programming languages others than Java
    ((JavaSerializedDeserializer) endpoint.getDeserializer()).setAlwaysUseJavaSerialization(true);
    
    
    // prepare to accept sockets
    this.endpoint.accept(
        new AppSocketReceiver() {

          public void receiveSocket(AppSocket socket) {
            if (logger.level <= Logger.FINE) {
              logger.log("Received Socket from " + socket);
            }
            socket.register(true, false, 10000, this);
            endpoint.accept(this);
          }

          public void receiveSelectResult(AppSocket socket, boolean canRead,
                boolean canWrite) {
            if (logger.level <= Logger.FINER) {
              logger.log("Reading from " + socket);
            }
            try {
              ByteBuffer[] bb = (ByteBuffer[]) pendingSocketTransactions.get(socket);
              if (bb == null) {
                // this is a new message

                // read the size
                bb = new ByteBuffer[1];
                bb[0] = ByteBuffer.allocate(4);
                if (socket.read(bb, 0, 1) == -1) {
                  close(socket);
                  return;
                }

                // TODO: need to handle the condition where we don't read the whole size...
                byte[] sizeArr = bb[0].array();
                int size = MathUtils.byteArrayToInt(sizeArr);

                if (logger.level <= Logger.FINER) {
                  logger.log("Found object of size " + size + " from " + socket);
                }

                // allocate a buffer to store the object, save it in the pst
                bb[0] = ByteBuffer.allocate(size);
                pendingSocketTransactions.put(socket, bb);
              }

              // now we have a bb

              // read some bytes
              if (socket.read(bb, 0, 1) == -1) {
                close(socket);
              }

              // deserialize or reregister
              if (bb[0].remaining() == 0) {
                // make sure to clear things up so we can keep receiving
                pendingSocketTransactions.remove(socket);

                if (logger.level <= Logger.FINEST) {
                  logger.log("bb[0].limit() " + bb[0].limit() + " bb[0].remaining() " + bb[0].remaining() + " from " + socket);
                }

                // deserialize the object
                SimpleInputBuffer sib = new SimpleInputBuffer(bb[0].array());

                short type = sib.readShort();
                
                //System.out.println(endpoint.getDeserializer());

                // get the message
                CoinMessage result = (CoinMessage) endpoint.getDeserializer().deserialize(sib, type, (byte) 0, null);
                
                // System.out.println("Received from socket " + result);
                
                // deliver to the application
                deliver(null, result);

              }

              // there will be more data on the socket if we haven't received everything yet
              // need to register either way to be able to read from the sockets when they are closed remotely, could alternatively close early
              // cause we are currently only sending 1 message per socket, but it's better to just keep reading in case we one day reuse sockets
              socket.register(true, false, 10000, this);

              // recursive call to handle next object
              // cant do this becasue calling read when not ready throws an exception
              // receiveSelectResult(socket, canRead, canWrite);
            } catch (IOException ioe) {
              receiveException(socket, ioe);
            }
          }

          public void receiveException(AppSocket socket, Exception e) {
            if (logger.level <= Logger.WARNING) {
              logger.logException("Error receiving message", e);
            }
            close(socket);
          }

          public void close(AppSocket socket) {
            if (socket == null) {
              return;
            }
            //System.out.println("Closing "+socket);
            pendingSocketTransactions.remove(socket);
            socket.close();
          }

        });
    
    
    endpoint.register();
  }
  
  
  
  /**
   * Send a message using a socket
   *
   * @param handle the NodeHandle which will receive the socket
   * @param m the message to be sent
   * @param c the continuation which will receive the result
   */
  private void sendViaSocket(final NodeHandle handle, final CoinMessage m, final Continuation c) {
    if (c != null) {
      CancellableTask timer = endpoint.scheduleMessage(new CoinMessageLostMessage(m.getUID(), getLocalNodeHandle(), null, m, handle), MESSAGE_TIMEOUT);
      insertPending(m.getUID(), timer, c);
    }
    
    //logger.log("sending message " + m.getUID() + " " + m);

    // create a bb[] to be written
    SimpleOutputBuffer sob = new SimpleOutputBuffer();
    try {
      sob.writeInt(0);
      // place holder for size...
      sob.writeShort(m.getType());
      m.serialize(sob);
    } catch (IOException ioe) {
      if (c != null) {
        c.receiveException(ioe);
      }
    }

    // add the size back to the beginning...
    int size = sob.getWritten() - 4;
    // remove the size of the size :)
    if (logger.level <= Logger.FINER) {
      logger.log("Sending size of " + size + " to " + handle + " to send " + m);
    }
    byte[] bytes = sob.getBytes();
    MathUtils.intToByteArray(size, bytes, 0);

    // prepare the bytes for writing
    final ByteBuffer[] bb = new ByteBuffer[1];
    bb[0] = ByteBuffer.wrap(bytes, 0, sob.getWritten());
    // the whole thing

    if (logger.level <= Logger.FINE) {
      logger.log("Opening socket to " + handle + " to send " + m);
    }
    endpoint.connect(handle,
          new AppSocketReceiver() {

            public void receiveSocket(AppSocket socket) {
              if (logger.level <= Logger.FINER) {
                logger.log("Opened socket to " + handle + ":" + socket + " to send " + m);
              }
              socket.register(false, true, 10000, this);
            }

            public void receiveSelectResult(AppSocket socket, boolean canRead,
                  boolean canWrite) {
              if (logger.level <= Logger.FINEST) {
                logger.log("Writing to " + handle + ":" + socket + " to send " + m);
              }

              try {
//          ByteBuffer[] outs = new ByteBuffer[1];
//          ByteBuffer out = ByteBuffer.wrap(endpoint.getLocalNodeHandle().getId().toByteArray());
//          outs[0] = out;
//          socket.write(outs, 0, 1);

                socket.write(bb, 0, 1);
              } catch (IOException ioe) {
                if (c != null) {
                  c.receiveException(ioe);
                } else if (logger.level <= Logger.WARNING) {
                  logger.logException("Error sending " + m, ioe);
                }
                return;
                // don't continue to try to send
              }
              if (bb[0].remaining() > 0) {
                socket.register(false, true, 10000, this);
              } else {
                socket.close();
              }
            }

            public void receiveException(AppSocket socket, Exception e) {
              if (c != null) {
                c.receiveException(e);
              }
            }
          },
          10000);
  }

  
  
  
  /**
   * Sends a request message across the wire, and stores the appropriate
   * continuation.
   *
   * @param id The destination id
   * @param message The message to send.
   * @param command The command to run once a result is received
   */
  protected void sendRequest(Id id, CoinMessage message, Continuation command) {
    sendRequest(id, message, null, command);
  }

  /**
   * Sends a request message across the wire, and stores the appropriate
   * continuation.
   *
   * @param handle The node handle to send directly to
   * @param message The message to send.
   * @param command The command to run once a result is received
   */
  protected void sendRequest(NodeHandle handle, CoinMessage message, Continuation command) {
    sendRequest(null, message, handle, command);
  }

  /**
   * Sends a request message across the wire, and stores the appropriate
   * continuation. Sends the message using the provided handle as a hint.
   *
   * @param id The destination id
   * @param message The message to send.
   * @param command The command to run once a result is received
   * @param hint DESCRIBE THE PARAMETER
   */
  protected void sendRequest(Id id, CoinMessage message, NodeHandle hint, Continuation command) {
    if (logger.level <= Logger.FINER) {
      logger.log("Sending request message " + message + " {" + message.getUID() + "} to id " + id + " via " + hint);
    }
    
    //logger.log("sending message " + message.getUID() + " " + message);
    
    CancellableTask timer = endpoint.scheduleMessage(new CoinMessageLostMessage(message.getUID(), getLocalNodeHandle(), id, message, hint), MESSAGE_TIMEOUT);
    insertPending(message.getUID(), timer, command);
    endpoint.route(id, message, hint);
  }
  
  
  /**
   * Handles the response message from a request.
   *
   * @param message The message that arrived
   */
  private void handleResponse(CoinMessage message) {
    if (logger.level <= Logger.FINE) {
      logger.log("handling reponse message " + message + " from the request");
    }
    Continuation command = removePending(message.getUID());

    command.receiveResult("Response Result!");
  }
  
  
  /**
   * Loads the provided continuation into the pending table
   *
   * @param uid The id of the message
   * @param command The continuation to run
   * @param timer DESCRIBE THE PARAMETER
   */
  private void insertPending(int uid, CancellableTask timer, Continuation command) {
    if (logger.level <= Logger.FINER) {
      logger.log("Loading continuation " + uid + " into pending table");
    }
    timers.put(new Integer(uid), timer);
    outstanding.put(new Integer(uid), command);
  }
  
  
  
  
  /**
   * Removes and returns the provided continuation from the pending table
   *
   * @param uid The id of the message
   * @return The continuation to run
   */
  private Continuation removePending(int uid) {
    if (logger.level <= Logger.FINER) {
      logger.log("Removing and returning continuation " + uid + " from pending table");
    }
    CancellableTask timer = timers.remove(new Integer(uid));

    if (timer != null) {
      timer.cancel();
    }

    return outstanding.remove(new Integer(uid));
  }
  
  
  /**
   * get the nodeHandle of the local Past node
   *
   * @return the nodehandle
   */
  public NodeHandle getLocalNodeHandle() {
    return endpoint.getLocalNodeHandle();
  }


  
  /**
   * Start a CashFlow Interaction.
   * the endpoint node is going to send amount to nh node
   * 
   * @param nh the node wich will receive endpoint money
   * @param amount the amount of money to send
   * @param command Command to be performed when the result is received
   */
  public void doCashFlow(NodeHandle nh, int amount, Continuation command) {
    logger.log("sending CashFlow of " + amount + " to " + nh);    
    CoinMessage msg = new CashFlowMessage(getUID(), endpoint.getLocalNodeHandle(), nh, amount);
    
    // send the message using socket
    sendViaSocket(nh, msg, command);
  }
  
  
  /**
   * Implementation of Coin.balanceRequest();
   */
  public void balanceRequest(NodeHandle nh, Continuation command) {
    // get the account holders root for the node we are doing the balance request
    Id accountRoot = getAccountRoot(nh);
    
    logger.log("requesting balance to " + accountRoot + " for " + nh);
    
    BalanceRequestMessage brm = new BalanceRequestMessage(getUID(), nh, accountRoot, getLocalNodeHandle()); // is accountRoot useful??
  
    // sending the BalanceRequestMessage to the account holders root
    sendRequest(accountRoot, brm, command);
  }
  
  /**
   * Implementation of Coin.funding();
   */
  public void funding(FundingMessage fm, Continuation command) {
    
    NodeHandle nh = fm.getSource();
    
    // get the account holders root for the node we are doing the balance request
    Id accountRoot = getAccountRoot(nh);
    
    logger.log("funding " + nh + " with amount " + fm.getAmount() + " - propagating to " + accountRoot);
     
    // sending the FundingMessage to the account holders root
    sendRequest(accountRoot, fm, command);
  }
  
  
  /**
   * Implementation of Coin.withdrawal();
   */
  public void withdrawal(int amount, Continuation command) {
    //  get the account holders root for the node we are doing the balance request
    NodeHandle nh = getLocalNodeHandle();
    
    Id accountRoot = getAccountRoot(nh);
    
    logger.log("funding " + nh + " with amount " + amount + " - propagating to " + accountRoot);
     
    WithDrawalMessage wdm = new WithDrawalMessage(getUID(), nh, accountRoot, amount);
    
    // sending the FundingMessage to the account holders root
    sendRequest(accountRoot, wdm, command);    
  }
  
  
  /**
   * Called when we receive a message.
   */
  public void deliver(Id id, Message message) {
    
    final CoinMessage msg = (CoinMessage) message;
    
    if (logger.level <= Logger.INFO) {
      logger.log("Received message " + message + " with destination " + id);
    }
    
    if(msg instanceof CashFlowMessage) {
      final CashFlowMessage cfm = (CashFlowMessage) message;
      
      logger.log("received a " + cfm);
      
      // check if we are waiting for the received message
      if(timers.containsKey(new Integer(cfm.getUID()))){
        logger.log("received response for cash flow message " + cfm.getUID());
        
        sendCashFlowUpdate(cfm.getDirectDest(), cfm, null);
      }
      else {          
        /*
         * We received a CashFlowMessage: we are going to receive an amount of money.
         * We need to send back a CashFlowMessage as response and proceed with interaction
         */
        
        // TODO: sign the message here
        
        // send the response to the received CashFlow
        sendViaSocket(cfm.getSource(), cfm, new Continuation() {
          // will be called if success in the cash flow 
          public void receiveResult(Object result) {
            boolean status = ((Boolean) result).booleanValue();
            if(status)
              logger.log("Cash Flow completed!");        
          }

          // will be called if failure in the lookup
          public void receiveException(Exception result) {
            logger.log("send via socket: There was an error: " + result);      
          }
        });
                  
        // send CashFlowUpdateMessage to the account holders for the CashFlow dest
        sendCashFlowUpdate(cfm.getSource(), cfm, null);
      }
    }
    else if(msg instanceof CashFlowUpdateMessage) {
      final CashFlowUpdateMessage cfum = (CashFlowUpdateMessage) msg;
      
      if(cfum.isResponse()) {
        logger.log("cash flow update successful");
        
        Continuation comm = removePending(cfum.getUID());
        if(comm != null) {
          comm.receiveResult(new Boolean(true));
        }
        else {
          logger.log("received CashFlowUpdateMessage without associated continuation");
        }
      }
      else {
        logger.log("ricevuta richiesta di aggiornamento conto da propagare " + cfum);
        
        final Continuation command = new Continuation() {
          public void receiveResult(Object o) {
            
            Boolean [] arr = (Boolean []) o;
            
            logger.log("propagateCashFlowUpdate result: " + arr.length + " updates successful");
            
            String logged = "";
            for(int i=0; i<arr.length; i++) {
              logged += " " + arr[i];
            }
            logger.log(logged);
            
            // we received the cash flow confirm. now send this back to the involved nodes
            CashFlowConfirmMessage cfcm = new CashFlowConfirmMessage(cfum.getUID(), cfum.getCashFlowMessage());
            // send to the money receiver
            sendViaSocket(cfcm.getCashFlowMessage().getSource(), cfcm, null);
            // send to the money sender
            sendViaSocket(cfcm.getCashFlowMessage().getDirectDest(), cfcm, null);
            
            // send a response to the cash flow update
            cfum.setResponse();
            sendViaSocket(cfum.getSender(), cfum, null);
          }

          public void receiveException(Exception e) {
            
            logger.log("propagateCashFlowUpdate exception: " + e);
          }
        };
        
        propagateCashFlowUpdate(endpoint.getId(),
            new MessageBuilder() {
              public CoinMessage buildMessage() {
                CashFlowUpdateReplicationMessage propagated = new CashFlowUpdateReplicationMessage(getUID(), cfum, getLocalNodeHandle());
                return propagated;
              }
            },command, false);

      }
    }
    else if(msg instanceof CashFlowUpdateReplicationMessage) {
      CashFlowUpdateReplicationMessage cfurm = (CashFlowUpdateReplicationMessage) msg;
      
      if(cfurm.isResponse()) {
        logger.log("ricevuta conferma di aggiornamento conto ");
        
        Continuation command = removePending(cfurm.getUID());
        command.receiveResult(new Boolean(true));
      }
      else {
        logger.log("ricevuta richiesta di aggiornamento conto propagata ");
        
        cfurm.setResponse();
        
        // now update the account
        Id updated = cfurm.getCashFlowUpdateMessage().getUpdated();
        Id moneyDest = cfurm.getCashFlowMessage().getDirectDest().getId();
        if(updated.compareTo(moneyDest) == 0) { // need to add money to updated account
          addMoney(updated, cfurm.getCashFlowMessage().getAmount());
        }
        else {
          removeMoney(updated, cfurm.getCashFlowMessage().getAmount());
          // TODO: handle case when we remove more money that available on account
        }
        
        endpoint.route(null, cfurm, cfurm.getAccountRoot());
      }
    }
    else if(msg instanceof CashFlowConfirmMessage) {
      CashFlowConfirmMessage cfcm = (CashFlowConfirmMessage) msg;
      
      CashFlowMessage cfm = cfcm.getCashFlowMessage();

      Continuation command = removePending(cfm.getUID());
      if(command != null)
        command.receiveResult(new Boolean(true));
      else
        logger.log("CashFlow Successful");
    }
    else if(msg instanceof BalanceRequestMessage) {
      final BalanceRequestMessage brm = (BalanceRequestMessage) msg;

      if(brm.isResponse()) {
        Continuation command = removePending(brm.getUID());
        if(command != null)
          command.receiveResult(brm.getResponse());
        else
          logger.log("BalanceRequest Successful");
      }
      else {
        logger.log("Received " + brm + " for " + id + "\nPropagating request to the leafset");
        
        
        final Continuation command = new Continuation() {
          public void receiveResult(Object o) {
            
            Integer [] arr = (Integer []) o;
            
            logger.log("propagate BalanceRequest result: " + arr.length + " requests successful");
            
            String logged = "values: ";
            for(int i=0; i<arr.length; i++) {
              logged += " " + arr[i];
            }
            logger.log(logged);
            
            // return a value by majority!
            Integer value = majorityDecision(arr);
            
            // successful received balancerequest responses. now send them to the requester!
            brm.setResponse(value);
            sendViaSocket(brm.getRequester(), brm, null);
          }

          public void receiveException(Exception e) {
            
            logger.log("propagateBalanceRequest exception: " + e);
            e.printStackTrace();
          }
        };
        
        final CoinApp app = this;
        
        propagateBalanceRequest(endpoint.getId(),
            new MessageBuilder() {
              public CoinMessage buildMessage() {
                BalanceRequestPropagatedMessage propagated = new BalanceRequestPropagatedMessage(getUID(), brm, getLocalNodeHandle());
                return propagated;
              }
            },command, false);
      }
    }
    else if(msg instanceof BalanceRequestPropagatedMessage) {
      final BalanceRequestPropagatedMessage brpm = (BalanceRequestPropagatedMessage) msg;
      
      BalanceRequestMessage brm = brpm.getBalanceRequestMessage();
      
      if(brpm.isResponse()) {
        logger.log("ricevuto risposta di balance request propagation");
        
        Continuation command = removePending(brpm.getUID());
        logger.log("value: " + brpm.getValue());
        command.receiveResult(new Integer(brpm.getValue()));
      }
      else {
        logger.log("ricevuta richiesta di balance request propagation");
        
        // get the value associated to that NodeHandler
        int value = getBalance(brm.getQueried().getId());
        
        brpm.setResponse(value);
        
        endpoint.route(null, brpm, brpm.getAccountRoot());
      }
    }
    else if(msg instanceof FundingMessage) {
      final FundingMessage fm = (FundingMessage) msg;
      
      
      if(fm.isResponse()) {
        logger.log("received funding message response " + fm);
        
        Continuation command = removePending(fm.getUID());
        command.receiveResult(fm.getResponse());
      }
      else {
        logger.log("received funding message " + fm + ". propagating to the leafset.");
        
        
        final Continuation command = new Continuation() {
          public void receiveResult(Object o) {
            
            Boolean [] arr = (Boolean []) o;
            
            logger.log("propagate Funding Message result: " + arr.length + " propagations successful");
            
            String logged = "values: ";
            for(int i=0; i<arr.length; i++) {
              logged += " " + arr[i];
            }
            logger.log(logged);
            
            // return a value by majority!
            //Integer value = majorityDecision(arr);
            
            // successful received balancerequest responses. now send them to the requester!
            fm.setResponse(new Boolean(true));
            sendViaSocket(fm.getSource(), fm, null);
          }

          public void receiveException(Exception e) {
            
            logger.log("propagateBalanceRequest exception: " + e);
            e.printStackTrace();
          }
        };
        
        
        propagateMoneyChange(endpoint.getId(),
            new MessageBuilder() {
              public CoinMessage buildMessage() {
                 AddMoneyMessage propagated = new AddMoneyMessage(getUID(), getLocalNodeHandle(), fm.getSource().getId(), fm, fm.getAmount(), fm.getSource().getId());
                return propagated;
              }
            },command, false);
      }
    }
    else if(msg instanceof WithDrawalMessage) {
      final WithDrawalMessage wd = (WithDrawalMessage) msg;
      
      
      if(wd.isResponse()) {
        logger.log("received funding message response " + wd);
        
        Continuation command = removePending(wd.getUID());
        command.receiveResult(wd.getResponse());
      }
      else {
        logger.log("received funding message " + wd + ". propagating to the leafset.");
        
        
        final Continuation command = new Continuation() {
          public void receiveResult(Object o) {
            
            Boolean [] arr = (Boolean []) o;
            
            logger.log("propagate Funding Message result: " + arr.length + " propagations successful");
            
            String logged = "values: ";
            for(int i=0; i<arr.length; i++) {
              logged += " " + arr[i];
            }
            logger.log(logged);
                        
            // successful received balancerequest responses. now send them to the requester!
            wd.setResponse(new Boolean(true));
            sendViaSocket(wd.getSource(), wd, null);
          }

          public void receiveException(Exception e) {
            
            logger.log("propagateBalanceRequest exception: " + e);
            e.printStackTrace();
          }
        };
        
        
        propagateMoneyChange(endpoint.getId(),
            new MessageBuilder() {
              public CoinMessage buildMessage() {
                 RemoveMoneyMessage propagated = new RemoveMoneyMessage(getUID(), getLocalNodeHandle(), wd.getSource().getId(), wd, wd.getAmount(), wd.getSource().getId());
                return propagated;
              }
            },command, false);
      }
    }
    else if(msg instanceof AddMoneyMessage) {
      final AddMoneyMessage amm = (AddMoneyMessage) msg;
      if(amm.isResponse()) {
        Continuation command = removePending(amm.getUID());
        logger.log("value: " + amm.getResponse());
        command.receiveResult(amm.getResponse());
      }
      else {
        logger.log("received add money message request" + amm);
        
        logger.log("updating " + amm.getDestination() + " account: adding " + amm.getAmount());

        // adding the money
        addMoney(amm.getUpdated(), amm.getAmount());
        
        // sending the response back
        amm.setResponse(new Boolean(true));
        sendViaSocket(amm.getSource(), amm, null);
      }
    }
    else if(msg instanceof RemoveMoneyMessage) {
      final RemoveMoneyMessage rmm = (RemoveMoneyMessage) msg;
      
      
      if(rmm.isResponse()) {
        logger.log("received remove money message " + rmm);
        
        Continuation command = removePending(rmm.getUID());
        logger.log("value: " + rmm.getResponse());
        command.receiveResult(rmm.getResponse());
      }
      else {
        logger.log("received remove money message request" + rmm);
        
        logger.log("updating " + rmm.getDestination() + " account: adding " + rmm.getAmount());
        removeMoney(rmm.getUpdated(), rmm.getAmount());
        
        // sending the response back
        rmm.setResponse(new Boolean(true));
        sendViaSocket(rmm.getSource(), rmm, null);
      }
    }
    else if(msg instanceof CoinMessageLostMessage) {
      Continuation cont = removePending(msg.getUID());
      
      if(cont != null)
        cont.receiveException(new CoinException("Message " + msg.getUID() + " lost!"));
      else
        logger.log("Received a " + msg + " id " + msg.getUID() + " without associated Continuation");
    }
    else {
      if (logger.level <= Logger.SEVERE) {
        logger.log("ERROR - Received message " + msg + "of unknown type.");
      }
    }
    
  }
  
  
  
  /**
   * Internal method which propage the BalanceRequest to accountRoot leafset
   *
   * @param id DESCRIBE THE PARAMETER
   * @param builder The object which builds the messages
   * @param command The command to call once done
   * @param useSocket DESCRIBE THE PARAMETER
   */
  private void propagateMoneyChange(final Id id, final MessageBuilder builder, Continuation command, final boolean useSocket) {
    // first, we get all of the replicas for this id
    getHandles(id, replicationFactor + 1,
          new StandardContinuation(command) {
            public void receiveResult(Object o) {
              NodeHandleSet replicas = (NodeHandleSet) o;
              if (logger.level <= Logger.FINER) {
                logger.log("Received replicas " + replicas + " for id " + id);
              }

              // then we send inserts to each replica and wait for at least
              // threshold * num to return successfully
              MultiContinuation multi =
                new MultiContinuation(parent, replicas.size()) {
                  public boolean isDone() throws Exception {
                    int numSuccess = 0;
                    for (int i = 0; i < haveResult.length; i++) {
                      if ((haveResult[i])) { //&& (Boolean.TRUE.equals(result[i]))) {
                        numSuccess++;
                        //System.out.println(result[i]);
                      }
                    }

                    if (numSuccess >= (SUCCESSFUL_BALANCE_THRESHOLD * haveResult.length)) {
                      return true;
                    }

                    if (super.isDone()) {
                      for (int i = 0; i < result.length; i++) {
                        if (result[i] instanceof Exception) {
                          if (logger.level <= Logger.WARNING) {
                            logger.logException("result[" + i + "]:", (Exception) result[i]);
                          }
                        }
                      }

                      throw new CoinException("Had only " + numSuccess + " successful inserts out of " + result.length + " - aborting.");
                    }
                    return false;
                  }

                  public Object getResult() {
                    Boolean[] values = new Boolean[result.length];
                    for (int i = 0; i < values.length; i++) {
                      values[i] = (Boolean) result[i];
                    }

                    return values;
                  }
                };

              for (int i = 0; i < replicas.size(); i++) {
                NodeHandle handle = replicas.getHandle(i);
                CoinMessage m = builder.buildMessage();
                Continuation c = new NamedContinuation("InsertMessage to " + replicas.getHandle(i) + " for " + id, multi.getSubContinuation(i));
                if (useSocket) {
                  sendViaSocket(handle, m, c);
                } else {
                  sendRequest(handle, m, c);
                }
              }
            }
          });
  }
  
  
  
  /**
   * Internal method which propage the BalanceRequest to accountRoot leafset
   *
   * @param id DESCRIBE THE PARAMETER
   * @param builder The object which builds the messages
   * @param command The command to call once done
   * @param useSocket DESCRIBE THE PARAMETER
   */
  private void propagateBalanceRequest(final Id id, final MessageBuilder builder, Continuation command, final boolean useSocket) {
    // first, we get all of the replicas for this id
    getHandles(id, replicationFactor + 1,
          new StandardContinuation(command) {
            public void receiveResult(Object o) {
              NodeHandleSet replicas = (NodeHandleSet) o;
              if (logger.level <= Logger.FINER) {
                logger.log("Received replicas " + replicas + " for id " + id);
              }

              // then we send inserts to each replica and wait for at least
              // threshold * num to return successfully
              MultiContinuation multi =
                new MultiContinuation(parent, replicas.size()) {
                  public boolean isDone() throws Exception {
                    int numSuccess = 0;
                    for (int i = 0; i < haveResult.length; i++) {
                      if ((haveResult[i])) { //&& (Boolean.TRUE.equals(result[i]))) {
                        numSuccess++;
                        //System.out.println(result[i]);
                      }
                    }

                    if (numSuccess >= (SUCCESSFUL_BALANCE_THRESHOLD * haveResult.length)) {
                      return true;
                    }

                    if (super.isDone()) {
                      for (int i = 0; i < result.length; i++) {
                        if (result[i] instanceof Exception) {
                          if (logger.level <= Logger.WARNING) {
                            logger.logException("result[" + i + "]:", (Exception) result[i]);
                          }
                        }
                      }

                      throw new CoinException("Had only " + numSuccess + " successful inserts out of " + result.length + " - aborting.");
                    }
                    return false;
                  }

                  public Object getResult() {
                    Integer[] values = new Integer[result.length];
                    for (int i = 0; i < values.length; i++) {
                      values[i] = (Integer) result[i];
                    }

                    return values;
                  }
                };

              for (int i = 0; i < replicas.size(); i++) {
                NodeHandle handle = replicas.getHandle(i);
                CoinMessage m = builder.buildMessage();
                Continuation c = new NamedContinuation("InsertMessage to " + replicas.getHandle(i) + " for " + id, multi.getSubContinuation(i));
                if (useSocket) {
                  sendViaSocket(handle, m, c);
                } else {
                  sendRequest(handle, m, c);
                }
              }
            }
          });
  }
  
  
  
  /**
   * Internal method which propage the CashFlowUpdate to accountRoot leafset
   *
   * @param id DESCRIBE THE PARAMETER
   * @param builder The object which builds the messages
   * @param command The command to call once done
   * @param useSocket DESCRIBE THE PARAMETER
   */
  private void propagateCashFlowUpdate(final Id id, final MessageBuilder builder, Continuation command, final boolean useSocket) {
    // first, we get all of the replicas for this id
    getHandles(id, replicationFactor + 1,
          new StandardContinuation(command) {
            public void receiveResult(Object o) {
              NodeHandleSet replicas = (NodeHandleSet) o;
              if (logger.level <= Logger.FINER) {
                logger.log("Received replicas " + replicas + " for id " + id);
              }

              // then we send inserts to each replica and wait for at least
              // threshold * num to return successfully
              MultiContinuation multi =
                new MultiContinuation(parent, replicas.size()) {
                  public boolean isDone() throws Exception {
                    int numSuccess = 0;
                    for (int i = 0; i < haveResult.length; i++) {
                      if ((haveResult[i]) && (Boolean.TRUE.equals(result[i]))) {
                        numSuccess++;
                      }
                    }

                    if (numSuccess >= (SUCCESSFUL_CASHFLOW_THRESHOLD * haveResult.length)) {
                      return true;
                    }

                    if (super.isDone()) {
                      for (int i = 0; i < result.length; i++) {
                        if (result[i] instanceof Exception) {
                          if (logger.level <= Logger.WARNING) {
                            logger.logException("result[" + i + "]:", (Exception) result[i]);
                          }
                        }
                      }

                      throw new CoinException("Had only " + numSuccess + " successful inserts out of " + result.length + " - aborting.");
                    }
                    return false;
                  }

                  public Object getResult() {
                    Boolean[] b = new Boolean[result.length];
                    for (int i = 0; i < b.length; i++) {
                      b[i] = new Boolean((result[i] == null) || Boolean.TRUE.equals(result[i]));
                    }

                    return b;
                  }
                };

              for (int i = 0; i < replicas.size(); i++) {
                NodeHandle handle = replicas.getHandle(i);
                CoinMessage m = builder.buildMessage();
                Continuation c = new NamedContinuation("InsertMessage to " + replicas.getHandle(i) + " for " + id, multi.getSubContinuation(i));
                if (useSocket) {
                  sendViaSocket(handle, m, c);
                } else {
                  sendRequest(handle, m, c);
                }
              }
            }
          });
  }
  
  
  /**
   * Send a CashFlowUpdateMessage to the accountRoot of the given NodeHandle
   * @param nh the nodeHandle form where the accountRoot will be get
   * @param cfm the CashFlowMessage which will be used to generate the CashFlowUpdateMessage
   * @param command the continuation which will recieve the result to this call
   */
  private void sendCashFlowUpdate(NodeHandle nh, CashFlowMessage cfm, Continuation command) {

    
    //get the account holders root for the source node of the CashFlow
    Id accountRoot = getAccountRoot(nh);
    
    logger.log("now I have to contact " + accountRoot);
    
    if(command == null) {
      // sending the CashFlowUpdateMessage to the account holders root
      sendRequest(accountRoot, new CashFlowUpdateMessage(getUID(), cfm, getLocalNodeHandle(), nh.getId()), new Continuation() {
        // will be called if success in the lookup
        public void receiveResult(Object result) {
       
          logger.log("cash flow update result: " + result);        
        }
    
        // will be called if failure in the lookup
        public void receiveException(Exception result) {
          logger.log("send cash flow update error: " + result);      
        }
      });
    }
    else {
      // sending the CashFlowUpdateMessage to the account holders root
      sendRequest(accountRoot, new CashFlowUpdateMessage(getUID(), cfm, getLocalNodeHandle(), nh.getId()), command);
    }
  }
  
  
  /**
   * Get the Id of the account holder for the given NodeHandle
   * 
   * @param nh the node which will receive the money
   * @return the id of the account holders root
   */
  public Id getAccountRoot(NodeHandle nh) {
    
    // get the byte rappresentation of nh's Id
    byte[] nhIdBytes = nh.getId().toByteArray();

    MessageDigest md = null;
    try {
      md = MessageDigest.getInstance("SHA");
    } catch (NoSuchAlgorithmException e) {
      if (logger.level <= Logger.SEVERE) {
        logger.log("No SHA support!");
      }
      throw new RuntimeException("No SHA support!", e);
    }
    
    // calculate the account holder Id using an SHA ash on nh's Id
    md.update(nhIdBytes);
    byte[] digest = md.digest();
    
    // create an Id from the generated digest
    Id id = rice.pastry.Id.build(digest);

    return id;
  }
  
  
  
  /**
   * Internal method which returns the handles to an object. It first checks to
   * see if the handles can be determined locally, and if so, returns.
   * Otherwise, it sends a LookupHandles messsage out to find out the nodes.
   *
   * @param id The id to fetch the handles for
   * @param max The maximum number of handles to return
   * @param command The command to call with the result (NodeHandle[])
   */
  protected void getHandles(Id id, int max, Continuation command) {
    NodeHandleSet set = endpoint.replicaSet(id, max);

    //if (set.size() == max) {
      command.receiveResult(set);
    //}
      /*
    else {
      sendRequest(id, new LookupHandlesMessage(getUID(), id, max, getLocalNodeHandle(), id),
          new StandardContinuation(command) {
            public void receiveResult(Object o) {
              NodeHandleSet replicas = (NodeHandleSet) o;

              // check to make sure we've fetched the correct number of replicas
              if (endpoint.replicaSet(endpoint.getLocalNodeHandle().getId(), 10 + 1).size() > replicas.size()) {
                parent.receiveException(new CoinException("Only received " + replicas.size() + " replicas - cannot insert as we know about more nodes."));
              } else {
                parent.receiveResult(replicas);
              }
            }
          });
      } */
  }
  
  /**
   * Return the most frequent value available in arr
   * 
   * @param arr an array containing Integers
   * @return an Integer which is the most frequent value in arr
   */
  private Integer majorityDecision(Object [] arr) {
    Integer [] values = (Integer []) arr;
    Integer decision = new Integer(-2);
    
    HashMap map = new HashMap();
    for(int i=0; i<values.length; i++) {
      if(values[i] != null) {
        Integer counter = (Integer) map.get(values[i]);
        if(counter == null) {
          map.put(new Integer(values[i]), new Integer(1));
        }
        else {
          map.put(new Integer(values[i]), new Integer( counter.intValue() + 1));
        }
      }
    }
      
    
    
    if(map.size()==1) {
      return new Integer(values[0]);
    }
    else {
      Set coll = map.entrySet();
      
      /*Iterator iter = coll.iterator();
      Integer greatest = new Integer(-1);
      decision = null;
      while(iter.hasNext()) {
        Map.Entry entry = (Map.Entry) iter.next();
        Integer value = (Integer) entry.getValue();
        if(value.compareTo(greatest) > 0) {
          decision = entry.getKey();
        }
      }*/
      return decision;
    }
  }
  
  
  /**
   * Return the balance associated to a gived Node Id
   * @param id
   * @return
   */
  private int getBalance(Id id) {
    logger.log("getting account balance for id " + id);
    if(accounts.containsKey(id)) { // account information are stored: return them
      CoinAccount account = accounts.get(id);
      return account.getValue();
    }
    else { // account information are not stored: create a new account
      logger.log("account balance for id " + id + " not available: generating new account");
      CoinAccount account = new CoinAccount(0);
      accounts.put(id, account);
      return 0;
    }
  }
  
  /**
   * Add value to the balance associated to id
   * @param id 
   * @return
   */
  private int addMoney(Id id, int value) {
    logger.log("adding " + value + " to account id " + id);
    if(accounts.containsKey(id)) { // account information are stored: return them
      CoinAccount account = accounts.get(id);
      return account.add(value);
    }
    else { // account information are not stored: create a new account
      logger.log("account balance for id " + id + " not available: generating new account");
      CoinAccount account = new CoinAccount(value);
      accounts.put(id, account);
      return value;
    }
  }
  
  
  /**
   * Remove value to the balance associated to id
   * @param id 
   * @return
   */
  private int removeMoney(Id id, int value) {
    logger.log("remove " + value + " to account id " + id);
    if(accounts.containsKey(id)) { // account information are stored: return them
      CoinAccount account = accounts.get(id);
      return account.remove(value);
    }
    else { // account information are not stored: throws exception
      throw new IllegalArgumentException("Cannot remove money from an account I do not have in memory!");
      
    }
  }
  

  /**
   * Called when you hear about a new neighbor.
   * Don't worry about this method for now.
   */
  public void update(NodeHandle handle, boolean joined) {
    if(joined) {
      logger.log(handle + " joined the neightbourhood set");
    }
    else {
      logger.log(handle + " left the neightbourhood set");
    }
  }
  
  /**
   * Called a message travels along your path.
   * Don't worry about this method for now.
   */
  public boolean forward(RouteMessage message) {
    return true;
  }
  
  
  
  /**
   * Returns a new uid for a message
   *
   * @return A new id
   */
  public synchronized int getUID() {
    return id++;
  }
  
  public String toString() {
    return "CoinApp " + endpoint.getId();
  }
  
  
  
  /**
   * Class which builds a message
   *
   * @version $Id: pretty.settings 2305 2005-03-11 20:22:33Z jeffh $
   * @author jeffh
   */
  public interface MessageBuilder {
    /**
     * DESCRIBE THE METHOD
     *
     * @return DESCRIBE THE RETURN VALUE
     */
    public CoinMessage buildMessage();
  }
  

}






