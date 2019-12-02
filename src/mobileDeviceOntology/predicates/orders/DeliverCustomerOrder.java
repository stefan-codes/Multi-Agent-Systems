package mobileDeviceOntology.predicates.orders;

import jade.content.onto.annotations.Slot;
import mobileDeviceOntology.concepts.orders.CustomerOrder;

public class DeliverCustomerOrder extends DeliverOrder {

    private CustomerOrder customerOrder;

    /* Getters */

    @Slot(mandatory = true)
    public CustomerOrder getCustomerOrder() {
        return customerOrder;
    }

    /* Setters */

    public void setCustomerOrder(CustomerOrder customerOrder) {
        this.customerOrder = customerOrder;
    }
}
