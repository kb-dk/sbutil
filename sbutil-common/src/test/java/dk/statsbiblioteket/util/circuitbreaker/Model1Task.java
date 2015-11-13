package dk.statsbiblioteket.util.circuitbreaker;


public class Model1Task implements CircuitBreakerTask<Object, Integer> {


    int result = 0;
    private TestModel1 model = new TestModel1();

    public Integer invoke(Object in) throws Exception {

        result = model.getNumber();

        return result;
    }

    public int getResult() {
        return result;

    }


    public TestModel1 getModel() {
        return model;
    }

}
