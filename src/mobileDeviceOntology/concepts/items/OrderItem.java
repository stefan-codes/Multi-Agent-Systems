package mobileDeviceOntology.concepts.items;

import jade.content.onto.annotations.Slot;

public class OrderItem extends Item {

    private int quantity;

    /* Getters */

    @Slot (mandatory = true)
    public int getQuantity() {
        return quantity;
    }

    /* Setters */

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
