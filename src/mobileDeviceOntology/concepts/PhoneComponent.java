package mobileDeviceOntology.concepts;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

import java.util.UUID;

public class PhoneComponent implements Concept {

    private String serialNumber;

    /* Getters */

    @Slot (mandatory = true)
    public String getSerialNumber() {
        return serialNumber;
    }

    /* Setters */

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
}
