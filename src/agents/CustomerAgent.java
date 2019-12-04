package agents;

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
import mobileDeviceOntology.agentActions.orders.PlaceCustomerOrder;
import mobileDeviceOntology.concepts.*;
import mobileDeviceOntology.concepts.components.PhoneComponent;
import mobileDeviceOntology.concepts.orders.CustomerOrder;
import mobileDeviceOntology.predicates.orders.DeliverCustomerOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomerAgent extends Agent {

    private static final Logger LOGGER = Logger.getLogger( CustomerAgent.class.getName() );
    private Codec codec = new SLCodec();
    private Ontology ontology = ECommerceOntology.getInstance();

    private int day;
    private AID simulation;
    private AID manufacturer;

    private HashMap<String, CustomerOrder> confirmedOrders;
    private CustomerOrder openOrder;

    // Called when the agent is being created
    @Override
    protected void setup() {
        confirmedOrders = new HashMap<>();
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
            // Listen for New day or finish commands from the simulation
            MessageTemplate mt = MessageTemplate.MatchConversationId("synchronizer");
            ACLMessage msg = myAgent.receive(mt);

            if(msg != null) {
                // If we dont have a reference to the simulation
                if(simulation == null) {
                    simulation = msg.getSender();
                }

                // If it tells us to terminate
                if (msg.getContent().equals("terminate")) {
                    // message to end simulation
                    if (confirmedOrders.size() > 0) {
                        LOGGER.log(Level.INFO, "{0}, has incomplete orders. {1}", new Object[]{ myAgent.getLocalName(), confirmedOrders.size() } );
                    }
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
                    dailyActivity.addSubBehaviour(new PlaceCustomerOrderBehaviour());
                    dailyActivity.addSubBehaviour(new WaitForCompleteOrdersBehaviour());
                    dailyActivity.addSubBehaviour(new EndDay());

                    myAgent.addBehaviour(dailyActivity);
                }
            }
            else{
                block();
            }
        }
    }

    /* Self Actions */

    // Register the agent with the DF agent for Yellow Pages
    private class RegisterWithDFAgent extends OneShotBehaviour {

        @Override
        public void action() {
            // Register with the DF agent for the yellow pages
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("customer");
            sd.setName(getLocalName() + "-customer-agent");

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
            // Create descriptions for each type of agent in the system
            DFAgentDescription manufacturerAD = new DFAgentDescription();
            ServiceDescription manufacturerSD = new ServiceDescription();
            manufacturerSD.setType("manufacturer");
            manufacturerAD.addServices(manufacturerSD);

            // Try to find all agents and add them to the list
            try {
                DFAgentDescription[] manufacturerAgents = DFService.search(myAgent, manufacturerAD);
                for(DFAgentDescription m : manufacturerAgents) {
                    manufacturer = m.getName();
                }
            }
            catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    /* Place Order */

    // Creates and sends an order to all known manufacturers, and listen for respond to it
    private class PlaceCustomerOrderBehaviour extends Behaviour {

        boolean waitingForConfirmation = false;
        boolean terminate = false;

        @Override
        public void action() {

            if (waitingForConfirmation) {
                MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                                                        MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    if (msg.getPerformative() == ACLMessage.AGREE && msg.getConversationId().equals(openOrder.getOrderNumber())) {
                        confirmedOrders.put(openOrder.getOrderNumber(), openOrder);
                        openOrder = null;
                    }

                    if (msg.getPerformative() == ACLMessage.REFUSE && msg.getConversationId().equals(openOrder.getOrderNumber())) {
                        openOrder = null;
                    }

                    terminate = true;
                } else {
                    block();
                }
            } else {
                /* Comprise phone type */
                Smartphone smartphone = generateSmartphone();

                /* Create an order */
                CustomerOrder order = generateCustomerOrder(day, myAgent.getAID(), smartphone);

                /* Create an action */
                PlaceCustomerOrder placeCustomerOrder = new PlaceCustomerOrder();
                placeCustomerOrder.setCustomerOrder(order);
                placeCustomerOrder.setDayPlaced(day);

                // Prepare the message
                ACLMessage proposal = new ACLMessage(ACLMessage.REQUEST);
                proposal.setLanguage(codec.getName());
                proposal.setOntology(ontology.getName());
                proposal.addReceiver(manufacturer);
                proposal.setConversationId(order.getOrderNumber());

                Action request = new Action();
                request.setAction(placeCustomerOrder);
                request.setActor(manufacturer);

                // Fill content and send
                try {
                    getContentManager().fillContent(proposal, request);
                    send(proposal);
                    openOrder = order;
                    waitingForConfirmation = true;
                }
                catch (CodecException | OntologyException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean done() {
            return terminate;
        }
    }

    // Compiles a smartphone based on the constrains and returns an object instance of it
    private Smartphone generateSmartphone() {

        Random random = new Random();
        ArrayList<PhoneComponent> components = new ArrayList<>();

        PhoneComponent screen = new PhoneComponent();
        screen.setType("screen");
        PhoneComponent storage = new PhoneComponent();
        storage.setType("storage");
        PhoneComponent ram = new PhoneComponent();
        ram.setType("ram");
        PhoneComponent battery = new PhoneComponent();
        battery.setType("battery");
        Smartphone smartphone = new Smartphone();

        // screen size and battery
        if (random.nextFloat() < 0.5) {
            // small smarthphone
            screen.setSize(5);
            battery.setSize(2000);
        } else {
            // phablet
            screen.setSize(7);
            battery.setSize(3000);
        }

        // Ram
        if (random.nextFloat() < 0.5) {
            ram.setSize(4);
        } else {
            ram.setSize(8);
        }

        // Storage
        if (random.nextFloat() < 0.5) {
            storage.setSize(64);
        } else {
            storage.setSize(256);
        }

        components.add(screen);
        components.add(storage);
        components.add(ram);
        components.add(battery);

        smartphone.setComponents(components);

        return smartphone;
    }

    // Creates a PlaceOrderSmartphones based on the provided rules
    private CustomerOrder generateCustomerOrder(int day, AID agentAID, Smartphone smartphone) {

        Random random = new Random();

        try {
            // Quantity
            int quantity = (int) Math.floor(1+50*random.nextFloat());
            // Due date
            int dueDate = day + (int) Math.floor(1 + 10*random.nextFloat());
            // Price
            int pricePerUnit = (int) Math.floor(100 + 500*random.nextFloat());
            // Penalty
            int delayPenaltyPerDay = quantity * (int) Math.floor(1 + 50*random.nextFloat());

            CustomerOrder customerOrder = new CustomerOrder();
            // From order
            customerOrder.setDelayPenaltyPerDay(delayPenaltyPerDay);
            customerOrder.setDueDate(dueDate);
            customerOrder.setPricePerUnit(pricePerUnit);
            customerOrder.setQuantity(quantity);
            customerOrder.setSmartphone(smartphone);
            customerOrder.setOrderedBy(agentAID);
            customerOrder.setOrderNumber(UUID.randomUUID().toString());

            return customerOrder;
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    /* Receive Orders */

    // Listen for an answer to our proposal
    private class WaitForCompleteOrdersBehaviour extends Behaviour {
        boolean terminate = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = receive(mt);

            if (msg != null) {

                // If there are no more deliveries
                if (msg.getSender().equals(manufacturer) && msg.getContent().equals("no delivery")) {
                    terminate = true;
                } else if (msg.getSender().equals(manufacturer)){
                    try {
                        ContentElement ce = getContentManager().extractContent(msg);
                        if (ce instanceof DeliverCustomerOrder) {
                            DeliverCustomerOrder dco = (DeliverCustomerOrder)ce;
                            // TODO: COULD - actually check if the order is ok but no time...
                            confirmedOrders.remove(dco.getCustomerOrder().getOrderNumber());
                            // TODO: SHOULD - Pay
                        }
                    }
                    catch (CodecException | OntologyException e) {
                        // If we get an error, just reject the order
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public boolean done() {
            return terminate;
        }
    }

    /* End of the day */

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





}
