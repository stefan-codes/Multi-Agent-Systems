package mobileDeviceOntology.concepts;

import jade.content.onto.annotations.Slot;

public class RAM extends PhoneComponent {

    private int size;

    /* Getters */

    @Slot(mandatory = true)
    public int getSize() {
        return size;
    }

    /* Setters */

    public void setSize(int size) {
        this.size = size;
    }
}
