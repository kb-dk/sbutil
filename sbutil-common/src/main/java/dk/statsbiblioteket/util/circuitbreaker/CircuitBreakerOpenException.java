package dk.statsbiblioteket.util.circuitbreaker;

/**
 * Thrown by the circuit breaker when an attempt to perform a task is
 * performed while in an open state.
 * This normally means that something is seriously wrong with the external
 * system.
 */
public class CircuitBreakerOpenException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

}
