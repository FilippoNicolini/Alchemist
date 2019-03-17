package it.unibo.alchemist.model.implementations.actions;


import it.unibo.alchemist.model.interfaces.Action;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Reaction;

/**
 * Agent used to deliver messages between other agents.
 */
public class PostmanAgent extends AbstractAgent {

    /**
     * Constructor for the agent.
     * @param name name of the agent.
     * @param node node where the agent is placed.
     */
    public PostmanAgent(final String name, final Node<Object> node) {
        super(name, node);
    }

    @Override
    public Action<Object> cloneAction(final Node<Object> n, final Reaction<Object> r) {
        return new PostmanAgent("cloned_" + this.getAgentName(), getNode());
    }

    @Override
    public void execute() {
        getNode().postman();
    }

}
