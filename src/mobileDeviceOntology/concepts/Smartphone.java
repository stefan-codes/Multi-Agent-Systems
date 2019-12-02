package mobileDeviceOntology.concepts;

import jade.content.Concept;
import jade.content.onto.annotations.AggregateSlot;
import mobileDeviceOntology.concepts.components.PhoneComponent;

import java.util.ArrayList;

public class Smartphone implements Concept {

    private ArrayList<PhoneComponent> components;

    /* Getters */

    @AggregateSlot (cardMin = 1)
    public ArrayList<PhoneComponent> getComponents() {
        return components;
    }

    /* Setters */

    public void setComponents(ArrayList<PhoneComponent> components) {
        this.components = components;
    }
}
