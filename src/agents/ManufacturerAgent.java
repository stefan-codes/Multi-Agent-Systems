package agents;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import mobileDeviceOntology.ECommerceOntology;
import mobileDeviceOntology.agentActions.orders.PlaceComponentsOrder;
import mobileDeviceOntology.agentActions.orders.PlaceCustomerOrder;
import mobileDeviceOntology.concepts.components.*;
import mobileDeviceOntology.concepts.items.InventoryItem;
import mobileDeviceOntology.concepts.items.Item;
import mobileDeviceOntology.concepts.items.OrderItem;
import mobileDeviceOntology.concepts.orders.CustomerOrder;
import mobileDeviceOntology.predicates.SendInventory;
import mobileDeviceOntology.concepts.*;

import java.util.*;

public class ManufacturerAgent extends Agent {

    // Ontology
    private Codec codec = new SLCodec();
    private Ontology ontology = ECommerceOntology.getInstance();

    // System
    private int day;
    private Stats stats = new Stats();

    // Agents
    private AID simulation;
    private ArrayList<AID> suppliers = new ArrayList<>();
    private ArrayList<AID> customers = new ArrayList<>();

    //
    private HashMap<AID, Inventory> supplierCatalogue = new HashMap<>();
    private HashMap<Integer, Integer> calendar = new HashMap<>();
    private HashMap<Integer, ArrayList<Work>> workSchedule = new HashMap<>();
    private HashMap<Integer, PartsToBeOrdered> orderSchedule = new HashMap<>();
    private HashMap<CustomerOrder, Integer> openOrders = new HashMap<>();

    // Properties for the strategy
    private int workCapacity = 50;
    private int minimumProfitPerPhone = 20;

    // Called when the agent is being created
    @Override
    protected void setup() {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        // Register with DF
        addBehaviour(new RegisterWithDFAgent());

        // Add listener behaviour
        addBehaviour(new WaitForNewDay());
    }

