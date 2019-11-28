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
            add("mobileDeviceOntology.agentActions");
            add("mobileDeviceOntology.concepts");
            //add("mobileDeviceOntology.predicates");
        }
        catch (BeanOntologyException e) {
            e.printStackTrace();
        }
    }


}
