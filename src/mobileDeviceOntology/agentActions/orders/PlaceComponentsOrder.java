package mobileDeviceOntology.agentActions.orders;

import jade.content.onto.annotations.Slot;
import mobileDeviceOntology.concepts.orders.ComponentsOrder;

public class PlaceComponentsOrder extends PlaceOrder {

    private ComponentsOrder componentsOrder;

    /* Getters */

    @Slot (mandatory = true)
    public ComponentsOrder getComponentsOrder() {
        return componentsOrder;
    }

    /* Setters */

    public void setComponentsOrder(ComponentsOrder componentsOrder) {
        this.componentsOrder = componentsOrder;
    }
}
