package dk.statsbiblioteket.util.circuitbreaker;

import junit.framework.TestCase;


public class CircuitBreakerTest extends TestCase {


    public void testModel1(){

        Model1Task task = new Model1Task();

        CircuitBreaker cb = CircuitBreaker.getInstance("CircuitBreakModel1Test",5,20,5000);
        assertTrue("State ikke CLOSED som forventet",cb.isClosedState());    

        //kald går godt    
        try{
            cb.attemptTask(task);
        }
        catch(CircuitBreakerException e){
            fail();      
        }

        assertTrue(cb.isClosedState());

        task.getModel().setFailMode(true);

        // 5 kald som fejler, men maxFailures ikke nået
        for (int i=1;i<=5;i++){
            try{
                cb.attemptTask(task);
                fail();
            }
            catch(CircuitBreakerException e){

            }
        }
        assertTrue(cb.isClosedState());
        assertEquals(5,cb.getFailures());


        //Max failures overskredes med denne
        try{
            cb.attemptTask(task);
            fail();
        }
        catch(CircuitBreakerException e){

        }    
        assertEquals(6,cb.getFailures());    
        assertTrue(cb.isOpenState());

        //Forventer circuitbreakeropen-exception. Task skal ikke afvikles
        try{
            cb.attemptTask(task);
            fail();
        }
        catch(CircuitBreakerOpenException e){

        }    
        assertEquals(7,cb.getFailures());    
        assertTrue(cb.isOpenState());




        //timeCoolDown reached
        try {
            Thread.sleep(6000);
        }
        catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try{
            cb.attemptTask(task);
            fail();
        }
        catch(CircuitBreakerException e){

        }    
        assertTrue(cb.isOpenState());
        assertEquals(8,cb.getFailures());

        //timeCoolDown reached
        try {
            Thread.sleep(6000);
        }
        catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } 

        task.getModel().setFailMode(false);

