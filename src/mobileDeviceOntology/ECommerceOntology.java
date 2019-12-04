package mobileDeviceOntology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

public class ECommerceOntology extends BeanOntology {

    private static Ontology instance = new ECommerceOntology("myOntology");

    public static Ontology getInstance(){
        return instance;
    }

    private ECommerceOntology(String name) {
        super(name);
        try {
            // Actions
            add("mobileDeviceOntology.agentActions");
            add("mobileDeviceOntology.agentActions.orders");
            // Concepts
            add("mobileDeviceOntology.concepts");
            add("mobileDeviceOntology.concepts.components");
            add("mobileDeviceOntology.concepts.items");
            add("mobileDeviceOntology.concepts.orders");
            // Predicates
            add("mobileDeviceOntology.predicates");
            add("mobileDeviceOntology.predicates.orders");
        }
        catch (BeanOntologyException e) {
            e.printStackTrace();
        }
    }


}
