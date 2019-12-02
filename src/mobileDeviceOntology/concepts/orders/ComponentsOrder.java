package mobileDeviceOntology.concepts.orders;

import jade.content.onto.annotations.AggregateSlot;
import mobileDeviceOntology.concepts.items.OrderItem;
import java.util.ArrayList;

public class ComponentsOrder extends Order {

    ArrayList<OrderItem> items;

    /* Getters */

    @AggregateSlot (cardMin = 1)
    public ArrayList<OrderItem> getItems() {
        return items;
    }

    /* Setters */

    public void setItems(ArrayList<OrderItem> items) {
        this.items = items;
    }
}
