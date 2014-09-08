package dk.statsbiblioteket.util.circuitbreaker;

    public interface CircuitBreakerTask <IN extends Object,OUT extends Object>{
           public OUT invoke(IN input) throws Exception;              
    }             
