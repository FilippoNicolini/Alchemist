package it.unibo.alchemist.model.implementations.reactions;

import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Time;
import it.unibo.alchemist.model.interfaces.TimeDistribution;
import it.unibo.alchemist.model.interfaces.Reaction;

/**
 * Reaction of the agent.
 */
public class AgentReaction extends AbstractReaction<Object> {

    private final String agentReactionName;

    /**
     * Constructor for the reaction.
     * @param agentReactionName name of the reaction.
     * @param node node to which the reaction refers.
     * @param time time distribution of the reaction.
     */
    public AgentReaction(final String agentReactionName, final Node<Object> node, final TimeDistribution<Object> time) {
        super(node, time);
        this.agentReactionName = agentReactionName;
    }

    @Override
    public void updateInternalStatus(final Time curTime, final boolean executed, final Environment<Object, ?> env) {
        // TODO come implementare?
    }

    @Override
    public Reaction<Object> cloneOnNewNode(final Node<Object> node, final Time currentTime) {
        return new AgentReaction("cloned_" + this.agentReactionName, node, getTimeDistribution());
    }

    @Override
    public double getRate() {
        return getTimeDistribution().getRate();
    }
}
