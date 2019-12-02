package mobileDeviceOntology.predicates;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import mobileDeviceOntology.concepts.Inventory;

public class SendInventory implements Predicate {

    private Inventory inventory;
    private int daySent;

    /* Getters */

    @Slot (mandatory = true)
    public Inventory getInventory() {
        return inventory;
    }

    @Slot (mandatory = true)
    public int getDaySent() {
        return daySent;
    }

    /* Setters */

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setDaySent(int daySent) {
        this.daySent = daySent;
    }
}
