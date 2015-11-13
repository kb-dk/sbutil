package dk.statsbiblioteket.util.circuitbreaker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The circuit breaker has three states, Closed, Open and Half-Open
 * The state names can be counterintuitive. OPEN='BROKEN', CLOSED='WORKING' (like a fuse)
 *  
 * In the closed state, everything is let through, and the circuit breaker will only monitor
 * the number of concurrent attempts, as well as the number of failures in a row.
 * 
 * If the number of concurrent or number of failures in a row exceeds the limits, the circuit 
 * breaker will switch state to open, similar to a burned out fuse.
 * 
 * In the open state, everything will be rejected by the circuit breaker, while it waits for 
 * the cooldown period to pass. After it has passed, the state will be changed to half-open.
 * 
 * In the half-open state, the circuit breaker is on it toes, letting attempts pass through like
 * the closed state, but just a single failure will return the state to open and a single success (which comes first)
 * will return the state to closed.
 *  
 * After the circuit breaker has opened, a cooldown period is introduced in order to
 * give the external system a chance to recover, as well as not spamming our own logfiles.
 * The cooldown timeout is specified in milliseconds
 * 
 * 
 * To create a new circuitbreaker use:
 * public CircuitBreaker(String name, int maxFailures, int maxConcurrent, int timeCooldown)
 *  
 * To get an existing circuitbreaker use:
 * public static CircuitBreaker<Object,Object> getInstance(String name)
 * 
 * Based loosly on Michael Nygards idea.
 * 
 * @author Thomas Egense 
 * @author Henrik Nielsen
 *
 *
 */
public class CircuitBreaker<IN, OUT> {
    
  /* **************************************************************************  
   * AVAILABLE STATES 
   ****************************************************************************/
  
  private static final State STATE_CLOSED = new ClosedState();
  private static final State STATE_OPEN = new OpenState();
  private static final State STATE_HALFOPEN = new HalfOpenState();

  /* **************************************************************************
   * CONFIGURATION
   ****************************************************************************/
  private String name;
  private int maxConcurrent;
  private int maxFailures;
  private int timeCooldown;

  /* **************************************************************************
   * CURRENT STATE AND COUNTS
   ****************************************************************************/
  private State state = STATE_CLOSED;
  private int concurrent;
  private int failures;

  private long lastChangeToClosed;
  private long lastChangeToOpen;
  private long lastChangeToHalfOpen;

  private long currentSucceeded;
  private long currentFailed;
  private long currentRejected;

  private long totalSucceeded;
  private long totalFailed;
  private long totalRejected;
  
  private static Log log = LogFactory.getLog(CircuitBreaker.class);

  private static Map<String, CircuitBreaker<Object, Object>> circuitBreakerMap = new HashMap<String, CircuitBreaker<Object, Object>>();

  /* **************************************************************************
   * CONSTRUCTION
   ****************************************************************************/
  /**
   * Will create a new Circuitbreaker with the given name. 
   * If trying to create another Circuitbreaker with the same name it will throw an error
   * 
   * @param name
   * @param maxFailures
   * @param maxConcurrent
   * @param timeCooldown
   */
  public CircuitBreaker(String name, int maxFailures, int maxConcurrent, int timeCooldown) {
    super();
  
    synchronized (circuitBreakerMap) {
      if (circuitBreakerMap.containsKey(name)) {
        throw new IllegalArgumentException("There already is circuitbreaker with name:" + name);
      }
    }
    
    this.name = name;
    this.maxFailures = maxFailures;
    this.maxConcurrent = maxConcurrent;
    this.timeCooldown = timeCooldown;


    circuitBreakerMap.put(name, (CircuitBreaker<Object, Object>) this);
  }
     
  

  /**
   * Method for getting a circuit breaker based on a name.
   *  
   * @param name Name of the circuit breaker to be created.
   * @return Circuit breaker configured and ready for use
   */
  public static CircuitBreaker<Object, Object> getInstance(String name) {
    synchronized (circuitBreakerMap) {
      if (!circuitBreakerMap.containsKey(name)) {
        throw new IllegalArgumentException("There is no circuitbreaker with name:" + name);
      }
      return  circuitBreakerMap.get(name);
    }
  }

  /**   
   * 
   * @return list of status for all registered circuitbreakers.
   */
  public static  List<CircuitBreakerStatus> getCircuitBreakersStatus() {  
      ArrayList<CircuitBreakerStatus>   circuitBreakerList = new ArrayList<CircuitBreakerStatus> ();

      for (String name : circuitBreakerMap.keySet()) {
          circuitBreakerList.add(circuitBreakerMap.get(name).getStatus());
      }
      return circuitBreakerList;
  }

  
  
