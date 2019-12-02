package mobileDeviceOntology.concepts.orders;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

public class Order implements Concept {

    private AID orderedBy;
    private String orderNumber;

    /* Getters */

    @Slot(mandatory = true)
    public String getOrderNumber() {
        return orderNumber;
    }

    @Slot (mandatory = true)
    public AID getOrderedBy() {
        return orderedBy;
    }

    /* Setters */

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setOrderedBy(AID orderedBy) {
        this.orderedBy = orderedBy;
    }

}
