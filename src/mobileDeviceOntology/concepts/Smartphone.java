package mobileDeviceOntology.concepts;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class Smartphone implements Concept {

    private Screen screen;
    private Storage storage;
    private RAM ram;
    private Battery battery;

    /* Getters */

    @Slot (mandatory = true)
    public Screen getScreen() {
        return screen;
    }

    @Slot (mandatory = true)
    public Storage getStorage() {
        return storage;
    }

    @Slot (mandatory = true)
    public RAM getRam() {
        return ram;
    }

    @Slot (mandatory = true)
    public Battery getBattery() {
        return battery;
    }

    /* Setters */

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public void setRam(RAM ram) {
        this.ram = ram;
    }

    public void setBattery(Battery battery) {
        this.battery = battery;
    }
}
