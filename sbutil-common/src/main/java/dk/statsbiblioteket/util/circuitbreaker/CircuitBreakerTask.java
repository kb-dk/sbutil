package dk.statsbiblioteket.util.circuitbreaker;

    public interface CircuitBreakerTask <IN,OUT>{
           public OUT invoke(IN input) throws Exception;              
    }             
