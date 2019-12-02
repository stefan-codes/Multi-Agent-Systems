package defaultPackage;

import agents.SupplierAgent;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import mobileDeviceOntology.concepts.Inventory;
import mobileDeviceOntology.concepts.components.*;
import mobileDeviceOntology.concepts.items.InventoryItem;

import java.util.ArrayList;

class SupplierManufacturer {

    static void CreateSupplier(ContainerController cc, String name, int type) {
        try{
            AgentController supplier = cc.createNewAgent(name, SupplierAgent.class.getCanonicalName(), new Inventory[]{defineInventory(type)});
            supplier.start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // This way we have room for improvement in the future
    private static Inventory defineInventory(int type) {
        ArrayList<InventoryItem> items = new ArrayList<>();
        Inventory inventory = new Inventory();
        InventoryItem inventoryItem;
        PhoneComponent phoneComponent;

        if (type == 0) {

            /* Screen 5" */
            phoneComponent = new PhoneComponent();
            phoneComponent.setType("screen");
            phoneComponent.setSize(5);
            inventoryItem = new InventoryItem();
            inventoryItem.setComponent(phoneComponent);
            inventoryItem.setPrice(100);
            items.add(inventoryItem);

            /* Screen 7" */
            phoneComponent = new PhoneComponent();
            phoneComponent.setType("screen");
            phoneComponent.setSize(7);
            inventoryItem = new InventoryItem();
            inventoryItem.setComponent(phoneComponent);
            inventoryItem.setPrice(150);
            items.add(inventoryItem);

            /* Storage 64 */
            phoneComponent = new PhoneComponent();
            phoneComponent.setType("storage");
            phoneComponent.setSize(64);
            inventoryItem = new InventoryItem();
            inventoryItem.setComponent(phoneComponent);
            inventoryItem.setPrice(25);
            items.add(inventoryItem);

            /* Storage 256 */
            phoneComponent = new PhoneComponent();
            phoneComponent.setType("storage");
            phoneComponent.setSize(256);
            inventoryItem = new InventoryItem();
            inventoryItem.setComponent(phoneComponent);
            inventoryItem.setPrice(50);
            items.add(inventoryItem);

            /* ram 4 */
            phoneComponent = new PhoneComponent();
            phoneComponent.setType("ram");
            phoneComponent.setSize(4);
            inventoryItem = new InventoryItem();
            inventoryItem.setComponent(phoneComponent);
            inventoryItem.setPrice(30);
            items.add(inventoryItem);

            /* ram 8 */
            phoneComponent = new PhoneComponent();
            phoneComponent.setType("ram");
            phoneComponent.setSize(8);
            inventoryItem = new InventoryItem();
            inventoryItem.setComponent(phoneComponent);
            inventoryItem.setPrice(60);
            items.add(inventoryItem);

            /* Battery 2000 */
            phoneComponent = new PhoneComponent();
            phoneComponent.setType("battery");
            phoneComponent.setSize(2000);
            inventoryItem = new InventoryItem();
            inventoryItem.setComponent(phoneComponent);
            inventoryItem.setPrice(70);
            items.add(inventoryItem);

            /* battery 3000 */
            phoneComponent = new PhoneComponent();
            phoneComponent.setType("battery");
            phoneComponent.setSize(3000);
            inventoryItem = new InventoryItem();
            inventoryItem.setComponent(phoneComponent);
            inventoryItem.setPrice(100);
            items.add(inventoryItem);

            // Update the inventory
            inventory.setItems(items);
            inventory.setDeliverySpeed(1);

            return inventory;
        }

        if (type == 1) {

            /* Storage 64 */
            phoneComponent = new PhoneComponent();
            phoneComponent.setType("storage");
            phoneComponent.setSize(64);
            inventoryItem = new InventoryItem();
            inventoryItem.setComponent(phoneComponent);
            inventoryItem.setPrice(15);
            items.add(inventoryItem);

            /* Storage 256 */
            phoneComponent = new PhoneComponent();
            phoneComponent.setType("storage");
            phoneComponent.setSize(256);
            inventoryItem = new InventoryItem();
            inventoryItem.setComponent(phoneComponent);
            inventoryItem.setPrice(40);
            items.add(inventoryItem);

            /* ram 4 */
            phoneComponent = new PhoneComponent();
            phoneComponent.setType("ram");
            phoneComponent.setSize(4);
            inventoryItem = new InventoryItem();
            inventoryItem.setComponent(phoneComponent);
            inventoryItem.setPrice(20);
            items.add(inventoryItem);

            /* ram 8 */
            phoneComponent = new PhoneComponent();
            phoneComponent.setType("ram");
            phoneComponent.setSize(8);
            inventoryItem = new InventoryItem();
            inventoryItem.setComponent(phoneComponent);
            inventoryItem.setPrice(35);
            items.add(inventoryItem);

            // Update the inventory
            inventory.setItems(items);
            inventory.setDeliverySpeed(4);

            return inventory;
        }

        return null;
    }
}
