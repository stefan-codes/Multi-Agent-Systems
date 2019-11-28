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
import mobileDeviceOntology.agentActions.Order;
import mobileDeviceOntology.agentActions.OrderSmartphones;
import mobileDeviceOntology.agentActions.SendPriceList;
import mobileDeviceOntology.concepts.*;

import java.util.*;

public class ManufacturerAgent extends Agent {

    private Codec codec = new SLCodec();
    private Ontology ontology = ECommerceOntology.getInstance();

    private int day;
    private AID simulation;
    private ArrayList<AID> suppliers = new ArrayList<>();
    private ArrayList<AID> customers = new ArrayList<>();
    private HashMap<AID, PriceList> priceCatalogue = new HashMap<>();


    private HashMap<Integer, Order> workSchedule = new HashMap<>();
    private boolean orderTaken = false;

    private int totalDeals = 0;

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
            MessageTemplate mt = MessageTemplate.MatchConversationId("synchronizer");
            ACLMessage msg = myAgent.receive(mt);

            if(msg != null) {
                // If we dont have a reference to the simulation
                if(simulation == null) {
                  simulation = msg.getSender();
                }

                if(msg.getContent().equals("terminate")) {
                    // message to end simulation
                    System.out.println(myAgent.getLocalName() + " got total of " + totalDeals);
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

                    priceCatalogue.clear();

                    // New Sequential behaviour for daily activities
                    SequentialBehaviour dailyActivity = new SequentialBehaviour();

                    // Add sub-behaviours (executed in the same order)
                    dailyActivity.addSubBehaviour(new UpdateAgentList(myAgent));
                    // TODO: Receive items from suppliers
                    dailyActivity.addSubBehaviour(new UpdatePriceCatalogue(myAgent));
                    System.out.println("Catalogue has: " + priceCatalogue.size());
                    dailyActivity.addSubBehaviour(new RespondToCustomers(myAgent));
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

        UpdateAgentList(Agent agent){
            super(agent);
        }

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
    private class RespondToCustomers extends Behaviour {

        int receivedOrders;

        RespondToCustomers(Agent a) { super(a); }

        @Override
        public void onStart() {
            receivedOrders = 0;
        }

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
            ACLMessage msg = receive(mt);

            if(msg != null) {
                // When we find a proposal
                ACLMessage reply;

                // Get the content out of the message
                try {
                    ContentElement ce = getContentManager().extractContent(msg);
                    if(ce instanceof Action) {
                        Concept c = ((Action)ce).getAction();
                        if(c instanceof OrderSmartphones) {
                            OrderSmartphones order = (OrderSmartphones) c;
                            if (decideIfShouldTakeOrder(order)) {
                                // TODO: choose another way of scheduling work
                                // Currently takes one order perday for tomorrow
                                workSchedule.put(day+1,order);
                                reply = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            } else {
                                reply = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                System.out.println("PPU - " + order.getPricePerUnit() + ". Quantity - " +  order.getQuantity() + ". DueDate - " + (order.getDueDate()-day) + ". Penalty - " + order.getDelayPenaltyPerDay());
                            }
                        } else {
                            System.out.println("Its not a orderSmartphone...");
                            reply = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                        }
                    } else {
                        System.out.println("The action is fucked up...");
                        reply = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    }
                }
                catch (CodecException | OntologyException e) {
                    // If we get an error, just reject the order
                    reply = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    e.printStackTrace();
                }


                reply.addReceiver(msg.getSender());
                reply.setReplyWith(msg.getReplyWith());
                myAgent.send(reply);

                receivedOrders++;
                totalDeals++;
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

        int suppliersAnswered = 0;

        UpdatePriceCatalogue(Agent agent) { super(agent); }
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = receive(mt);

            if(msg != null) {
                try {
                    ContentElement ce = getContentManager().extractContent(msg);
                    if(ce instanceof Action) {
                        Concept c = ((Action)ce).getAction();
                        if(c instanceof SendPriceList) {
                            SendPriceList pl = (SendPriceList) c;
                            // Update the price catalogue
                            // TODO: ERROR - why does it not remember it?
                            priceCatalogue.put(pl.getPriceList().getFromSupplier(), pl.getPriceList());
                            suppliersAnswered++;
                        }
                    }
                }
                catch (CodecException | OntologyException e) {
                    // If we get an error
                    e.printStackTrace();
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

    // TODO: Need to implement - Finished here
    private class OrderComponents extends OneShotBehaviour {

        @Override
        public void action() {

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

    // TODO: IMPLEMENT - Decides if I should take the given order
    private boolean decideIfShouldTakeOrder(OrderSmartphones order) {
        return !orderTaken;
    }

}
