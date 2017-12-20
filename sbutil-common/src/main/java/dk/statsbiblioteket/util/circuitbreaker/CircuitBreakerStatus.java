package dk.statsbiblioteket.util.circuitbreaker;

import java.util.Date;


public class CircuitBreakerStatus {

  private String name;
  private String state;
  private int maxConcurrent;
  private int maxFailures;
  private int cooldownTime;
  private int currentConcurrent;
  private long currentSucceeded;
  private long currentFailed;
  private long currentRejected;
  private long lastChangeToClosed;
  private long lastChangeToOpen;
  private long lastChangeToHalfOpen;
  private long totalSucceeded;
  private long totalFailed;
  private long totalRejected;

  protected CircuitBreakerStatus() {
  }
  
  /**
   * 
   * @return the name of the circuit breaker
   */
  public String getName() {
    return name;
  }
  
  protected void setName(String name) {
    this.name = name;
  }
  
  /**
   * 
   * @return the state of the circuit breaker, either CLOSED, OPEN or HALF-OPEN
   */
  public String getState() {
    return state;
  }
  
  protected void setState(String state) {
    this.state = state;
  }
  
  /**
   * 
   * @return configuration value for maximum concurrent attempts
   */
  public int getMaxConcurrent() {
    return maxConcurrent;
  }
  
  protected void setMaxConcurrent(int maxConcurrent) {
    this.maxConcurrent = maxConcurrent;
  }
  
  /**
   * 
   * @return configuration value for maximum failures in a row
   */
  public int getMaxFailures() {
    return maxFailures;
  }
  
  protected void setMaxFailures(int maxFailures) {
    this.maxFailures = maxFailures;
  }
  
  /**
   * 
   * @return configuration value for cooldown time in milliseconds
   */
  public int getCooldownTime() {
    return cooldownTime;
  }
  
  protected void setCooldownTime(int cooldownTime) {
    this.cooldownTime = cooldownTime;
  }
  
  /**
   * 
   * @return current count of concurrent attempts
   */
  public int getCurrentConcurrent() {
    return currentConcurrent;
  }
  
  protected void setCurrentConcurrent(int currentConcurrent) {
    this.currentConcurrent = currentConcurrent;
  }
  
  /**
   * 
   * @return current count of succeeded attempts since last state change
   */
  public long getCurrentSucceeded() {
    return currentSucceeded;
  }
  
  public void setCurrentSucceeded(long currentSucceeded) {
    this.currentSucceeded = currentSucceeded;
  }
  
  /**
   * 
   * @return current count of failed attempts since last state change
   */
  public long getCurrentFailed() {
    return currentFailed;
  }
  
  public void setCurrentFailed(long currentFailed) {
    this.currentFailed = currentFailed;
  }
  
  /**
   * 
   * @return current count of rejected attempts since last state change
   */
  public long getCurrentRejected() {
    return currentRejected;
  }

  
  public void setCurrentRejected(long currentRejected) {
    this.currentRejected = currentRejected;
  }

  /**
   * 
   * @return last time the circuit breaker were changed to closed state, or 0 if it has never happened
   */
  public long getLastChangeToClosed() {
    return lastChangeToClosed;
  }
  
  protected void setLastChangeToClosed(long lastChangeToClosed) {
    this.lastChangeToClosed = lastChangeToClosed;
  }
  
  /**
   * 
   * @return last time the circuit breaker were changed to open state, or 0 if it has never happened
   */
  public long getLastChangeToOpen() {
    return lastChangeToOpen;
  }
  
  protected void setLastChangeToOpen(long lastChangeToOpen) {
    this.lastChangeToOpen = lastChangeToOpen;
  }
  
  /**
   * 
   * @return last time the circuit breaker were changed to half-open state, or 0 if it has never happened
   */
  public long getLastChangeToHalfOpen() {
    return lastChangeToHalfOpen;
  }
  
  protected void setLastChangeToHalfOpen(long lastChangeToHalfOpen) {
    this.lastChangeToHalfOpen = lastChangeToHalfOpen;
  }
  
  /**
   * 
   * @return total number of succeeded attempts, in the entire lifetime of this circuit breaker
   */
  public long getTotalSucceeded() {
    return totalSucceeded;
  }
  
  protected void setTotalSucceeded(long totalSucceeded) {
    this.totalSucceeded = totalSucceeded;
  }
  
  /**
   * 
   * @return total number of failed attempts, in the entire lifetime of this circuit breaker
   */
  public long getTotalFailed() {
    return totalFailed;
  }
  
  protected void setTotalFailed(long totalFailed) {
    this.totalFailed = totalFailed;
  }
  
  /**
   * 
   * @return total number of rejected attempts, in the entire lifetime of this circuit breaker
   */
  public long getTotalRejected() {
    return totalRejected;
  }
  
  protected void setTotalRejected(long totalRejected) {
    this.totalRejected = totalRejected;
  }

  /**
   * 
   * @return a nice textual version of the status, suitable for logging
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Name: ").append(name).append("\n");
    sb.append("Config:\n");
    sb.append("  Concurrent: "+maxConcurrent+"\n");
    sb.append("  Failures:   "+maxFailures+"\n");
    sb.append("  Cooldown:   "+cooldownTime+" ms\n");
    if ( getState().equals("CLOSED") ) {
      sb.append("Current: (Since "+(lastChangeToClosed>0?new Date(lastChangeToClosed).toString():"Restart")+")\n");
      sb.append("  State:      "+state+"\n");
      sb.append("  Concurrent: "+currentConcurrent+"\n");
      sb.append("  Counters:\n");
      long total = currentFailed+currentSucceeded;
      sb.append("    Succeeded:"+currentSucceeded+" ("+(100.0*currentSucceeded/total)+" %)\n");
      sb.append("    Failed:   "+currentFailed+" ("+(100.0*currentFailed/total)+" %)\n");      
      sb.append("    Total:    "+total+" (100 %)\n");
    } else {
      sb.append("Current: (Since "+(lastChangeToOpen>0?new Date(lastChangeToOpen).toString():"Restart")+")\n");
      sb.append("  State:      "+state+"\n");
      sb.append("  Concurrent: "+currentConcurrent+"\n");
      sb.append("  Rejected:   "+currentRejected+"\n");      
    }
    sb.append("Last change to:\n");
    if ( lastChangeToClosed > 0 ) {
      sb.append("  Closed:     "+new Date(lastChangeToClosed)+"\n");
    } else {
      sb.append("  Closed:     Never\n");
    }
    if ( lastChangeToOpen > 0 ) {
      sb.append("  Open:       "+new Date(lastChangeToOpen)+"\n");
    } else {
      sb.append("  Open:       Never\n");
    }
    if ( lastChangeToHalfOpen > 0 ) {
      sb.append("  Half-Open:  "+new Date(lastChangeToHalfOpen)+"\n");
    } else {
      sb.append("  Half-Open:  Never\n");
    }
    sb.append("Totals:\n");
    long total = totalFailed+totalSucceeded+totalRejected;
    sb.append("  Succeeded:  "+totalSucceeded+" ("+(100.0*totalSucceeded/total)+" %)\n");
    sb.append("  Failed:     "+totalFailed+" ("+(100.0*totalFailed/total)+" %)\n");
    sb.append("  Rejected:   "+totalRejected+" ("+(100.0*totalRejected/total)+" %)\n");
    sb.append("  Total:      "+total+" (100 %)\n");
    return sb.toString();
  }
  
}
