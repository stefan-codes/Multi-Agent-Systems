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
import mobileDeviceOntology.concepts.Inventory;
import mobileDeviceOntology.concepts.items.InventoryItem;
import mobileDeviceOntology.concepts.items.OrderItem;
import mobileDeviceOntology.concepts.orders.ComponentsOrder;
import mobileDeviceOntology.predicates.SendInventory;
import mobileDeviceOntology.predicates.orders.DeliverComponentsOrder;

import java.util.ArrayList;
import java.util.HashMap;

public class SupplierAgent extends Agent {

    private Codec codec  = new SLCodec();
    private Ontology ontology = ECommerceOntology.getInstance();

    private int day;
    private AID simulation;
    private AID manufacturer;

    private Inventory inventory;

    private HashMap<Integer, ArrayList<ComponentsOrder>> toDo = new HashMap<>();

    /* Defaults */

    // Called when the agent is being created
    @Override
    protected void setup() {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        // Register with DF
        addBehaviour(new RegisterWithDFAgent(this));

        // Update the price list
        createInventory(this);

        // Add listener behaviour
        addBehaviour(new WaitForNewDay(this));
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

    // Execute at the end of my daily activities
    private class EndDay extends OneShotBehaviour {

        @Override
        public void action() {
            // Tell the simulation that I am done for today
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(simulation);
            msg.setContent("done");
            send(msg);
        }
    }

    // Register the agent with the DF agent for Yellow Pages
    private class RegisterWithDFAgent extends OneShotBehaviour {

        RegisterWithDFAgent(Agent agent){
            super(agent);
        }

        @Override
        public void action() {
            // Register with the DF agent for the yellow pages
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("supplier");
            sd.setName(getLocalName() + "-supplier-agent");

            dfd.addServices(sd);

            try {
                DFService.register(myAgent, dfd);
            }
            catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    /* Custom Behaviours */

    // A behaviour to way for a new day - MAIN behaviour
    private class WaitForNewDay extends CyclicBehaviour {

        // Constructor with an agent
        WaitForNewDay(Agent agent){
            super(agent);
        }

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

                    dailyActivity.addSubBehaviour(new UpdateAgentList());
                    dailyActivity.addSubBehaviour(new SendPriceListAction());
                    dailyActivity.addSubBehaviour(new SendDailyOrders());
                    dailyActivity.addSubBehaviour(new ListenForOrders());
                    dailyActivity.addSubBehaviour(new EndDay());

                    myAgent.addBehaviour(dailyActivity);
                }
            }
            else{
                block();
            }
        }
    }

    // Update the list of agents in the simulation (Only Manufacturer)
    private class UpdateAgentList extends OneShotBehaviour {

