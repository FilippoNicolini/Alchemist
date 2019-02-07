package it.unibo.alchemist.model.implementations.reactions;

import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Time;
import it.unibo.alchemist.model.interfaces.TimeDistribution;
import it.unibo.alchemist.model.interfaces.Reaction;

public class AgentReaction extends AbstractReaction<Object> {

    private final String agentName;

    public AgentReaction(final String agentName, final Node node, final TimeDistribution<Object> time) {
        super(node, time);
        this.agentName = agentName;
    }

    public String getAgentName() {
        return this.agentName;
    }

    @Override
    public void updateInternalStatus(final Time curTime, final boolean executed, final Environment<Object, ?> env) {
        // TODO come implementare?
    }

    @Override
    public Reaction<Object> cloneOnNewNode(final Node<Object> node, final Time currentTime) {
        return this.cloneReaction("cloned_" + this.getAgentName(), node, currentTime);
    }

    @Override
    public double getRate() {
        return getTimeDistribution().getRate();
    }

    public Reaction<Object> cloneReaction(final String name, final Node<Object> node, final Time currentTime) {
        return null; // TODO come implementare?
    }
}
