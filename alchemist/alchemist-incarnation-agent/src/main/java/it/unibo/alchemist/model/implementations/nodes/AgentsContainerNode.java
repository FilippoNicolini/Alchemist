package it.unibo.alchemist.model.implementations.nodes;

import it.unibo.alchemist.model.AgentIncarnation;
import it.unibo.alchemist.model.implementations.actions.AbstractAgent;
import it.unibo.alchemist.model.implementations.actions.Blackboard;
import it.unibo.alchemist.model.implementations.environments.Continuous2DEnvironment;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Position;
import it.unibo.alchemist.model.interfaces.Time;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;

/**
 * Node for Agent Incarnation.
 */
public class AgentsContainerNode extends AbstractNode<Object> {

    private final String param;
    private final Environment<Object, Position<? extends Continuous2DEnvironment>> environment;
    private final Map<String, AbstractAgent> agents = new LinkedHashMap<>();
    private RandomGenerator randomGenerator;

    private double nodeDirectionAngle = 0.0; // [0-360] Direction zero means to point to the north
    private double nodeSpeed = 0.0; // Speed zero means that the node is stopped
    private Time lastNodePositionUpdateTau;

    /**
     * Constructor for the agents container.
     * @param p is the param from the simulation configuration file.
     * @param env environment of node.
     */
    public AgentsContainerNode(final String p, final Environment<Object, Position<? extends Continuous2DEnvironment>> env, final RandomGenerator rand) {
        super(env);
        this.environment = env;
        this.randomGenerator = rand;
        this.param = p;
        this.lastNodePositionUpdateTau = new DoubleTime(); // Initialize the last update to time zero
        System.out.println("ENVIRONMENT CLASS: " + this.environment.getClass().toString());
    }

    @Override
    protected Object createT() {
        return new AgentsContainerNode(this.param, this.environment, this.randomGenerator);
    }

    /**
     * Get node direction angle.
     * @return node angle in rad.
     */
    public double getNodeDirectionAngle() {
        return this.nodeDirectionAngle;
    }

    /**
     * Get the speed of the node.
     * @return double of the speed.
     */
    public double getNodeSpeed() {
        return this.nodeSpeed;
    }

    /**
     * Get the current Position of the node in the environment.
     * @return position of the node.
     */
    public Position getNodePosition() {
        return this.environment.getPosition(this);
    }

    /**
     * Update the direction of the node.
     * @param angle value of direction in radians.
     * @param isDelta boolean to indicate if the new value is a delta to add to the previous value.
     */
    public void changeDirectionAngle(final double angle, final boolean isDelta) {
        if (isDelta) {
            this.nodeDirectionAngle += angle;
            /*if (this.nodeDirectionAngle > 360) {
                this.nodeDirectionAngle = this.nodeDirectionAngle % 360;
            }*/
        } else {
            this.nodeDirectionAngle = angle;
        }
    }

    /**
     * Update the speed of the node.
     * @param speed value of the speed.
     */
    public void changeNodeSpeed(final double speed) {
        this.nodeSpeed = speed;
    }

    /**
     * Add agent reference to the map.
     * @param agent agent to add to the map.
     */
    public void addAgent(final AbstractAgent agent) {
        this.agents.put(agent.getAgentName(), agent);
    }

    /**
     * Get the map that contains references of the agents.
     * @return map of the agents.
     */
    public Map<String, AbstractAgent> getAgentsMap() {
        return this.agents;
    }

    /**
     * Use the environment to move the node. The new position is obtained by a circle whose center is the current position and radius the calculated distance (Time * Speed).
     * The direction (or angle) defines which point on the circumference will be the next node position.
     * @param updateTau tau of the update
     */
    public void changeNodePosition(final Time updateTau) {
        final Position currentPosition = this.getNodePosition();
        final double radAngle = this.nodeDirectionAngle;
        final double radius = (updateTau.toDouble() - this.lastNodePositionUpdateTau.toDouble()) * this.nodeSpeed; // radius = space covered = time spent * speed
        final double x = currentPosition.getCoordinate(0) + radius * Math.cos(radAngle);
        final double y = currentPosition.getCoordinate(1) + radius * Math.sin(radAngle);

        this.environment.moveNodeToPosition(this, this.environment.makePosition(x, y));
        this.lastNodePositionUpdateTau = updateTau;
    }

    /**
     * Called from postman agent. Collects outgoing messages from all agents and deliver them to recipients.
     */
    public void postman() {
        final Map<String, AbstractAgent> tmpAgentMap = new LinkedHashMap<>();
        final List<AbstractAgent.OutMessage> outMessages = new ArrayList<>();

        // For each node in the environment get all its agents
        this.environment.getNodes().forEach(node -> {
            tmpAgentMap.putAll(((AgentsContainerNode) node).getAgentsMap());
        });

        // For each agent takes outgoing messages
        tmpAgentMap.forEach((agentName, agent) -> {
            outMessages.addAll(agent.consumeOutgoingMessages());
        });

        // Send each message to the receiver
        outMessages.forEach(message -> {
            tmpAgentMap.get(message.getReceiver().toString()).addIncomingMessage(message);
        });
    }

    /**
     * Get a map with the distances from each agent in the environment.
     * @return a map with distances from other agents.
     */
    public Map<String, Double> getNeighborhoodDistances() {
        final Map<String, Double> agentsDistances = new LinkedHashMap<>();

        // For each neighbor node
        this.environment.getNeighborhood(this).getNeighbors().forEach(node -> {
            // Calculates the distance between nodes
            final double distance = this.environment.getDistanceBetweenNodes(this, node);
            // Sets the distance for each agent inside the node (excluding the blackboards)
            ((AgentsContainerNode) node).getAgentsMap().forEach((strAgentName, agent) -> {
                if (!agent.getClass().equals(Blackboard.class)) {
                    agentsDistances.put(strAgentName, distance);
                }
            });
        });
        return agentsDistances;
    }

    /**
     * Retrieve the nearest blackboard instance if presents in the neighborhood.
     * @return agent instance if founded.
     */
    public AbstractAgent getNearestBlackboard() {
        AbstractAgent nearestBlackboard = null;
        double minDist = Double.MAX_VALUE;

        // For each neighbor node
        for (Object obj : this.environment.getNeighborhood(this).getNeighbors()) {
            final AgentsContainerNode node = (AgentsContainerNode) obj;
            final double currDist = this.environment.getDistanceBetweenNodes(this, node);
            if (node.getAgentsMap().keySet().contains(AgentIncarnation.BLACKBOARD_AGENT_NAME) && currDist < minDist) {
                minDist = currDist;
                nearestBlackboard = node.getAgentsMap().get(AgentIncarnation.BLACKBOARD_AGENT_NAME);
            }
        }
        return nearestBlackboard;
    }

    /**
     * Retrieve a list of blackboard neighbors
     * @return list of blackboards
     */
    public List<Blackboard> getBlackboardNeighborhood() {
        final List<Blackboard> blackboardNeighborhood = new ArrayList<>();
        // For each neighbor node
        for (Object obj : this.environment.getNeighborhood(this).getNeighbors()) {
            ((AgentsContainerNode) obj).getAgentsMap().forEach((strAgentName, agent) -> {
                if (agent.getClass().equals(Blackboard.class)) {
                    blackboardNeighborhood.add(((Blackboard) agent));
                }
            });
        }
        return blackboardNeighborhood;
    }
}
