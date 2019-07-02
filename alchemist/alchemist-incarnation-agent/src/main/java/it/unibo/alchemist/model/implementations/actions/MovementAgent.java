package it.unibo.alchemist.model.implementations.actions;

import it.unibo.alchemist.model.interfaces.Action;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Reaction;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Agent that allows the node to be moved.
 */
public class MovementAgent extends AbstractAgent {

    /**
     * Constructor for the agent.
     * @param name name of the agent.
     * @param node node where the agent is placed.
     * @param reaction reaction of the agent.
     * @param rand random generator.
     */
    public MovementAgent(final String name, final Node<Object> node, final RandomGenerator rand, final Reaction<Object> reaction) {
        super(name, node, rand, reaction);
    }

    /**
     * Clone the action.
     * @param node the node which the action is referred.
     * @param reaction the reaction which the action is referred.
     * @return a new MovementAgent.
     */
    @Override
    public Action<Object> cloneAction(final Node<Object> node, final Reaction<Object> reaction) {
        return new MovementAgent("cloned_" + getAgentName(), node, this.getAgentRandomGenerator(), reaction);
    }

    /**
     * Execute the MovementAgent reasoning cycle.
     */
    @Override
    public void execute() {
        this.getNode().changeNodePosition(this.getAgentReaction().getTau());

        this.getNode().updateAgentsPosition();
    }
}
