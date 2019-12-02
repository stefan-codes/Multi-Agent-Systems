package mobileDeviceOntology.concepts.items;

import jade.content.onto.annotations.Slot;

public class InventoryItem extends Item{

    private int price;

    /* Getters */

    @Slot(mandatory = true)
    public int getPrice() {
        return price;
    }

    /* Setters */

    public void setPrice(int price) {
        this.price = price;
    }
}
