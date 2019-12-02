package mobileDeviceOntology.concepts.components;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class PhoneComponent implements Concept {

    private String type;
    private int size;

    /* Getters */

    @Slot (mandatory = true)
    public String getType() {
        return type;
    }

    @Slot (mandatory = true)
    public int getSize() {
        return size;
    }

    /* Setters */

    public void setType(String type) {
        this.type = type;
    }

    public void setSize(int size) {
        if (size == 2000 || size == 3000) {
            this.size = size;
        } else {
            throw new IllegalArgumentException("Unrecognised battery size");
        }
    }
}
