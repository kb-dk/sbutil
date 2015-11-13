package dk.statsbiblioteket.util.circuitbreaker;

public interface CircuitBreakerTask<IN, OUT> {
    OUT invoke(IN input) throws Exception;
}
