package mobileDeviceOntology.agentActions;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

import java.util.UUID;

public class Order implements AgentAction {
    private String orderNumber;
    private int dayPlaced;
    private AID orderedBy;

    /* Getters */

    @Slot (mandatory = true)
    public String getOrderNumber() {
        return orderNumber;
    }

    @Slot (mandatory = true)
    public int getDayPlaced() {
        return dayPlaced;
    }

    @Slot (mandatory = true)
    public AID getOrderedBy() {
        return orderedBy;
    }

    /* Setters */

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setDayPlaced(int dayPlaced) {
        this.dayPlaced = dayPlaced;
    }

    public void setOrderedBy(AID orderedBy) {
        this.orderedBy = orderedBy;
    }
}
