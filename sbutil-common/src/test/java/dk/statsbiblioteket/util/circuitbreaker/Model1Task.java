package dk.statsbiblioteket.util.circuitbreaker;


public class Model1Task implements CircuitBreakerTask{

  
  int result=0;
  private TestModel1 model= new TestModel1();

  public void invoke() throws Exception{
      
    result=model.getNumber();
             
  }
  
  public int getResult(){
    return result;
    
  }

  
  public TestModel1 getModel() {
    return model;
  }

   
  
  
}
