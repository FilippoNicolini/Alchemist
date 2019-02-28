package it.unibo.alchemist.model.implementations.nodes;

import it.unibo.alchemist.model.implementations.actions.AbstractAgent;
import it.unibo.alchemist.model.implementations.actions.SimpleAgent;
import it.unibo.alchemist.model.implementations.environments.Continuous2DEnvironment;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Position;
import it.unibo.alchemist.model.interfaces.Time;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Node for Agent Incarnation.
 */
public class AgentsContainerNode extends AbstractNode<Object> {

    private final String param;
    private final Environment<Object, Position<? extends Continuous2DEnvironment>> environment;
    private final Map<String, AbstractAgent> agents = new LinkedHashMap<>();

    private Integer nodeDirectionAngle = 0; // [0-360] Direction zero means to point to the north
    private Double nodeSpeed = 0.0; // Speed zero means that the node is stopped
    private Time lastNodePositionUpdateTau;

    /**
     * Constructor for the agents container.
     * @param p is the param from the simulation configuration file.
     * @param env environment of node.
     */
    public AgentsContainerNode(final String p, final Environment<Object, Position<? extends Continuous2DEnvironment>> env) {
        super(env);
        this.environment = env;
        this.param = p;
        this.lastNodePositionUpdateTau = new DoubleTime(); // Initialize the last update to time zero
        System.out.println("ENVIRONMENT CLASS: " + this.environment.getClass().toString());
    }

    @Override
    protected Object createT() {
        return new AgentsContainerNode(this.param, this.environment);
    }

    public synchronized Integer getNodeDirectionAngle() {
        return this.nodeDirectionAngle;
    }

    public synchronized Double getNodeSpeed() {
        return this.nodeSpeed;
    }

    /**
     * Get the current Position of the node in the environment.
     * @return position of the node.
     */
    public Position getNodePosition() {
        return this.environment.getPosition(this);
    }

    // TODO verificare sincronizzazione
    public synchronized void changeDirectionAngle(final int angle, final boolean isDelta) {
        if (isDelta) {
            this.nodeDirectionAngle += angle;
            /*if (this.nodeDirectionAngle > 360) {
                this.nodeDirectionAngle = this.nodeDirectionAngle % 360;
            }*/
        } else {
            this.nodeDirectionAngle = angle;
        }
    }

    // TODO verificare sincronizzazione
    public synchronized void changeNodeSpeed(final double speed) {
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
        final double radAngle =  (90 - this.nodeDirectionAngle) * Math.PI / 180; // convert degrees to radians (- 90 is the correction angle)
        final double radius = (updateTau.toDouble() - this.lastNodePositionUpdateTau.toDouble()) * this.nodeSpeed; // radius = space covered = time spent * speed
        final Number x = currentPosition.getCoordinate(0) + radius * Math.cos(radAngle);
        final Number y = currentPosition.getCoordinate(1) + radius * Math.sin(radAngle);
        // TODO per la y considerare la direzione dell'asse delle ordinate: se aumenta verso l'altro usare il +, altrimenti usare il -

        this.environment.moveNodeToPosition(this, this.environment.makePosition(x, y));
        this.lastNodePositionUpdateTau = updateTau;
    }

    /**
     * Called from postman agent. Collects outgoing messages from all agents and deliver them to recipients.
     */
    public void postman() {
        final Map<String, AbstractAgent> tmpAgentMap = new LinkedHashMap<>();
        final List<SimpleAgent.OutMessage> outMessages = new ArrayList<>();

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
            // Sets the distance for each agent inside the node
            ((AgentsContainerNode) node).getAgentsMap().keySet().forEach(strNeighborAgentName -> {
                agentsDistances.put(strNeighborAgentName, distance);
            });
        });
        return agentsDistances;
    }

    /**
     * Retrieve the agent instance, using the agent name, if presents in the neighborhood.
     * @param agentName the name of agent to find.
     * @return agent instance or null.
     */
    public AbstractAgent getNeighborAgent(final String agentName) {
        // For each neighbor node
        List<Node<Object>> neighbor = this.environment.getNeighborhood(this).getNeighbors()
                .stream()
                .filter(node -> ((AgentsContainerNode) node).getAgentsMap().keySet().contains(agentName))
                .collect(Collectors.toList());

        if (neighbor.size() > 0) {
            return ((AgentsContainerNode) neighbor.get(0)).getAgentsMap().get(agentName);
        } else {
            return null;
        }
    }
}
