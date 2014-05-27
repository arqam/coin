package unito.p2p.coin.testing;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.Iterator;
import java.util.Vector;
import java.util.Scanner;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.leafset.LeafSet;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;
import rice.Continuation;
import rice.environment.logging.Logger;

import unito.p2p.coin.CoinApp;
import unito.p2p.coin.Coin;
import unito.p2p.coin.messaging.WithDrawalMessage;
import unito.p2p.coin.messaging.FundingMessage;

/**
 * Tests COIN interactively
 * 
 * @author Fabio Varesano
 */
public class DistInteractiveCoin {

  private Coin app;
  private NodeIdFactory nidFactory;
  private PastryNodeFactory factory;
  private PastryNode node;
  
  /**
   * This constructor sets up a PastryNode.  It will bootstrap to an 
   * existing ring if it can find one at the specified location, otherwise
   * it will start a new ring.
   * 
   * @param bindport the local port to bind to 
   * @param bootaddress the IP:port of the node to boot from
   * @param env the environment for these nodes
   * @param action the action to be taken (good values are new and join)
   */
  public DistInteractiveCoin(int bindport, InetSocketAddress bootaddress, Environment env) throws Exception {
    
    // Generate the NodeIds Randomly
    nidFactory = new RandomNodeIdFactory(env);
    
    // construct the PastryNodeFactory, this is how we use rice.pastry.socket
    factory = new SocketPastryNodeFactory(nidFactory, bindport, env);

    // This will return null if we there is no node at that location
    NodeHandle bootHandle = ((SocketPastryNodeFactory) factory).getNodeHandle(bootaddress);

    // construct a node, passing the null boothandle on the first loop will cause the node to start its own ring
    node = factory.newNode(bootHandle);
      
    synchronized (node) {
      while (!node.isReady() && !node.joinFailed()) {
        // delay so we don't busy-wait
        node.wait(500);

        // abort if can't join
        if (node.joinFailed()) {
          throw new IOException("Could not join the FreePastry ring.  Reason:" + node.joinFailedReason());
        }
      }
    }
    
    System.out.println("[main] Finished creating new node " + node);
    
    // construct a new CoinApp
    app = new CoinApp(node, "myinstance");
    
    /*
    Logger logger = env.getLogManager().getLogger(getClass(), "myinstance");
    logger.level = Logger.ALL;
    */
    
    // wait 10 seconds
    System.out.print("[main] waiting 10 seconds");
    waiter(10);
    
  }
  
  private void waiter(int sec) throws Exception{
    while(sec >=0 ) {
      System.out.print(".");
      sec--;
      Thread.sleep(1000);
    }
    System.out.println();
  }
  
  
  private NodeHandle nodeHandleCreate(Scanner sc, String msg) throws Exception {
    System.out.println(msg);
    System.out.print("address: ");
    InetAddress destaddr = InetAddress.getByName(sc.next());
    System.out.print("port: ");
    int destport = sc.nextInt();
    InetSocketAddress bootaddress = new InetSocketAddress(destaddr, destport);
    
    return ((SocketPastryNodeFactory) factory).getNodeHandle(bootaddress);
  }
  
  
  
  private void cashFlow(Scanner sc) throws Exception {
    
    NodeHandle destHandle = nodeHandleCreate(sc, "Insert the destinationnode information for the cash flow:");
    
    System.out.println("[main] got node handle: " + destHandle);
    
    System.out.print("Insert the amount of money to send: ");
    int amount = sc.nextInt();
    
    // do the cash flow
    app.doCashFlow(destHandle, amount, new Continuation() {
      // will be called if success in the lookup
      public void receiveResult(Object result) {
     
        System.out.println("[main] completed cash flow. result is: " + result);        
      }

      // will be called if failure in the lookup
      public void receiveException(Exception result) {
        System.out.println("[main] error in cash flow: "+result);      
      }
    });
    
    waiter(5);
  }
  
  private void balanceRequest(NodeHandle nh) {
    app.balanceRequest(nh, new Continuation() {
      // will be called if success in the lookup
      public void receiveResult(Object result) {
     
        System.out.println("[main] completed balance request. result is: " + result);        
      }

      // will be called if failure in the lookup
      public void receiveException(Exception result) {
        System.out.println("[main] error in balance request: "+result);      
      }
    });
    
  }
  
  
  private void localBalanceRequest(Scanner sc) throws Exception {
    balanceRequest((NodeHandle) node.getLocalNodeHandle());
    waiter(5);
  }
  
  private void remoteBalanceRequest(Scanner sc) throws Exception {
    NodeHandle nh = nodeHandleCreate(sc, "Insert the node information for the funding request:");
    balanceRequest(nh);
    waiter(5);
  }
  
  
  private void withdrawal(Scanner sc) throws Exception {
    
    System.out.print("amount: ");
    int amount = sc.nextInt(); 
    
    app.withdrawal(amount, new Continuation() {
      // will be called if success in the lookup
      public void receiveResult(Object result) {
     
        System.out.println("[main] completed withdrawal request. result is: " + result);        
      }

      // will be called if failure in the lookup
      public void receiveException(Exception result) {
        System.out.println("[main] error in withdrawal request: "+result);      
      }
    });
    waiter(5);
  }
  
  
private void funding(Scanner sc) throws Exception {
    
    System.out.print("amount: ");
    int amount = sc.nextInt();
    FundingMessage fm = new FundingMessage(((CoinApp) app).getUID(), node.getLocalNodeHandle(), (Id) null, amount);
    
    app.funding(fm, new Continuation() {
      // will be called if success in the lookup
      public void receiveResult(Object result) {
     
        System.out.println("[main] completed funding request. result is: " + result);        
      }

      // will be called if failure in the lookup
      public void receiveException(Exception result) {
        System.out.println("[main] error in funding request: "+result);      
      }
    });
    waiter(5);
  }
  

  
  
  private boolean doMenu(Scanner sc) throws Exception {
    System.out.println("Select your action:");
    System.out.println("1 - cash flow");
    System.out.println("2 - balance request local");
    System.out.println("3 - balance request remote");
    System.out.println("4 - funding");
    System.out.println("5 - withdrawal");
    
    try {
      System.out.println("10 - exit");
      int action = sc.nextInt();
      if(action != 10) {
      
        if(action == 1) {
          cashFlow(sc);
        }
        else if (action == 2) {
          localBalanceRequest(sc);
        }
        else if (action == 3) {
          remoteBalanceRequest(sc);
        }
        else if (action == 4) {
          funding(sc);
        }
        else if (action == 5) {
          withdrawal(sc);
        }
        
        
        return true;
      }
      else {
        return false;
      }
    }
    catch(Exception e) {
      System.out.println("Exception: " + e);
      e.printStackTrace();
      return true;
    }
  }

  /**
   * 
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    // Loads pastry settings
    Environment env = new Environment();

    Scanner sc = new Scanner(System.in);
    
    int bindport = Integer.parseInt(args[0]);
    
    // get boot node address
    InetAddress bootaddr = InetAddress.getByName(args[1]);
    
    //get boot node port
    int bootport = Integer.parseInt(args[2]);
    
    InetSocketAddress bootaddress = new InetSocketAddress(bootaddr,bootport);
    
    System.out.println("[main] Initializing application...");
    
    DistInteractiveCoin dic = new DistInteractiveCoin(bindport, bootaddress, env);
    
    while(dic.doMenu(sc)) ;
    
    env.destroy();
  }
}

