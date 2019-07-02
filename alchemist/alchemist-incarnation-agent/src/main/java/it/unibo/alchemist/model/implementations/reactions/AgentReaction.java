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

    /**
     * Update the internal status.
     * @param curTime
     *            the current simulation time
     * @param executed
     *            true if this reaction has just been executed, false if the
     *            update has been triggered due to a dependency
     * @param env
     *            the environment
     */
    @Override
    public void updateInternalStatus(final Time curTime, final boolean executed, final Environment<Object, ?> env) {
        // TODO come implementare?
    }

    /**
     * Clone the current object into a new AgentReaction.
     * @param node
     *            the node at which the reaction is referred.
     * @param currentTime
     *            the time at which the clone is created (required to correctly clone the {@link TimeDistribution}s)
     * @return a cloned reaction.
     */
    @Override
    public Reaction<Object> cloneOnNewNode(final Node<Object> node, final Time currentTime) {
        return new AgentReaction("cloned_" + this.agentReactionName, node, getTimeDistribution());
    }

    /**
     * Get the time distribution rate.
     * @return the double of the rate.
     */
    @Override
    public double getRate() {
        return getTimeDistribution().getRate();
    }
}