    // Called when the agent is being decommissioned
    @Override
    protected void takeDown() {
        // Deregister the agent! its important to do!
        try {
            DFService.deregister(this);
        }
        catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    // A behaviour to way for a new day - MAIN behaviour
    private class WaitForNewDay extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchConversationId("synchronizer");
            ACLMessage msg = myAgent.receive(mt);

            if(msg != null) {
                // If we dont have a reference to the simulation
                if(simulation == null) {
                  simulation = msg.getSender();
                }

                if(msg.getContent().equals("terminate")) {
                    // message to end simulation
                    stats.printStats();
                    System.out.println(myAgent.getLocalName() + " decommissioned.");
                    myAgent.doDelete();
                } else {
                    // It is a new day
                    try {
                        day = Integer.parseInt(msg.getContent());
                    }
                    catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                    // New Sequential behaviour for daily activities
                    SequentialBehaviour dailyActivity = new SequentialBehaviour();

                    // Add sub-behaviours (executed in the same order)
                    dailyActivity.addSubBehaviour(new UpdateAgentList());
                    // TODO: SHOULD - Receive items from suppliers, but I assume they came
                    dailyActivity.addSubBehaviour(new UpdatePriceCatalogue());
                    dailyActivity.addSubBehaviour(new ReceiveCustomerOrders());
                    dailyActivity.addSubBehaviour(new OrderComponentsBehaviour());
                    // TODO COULD - Check if I get a confirmation on order
                    dailyActivity.addSubBehaviour(new AssembleAndSendSmartphones());
                    dailyActivity.addSubBehaviour(new EndDay(myAgent));

                    myAgent.addBehaviour(dailyActivity);
                }
            }
            else{
                block();
            }
        }
    }

    // Register the agent with the DF agent for Yellow Pages
    private class RegisterWithDFAgent extends OneShotBehaviour {

        @Override
        public void action() {
            // Register with the DF agent for the yellow pages
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("manufacturer");
            sd.setName(getLocalName() + "-manufacturer-agent");

            dfd.addServices(sd);

            try {
                DFService.register(myAgent, dfd);
            }
            catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    // Update the list of agents in the simulation
    private class UpdateAgentList extends OneShotBehaviour {

        @Override
        public void action() {
            customers.clear();
            suppliers.clear();
            // Create descriptions for each type of agent in the system
            DFAgentDescription customerAD = new DFAgentDescription();
            ServiceDescription customerSD = new ServiceDescription();
            customerSD.setType("customer");
            customerAD.addServices(customerSD);

            DFAgentDescription supplierAD = new DFAgentDescription();
            ServiceDescription supplierSD = new ServiceDescription();
            supplierSD.setType("supplier");
            supplierAD.addServices(supplierSD);

            // Try to find all agents and add them to the list
            try {
                DFAgentDescription[] customerAgents = DFService.search(myAgent, customerAD);
                for (DFAgentDescription customer : customerAgents) {
                    customers.add(customer.getName());
                }

                DFAgentDescription[] supplierAgents = DFService.search(myAgent, supplierAD);
                for (DFAgentDescription supplier : supplierAgents) {
                    suppliers.add(supplier.getName());
                }

            }
            catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    // Receive and order, decide if we want to take it, send a response
    private class ReceiveCustomerOrders extends Behaviour {

        int receivedOrders;

        @Override
        public void onStart() {
            receivedOrders = 0;
        }

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = receive(mt);

            if(msg != null) {
                // When we find a proposal
                ACLMessage reply;

                // Get the content out of the message
                try {
                    ContentElement ce = getContentManager().extractContent(msg);
                    if(ce instanceof Action) {
                        Concept c = ((Action)ce).getAction();
                        if(c instanceof PlaceCustomerOrder) {
                            PlaceCustomerOrder placeCustomerOrder = (PlaceCustomerOrder) c;
                            CustomerOrder customerOrder = placeCustomerOrder.getCustomerOrder();
                            if (decideIfShouldTakeOrder(customerOrder)) {
                                reply = new ACLMessage(ACLMessage.AGREE);
                            } else {
                                reply = new ACLMessage(ACLMessage.REFUSE);
                            }
                        } else {
                            System.out.println("Its not a orderSmartphone...");
                            reply = new ACLMessage(ACLMessage.REFUSE);
                        }
                    } else {
                        System.out.println("The action is fucked up...");
                        reply = new ACLMessage(ACLMessage.REFUSE);
                    }
                }
                catch (CodecException | OntologyException e) {
                    // If we get an error, just reject the order
                    reply = new ACLMessage(ACLMessage.REFUSE);
                    e.printStackTrace();
                }


                reply.addReceiver(msg.getSender());
                reply.setReplyWith(msg.getReplyWith());
                myAgent.send(reply);
                receivedOrders++;
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return receivedOrders >= customers.size();
        }
    }

    // Update the price catalogue
    private class UpdatePriceCatalogue extends Behaviour {
        // TODO: COULD - request the update from the suppliers. Currently they automatically send theirs.
        int suppliersAnswered = 0;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = receive(mt);

            if(msg != null) {
                if (msg.getConversationId().equals("inventory")) {
                    try {
                        ContentElement ce = getContentManager().extractContent(msg);
                        if(ce instanceof SendInventory) {
                            SendInventory sendInventory = (SendInventory) ce;
                            Inventory inventory = sendInventory.getInventory();
                            supplierCatalogue.put(inventory.getOwner(), inventory);
                            suppliersAnswered++;
                        }
                    }
                    catch (CodecException | OntologyException e) {
                        // If we get an error
                        e.printStackTrace();
                    }
                }
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return suppliersAnswered == suppliers.size();
        }
    }

    // PlaceOrder the components needed for tomorrow TODO: SHOULD - order from different suppliers
    private class OrderComponentsBehaviour extends OneShotBehaviour {

        @Override
        public void action() {

            // Prepare message for everyone
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            for (AID aid : suppliers) {
                message.addReceiver(aid);
            }
            message.setContent("no orders");

            // For every day
            if (orderSchedule.containsKey(day)){
                for (PartsToBeOrdered partsToBeOrdered : orderSchedule.values()) {
                    // If we have items to order
                    if (!partsToBeOrdered.getItemsToOrder().isEmpty()) {
                        /* PlaceOrder from the supplier */
                        // Prepare the order
                        PlaceComponentsOrder placeComponentsOrder = new PlaceComponentsOrder();
                        placeComponentsOrder.setDayPlaced(day);

                        // Prepare the message
                        ACLMessage proposal = new ACLMessage(ACLMessage.REQUEST);
                        proposal.setLanguage(codec.getName());
                        proposal.setOntology(ontology.getName());
                        proposal.addReceiver(aidToOrderFrom);
                        proposal.setReplyWith(orderComponents.getOrderNumber());

                        // Wrapper action
                        Action request = new Action();
                        request.setAction(orderComponents);
                        request.setActor(aidToOrderFrom);

                        // Fill content and send
                        try {
                            getContentManager().fillContent(proposal, request);
                            send(proposal);
                            payForComponents(orderComponents);
                        }
                        catch (CodecException | OntologyException e) {
                            e.printStackTrace();
                        }
                        message.removeReceiver(aidToOrderFrom);
                    }
                }
            }

            // If we have orders for tomorrow
            if (workSchedule.containsKey(day+1)) {
                // Prepare the items to order
                ArrayList<Item> items = new ArrayList<>();
                for(Work work : workSchedule.get(day+1)){
                    // Screen
                    Item item = new Item();
                    item.setComponent(work.getOrder().getSmartphone().getScreen());
                    item.setQuantity(work.getOrder().getQuantity());
                    items.add(item);

                    // Storage
                    item = new Item();
                    item.setComponent(work.getOrder().getSmartphone().getStorage());
                    item.setQuantity(work.getOrder().getQuantity());
                    items.add(item);

                    // Ram
                    item = new Item();
                    item.setComponent(work.getOrder().getSmartphone().getRam());
                    item.setQuantity(work.getOrder().getQuantity());
                    items.add(item);

                    // Battery
                    item = new Item();
                    item.setComponent(work.getOrder().getSmartphone().getBattery());
                    item.setQuantity(work.getOrder().getQuantity());
                    items.add(item);
                }



            }
            /* tell the rest we dont need anything from them */
            send(message);
        }
    }

    //
    private class AssembleAndSendSmartphones extends OneShotBehaviour {

        @Override
        public void action() {
            // TODO: MUST - implement some logic
            System.out.println("Pressume we assembled them them");
        }
    }

    // Execute at the end of my daily activities
    private class EndDay extends OneShotBehaviour {
        EndDay(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            // Tell the simulation that I am done for today
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(simulation);
            msg.setContent("done");
            myAgent.send(msg);
        }
    }

    /* Methods */

    // Need to update, currently I take only if I can do them in one of the days
    private boolean decideIfShouldTakeOrder(CustomerOrder order) {
        int dayToStartWork;


        /* Can I complete in between 4 and DueDate */
        if (day > 3){
            dayToStartWork = day+4;
            if (ShouldManufacturerCompleteTheOrder(order, dayToStartWork)) return true;
        }

        /* Can I complete in between 1 and DueDate */
        dayToStartWork = day+1;
        if (ShouldManufacturerCompleteTheOrder(order, dayToStartWork)) return true;

        /* Can I complete in between 1 and after DueDate still with profit */
        // TODO: COULD - decide if I can take it and still profit

        return false;
    }

    // Can I do it in 4-max days? If yes, take the order
    private boolean ShouldManufacturerCompleteTheOrder(CustomerOrder order, int dayToStartWork){

        if (dayToStartWork > 100) return false;
        int toMake = order.getQuantity();


        // Calculate free capacity
        int workingDaysLeft = Math.min(order.getDueDate(), 100) - dayToStartWork;
        int freeCapacity = workCapacity * workingDaysLeft;
        for (int i = 0; i < workingDaysLeft; i++){
            calendar.putIfAbsent(dayToStartWork+i, 0);
            freeCapacity -= calendar.get(dayToStartWork+i);
        }

        // If I can, take it
        if (freeCapacity >= toMake) {
            // Check if its worth
            OrderOffer orderOffer = getPhoneCost(order.getSmartphone(), workingDaysLeft);
            if (orderOffer.price > order.getPricePerUnit() - minimumProfitPerPhone ) return false;

            // If we have the work capacity and It makes sense, take the order
            int dayInUse = dayToStartWork;
            while (toMake > 0) {
                int freeToMakeToday = workCapacity - calendar.get(dayInUse);
                // Schedule the work
                workSchedule.putIfAbsent(dayInUse, new ArrayList<>());
                Work work = new Work();
                work.setOrder(order);
                work.setOrderOffer(orderOffer);

                if (toMake <= freeToMakeToday) {
                    work.setToMake(toMake);
                    calendar.put(dayInUse, calendar.get(dayInUse) + toMake);
                    toMake = 0;
                } else {
                    toMake -= freeToMakeToday;
                    work.setToMake(freeToMakeToday);
                    calendar.put(dayInUse, calendar.get(dayInUse) + freeToMakeToday);
                }

                // Add to the work schedule
                workSchedule.get(dayInUse).add(work);

                // Add to the order schedule
                // TODO: COULD - record what I am expecting so I can check if I actually receive them
                for(PhoneComponent phoneComponent : orderOffer.getSource().keySet()){
                    // Create the orderItem
                    OrderItem orderItem = new OrderItem();
                    orderItem.setComponent(phoneComponent);
                    orderItem.setQuantity(work.getToMake());

                    // When to be scheduled
                    int dayToBeOrdered = dayInUse - supplierCatalogue.get(orderOffer.getSource().get(phoneComponent)).getDeliverySpeed();

                    // Get the existing object
                    orderSchedule.putIfAbsent(dayToBeOrdered, new PartsToBeOrdered());
                    PartsToBeOrdered partsBeingOrdered = orderSchedule.get(dayToBeOrdered);

                    // Get the existing hashmap
                    HashMap<AID, ArrayList<OrderItem>> existingItemsToBeOrdered = partsBeingOrdered.getItemsToOrder();

                    // Get the seller for the component from the offer
                    AID toOrderFrom = orderOffer.getSource().get(phoneComponent);
                    existingItemsToBeOrdered.get(toOrderFrom).add(orderItem);

                    // Re-enter in the order schedule
                    orderSchedule.put(dayToBeOrdered, partsBeingOrdered);
                }
                dayInUse++;
            }
            return true;
        }
        return false;
    }


    /*
    Get the cost to manufacture 1 smartphone if its due in dueInDays.
    Return the cheapest price found.
    Returns OrderOffer -> price, HashMap<componentName, AID>
     */
    private OrderOffer getPhoneCost(Smartphone smartphone, int dueInDays) {
        OrderOffer offer = new OrderOffer();
        HashMap<PhoneComponent, Integer> bestPrices = new HashMap<>();
        int totalPrice = 0;

        // For each supplier
        for (AID supplier : suppliers) {
            Inventory inventory = supplierCatalogue.get(supplier);
            if (inventory.getDeliverySpeed() > dueInDays) {
                continue;
            }

            // Try to update each component
            for (PhoneComponent component : smartphone.getComponents()) {
                for (InventoryItem inventoryItem : inventory.getItems()) {
                    // If they are of the same type i.e. Screens
                    if (component.getType().equals(inventoryItem.getComponent().getType())) {
                        // If they are of the same size i.e. 5"
                        if (component.getSize() == inventoryItem.getComponent().getSize()) {
                            bestPrices.putIfAbsent(component, 0);
                            // If the price of the seller is better than the currently known best, update it
                            if (bestPrices.get(component) > inventoryItem.getPrice()) {
                                bestPrices.put(component, inventoryItem.getPrice());
                                offer.getSource().put(component, supplier);
                            }
                        }
                    }
                }
            }
        }

        for (Integer individualPrice : bestPrices.values()) {
            totalPrice += individualPrice;
        }

        offer.setPrice(totalPrice);

        return offer;
    }

    private void payForComponents(PlaceComponentsOrder order) {
        PriceList tempPriceList = supplierCatalogue.get(suppliers.get(0)).getPriceList();
        for (Item i : order.getItems()){
            if (i.getComponent() instanceof Screen) {
                Screen screen = (Screen) i.getComponent();
                if (screen.getSize() == 5) {
                    stats.profit -= tempPriceList.getScreen5Price();
                } else {
                    stats.profit -= tempPriceList.getScreen7Price();
                }
            }

            if (i.getComponent() instanceof Storage) {
                Storage comp = (Storage) i.getComponent();
                if (comp.getSize() == 64) {
                    stats.profit -= tempPriceList.getStorage64Price();
                } else {
                    stats.profit -= tempPriceList.getStorage256Price();
                }
            }

            if (i.getComponent() instanceof RAM) {
                RAM comp = (RAM) i.getComponent();
                if (comp.getSize() == 4) {
                    stats.profit -= tempPriceList.getRam4Price();
                } else {
                    stats.profit -= tempPriceList.getRam8Price();
                }
            }

            if (i.getComponent() instanceof Battery) {
                Battery comp = (Battery) i.getComponent();
                if (comp.getSize() == 2000) {
                    stats.profit -= tempPriceList.getBattery2000Price();
                } else {
                    stats.profit -= tempPriceList.getBattery3000Price();
                }
            }
        }
    }

    // Return the price of the component from the seller, if they dont sell, it is 0
    private int doesSellerSellsPart(AID seller, PhoneComponent phoneComponent) {
        int result = 0;

        Inventory inventory = supplierCatalogue.get(seller);
        for (InventoryItem inventoryItem : inventory.getItems()){
            if (phoneComponent.getType().equals(inventoryItem.getComponent().getType()) && phoneComponent.getSize() == inventoryItem.getComponent().getSize()) {
                result = inventoryItem.getPrice();
            }
        }

        return result;
    }

    private class Work {
        private CustomerOrder order;
        private OrderOffer orderOffer;
        private int toMake;

        CustomerOrder getOrder() {
            return order;
        }

        void setOrder(CustomerOrder order) {
            this.order = order;
        }

        OrderOffer getOrderOffer() {
            return orderOffer;
        }

        void setOrderOffer(OrderOffer orderOffer) {
            this.orderOffer = orderOffer;
        }

        int getToMake() {
            return toMake;
        }

        void setToMake(int toMake) {
            this.toMake = toMake;
        }
    }

    private class Stats {
        private int profit = 0;
        private int delayPenalties = 0;
        private int storagePenalties = 0;
        private int numOfOrders = 0;

        void printStats(){
            System.out.println("#####################################");
            System.out.println("Orders completed: "+ numOfOrders);
            System.out.println("Profit made: " + profit);
            System.out.println("#####################################");
        }

        public int getProfit() {
            return profit;
        }

        public void setProfit(int profit) {
            this.profit = profit;
        }

        public int getDelayPenalties() {
            return delayPenalties;
        }

        public void setDelayPenalties(int delayPenalties) {
            this.delayPenalties = delayPenalties;
        }

        public int getStoragePenalties() {
            return storagePenalties;
        }

        public void setStoragePenalties(int storagePenalties) {
            this.storagePenalties = storagePenalties;
        }

        public int getNumOfOrders() {
            return numOfOrders;
        }

        public void setNumOfOrders(int numOfOrders) {
            this.numOfOrders = numOfOrders;
        }
    }

    private class OrderOffer {
        private int price;
        private HashMap<PhoneComponent, AID> source = new HashMap<>();

        public int getPrice() {
            return price;
        }

        public void setPrice(int price) {
            this.price = price;
        }

        public HashMap<PhoneComponent, AID> getSource() {
            return source;
        }

        public void setSource(HashMap<PhoneComponent, AID> source) {
            this.source = source;
        }
    }

    private class PartsToBeOrdered {
        private HashMap<AID, ArrayList<OrderItem>> itemsToOrder = new HashMap<>();

        public HashMap<AID, ArrayList<OrderItem>> getItemsToOrder() {
            return itemsToOrder;
        }

        public void setItemsToOrder(HashMap<AID, ArrayList<OrderItem>> itemsToOrder) {
            this.itemsToOrder = itemsToOrder;
        }
    }

}
