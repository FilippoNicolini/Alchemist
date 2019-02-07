package it.unibo.alchemist.model.implementations.actions;


import it.unibo.alchemist.model.implementations.nodes.AgentsContainerNode;
import it.unibo.alchemist.model.interfaces.Node;

public class PostmanAction extends SimpleAgentAction {


    public PostmanAction(final String name, final Node<Object> node) {
        super(name, node);
    }

    @Override
    public void execute() {
        ((AgentsContainerNode) getNode()).postman();
    }

}
