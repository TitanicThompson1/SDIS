package src.data;

/**
 * This class implements a simple counter, with minimum value 0
 */
public class Counter {
    int counter;    // current counter value

    /**
     * Default constructor. Sets the initial value of counter to initialValue
     * @param initialValue the counter initial value
     */
    public Counter(int initialValue){
        if(initialValue < 0)
            this.counter = 0;
        else
            this.counter = initialValue;
    }

    /**
     * Simple Constructor. Assumes initial value of 0
     */
    public Counter(){
        this.counter = 0;
    }

    /**
     * Constructor for string value
     * @param initialValue the counter initial value, in a string
     */
    public Counter(String initialValue){
        this(Integer.parseInt(initialValue));
    }

    /**
     * Increments counter by one
     */
    public void increment(){
        this.counter++;
    }

    
    /**
     * Decrements counter by one
     */
    public void decrement(){
        if(this.counter > 0)
            this.counter--;
    }


    public int getCurrentValue(){
        return this.counter;
    }

    @Override
    public String toString(){
        return String.valueOf(this.counter);
    }
}
