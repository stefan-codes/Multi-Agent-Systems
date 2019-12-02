package mobileDeviceOntology.concepts;

import jade.content.Concept;
import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;
import jade.core.AID;
import mobileDeviceOntology.concepts.items.InventoryItem;

import java.util.ArrayList;

public class Inventory implements Concept {

    private AID owner;
    private ArrayList<InventoryItem> items;
    private int deliverySpeed;

    /* Getters */

    @Slot (mandatory = true)
    public AID getOwner() {
        return owner;
    }

    @AggregateSlot (cardMin = 1)
    public ArrayList<InventoryItem> getItems() {
        return items;
    }

    @Slot (mandatory = true)
    public int getDeliverySpeed() {
        return deliverySpeed;
    }

    /* Setters */

    public void setOwner(AID owner) {
        this.owner = owner;
    }

    public void setItems(ArrayList<InventoryItem> items) {
        this.items = items;
    }

    public void setDeliverySpeed(int deliverySpeed) {
        this.deliverySpeed = deliverySpeed;
    }
}