  /* **************************************************************************
   * PUBLIC METHODS
   ****************************************************************************/

  /**
   * This method will attempt to invoke the specified task, and if failed, will return an
   * exception signaling what went wrong. 
   * 
   * @param task The task to invoke
   * @throws CircuitBreakerException if the task threw an exception. The original exception will be wrapped inside
   * @throws CircuitBreakerOpenException if the circuit breaker is open.
   */
  public OUT attemptTask(CircuitBreakerTask<IN, OUT> task, IN input) throws CircuitBreakerOpenException, CircuitBreakerException {
    State state = getState();
    try {
        state.preInvoke(this);
        OUT out = task.invoke(input);  
        state.onSucces(this);
        return out;
        
    } catch (CircuitBreakerOpenException t) { // Not catching Errors
      state.onError(this, t);
      throw t;   
    } catch (Exception t) { // Not catching Errors
      state.onError(this, t);
      throw new CircuitBreakerException(t);
    }
  }
  /**
   * Same as:
   * public void attemptTask(CircuitBreakerTask<Object,Object> task, Object input)
   * Just no input object
   */
  public void attemptTask(CircuitBreakerTask<IN, OUT> task) throws CircuitBreakerOpenException, CircuitBreakerException {
    State state = getState();
    try {
        state.preInvoke(this);
        task.invoke(null); //This input is not used  
        state.onSucces(this);
    } catch (CircuitBreakerOpenException t) { // Not catching Errors
      state.onError(this, t);
      throw t;   
    } catch (Exception t) { // Not catching Errors
      state.onError(this, t);
      throw new CircuitBreakerException(t);
    }
  }
 
  public CircuitBreakerStatus getStatus() {
    CircuitBreakerStatus status = new CircuitBreakerStatus();
    status.setCooldownTime(timeCooldown);
    status.setCurrentConcurrent(getConcurrent());
    status.setCurrentFailed(currentFailed);
    status.setCurrentRejected(currentRejected);
    status.setCurrentSucceeded(currentSucceeded);
    status.setLastChangeToClosed(lastChangeToClosed);
    status.setLastChangeToHalfOpen(lastChangeToHalfOpen);
    status.setLastChangeToOpen(lastChangeToOpen);
    status.setMaxConcurrent(maxConcurrent);
    status.setMaxFailures(maxFailures);
    status.setName(name);
    status.setState(getState().toString());
    status.setTotalFailed(totalFailed);
    status.setTotalRejected(totalRejected);
    status.setTotalSucceeded(totalSucceeded);
    
    return status;
  }
  
  public static void logAllCircuitBreakerStatus() {
      log.info("--------- Circuitbreakers info start----------------");
      for (String name : circuitBreakerMap.keySet()) {
             log.info(circuitBreakerMap.get(name).toString());
     //  System.out.println(circuitBreakerMap.get(name).getStatus().toString());  
      }
      log.info("--------- Circuitbreakers end start----------------");            
  }
  

  /* **************************************************************************
   * STATE
   ****************************************************************************/
  /**
   * Method to determine if an attempt has a change to succeed or is guaranteed to fail.
   * 
   * @return true if the circuit breaker thinks the attempt might succeed, false if the ciruit breaker thinks the attempt is bound to fail.
   */
  public synchronized boolean isAvailable() {
    return isClosedState();
  }

  /**
   * Method used by unit tests to determing the circuit breakers state
   * @return
   */
  protected synchronized boolean isClosedState() {
    return state == STATE_CLOSED;
  }

  /**
   * Method used by unit tests to determing the circuit breakers state
   * @return
   */
  protected synchronized boolean isOpenState() {
    return state == STATE_OPEN;
  }

  /**
   * Method used by unit tests to determing the circuit breakers state
   * @return
   */
  protected synchronized boolean isHalfOpenState() {
    return state == STATE_HALFOPEN;
  }

  /**
   * Returns the circuit breakers current state.
   * @return current state
   */
  private synchronized State getState() {
    return state;
  }

  /**
   * Will change the current state to closed, make a log statement and note the current time.
   */
  private synchronized void setStateClosed() {
    resetStatsSucceeded();
    resetStatsFailed();
    log.warn("Changing state on " + name + " to Closed/Sluttet - " + currentRejected + " rejected attempts during open state");
    state = STATE_CLOSED;
    lastChangeToClosed = System.currentTimeMillis();
  }

