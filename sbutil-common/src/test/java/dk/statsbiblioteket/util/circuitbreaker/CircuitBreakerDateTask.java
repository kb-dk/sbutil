package dk.statsbiblioteket.util.circuitbreaker;

import java.util.Date;

public class CircuitBreakerDateTask implements CircuitBreakerTask<Long, Date>{

    @Override
    public Date invoke(Long input) throws Exception {
        return new Date(input);
    }
    

}
