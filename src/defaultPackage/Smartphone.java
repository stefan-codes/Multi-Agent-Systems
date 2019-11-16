package defaultPackage;

import java.util.Random;

public class Smartphone {

    private String type;
    private int screenSize;
    private int storage;
    private int ram;
    private int battery;

    Smartphone(){

        Random random = new Random();

        // Type, screen size and battery
        if (random.nextFloat() < 0.5) {
            // small smarthphone
            this.type = "small";
            this.screenSize = 5;
            this.battery = 2000;
        } else {
            // phablet
            this.type = "phablet";
            this.screenSize = 7;
            this.battery = 3000;
        }

        // Ram
        if (random.nextFloat() < 0.5) {
            this.ram = 4;
        } else {
            this.ram = 8;
        }

        // Storage
        if (random.nextFloat() < 0.5) {
            this.storage = 64;
        } else {
            this.storage = 256;
        }
    }

    // Getters
    public String getType() {
        return type;
    }

    public int getScreenSize() {
        return screenSize;
    }

    public int getStorage() {
        return storage;
    }

    public int getRam() {
        return ram;
    }

    public int getBattery() {
        return battery;
    }
}