  /**
   * Will change the current state to open, make a log statement and note the current time.
   * Will also reset the counter of calls made during the open state if the state is changed from closed.
   */
  private synchronized void setStateOpen() {
    if (state == STATE_CLOSED) {
      resetStatsRejected();
      log.warn("Changing state on " + name + " to Open/Brudt - " + getConcurrent() + " concurrent and " + getFailures() + " failures");
    } else {
      log.warn("Extending state Open/Brudt for " + name + " - " + getConcurrent() + " concurrent and " + getFailures() + " failures");
    }
    state = STATE_OPEN;
    lastChangeToOpen = System.currentTimeMillis();
  }

  /**
   * Will change the current state to half-open, make a log statement and note the current time.
   */
  private synchronized void setStateHalfOpen() {
    log.warn("Changing state on " + name + " to HalfOpen");
    state = STATE_HALFOPEN;
    lastChangeToHalfOpen = System.currentTimeMillis();
  }
  
  
  /* **************************************************************************
   * CONCURRENT
   ****************************************************************************/
  /**
   * Gets the count of concurrent attempts
   * @return count of concurrent attempts
   */
  protected synchronized int getConcurrent() {
      return concurrent;
  }

  /**
   * Increase the count of concurrent attempts by one
   */
  private synchronized void incConcurrent() {
      concurrent++;
  }
  
  /**
   * Decrease the count of concurrent attempts by one
   */
  private synchronized void decConcurrent() {
      concurrent--;
  }

  /* **************************************************************************
   * FAILURES
   ****************************************************************************/
  /**
   * Gets the number of failures in a row
   * @return number of failures in a row
   */
  protected synchronized int getFailures() {
      return failures;
  }

  /**
   * Increase the count of failures in a row by one
   */
  private synchronized void incFailures() {
      failures++;
  }

  /**
   * Reset the count of failures in a row to zero
   */
  private synchronized void resetFailures() {
      failures = 0;
  }

  /* **************************************************************************
   * STATS COUNTERS
   ****************************************************************************/
  private synchronized void resetStatsSucceeded() {
    currentSucceeded = 0;
  }
  private synchronized void incStatsSucceeded() {
    currentSucceeded++;
    totalSucceeded++;
  }
  private synchronized void resetStatsFailed() {
    currentFailed = 0;
  }
  private synchronized void incStatsFailed() {
    currentFailed++;
    totalFailed++;
  }
  private synchronized void resetStatsRejected() {
    currentRejected = 0;
  }
  private synchronized void incStatsRejected() {
    currentRejected++;
    totalRejected++;
  }

  /* **************************************************************************
   * INNER CLASSES AND INTERFACES
   ****************************************************************************/
  /**
   * The central state class in circuit breaker. This is the interface that states will implement.
   */
  private static interface State {
    /**
     * This method should at least increment the concurrent counter and
     * determine if the attempt should be let through, or an exception should be thrown
     * 
     * @param circuitBreaker the circuit breaker to handle the attempt
     */
    void preInvoke(CircuitBreaker circuitBreaker);

    /**
     * This method should at least decrement the concurrent counter
     * 
     * @param circuitBreaker the circuit breaker to handle the attempt
     */
    void onSucces(CircuitBreaker circuitBreaker);

    /**
     * This method should at least decrement the concurrent counter
     * 
     * @param circuitBreaker the circuit breaker to handle the attempt
     */
    void onError(CircuitBreaker circuitBreaker, Throwable t);
  }

  /* **************************************************************************
   * STATE CLOSED (SLUTTET)
   ****************************************************************************/
  /**
   * Class representing the normal closed state.
   * In this state, everything is let through, and the circuit breaker will only
   * monitor the count of concurrent attempts, as well as the count of failures
   * in a row.
   */
  private static class ClosedState implements State {

    /**
     * Allow all tasks to be executed, as long as there are fewer than maxConcurrent
     * concurrent tasks, and fewer than maxFailures failures in a row.
     * If one of these limits are reached, the state for future attempts will be changed
     * to open (brudt), and this attempt will fail with an exception.
     */
    public void preInvoke(CircuitBreaker cb) {
      cb.incConcurrent();
      if (!(cb.getConcurrent() <= cb.maxConcurrent && cb.getFailures() <= cb.maxFailures)) {
        cb.setStateOpen();
        throw new CircuitBreakerOpenException();
      }
    }
    
