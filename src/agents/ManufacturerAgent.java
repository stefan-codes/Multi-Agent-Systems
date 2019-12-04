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
import jade.tools.sniffer.Message;
import mobileDeviceOntology.ECommerceOntology;
import mobileDeviceOntology.agentActions.RequestInventory;
import mobileDeviceOntology.agentActions.orders.PlaceComponentsOrder;
import mobileDeviceOntology.agentActions.orders.PlaceCustomerOrder;
import mobileDeviceOntology.concepts.components.*;
import mobileDeviceOntology.concepts.items.InventoryItem;
import mobileDeviceOntology.concepts.items.OrderItem;
import mobileDeviceOntology.concepts.orders.ComponentsOrder;
import mobileDeviceOntology.concepts.orders.CustomerOrder;
import mobileDeviceOntology.predicates.SendInventory;
import mobileDeviceOntology.concepts.*;
import mobileDeviceOntology.predicates.orders.DeliverComponentsOrder;
import mobileDeviceOntology.predicates.orders.DeliverCustomerOrder;
import org.w3c.dom.ls.LSOutput;

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

    // Work data structures
    private HashMap<Integer, Integer> calendar = new HashMap<>();
    private HashMap<Integer, ArrayList<Work>> workSchedule = new HashMap<>();
    private HashMap<Integer, PartsToBeOrdered> orderSchedule = new HashMap<>();
    private HashMap<CustomerOrder, Integer> openOrders = new HashMap<>();

    // Component Orders
    private HashMap<String, ComponentsOrder> unconfirmedComponentsOrders = new HashMap<>();
    private HashMap<String, ComponentsOrder> confirmedComponentsOrders = new HashMap<>();

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
                    dailyActivity.addSubBehaviour(new ReceiveComponentsDeliveryBehaviour());
                    dailyActivity.addSubBehaviour(new UpdateSupplierInventoriesBehaviour());
                    dailyActivity.addSubBehaviour(new ProcessCustomerOrdersBehaviour());
                    dailyActivity.addSubBehaviour(new OrderComponentsBehvaiour());
                    dailyActivity.addSubBehaviour(new AssembleAndProcessCompleteOrdersBehaviour());
                    dailyActivity.addSubBehaviour(new InformCustomersEndDayBehaviour());
                    dailyActivity.addSubBehaviour(new InformSuppliersEndDayBehaviour());
                    dailyActivity.addSubBehaviour(new EndDay());

                    myAgent.addBehaviour(dailyActivity);
                }
            }
            else{
                block();
            }
        }
    }

    /* #################### Self Actions #################### */

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

    /* #################### Receive Parts #################### */

    // Awaiting to receive components from the supplier, or get a "no delivery"
    private class ReceiveComponentsDeliveryBehaviour extends Behaviour {
        int terminateAnswers = 0;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = receive(mt);

            if (msg != null) {

                // If we get a message for parts, receive them
                if (msg.getConversationId().equals("components delivery") && msg.getContent().equals("no delivery")) {
                    terminateAnswers++;
                } else if (msg.getConversationId().equals("components delivery")) {
                    try {
                        ContentElement ce = getContentManager().extractContent(msg);
                        if (ce instanceof DeliverComponentsOrder) {
                            DeliverComponentsOrder dco = (DeliverComponentsOrder)ce;
                            // TODO: COULD - actually check if the order is ok but no time...
                            terminateAnswers++;
                        }
                    }
                    catch (CodecException | OntologyException e) {
                        // If we get an error, just reject the order
                        e.printStackTrace();
                    }
                }
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return terminateAnswers >= suppliers.size();
        }
    }

    /* #################### Update Inventory Records #################### */

    // Request an inventory from each supplier, and wait for their answer
    private class UpdateSupplierInventoriesBehaviour extends Behaviour {
        int terminateAnswers = 0;
        boolean waitingForAnswer = false;

        @Override
        public void action() {
            if (waitingForAnswer) {
                MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                                                        MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    if (msg.getPerformative() == ACLMessage.AGREE && msg.getConversationId().equals("inventory")) {
                        try {
                            ContentElement ce = getContentManager().extractContent(msg);
                            if (ce instanceof SendInventory) {
                                SendInventory sendInventory = (SendInventory)ce;
                                supplierCatalogue.put(sendInventory.getInventory().getOwner(), sendInventory.getInventory());
                            }
                        }
                        catch (CodecException | OntologyException e) {
                            e.printStackTrace();
                        }
                        terminateAnswers++;
                    }

                    if (msg.getPerformative() == ACLMessage.REFUSE && msg.getConversationId().equals("inventory")) {
                        // TODO: COULD - add more logic here
                        terminateAnswers++;
                    }
                } else {
                    block();
                }
            } else {
                ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
                message.setContent(codec.getName());
                message.setLanguage(ontology.getName());
                message.setConversationId("inventory");
                message.setContent("inventory");

                for (AID aid : suppliers) {
                    message.addReceiver(aid);
                }

                send(message);
                waitingForAnswer = true;
            }
        }

        @Override
        public boolean done() {
            return terminateAnswers >= suppliers.size();
        }
    }

    /* #################### Deal with Customer Orders #################### */

    // Accept or Refuse the customer orders
    private class ProcessCustomerOrdersBehaviour extends Behaviour {
        int terminateAnswers = 0;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = receive(mt);

            if (msg != null) {
                // Prepare a response
                ACLMessage reply = new ACLMessage(ACLMessage.REFUSE);

                // Get the content out of the message
                try {
                    ContentElement ce = getContentManager().extractContent(msg);
                    if(ce instanceof Action) {
                        Concept c = ((Action)ce).getAction();
                        if(c instanceof PlaceCustomerOrder) {
                            PlaceCustomerOrder placeCustomerOrder = (PlaceCustomerOrder) c;
                            CustomerOrder customerOrder = placeCustomerOrder.getCustomerOrder();
                            if (decideIfShouldTakeOrder(customerOrder)) {
                                reply.setPerformative(ACLMessage.AGREE);
                                openOrders.put(customerOrder, customerOrder.getQuantity());
                            }
                        }
                    }
                }
                catch (CodecException | OntologyException e) {
                    // If we get an error, just reject the order
                    reply = new ACLMessage(ACLMessage.REFUSE);
                    e.printStackTrace();
                }


                reply.addReceiver(msg.getSender());
                reply.setConversationId(msg.getConversationId());
                myAgent.send(reply);
                terminateAnswers++;
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return terminateAnswers >= customers.size();
        }
    }

    // Need to update, currently I take only if I can do them in one of the days
    private boolean decideIfShouldTakeOrder(CustomerOrder order) {
        int dayToStartWork;
        boolean works = false;

        /* Can I complete in between 4 and DueDate */
        if (day > 3){
            dayToStartWork = day+4;
            works = EvaluateIfItsProfitable(order, dayToStartWork);
        }

        if (works) {
            return true;
        }

        /* Can I complete in between 1 and DueDate */
        dayToStartWork = day+1;

        return EvaluateIfItsProfitable(order, dayToStartWork);

        /* Can I complete in between 1 and after DueDate still with profit */
        // TODO: COULD - decide if I can take it and still profit
    }

    // Need to update the description
    private boolean EvaluateIfItsProfitable(CustomerOrder order, int dayToStartWork){

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


                /*
                For each item -> check from where I am ordering, and schedule the order correctly
                 */


                // Add to the order schedule
                for(PhoneComponent phoneComponent : orderOffer.getSource().keySet()){

                    // Create the orderItem
                    OrderItem orderItem = new OrderItem();
                    orderItem.setComponent(phoneComponent);
                    orderItem.setQuantity(work.getToMake());
                    AID currentAID = orderOffer.getSource().get(phoneComponent);
                    // When to be scheduled
                    int dayToBeOrdered = dayInUse - supplierCatalogue.get(currentAID).getDeliverySpeed();

                    // Get the existing object
                    orderSchedule.putIfAbsent(dayToBeOrdered, new PartsToBeOrdered());
                    orderSchedule.get(dayToBeOrdered).getItemsToOrder().putIfAbsent(currentAID, new ArrayList<>());

                    // finally, add the orderItem
                    orderSchedule.get(dayToBeOrdered).getItemsToOrder().get(currentAID).add(orderItem);
                }
                dayInUse++;
            }
            return true;
        }
        return false;
    }

    //Get best offer to manufacture 1 smartphone if its due in dueInDays.
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
                            bestPrices.putIfAbsent(component, Integer.MAX_VALUE);
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

    /* #################### Order Components #################### */

    //
    private class OrderComponentsBehvaiour extends Behaviour {
        boolean waitingForConfirmation = false;
        boolean terminate = false;

        @Override
        public void action() {
            if (waitingForConfirmation) {
                MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                                                        MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    if (unconfirmedComponentsOrders.containsKey(msg.getConversationId()) &&
                            msg.getPerformative() == ACLMessage.AGREE) {
                        confirmedComponentsOrders.put(msg.getConversationId(), unconfirmedComponentsOrders.get(msg.getConversationId()));
                        unconfirmedComponentsOrders.remove(msg.getConversationId());
                        payForComponents(confirmedComponentsOrders.get(msg.getConversationId()), msg.getSender());
                    }

                    if (unconfirmedComponentsOrders.containsKey(msg.getConversationId()) &&
                            msg.getPerformative() == ACLMessage.REFUSE) {
                        // TODO: COULD - Need to order again from a different supplier
                        unconfirmedComponentsOrders.remove(msg.getConversationId());
                    }
                } else {
                    block();
                }

                if (unconfirmedComponentsOrders.size() <= 0) {
                    terminate = true;
                }
            } else {
                // If we have parts to order today
                if (orderSchedule.containsKey(day)) {
                    PartsToBeOrdered partsToBeOrdered = orderSchedule.get(day);

                        // If we have items to order
                        if (!partsToBeOrdered.getItemsToOrder().isEmpty()) {
                            // For every supplier scheduled to order from
                            for (AID supplier : partsToBeOrdered.getItemsToOrder().keySet()) {
                                // Compose the component order
                                ComponentsOrder componentsOrder = new ComponentsOrder();
                                componentsOrder.setItems(partsToBeOrdered.getItemsToOrder().get(supplier));
                                componentsOrder.setOrderNumber(UUID.randomUUID().toString());
                                componentsOrder.setOrderedBy(myAgent.getAID());

                                // Prepare the order
                                PlaceComponentsOrder placeComponentsOrder = new PlaceComponentsOrder();
                                placeComponentsOrder.setDayPlaced(day);
                                placeComponentsOrder.setComponentsOrder(componentsOrder);

                                // Prepare the message
                                ACLMessage proposal = new ACLMessage(ACLMessage.REQUEST);
                                proposal.setLanguage(codec.getName());
                                proposal.setOntology(ontology.getName());
                                proposal.addReceiver(supplier);
                                proposal.setConversationId("components order");
                                proposal.setReplyWith(componentsOrder.getOrderNumber());

                                // Wrapper action
                                Action request = new Action();
                                request.setAction(placeComponentsOrder);
                                request.setActor(supplier);

                                // Fill content and send
                                try {
                                    getContentManager().fillContent(proposal, request);
                                    send(proposal);
                                    waitingForConfirmation = true;
                                    unconfirmedComponentsOrders.put(componentsOrder.getOrderNumber(), componentsOrder);
                                }
                                catch (CodecException | OntologyException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            terminate = true;
                        }
                } else {
                    terminate = true;
                }
            }
        }

        @Override
        public boolean done() {
            return terminate;
        }
    }

    // Pay for the componnets
    private void payForComponents(ComponentsOrder order, AID supplier) {
        int bill = 0;
        for (OrderItem orderItem : order.getItems()){
            for (InventoryItem inventoryItem : supplierCatalogue.get(supplier).getItems()){
                if (orderItem.getComponent().getType().equals(inventoryItem.getComponent().getType()) &&
                        orderItem.getComponent().getSize() == inventoryItem.getComponent().getSize()) {
                    bill += inventoryItem.getPrice();
                }
            }
        }

        stats.setCapital(stats.getCapital()-bill);
    }

    /* #################### Assemble and Deliver complete Orders #################### */

    // Assemble, ship and get paid
    private class AssembleAndProcessCompleteOrdersBehaviour extends Behaviour {
        boolean waitingForConfirmation = false;

        @Override
        public void action() {

            if (waitingForConfirmation) {
                // TODO: COULD - check if customers are happy with the order
            } else {
                workSchedule.putIfAbsent(day, new ArrayList<>());
                /* Assemble phones */
                for (Work work : workSchedule.get(day)) {
                    // TODO: COULD - check if I have the parts
                    CustomerOrder customerOrder = work.getOrder();
                    openOrders.put(customerOrder, openOrders.get(customerOrder) - work.getToMake());

                    /* Send any complete orders */
                    if (openOrders.get(customerOrder) <= 0) {
                        // Order is done, so send it back
                        // Create the message
                        ACLMessage informationMessage = new ACLMessage(ACLMessage.INFORM);
                        informationMessage.setLanguage(codec.getName());
                        informationMessage.setOntology(ontology.getName());
                        informationMessage.addReceiver(customerOrder.getOrderedBy());
                        informationMessage.setConversationId(customerOrder.getOrderNumber());

                        // Create the predicate
                        DeliverCustomerOrder deliverCustomerOrder = new DeliverCustomerOrder();
                        deliverCustomerOrder.setCustomerOrder(customerOrder);
                        deliverCustomerOrder.setDayDelivered(day);

                        // Fill content and send
                        try {
                            getContentManager().fillContent(informationMessage, deliverCustomerOrder);
                            send(informationMessage);
                            // Get paid and remove the order from open orders
                            getPaid(customerOrder);
                            openOrders.remove(customerOrder);
                        } catch (Codec.CodecException | OntologyException e) {
                            e.printStackTrace();
                        }
                    }
                }
                waitingForConfirmation = true;
            }
        }

        @Override
        public boolean done() {
            return waitingForConfirmation;
        }
    }

    //
    private void getPaid(CustomerOrder customerOrder) {
        stats.setCapital(stats.getCapital() + (customerOrder.getQuantity() * customerOrder.getPricePerUnit()));
    }

    /* #################### Tell everyone we dont need them anymore #################### */

    // Inform all customers there are no more complete orders for today
    private class InformCustomersEndDayBehaviour extends OneShotBehaviour {

        @Override
        public void action() {
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            for (AID aid : customers) {
                message.addReceiver(aid);
            }
            message.setContent("no delivery");
            send(message);
        }
    }

    // Inform all suppliers there are no more orders for today
    private class InformSuppliersEndDayBehaviour extends OneShotBehaviour {

        @Override
        public void action() {
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            for (AID aid : suppliers) {
                message.addReceiver(aid);
            }
            message.setContent("no orders");
            send(message);
        }
    }

    /* #################### End Day #################### */

    // Execute at the end of my daily activities
    private class EndDay extends OneShotBehaviour {

        @Override
        public void action() {
            // Tell the simulation that I am done for today
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(simulation);
            msg.setContent("done");

            myAgent.send(msg);
        }
    }

    /* #################### Data Structures #################### */

    // TODO: COULD - settings class

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
        private int capital = 0;
        private int delayPenalties = 0;
        private int storagePenalties = 0;
        private int numOfOrders = 0;

        void printStats(){
            System.out.println("#####################################");
            System.out.println("Orders completed: "+ numOfOrders);
            System.out.println("Profit made: " + capital);
            System.out.println("#####################################");
        }

        public int getCapital() {
            return capital;
        }

        public void setCapital(int capital) {
            this.capital = capital;
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
