package dk.statsbiblioteket.util.circuitbreaker;

    public interface CircuitBreakerTask {
        public void invoke() throws Exception;              
  }
