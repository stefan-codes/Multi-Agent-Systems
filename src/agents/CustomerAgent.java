package agents;

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
import mobileDeviceOntology.agentActions.OrderSmartphones;
import mobileDeviceOntology.concepts.*;
import java.util.Random;
import java.util.UUID;

public class CustomerAgent extends Agent {

    private Codec codec = new SLCodec();
    private Ontology ontology = ECommerceOntology.getInstance();

    private int day;
    private AID simulation;
    private AID manufacturer;
    private String orderNumber;

    // Called when the agent is being created
    @Override
    protected void setup() {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        // Register with DF
        addBehaviour(new RegisterWithDFAgent(this));

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
                    dailyActivity.addSubBehaviour(new UpdateAgentList(myAgent));
                    dailyActivity.addSubBehaviour(new CreateAndSendOrder(myAgent));
                    dailyActivity.addSubBehaviour(new ListenForResponse(myAgent));
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

        RegisterWithDFAgent(Agent agent){
            super(agent);
        }

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

        UpdateAgentList(Agent agent){
            super(agent);
        }

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

    // Creates and sends an order to all known manufacturers
    private class CreateAndSendOrder extends OneShotBehaviour {

        CreateAndSendOrder(Agent agent) {
            super(agent);
        }

        @Override
        public void action() {

            /* Comprise phone type */
            Smartphone smartphone = specifySmartphone();


            /* Create an order */
            OrderSmartphones order = generateOrderSmartphones(day, myAgent.getAID(), smartphone);
            orderNumber = order.getOrderNumber();

            /* Send the order */
            // Prepare the message
            ACLMessage proposal = new ACLMessage(ACLMessage.PROPOSE);
            proposal.setLanguage(codec.getName());
            proposal.setOntology(ontology.getName());
            proposal.addReceiver(manufacturer);
            proposal.setReplyWith(orderNumber);

            //IMPORTANT: According to FIPA, we need to create a wrapper Action object
            //with the action and the AID of the agent
            //we are requesting to perform the action
            //you will get an exception if you try to send the sell action directly
            //not inside the wrapper!!!
            // TODO: QUESTION - do we want the receiver or the one executing it?
            Action request = new Action();
            request.setAction(order);
            request.setActor(manufacturer);

            // Fill content and send
            try {
                getContentManager().fillContent(proposal, request);
                send(proposal);
            }
            catch (CodecException | OntologyException e) {
                e.printStackTrace();
            }
        }
    }

    // Listen for an answer to our proposal
    private class ListenForResponse extends Behaviour {

        boolean responseReceived = false;
        ListenForResponse(Agent agent){
            super(agent);
        }

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchReplyWith(orderNumber);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                // Logic what to do, but we dont need any
                if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    //System.out.println("Proposal accepted + " + myAgent.getLocalName());
                    responseReceived = true;
                }

                if (msg.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                    responseReceived = true;
                }
            }
        }

        @Override
        public boolean done() {
            return responseReceived;
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

    // Creates a OrderSmartphones based on the provided rules
    private OrderSmartphones generateOrderSmartphones(int day, AID agentAID, Smartphone smartphone) {

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

            OrderSmartphones orderSmartphones = new OrderSmartphones();
            // From order
            orderSmartphones.setOrderNumber(UUID.randomUUID().toString());
            orderSmartphones.setDayPlaced(day);
            orderSmartphones.setOrderedBy(agentAID);

            // OderSmartphone
            orderSmartphones.setSmartphone(smartphone);
            orderSmartphones.setQuantity(quantity);
            orderSmartphones.setDueDate(dueDate);
            orderSmartphones.setPricePerUnit(pricePerUnit);
            orderSmartphones.setDelayPenaltyPerDay(delayPenaltyPerDay);

            return orderSmartphones;
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Compiles a smartphone based on the constrains and returns an object instance of it
    private Smartphone specifySmartphone() {

        Random random = new Random();
        Screen screen = new Screen();
        screen.setSerialNumber(UUID.randomUUID().toString());
        Storage storage = new Storage();
        storage.setSerialNumber(UUID.randomUUID().toString());
        RAM ram = new RAM();
        ram.setSerialNumber(UUID.randomUUID().toString());
        Battery battery = new Battery();
        battery.setSerialNumber(UUID.randomUUID().toString());
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

        smartphone.setScreen(screen);
        smartphone.setBattery(battery);
        smartphone.setRam(ram);
        smartphone.setStorage(storage);

        return smartphone;
    }
}
