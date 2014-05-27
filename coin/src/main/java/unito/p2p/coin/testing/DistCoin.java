package unito.p2p.coin.testing;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.Iterator;
import java.util.Vector;

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
import unito.p2p.coin.messaging.FundingMessage;

/**
 * Tests COIN
 * 
 * @author Fabio Varesano
 */
public class DistCoin {

  /**
   * This constructor sets up a PastryNode. It will start a new ring.
   * 
   * @param bindport the local port to bind to 
   */
  public DistCoin(int bindport, Environment env) throws Exception {
    
    // Generate the NodeIds Randomly
    NodeIdFactory nidFactory = new RandomNodeIdFactory(env);
    
    // construct the PastryNodeFactory, this is how we use rice.pastry.socket
    PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);
    
    // construct a node, passing the null boothandle on the first loop will cause the node to start its own ring
    PastryNode node = factory.newNode(null);
      
    // the node may require sending several messages to fully boot into the ring
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
    Coin app = new CoinApp(node, "myinstance");
    
    // wait 10 seconds
    Thread.sleep(10000);
    
  }
  
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
  public DistCoin(int bindport, InetSocketAddress bootaddress, Environment env) throws Exception {
    
    // Generate the NodeIds Randomly
    NodeIdFactory nidFactory = new RandomNodeIdFactory(env);
    
    // construct the PastryNodeFactory, this is how we use rice.pastry.socket
    PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);

    // This will return null if we there is no node at that location
    NodeHandle bootHandle = ((SocketPastryNodeFactory) factory).getNodeHandle(bootaddress);

    // construct a node, passing the null boothandle on the first loop will cause the node to start its own ring
    PastryNode node = factory.newNode(bootHandle);
      
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
    Coin app = new CoinApp(node, "myinstance");
    
    Logger logger = env.getLogManager().getLogger(getClass(), "myinstance");
    logger.level = Logger.ALL;
    
    // wait 10 seconds
    Thread.sleep(10000);
    
    // get the balance to initialize the account
    System.out.println("[main] getting balance to initialize the account");
    app.balanceRequest(node.getLocalNodeHandle(), new Continuation() {
      // will be called if success in the lookup
      public void receiveResult(Object result) {
     
        System.out.println("[main] completed balance request. result is: " + result);        
      }

      // will be called if failure in the lookup
      public void receiveException(Exception result) {
        System.out.println("[main] Error of balance request: "+result);      
      }
    });
    
    Thread.sleep(5000);
    
    FundingMessage fm = new FundingMessage(((CoinApp) app).getUID(), node.getLocalNodeHandle(), (Id) null, 30);
    
    // performing a funding operation
    app.funding(fm, new Continuation() {
      // will be called if success in the lookup
      public void receiveResult(Object result) {
     
        System.out.println("[main] completed funding request. result is: " + result);        
      }

      // will be called if failure in the lookup
      public void receiveException(Exception result) {
        System.out.println("[main] error of funding request: "+result);
      }
    });
    
    
    
    Random rand = new Random();
    
    // as long as we're not the first node
    if (bootHandle != null) {
            

      // wait 10 seconds
      Thread.sleep(10000);
      
      // send directly to my leafset
      LeafSet leafSet = node.getLeafSet();
      
      NodeHandle nh = null;
      
      int j = 0;
      // this is a typical loop to cover your leafset.  Note that if the leafset
      // overlaps, then duplicate nodes will be sent to twice
      for (int i=-leafSet.ccwSize(); i<=leafSet.cwSize(); i++) {
        if (i != 0 && j==0) { // don't send to self
          // select the item
          nh = leafSet.get(i);
          
          //System.out.println("distcoin: sending cash flow " + i);  
          // send the message directly to the node
          app.doCashFlow(nh, rand.nextInt(50), 
              new Continuation() {
            // will be called if success in the lookup
            public void receiveResult(Object result) {
           
              System.out.println("[main] Received a " + result);        
            }

            // will be called if failure in the lookup
            public void receiveException(Exception result) {
              System.out.println("[main] There was an error: " + result);      
            }
          });
          
          j++;
          
          //app.doCashFlow(nh, rand.nextInt(50));
          
          // wait a sec
          Thread.sleep(1000);
        }
      }
      
      
      Thread.sleep(5000);
      // now get this node balance
      app.balanceRequest(node.getLocalNodeHandle(), new Continuation() {
        // will be called if success in the lookup
        public void receiveResult(Object result) {
       
          System.out.println("[main] completed balance request for source. result is: " + result);        
        }

        // will be called if failure in the lookup
        public void receiveException(Exception result) {
          System.out.println("[main] Error of balance request: "+result);      
        }
      });
      
      Thread.sleep(5000);
      // get dest balance
      app.balanceRequest(nh, new Continuation() {
        // will be called if success in the lookup
        public void receiveResult(Object result) {
       
          System.out.println("[main] completed balance request for dest. result is: " + result);        
        }

        // will be called if failure in the lookup
        public void receiveException(Exception result) {
          System.out.println("[main] Error of balance request for dest: "+result);      
        }
      });
    }
  }
  
  
  /**
   * This constructor launches numNodes PastryNodes.  They will bootstrap 
   * to an existing ring if one exists at the specified location, otherwise
   * it will start a new ring.
   * 
   * @param bindport the local port to bind to 
   * @param bootaddress the IP:port of the node to boot from
   * @param numNodes the number of nodes to create in this JVM
   * @param env the environment for these nodes
   */
  public DistCoin(int bindport, InetSocketAddress bootaddress, Environment env, int numNodes) throws Exception {
    
    Vector apps = new Vector();
    
    // Generate the NodeIds Randomly
    NodeIdFactory nidFactory = new RandomNodeIdFactory(env);
    
    // construct the PastryNodeFactory, this is how we use rice.pastry.socket
    PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);

    // loop to construct the nodes/apps
    for (int curNode = 0; curNode < numNodes; curNode++) {
      // This will return null if we there is no node at that location
      NodeHandle bootHandle = ((SocketPastryNodeFactory)factory).getNodeHandle(bootaddress);
  
      // construct a node, passing the null boothandle on the first loop will cause the node to start its own ring
      PastryNode node = factory.newNode(bootHandle);
        
      // the node may require sending several messages to fully boot into the ring
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
      
      System.out.println("[main] Finished creating new node "+node);
      
      // construct a new MyApp
      Coin app = new CoinApp(node, "myinstance");
      
      apps.add(app);
    }
  }

  /**
   * Usage:
   * create a new ring: java [-cp FreePastry-<version>.jar] unito.p2p.coin.testing.DistCoin new localbindport
   * join an existing ring: java [-cp FreePastry-<version>.jar] unito.p2p.coin.testing.DistCoin join localbindport bootIP bootPort
   */
  public static void main(String[] args) throws Exception {
    // Loads pastry settings
    Environment env = new Environment();

    try {
      // the action to do
      String action = args[0];
      
      // the port to use locally
      int bindport = Integer.parseInt(args[1]);
      
      if(action.equals("new")) { // we are creating a new ring
        DistCoin dc = new DistCoin(bindport, env);
        
      }
      else if(action.equals("join")) { // we are joining an existing ring
        // build the bootaddress from the command line args
        InetAddress bootaddr = InetAddress.getByName(args[2]);
        int bootport = Integer.parseInt(args[3]);
        InetSocketAddress bootaddress = new InetSocketAddress(bootaddr,bootport);
        
        // launch our node!
        DistCoin dc = new DistCoin(bindport, bootaddress, env);
      }
      else if(action.equals("multi")) {
        // build the bootaddress from the command line args
        InetAddress bootaddr = InetAddress.getByName(args[2]);
        int bootport = Integer.parseInt(args[3]);
        InetSocketAddress bootaddress = new InetSocketAddress(bootaddr,bootport);
        
        // get the number of nodes to create
        int num = Integer.parseInt(args[4]);
        
        // launch our nodes!
        DistCoin dc = new DistCoin(bindport, bootaddress, env, num);
      }
      else {
        System.out.println("No such action!");
      }
      
      
  
      
    } catch (Exception e) {
      // remind user how to use
      System.out.println("Usage:"); 
      System.out.println("create a new ring: java [-cp FreePastry-<version>.jar] unito.p2p.coin.testing.DistCoin new localbindport");
      System.out.println("  example: java unito.p2p.coin.testing.DistCoin new 9001");
      System.out.println("join an existing ring: java [-cp FreePastry-<version>.jar] unito.p2p.coin.testing.DistCoin join localbindport bootIP bootPort");
      System.out.println("  example: java unito.p2p.coin.testing.DistCoin join 9001 pokey.cs.almamater.edu 9001");
      System.out.println("create a big ring: java [-cp FreePastry-<version>.jar] unito.p2p.coin.testing.DistCoin multi localbindport bootIP bootPort numOfNodes");
      System.out.println("  example: java unito.p2p.coin.testing.DistCoin multi 9001 pokey.cs.almamater.edu 9001 100");
      throw e; 
    }
  }
}

