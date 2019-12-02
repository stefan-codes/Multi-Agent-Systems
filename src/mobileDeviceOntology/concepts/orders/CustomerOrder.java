package mobileDeviceOntology.concepts.orders;

import jade.content.onto.annotations.Slot;
import mobileDeviceOntology.concepts.Smartphone;

public class CustomerOrder extends Order {

    private Smartphone smartphone;
    private int quantity;
    private int dueDate;
    private int pricePerUnit;
    private int delayPenaltyPerDay;

    /* Getters */

    @Slot(mandatory = true)
    public Smartphone getSmartphone() {
        return smartphone;
    }

    @Slot (mandatory = true)
    public int getQuantity() {
        return quantity;
    }

    @Slot (mandatory = true)
    public int getDueDate() {
        return dueDate;
    }

    @Slot (mandatory = true)
    public int getPricePerUnit() {
        return pricePerUnit;
    }

    @Slot (mandatory = true)
    public int getDelayPenaltyPerDay() {
        return delayPenaltyPerDay;
    }

    /* Setters */

    public void setSmartphone(Smartphone smartphone) {
        this.smartphone = smartphone;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setDueDate(int dueDate) {
        this.dueDate = dueDate;
    }

    public void setPricePerUnit(int pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public void setDelayPenaltyPerDay(int delayPenaltyPerDay) {
        this.delayPenaltyPerDay = delayPenaltyPerDay;
    }
}