        //Går godt    
        try{
            cb.attemptTask(task);
        }
        catch(CircuitBreakerOpenException e){
            fail();
        }
        assertEquals(0,cb.getFailures()); //Reset
        assertEquals(0,cb.getConcurrent()); //Reset
        assertTrue(cb.isClosedState());


    }



    private static TestTask successTask = new TestTask();
    private static TestTask failTask = new TestTask();
    private static TestTask hangTask = new TestTask();
    static {
        failTask.setToThrow(new Exception("JUnit test exception"));
        hangTask.setTimeout(10000);
    }



    public void testStatus() {
        CircuitBreaker breaker = CircuitBreaker.getInstance("testStatus",5,10,3000);

        assertEquals("testStatus",breaker.getStatus().getName());
        assertEquals(0,breaker.getStatus().getTotalSucceeded());
        assertEquals(0,breaker.getStatus().getTotalFailed());
        assertEquals(0,breaker.getStatus().getTotalRejected());
        for ( int i = 0 ; i < 10 ; i++ ) { 
            try {
                breaker.attemptTask(failTask);
            } catch (Exception e) {
            }
        }
        System.out.println(breaker.getStatus());

        sleep(4000);    

        breaker.attemptTask(successTask);    
        breaker.attemptTask(successTask);
        breaker.attemptTask(successTask);

        System.out.println(breaker.getStatus());



    }

    public void testInstantiation() {

        CircuitBreaker cb1 = CircuitBreaker.getInstance("inst1",1,2,3);      
        assertEquals(1,cb1.getStatus().getMaxFailures());
        assertEquals(2,cb1.getStatus().getMaxConcurrent());      
        assertEquals(3,cb1.getStatus().getCooldownTime());

        CircuitBreaker cb2 = CircuitBreaker.getInstance("inst2",4,5,6);      
        CircuitBreaker cb3 = CircuitBreaker.getInstance("inst3",7,8,9);


        // Make sure instances are reused
        assertNotSame(CircuitBreaker.getInstance("inst1"),CircuitBreaker.getInstance("inst2"));
        assertSame(CircuitBreaker.getInstance("inst1"),CircuitBreaker.getInstance("inst1"));
        assertSame(CircuitBreaker.getInstance("inst2"),CircuitBreaker.getInstance("inst2"));

        //Test map
        CircuitBreaker.logAllCircuitBreakerStatus();
    }

    public void testFailures() {
        CircuitBreaker breaker = CircuitBreaker.getInstance("test",5,10,3000);
        assertTrue(breaker.isClosedState());
        assertEquals(0, breaker.getFailures());

        // Add some failures
        for ( int i = 1 ; i <= 5 ; i++ ) {
            try {
                breaker.attemptTask(failTask);
                fail();
            } catch (CircuitBreakerException e) {
            }
            assertTrue(breaker.isClosedState());
            assertEquals(i, breaker.getFailures());
        }

        // Reset on success
        breaker.attemptTask(successTask);
        assertTrue(breaker.isClosedState());
        assertEquals(0, breaker.getFailures());

        // Add some failures
        for ( int i = 1 ; i <= 5 ; i++ ) {
            try {
                breaker.attemptTask(failTask);
                fail();
            } catch (CircuitBreakerException e) {
            }
            assertTrue(breaker.isClosedState());
            assertEquals(i, breaker.getFailures());
        }

        // Fail on the next fail, as  this will push the failure-count above the limit
        try {
            breaker.attemptTask(failTask);
            fail();
        } catch (CircuitBreakerException e) {
        }
        assertTrue(breaker.isOpenState());
        assertEquals(6, breaker.getFailures());

        // Wait for cooldown
        sleep(5000);

        // Fail on the next fail, as  this will push the failure-count above the limit and extend the open state
        try {
            breaker.attemptTask(failTask);
            fail();
        } catch (CircuitBreakerException e) {
        }
        assertTrue(breaker.isOpenState());
        assertEquals(7, breaker.getFailures());

        // Attempt a lot of successTask while open
        for ( int i = 0 ; i != 10 ; i++ ) {
            try {
                breaker.attemptTask(successTask);
                fail();
            } catch (CircuitBreakerOpenException e) {
            }
            assertTrue(breaker.isOpenState());
            assertEquals(8+i, breaker.getFailures());
        }


        // Wait for cooldown
        sleep(5000);

        // Reset on success
        breaker.attemptTask(successTask);
        assertTrue(breaker.isClosedState());
        assertEquals(0, breaker.getFailures());

    }

    public void testConcurrent() {

        CircuitBreaker breaker = CircuitBreaker.getInstance("testConcurrent",5,10,3000);

        assertTrue(breaker.isClosedState());

        // Make 20 attempts, all successful
        for ( int i = 0 ; i != 20 ; i++ ) {
            attemptTaskAsync(breaker, successTask);
        }
        sleep(100); // Make sure all async attempts has started
        assertEquals("Not all attempts finished, or concurrent count not decreased correctly",0,breaker.getConcurrent());
        assertTrue(breaker.isClosedState());

        // make 30 normal attempts and 10 hang attempts, thereby queueing up 10 attempts
        for ( int i = 0 ; i != 10 ; i++ ) {
            breaker.attemptTask(successTask);
            breaker.attemptTask(successTask);
            breaker.attemptTask(successTask);
            attemptTaskAsync(breaker, hangTask);
        }
        sleep(500); // Make sure all async attempts has started

        // If the actual concurrent is bigger than 10, it might be because not all successTasks has finished. Extend the sleep above before fixing anything
        assertEquals("Not all hangTasks are hanging,  or concurrent count not decreased correctly",10,breaker.getConcurrent());  
        assertTrue(breaker.isClosedState());

        sleep(1000); // Now we wait

        assertEquals("Not all hangTasks are hanging,  or concurrent count not decreased correctly",10,breaker.getConcurrent());  // Should still be the truth
        assertTrue(breaker.isClosedState());

        // Should fail due to too many concurrent
        try {
            breaker.attemptTask(successTask);
            fail();
        } catch (CircuitBreakerOpenException e) {
        }
        // Out failed attempt should not break concurrent count.
        assertEquals(10,breaker.getConcurrent());  
        assertTrue(breaker.isOpenState());

        // Wait the cooldown period and some
        sleep(5000);

        // Still all hangTasks hanging
        assertEquals(10,breaker.getConcurrent());  
        assertTrue(breaker.isOpenState());

        // Should fail due to too many concurrent
        for ( int i = 0 ; i != 20 ; i++ ) {
            try {
                breaker.attemptTask(successTask);
                fail();
            } catch (CircuitBreakerOpenException e) {
            }
        }

        // Our failed attempt should not break concurrent count.
        assertEquals(10,breaker.getConcurrent());  
        assertTrue(breaker.isOpenState());

        // Wait for hangTasks to finish
        sleep(5000);
        assertEquals(0,breaker.getConcurrent());  

        // If an attempt fails in half-open, we should change state to open/brudt 
        try {
            breaker.attemptTask(failTask);
        } catch (CircuitBreakerException e) {
            assertEquals("JUnit test exception",e.getCause().getMessage());
        }
        assertEquals(0,breaker.getConcurrent());  
        assertTrue(breaker.isOpenState());

        // Wait the cooldown period and some
        sleep(5000);

        // Should complete normally and return the circuit breaker to closed state
        breaker.attemptTask(successTask);
        assertEquals(0,breaker.getConcurrent());  
        assertTrue(breaker.isClosedState());

        try {
            breaker.attemptTask(failTask);
        } catch (CircuitBreakerException e) {
            assertEquals("JUnit test exception",e.getCause().getMessage());
        }
        assertEquals(0,breaker.getConcurrent());  
        assertTrue(breaker.isClosedState());

    }

    private void sleep(int timeout) {
        try {
            Thread.sleep(timeout);
        }
        catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void attemptTaskAsync(final CircuitBreaker breaker, final CircuitBreakerTask task) {
        new Thread(){
            public void run() {
                try {
                    breaker.attemptTask(task);
                } catch (Exception e) {
                }
            }
        }.start();
    }  
    private static class TestTask implements CircuitBreakerTask {
        private Exception toThrow;
        private int timeout;
        public void invoke() throws Exception {
            if ( timeout > 0 ) {
                Thread.sleep(timeout);
            }
            if ( toThrow != null ) {
                throw toThrow;
            }

        }
        public void setToThrow(Exception toThrow) {
            this.toThrow = toThrow;
        }
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

}
