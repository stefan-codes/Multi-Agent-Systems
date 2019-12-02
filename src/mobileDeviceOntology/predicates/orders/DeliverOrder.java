package mobileDeviceOntology.predicates.orders;

import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

public class DeliverOrder implements Predicate {

    private int dayDelivered;

    /* Getters */

    @Slot (mandatory = true)
    public int getDayDelivered() {
        return dayDelivered;
    }


    /* Setters */

    public void setDayDelivered(int dayDelivered) {
        this.dayDelivered = dayDelivered;
    }
}
