package defaultPackage;

import java.util.Random;
import java.util.UUID;

public class CustomerOrder {

    // Properties
    private String uniqueID;
    private Smartphone smartphone;
    private int quantity;
    private int daysTillDue;
    private int pricePerUnit;
    private int perDayDelayPenalty;

    // Constructor
    CustomerOrder(){

        // Set the ID
        this.uniqueID = UUID.randomUUID().toString();

        // Smartphone type
        this.smartphone = new Smartphone();

        Random random = new Random();

        // Quantity
        this.quantity = floor(1+50*random.nextFloat());

        // Price
        this.pricePerUnit = floor(100 + 500*random.nextFloat());

        // Due date
        this.daysTillDue = floor(1 + 10*random.nextFloat());

        // Penalty
        this.perDayDelayPenalty = this.quantity * floor(1 + 50*random.nextFloat());

    }

    // Rounds down to the nearest integer
    private int floor(float value){
        try {
            return (int) Math.floor(value);
        }
        catch (ClassCastException ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    // Getters
    public String getUniqueID() {
        return uniqueID;
    }

    public Smartphone getSmartphone(){
        return smartphone;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getDaysTillDue() {
        return daysTillDue;
    }

    public int getPricePerUnit() {
        return pricePerUnit;
    }

    public int getPerDayDelayPenalty() {
        return perDayDelayPenalty;
    }

    // Setters
    public void setDaysTillDue(int daysTillDue) {
        this.daysTillDue = daysTillDue;
    }
}