    /**
     * Register a successful attempt, decreases the concurrent count, and resets the failure count.
     */
    public void onSucces(CircuitBreaker cb) {
      cb.incStatsSucceeded();
      cb.decConcurrent();
      cb.resetFailures();                
    }

    /**
     * Register a failed attempt, decreases the concurrent count, and increase the failure count.
     */
    public void onError(CircuitBreaker cb, Throwable t) {
      cb.incStatsFailed();
      cb.decConcurrent();
      cb.incFailures();
      if (cb.getFailures() > cb.maxFailures) { //Limit exceeded
        cb.setStateOpen();
      }
    
    }
    
    public String toString() {
      return "CLOSED";
    }
    
  }

  /* **************************************************************************
   * STATE OPEN (BRUDT)
   ****************************************************************************/
  /**
   * Class representing the open state.
   * In this state, nothing is let through, and the circuit breaker will just wait
   * for the cooldown period to pass, before the state is changed to half-open.
   */
  private static class OpenState implements State {
    
    /**
     * Denies all attempts unless we have waited more than our cooldownTime, and there are
     * fewer than maxConcurrent concurrent tasks.
     * If we have waited long enough, but there are too many concurrent tasks, the state will
     * be reset to open, allowing for a new cooldown.
     */
    public void preInvoke(CircuitBreaker cb) {  
        cb.incConcurrent();
      if (System.currentTimeMillis() - cb.lastChangeToOpen >= cb.timeCooldown) { // Vi har ventet laenge nok
        if (cb.concurrent <= cb.maxConcurrent) { // Ikke for mange samtidige kald
          // Allow future calls to pass thru.   
            cb.setStateHalfOpen();
          // This call will also be allowed, since we do not throw an exception
        } else {
          // Re-setting state to open, in order to extend cooldown time
          cb.setStateOpen();
          cb.incStatsRejected();
          throw new CircuitBreakerOpenException();
        }
      } else {
        cb.incStatsRejected();
        throw new CircuitBreakerOpenException();
      }
    }

    /**
     * Register a successful attempt, decreases concurrent, resetting failure count and
     * returning the circuit breaker to its normal closed state.
     */
    public void onSucces(CircuitBreaker cb) {
      cb.decConcurrent();
      cb.incStatsSucceeded();
      cb.resetFailures();
      cb.setStateClosed();
      
    }

    /**
     * Register a failed attempt, decreases concurrent, increasing failure count and
     * returning the circuit breaker to its open state if the state is not already open.
     */
    public void onError(CircuitBreaker cb, Throwable t) {
      cb.decConcurrent();
      cb.incFailures();
      if (!cb.isOpenState()) {
        cb.setStateOpen();
      }
    }

    public String toString() {
      return "OPEN";
    }
}

  /* **************************************************************************
   * STATE HALF-OPEN
   ****************************************************************************/
  /**
   * Class representing the half-open state.
   * In this state, everything is allowed to pass, but the circuit breaker will still
   * monitor its counts, like in the closed state.
   * If a single attempt fails, the state will be reset to open.
   * If an attempt succeed, the circuit breaker will be returned to the normal closed state.
   */
  private static class HalfOpenState implements State {

    /**
     * Allow all tasks to be executed, as long as there are fewer than maxConcurrent
     * concurrent tasks, and fewer than maxFailures failures in a row.
     * If one of these limits are reached, the state for future attempts will be changed
     * to open (brudt), and this attempt will fail with an exception.
     */
    public void preInvoke(CircuitBreaker cb) { 
        cb.incConcurrent();
      if (!(cb.getConcurrent() <= cb.maxConcurrent && cb.getFailures() <= cb.maxFailures)) {
        cb.setStateOpen();
        throw new CircuitBreakerOpenException();
      }
    }

    /**
     * Register a successful attempt, decreases concurrent, resetting failure count and
     * returning the circuit breaker to its normal closed state.
     */
    public void onSucces(CircuitBreaker cb) {
      cb.incStatsSucceeded();
      cb.decConcurrent();
      cb.resetFailures();
      cb.setStateClosed();
    }

    /**
     * Register a failed attempt, decreases concurrent, increasing failure count and
     * returning the circuit breaker to its open state.
     */
    public void onError(CircuitBreaker cb, Throwable t) {       
      cb.incStatsFailed();
      cb.decConcurrent();
      cb.incFailures();
      cb.setStateOpen();
    }
  
    public String toString() {
      return "HALFOPEN";
    }

  }
  
}
