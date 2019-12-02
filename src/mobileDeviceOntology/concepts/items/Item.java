package mobileDeviceOntology.concepts.items;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;
import mobileDeviceOntology.concepts.components.PhoneComponent;

// Item for an inventory, it has a phone components
public class Item implements Concept {

    private PhoneComponent component;

    /* Getters */

    @Slot(mandatory = true)
    public PhoneComponent getComponent() {
        return component;
    }

    /* Setters */

    public void setComponent(PhoneComponent component) {
        this.component = component;
    }

}
