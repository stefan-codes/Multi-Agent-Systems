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
import mobileDeviceOntology.concepts.Smartphone;
import mobileDeviceOntology.concepts.components.PhoneComponent;
import mobileDeviceOntology.concepts.items.InventoryItem;
import mobileDeviceOntology.concepts.items.OrderItem;
import mobileDeviceOntology.concepts.orders.ComponentsOrder;
import mobileDeviceOntology.concepts.orders.CustomerOrder;
import mobileDeviceOntology.predicates.SendInventory;
import mobileDeviceOntology.predicates.orders.DeliverComponentsOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

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
        System.out.println(this.getLocalName() + " is starting...");
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
                    dailyActivity.addSubBehaviour(new SendDailyOrdersBehaviour());
                    dailyActivity.addSubBehaviour(new ListenForInformOrRequestBehaviour());
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

    /* #################### Send Parts #################### */

    // Send the orders for today
    private class SendDailyOrdersBehaviour extends OneShotBehaviour {

        @Override
        public void action() {

            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.addReceiver(manufacturer);
            message.setConversationId("components delivery");

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
                message.setContent("no delivery");
                send(message);
            }
        }
    }

    /* #################### Listen for Informs or Requests #################### */

    // Listens for inventory requests, components orders and terminate signals
    private class ListenForInformOrRequestBehaviour extends Behaviour {
        boolean terminate = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            ACLMessage msg = receive(mt);

            if (msg != null) {
                // Receive requests for inventory
                if (msg.getPerformative() == ACLMessage.REQUEST && msg.getConversationId().equals("inventory")) {
                    // Create the message
                    ACLMessage informationMessage = new ACLMessage(ACLMessage.AGREE);
                    informationMessage.setLanguage(codec.getName());
                    informationMessage.setOntology(ontology.getName());
                    informationMessage.addReceiver(manufacturer);
                    informationMessage.setConversationId("inventory");

                    // Create the predicate
                    SendInventory sendInventory = new SendInventory();
                    sendInventory.setInventory(inventory);
                    sendInventory.setDaySent(day);

                    // Fill content and send
                    try {
                        getContentManager().fillContent(informationMessage, sendInventory);
                        send(informationMessage);
                    }
                    catch (Codec.CodecException | OntologyException e) {
                        e.printStackTrace();
                    }
                }

                // Informed there are no orders
                if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().equals("no orders")) {
                    terminate = true;
                }

                // Request for orders
                if (msg.getPerformative() == ACLMessage.REQUEST && msg.getConversationId().equals("components order")) {
                    ACLMessage reply = new ACLMessage(ACLMessage.REFUSE);
                    reply.addReceiver(msg.getSender());
                    reply.setReplyWith(msg.getReplyWith());

                    if (day + inventory.getDeliverySpeed() <= 100) {
                        try {
                            ContentElement ce = getContentManager().extractContent(msg);
                            if (ce instanceof Action) {
                                Concept c = ((Action) ce).getAction();
                                if (c instanceof PlaceComponentsOrder) {
                                    PlaceComponentsOrder placeComponentsOrder = (PlaceComponentsOrder) c;
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
                                            reply.setConversationId(order.getOrderNumber());
                                        } else {
                                            reply.setContent("I dont sell all of the components.");
                                        }
                                    }
                                }
                            }
                        } catch (CodecException | OntologyException e) {
                            e.printStackTrace();
                        }
                    }

                    send(reply);
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
}
