package mobileDeviceOntology.predicates.orders;

import jade.content.onto.annotations.Slot;
import mobileDeviceOntology.concepts.orders.ComponentsOrder;

public class DeliverComponentsOrder extends DeliverOrder {

    private ComponentsOrder componentsOrder;

    /* Getters */

    @Slot(mandatory = true)
    public ComponentsOrder getComponentsOrder() {
        return componentsOrder;
    }

    /* Setters */

    public void setComponentsOrder(ComponentsOrder componentsOrder) {
        this.componentsOrder = componentsOrder;
    }
}
