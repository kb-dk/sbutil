package dk.statsbiblioteket.util.circuitbreaker;


public class TestModel1 {

  private static int count=0;
  boolean failMode=false;
 
  // Simpel metode
  //Gør godt på kald 1..10
  //Fejl kald 11..20
  //Gør godt på kald 21..30
     
  public int getNumber() throws Exception{
    count++;
    
    if (failMode ){
      //System.out.println("fejler:"+count);
      throw new Exception("count error:"+count);
    }
    //System.out.println("g�r godt:"+count);  
    return count;
        
  }
  
  public void setFailMode(boolean failMode){
   this.failMode=failMode; 
    
  }
  
  
}
