package mobileDeviceOntology.concepts;

import jade.content.Concept;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

import java.util.HashMap;

public class PriceList implements Concept {

    private AID fromSupplier;
    private int screen5Price;
    private int screen7Price;
    private int storage64Price;
    private int storage256Price;
    private int ram4Price;
    private int ram8Price;
    private int battery2000Price;
    private int battery3000Price;
    private int daysToDeliver;


    /* Getters */

    @Slot (mandatory = true)
    public AID getFromSupplier() {
        return fromSupplier;
    }

    @Slot (mandatory = true)
    public int getScreen5Price() {
        return screen5Price;
    }

    @Slot (mandatory = true)
    public int getScreen7Price() {
        return screen7Price;
    }

    @Slot (mandatory = true)
    public int getStorage64Price() {
        return storage64Price;
    }

    @Slot (mandatory = true)
    public int getStorage256Price() {
        return storage256Price;
    }

    @Slot (mandatory = true)
    public int getRam4Price() {
        return ram4Price;
    }

    @Slot (mandatory = true)
    public int getRam8Price() {
        return ram8Price;
    }

    @Slot (mandatory = true)
    public int getBattery2000Price() {
        return battery2000Price;
    }

    @Slot (mandatory = true)
    public int getBattery3000Price() {
        return battery3000Price;
    }

    @Slot (mandatory = true)
    public int isNextDayDelivery() {
        return daysToDeliver;
    }

    /* Setters */

    public void setFromSupplier(AID fromSupplier) {
        this.fromSupplier = fromSupplier;
    }

    public void setScreen5Price(int screen5Price) {
        this.screen5Price = screen5Price;
    }

    public void setScreen7Price(int screen7Price) {
        this.screen7Price = screen7Price;
    }

    public void setStorage64Price(int storage64Price) {
        this.storage64Price = storage64Price;
    }

    public void setStorage256Price(int storage256Price) {
        this.storage256Price = storage256Price;
    }

    public void setRam4Price(int ram4Price) {
        this.ram4Price = ram4Price;
    }

    public void setRam8Price(int ram8Price) {
        this.ram8Price = ram8Price;
    }

    public void setBattery2000Price(int battery2000Price) {
        this.battery2000Price = battery2000Price;
    }

    public void setBattery3000Price(int battery3000Price) {
        this.battery3000Price = battery3000Price;
    }

    public void setDaysToDeliver(int daysToDeliver) {
        this.daysToDeliver = daysToDeliver;
    }
}
