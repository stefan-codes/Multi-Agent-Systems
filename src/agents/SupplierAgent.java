package agents;

import jade.content.AgentAction;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
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
import mobileDeviceOntology.agentActions.SendPriceList;
import mobileDeviceOntology.concepts.PriceList;

public class SupplierAgent extends Agent {

    private Codec codec  = new SLCodec();
    private Ontology ontology = ECommerceOntology.getInstance();

    private int day;
    private AID simulation;
    private AID manufacturer;
    private PriceList priceList;


    // Called when the agent is being created
    @Override
    protected void setup() {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        // Register with DF
        addBehaviour(new RegisterWithDFAgent(this));

        // Update priceList
        Object[] args = getArguments();
        if (args != null && args.length>0) {
            try {
                updatePriceList(this, args[0]);
            }
            catch (NumberFormatException nfe){
                nfe.printStackTrace();
            }
        }


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

                    // Add sub-behaviours (executed in the same order)
                    dailyActivity.addSubBehaviour(new UpdateAgentList(myAgent));
                    // TODO: Send daily orders
                    dailyActivity.addSubBehaviour(new SendPriceListAction(myAgent));
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
                for (DFAgentDescription m : manufacturerAgents) {
                    manufacturer = m.getName();
                }

            }
            catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    // Send the priceList to the manufacturer
    private class SendPriceListAction extends OneShotBehaviour {

        SendPriceListAction(Agent agent) { super(agent); }

        @Override
        public void action() {

            SendPriceList sendPriceList = new SendPriceList();
            sendPriceList.setPriceList(priceList);

            ACLMessage informationMessage = new ACLMessage(ACLMessage.INFORM);
            informationMessage.setLanguage(codec.getName());
            informationMessage.setOntology(ontology.getName());
            informationMessage.addReceiver(manufacturer);

            Action informAction = new Action();
            informAction.setAction(sendPriceList);
            informAction.setActor(manufacturer);

            // Fill content and send
            try {
                getContentManager().fillContent(informationMessage, informAction);
                send(informationMessage);
            }
            catch (Codec.CodecException | OntologyException e) {
                e.printStackTrace();
            }
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

    // Update the priceList
    private void updatePriceList(Agent agent, Object id) {
        int type = (int) id;
        priceList = new PriceList();
        priceList.setFromSupplier(agent.getAID());
        if (type == 0) {
            priceList.setScreen5Price(100);
            priceList.setScreen7Price(150);
            priceList.setStorage64Price(25);
            priceList.setStorage256Price(50);
            priceList.setRam4Price(30);
            priceList.setRam8Price(60);
            priceList.setBattery2000Price(70);
            priceList.setBattery3000Price(100);
            priceList.setDaysToDeliver(1);
        } else {
            // For now we have only two types of suppliers
            priceList.setScreen5Price(-1);
            priceList.setScreen7Price(-1);
            priceList.setStorage64Price(15);
            priceList.setStorage256Price(40);
            priceList.setRam4Price(20);
            priceList.setRam8Price(35);
            priceList.setBattery2000Price(-1);
            priceList.setBattery3000Price(-1);
            priceList.setDaysToDeliver(4);
        }

    }
}
