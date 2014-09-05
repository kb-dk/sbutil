package dk.statsbiblioteket.util.circuitbreaker;

/**
 * Thrown when the task performed by the circuit breaker results in an exception.
 * The original exception is wrapped inside.
 */
public class CircuitBreakerException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public CircuitBreakerException(Throwable t) {
    super(t);
  }

}