        @Override
        public void action() {
            // Create descriptions for each type of agent in the system
            DFAgentDescription manufacturerAD = new DFAgentDescription();
            ServiceDescription manufacturerSD = new ServiceDescription();
            manufacturerSD.setType("manufacturer");
            manufacturerAD.addServices(manufacturerSD);

            // Try to find all agents and add them to the list
            try {
                DFAgentDescription[] manufacturerAgents = DFService.search(myAgent, manufacturerAD);
                for (DFAgentDescription m : manufacturerAgents) {
                    manufacturer = m.getName();
                }

            }
            catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    // Send the orders for today
    private class SendDailyOrders extends OneShotBehaviour {

        @Override
        public void action() {

            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.addReceiver(manufacturer);

            if (toDo.containsKey(day)) {
                // Send the components
                for (ComponentsOrder componentsOrder : toDo.get(day)) {
                    // Set the order
                    DeliverComponentsOrder deliverComponentsOrder = new DeliverComponentsOrder();
                    deliverComponentsOrder.setComponentsOrder(componentsOrder);
                    deliverComponentsOrder.setDayDelivered(day);

                    // Prepare the message
                    message.setLanguage(codec.getName());
                    message.setOntology(ontology.getName());
                    message.setReplyWith(componentsOrder.getOrderNumber());

                    // Fill content and send
                    try {
                        getContentManager().fillContent(message, deliverComponentsOrder);
                        send(message);
                    }
                    catch (Codec.CodecException | OntologyException e) {
                        e.printStackTrace();
                    }
                }

                toDo.get(day).clear();
            } else {
                message.setContent("no orders");
                send(message);
            }
        }
    }

    // Send the priceList to the manufacturer
    private class SendPriceListAction extends OneShotBehaviour {

        @Override
        public void action() {

            // Create the predicate
            SendInventory sendInventory = new SendInventory();
            sendInventory.setInventory(inventory);
            sendInventory.setDaySent(day);

            // Create the message
            ACLMessage informationMessage = new ACLMessage(ACLMessage.INFORM);
            informationMessage.setLanguage(codec.getName());
            informationMessage.setOntology(ontology.getName());
            informationMessage.addReceiver(manufacturer);
            informationMessage.setConversationId("inventory");

            // Fill content and send
            try {
                getContentManager().fillContent(informationMessage, sendInventory);
                send(informationMessage);
            }
            catch (Codec.CodecException | OntologyException e) {
                e.printStackTrace();
            }
        }
    }

    // Accepts new order if it can deliver
    private class ListenForOrders extends Behaviour {

        boolean terminate;

        @Override
        public void onStart() {
            terminate = false;
        }

        @Override
        public void action() {

            MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msg = receive(mt);

            if(msg != null) {

                if (msg.getPerformative() == ACLMessage.INFORM) {
                    if (msg.getContent().equals("no orders")){
                        terminate = true;
                    }
                }

                // If we get a proposal, review it
                if (msg.getPerformative() == ACLMessage.REQUEST){
                    // When we find a proposal
                    ACLMessage reply = new ACLMessage(ACLMessage.REFUSE);
                    reply.addReceiver(msg.getSender());
                    reply.setReplyWith(msg.getReplyWith());

                    if (day + inventory.getDeliverySpeed() <= 100) {
                        // Get the content out of the message
                        try {
                            ContentElement ce = getContentManager().extractContent(msg);
                            if (ce instanceof Action) {
                                Concept c = ((Action) ce).getAction();
                                if (c instanceof PlaceComponentsOrder) {
                                    ComponentsOrder order = ((PlaceComponentsOrder) c).getComponentsOrder();

                                    if (order.getItems().isEmpty()) {
                                        System.out.println("Got an empty request, wtf");
                                    } else {
                                        // Check if I sell every component
                                        if (checkIfISellTheComponents(order)) {
                                            // Take the order
                                            toDo.putIfAbsent(day + inventory.getDeliverySpeed(), new ArrayList<>());
                                            toDo.get(day + inventory.getDeliverySpeed()).add(order);
                                            reply.setPerformative(ACLMessage.AGREE);
                                        } else {
                                            System.out.println("I dont have all components, someone ordered wrong.");
                                            reply.setContent("I dont sell all of the components.");
                                        }
                                    }
                                }
                            }
                        } catch (CodecException | OntologyException e) {
                            // If we get an error, just reject the order
                            e.printStackTrace();
                        }
                    }

                    myAgent.send(reply);
                }

            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return terminate;
        }
    }


    /* Methods */

    // Create the inventory
    private void createInventory(Agent agent) {
        Object[] args = getArguments();
        if (args != null && args.length>0) {
            try {
                inventory = (Inventory) args[0];
                inventory.setOwner(agent.getAID());
            }
            catch (ClassCastException e){
                e.printStackTrace();
            }
        }
    }

    // Check if the supplier sells each item in the order
    private boolean checkIfISellTheComponents(ComponentsOrder order) {
        int counter = 0;
        for(OrderItem orderItem : order.getItems()) {
            for (InventoryItem inventoryItem : inventory.getItems()) {
                if (orderItem.getComponent().getType().equals(inventoryItem.getComponent().getType()) && orderItem.getComponent().getSize() == inventoryItem.getComponent().getSize()){
                    counter++;
                }
            }
        }

        return counter == order.getItems().size();
    }
}
