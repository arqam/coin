package unito.p2p.coin;


/**
 * Class which implement an account
 * 
 * @author Fabio Varesano
 */
class CoinAccount {
 
  private int value;
  
  
  public CoinAccount(int value) {
    if(value < 0)
      throw new IllegalArgumentException("Cannot create a CoinAccount with value < 0");
    this.value = value;
  }
  
  /**
   * Get the amount of money of this account
   * @return
   */
  public int getValue() {
    return value;
  }
  
  /**
   * Add money to the account
   * 
   * @param added the amount of money to add
   * @return the new balance
   */
  public int add(int added) {
    value += added;
    return value;
  }
  
  /**
   * Remove money from the account
   * 
   * @param removed the amount of money to add
   * @return the new balance if successful, -1 if not enought money
   */
  public int remove(int removed) {
    //if(removed <= value) {
      this.value -= removed;
      return this.value;
    /*}
    else {
      return -1;
    }*/
  }
  
}



