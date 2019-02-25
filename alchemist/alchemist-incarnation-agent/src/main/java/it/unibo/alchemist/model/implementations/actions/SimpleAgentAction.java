package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.*;
import it.unibo.alchemist.model.implementations.nodes.AgentsContainerNode;
import it.unibo.alchemist.model.interfaces.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Double;
import java.util.*;

public class SimpleAgentAction extends AbstractAction<Object> {

    private final static String BASE_THEORY = "agent_library";

    // Messages
    private final static String IO_MSG = "Failed reading agent's theory file.";
    private final static String INVALID_THEORY_MSG = "Theory not valid.";
    private final static String MALFORMED_GOAL_MSG = "Malformed goal";
    private final static String NO_SOLUTION_MSG = "No solution for goal";
    private final static String UNKNOWN_VAR_MSG = "Error retrieving the term in the solution of goal";
    private final static String NO_MORE_SOLUTION_MSG = "Error retrieving next solution of goal";
    private final static String SEPARATOR = " || ";
    private final static String SUCCESS_PLAN = "plan done successfully.";
    private final static String TRIGGERED_PLAN = "Triggered plan";
    private final static String NO_IMPLEMENTATION_FOUND = "No implementation found for ";

    private final String agentName; // String for the agent name
    private boolean isInitialized = false; // Flag for init of the agent
    private final Queue<InMessage> inbox = new LinkedList<>(); // Mailbox IN queue
    private final Queue<OutMessage> outbox = new LinkedList<>(); // Mailbox OUT queue
    private final Prolog engine = new Prolog(); // tuProlog engine
    private Reaction agentReaction; // Reference to reaction
    private final Map<Term,String> beliefBaseChanges = new LinkedHashMap<>();

    public SimpleAgentAction(final String name, final Node<Object> node) {
        super(node);
        this.agentName = name;

        // Loads the theory only if the agent isn't the postman
        if (!"postman".equals(this.agentName)) {
            try {
                this.engine.setTheory(new Theory(new FileInputStream(new File("alchemist-incarnation-agent/src/main/resources/" + BASE_THEORY + ".pl"))));
                this.engine.addTheory(new Theory(new FileInputStream(new File("alchemist-incarnation-agent/src/main/resources/" + this.getAgentName() + ".pl"))));
            } catch (IOException e) {
                System.err.println(this.getAgentName() + SEPARATOR + IO_MSG);
            } catch (InvalidTheoryException e) {
                System.err.println(this.getAgentName() + SEPARATOR + INVALID_THEORY_MSG);
            }
        }
    }

    @Override
    public Action<Object> cloneAction(final Node<Object> n, final Reaction<Object> r) {
        return new SimpleAgentAction("cloned_" + this.getAgentName(),getNode());
    }

    @Override
    public Context getContext() {
        return Context.NEIGHBORHOOD; // TODO va bene come profondità?
    }

    private void setInitialized() {
        this.isInitialized = true;
    }

    private boolean isInitialized() {
        return isInitialized;
    }

    public String getAgentName() {
        return agentName;
    }

    private boolean inboxIsEmpty() {
        return this.inbox.size() == 0;
    }

    public void setAgentReaction(final Reaction reaction) {
        this.agentReaction = reaction;
    }

    @Override
    public void execute() {
        if (!this.isInitialized()) {
            this.inizializeAgent();

            System.out.println("Nodo: " + getNode().getId() + " || agent " + this.getAgentName() + " inizializzato");
            try {
                // Computes the init (e.g. In ping/pong problem send the first message).
                final SolveInfo init = this.engine.solve("init.");
                if (init.isSuccess()) {
                    System.out.println(this.getAgentName() + SEPARATOR + "init " + SUCCESS_PLAN);
                } else {
                    System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " init.");
                }
                this.hanldeOutGoingMessages();
            } catch (MalformedGoalException e) {
                System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " init.");
            }
        } else {
            //Agent's reasoning cycle

            this.handleIncomingMessages();

            // Receives message (invokes plan 'onReceivedMessage(S,M)')
            this.getMessage();

            this.positionUpdate();

            this.getBeliefBaseChanges();

            this.notifyBeliefBaseChanges();

            this.hanldeOutGoingMessages();
        }
    }

    //------------------------------------------
    // Agent's internal action
    //------------------------------------------

    /**
     * Initilizes the agent (puts in the theory his name).
     */
    private void inizializeAgent() {
        // Prepares belief with the name of the agent
        final Struct self = new Struct("self", Term.createTerm(this.getAgentName()));

        // Sets 'self' in the theory of the agent
        this.engine.getTheoryManager().assertA(self, true, null, false);

        // Node's starting position
        final Position nodePosition = ((AgentsContainerNode)getNode()).getNodePosition();
        final Struct position = new Struct(
                "belief",
                new Struct(
                        "position",
                        Term.createTerm(Double.toString(nodePosition.getCoordinate(0))),
                        Term.createTerm(Double.toString(nodePosition.getCoordinate(1))))
        );

        // Node's initial speed and direction
        final Struct movement = new Struct(
                "belief",
                new Struct(
                        "movement",
                        Term.createTerm(((AgentsContainerNode)getNode()).getNodeSpeed().toString()),
                        Term.createTerm(((AgentsContainerNode)getNode()).getNodeDirectionAngle().toString()))
        );

        // Sets the node position
        this.engine.getTheoryManager().assertA(position, true, null, false);

        // Sets the node movement
        this.engine.getTheoryManager().assertA(movement, true, null, false);

        // Set the beliefs with the distance to other agents
        this.agentsDistancesUpdate();

        // Updates the initialization flag
        this.setInitialized();
    }

    /**
     * Inserts messages received into the theory
     */
    private void handleIncomingMessages() {
        while (!this.inboxIsEmpty()) {
            final InMessage msg = this.inbox.poll();
            if (Objects.nonNull(msg)) {
                final Struct incoming = new Struct("ingoing", msg.getSender(),msg.getPayload());
                this.engine.getTheoryManager().assertZ(incoming, true, null, false);
            }
        }
    }

    /**
     * Prepares messages to send
     */
    private void hanldeOutGoingMessages() {
        try {
            SolveInfo outgoing = this.engine.solve("retract(outgoing(S,R,M)).");
            while (outgoing != null && outgoing.isSuccess()) {
                this.outbox.add(new OutMessage(outgoing.getTerm("S"), outgoing.getTerm("R"), outgoing.getTerm("M")));
                if (outgoing.hasOpenAlternatives()) {
                    outgoing = this.engine.solveNext();
                } else {
                    outgoing = null;
                }
            }
        } catch (MalformedGoalException e) {
            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " outgoing.");
        } catch (NoSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " outgoing.");
        } catch (UnknownVarException e) {
            System.err.println(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " outgoing.");
        } catch (NoMoreSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " outgoing.");
        }
    }

    /**
     * Solve receive plan.
     */
    private void getMessage() {
        try {
            final SolveInfo receive = this.engine.solve("receiveMessage.");
            if (receive.isSuccess()) {
                System.out.println(this.getAgentName() + SEPARATOR + this.agentReaction.getTau().toDouble() + SEPARATOR + "receiveMessage " + SUCCESS_PLAN);
            }
        } catch (MalformedGoalException e) {
            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " receiveMessage.");
        }
    }

    /**
     * Takes changes to the belief base occurred in the reasoning cycle.
     */
    private void getBeliefBaseChanges() {
        try {
            // Gets added beliefs
            try {
                SolveInfo insertBelief = this.engine.solve("retract(added_belief(B)).");
                while (insertBelief != null && insertBelief.isSuccess()) {
                    // Puts beliefs in the map for the notification
                    final Term currentBelief = insertBelief.getTerm("B");
                    this.beliefBaseChanges.put(currentBelief,"added");

                    // Updates node values of speed and direction (that are contained into the belief 'movement')
                    final JSONObject jsonObj = new JSONObject(currentBelief.toJSON());
                    if ("movement".equals(jsonObj.get("name"))) {
                        final JSONArray args = (JSONArray) jsonObj.get("arg");
                        // Speed
                        final String speed = args.getJSONObject(0).get("value").toString();
                        if (!((AgentsContainerNode)getNode()).getNodeSpeed().toString().equals(speed)) {
                            //System.out.println(this.getAgentName() + SEPARATOR + "------------------SPEED DIFFERENT (Agent: " + speed + ", Node: " + ((AgentsContainerNode)getNode()).getNodeSpeed() + ")");
                            ((AgentsContainerNode)getNode()).changeNodeSpeed(Double.parseDouble(speed));
                        }

                        // Direction
                        final String direction = args.getJSONObject(1).get("value").toString();
                        if (!((AgentsContainerNode)getNode()).getNodeDirectionAngle().toString().equals(direction)) {
                            //System.out.println(this.getAgentName() + SEPARATOR + "------------------DIRECTION DIFFERENT (Agent: " + direction + ", Node: " + ((AgentsContainerNode)getNode()).getNodeDirectionAngle() + ")");
                            ((AgentsContainerNode)getNode()).changeDirectionAngle(Integer.parseInt(direction), false);
                        }
                    }

                    if (insertBelief.hasOpenAlternatives()) {
                        insertBelief = this.engine.solveNext();
                    } else {
                        insertBelief = null;
                    }
                }
            } catch (MalformedGoalException e) {
                System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " added_belief.");
            }

            // Gets removed beliefs
            try {
                SolveInfo removeBelief = this.engine.solve("retract(removed_belief(B)).");
                while (removeBelief != null && removeBelief.isSuccess()) {
                    this.beliefBaseChanges.put(removeBelief.getTerm("B"),"removed");
                    if (removeBelief.hasOpenAlternatives()) {
                        removeBelief = this.engine.solveNext();
                    } else {
                        removeBelief = null;
                    }
                }
            } catch (MalformedGoalException e) {
                System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " removed_belief.");
            }
        } catch (NoSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " addBelief/removeBelief.");
        } catch (UnknownVarException e) {
            System.err.println(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " addBelief/removeBelief.");
        } catch (NoMoreSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " addBelief/removeBelief.");
        }
    }

    /**
     * Triggers events in the agent for changes to the belief base.
     */
    private void notifyBeliefBaseChanges() {
        // For each copule of the map trigger plan 'onAddBelief(term)' or 'onRemoveBelief(term)' depending by the value of str
        this.beliefBaseChanges.forEach((term, str) -> {
            if(str.equals("added")) {
                try {
                    final SolveInfo onAdd = this.engine.solve("onAddBelief(" + term + ").");
                    if (onAdd.isSuccess()) {
                        System.out.println(this.getAgentName() + SEPARATOR + TRIGGERED_PLAN + " onAddBelief(" + term + ").");
                    } else {
                        System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " onAddBelief(" + term + ").");
                    }
                } catch (MalformedGoalException e) {
                    System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " onAddBelief.");
                }
            } else {
                try {
                    final SolveInfo onRemove = this.engine.solve("onRemoveBelief(" + term + ").");
                    if (onRemove.isSuccess()) {
                        System.out.println(this.getAgentName() + SEPARATOR + TRIGGERED_PLAN + " onRemoveBelief(" + term + ").");
                    } else {
                        System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " onRemoveBelief(" + term + ").");
                    }
                } catch (MalformedGoalException e) {
                    System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " onRemoveBelief.");
                }
            }
        });
        this.beliefBaseChanges.clear();
    }

    /**
     * Updates node location, notifies update and then overwrites distances from other agents.
     */
    private void positionUpdate() {
        ((AgentsContainerNode) getNode()).changeNodePosition(this.agentReaction.getTau());

        // Updates the belief 'position' in the agent
        try {
            final SolveInfo oldPosition = this.engine.solve("retract(belief(position(X,Y))).");
            if (oldPosition.isSuccess()) {
                final Position nodePosition = ((AgentsContainerNode)getNode()).getNodePosition();
                final Struct positionBelief = new Struct(
                        "position",
                        Term.createTerm(Double.toString(nodePosition.getCoordinate(0))),
                        Term.createTerm(Double.toString(nodePosition.getCoordinate(1))));
                final Struct newPosition = new Struct("belief",positionBelief);

                this.engine.getTheoryManager().assertA(newPosition, true, null, false);
                // Adds the position updating as a add belief notification
                this.beliefBaseChanges.put(positionBelief,"added");

                // Updates beliefs with the distance to other agents
                this.agentsDistancesUpdate();
            } else {
                System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " position belief.");
            }
        } catch (MalformedGoalException e) {
            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " position belief.");
        }
    }

    /**
     * Updates distance from agent's node to all other agents
     */
    private void agentsDistancesUpdate() {
        // Removes all previews distances
        try {
            SolveInfo removeDitancesBeliefs = this.engine.solve("retract(belief(distance(_,_))).");
            while (removeDitancesBeliefs != null && removeDitancesBeliefs.isSuccess()) {
                if (removeDitancesBeliefs.hasOpenAlternatives()) {
                    removeDitancesBeliefs = this.engine.solveNext();
                } else {
                    removeDitancesBeliefs = null;
                }
            }
        } catch (MalformedGoalException e) {
            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " to remove agents distances.");
        } catch (NoMoreSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " to remove agents distances.");
        }

        // Inserts the distances updated and triggers notification
        ((AgentsContainerNode)getNode()).getNeighborhoodDistances(this.getAgentName()).forEach((strAgentName,distance) -> {

            final Struct distanceBelief = new Struct(
                    "distance",
                    Term.createTerm(strAgentName),
                    Term.createTerm(distance.toString()));
            final Struct newDistance = new Struct("belief", distanceBelief);

            this.engine.getTheoryManager().assertA(newDistance, true, null, false);
            // Adds the position updating as a add belief notification
            this.beliefBaseChanges.put(distanceBelief,"added");
        });
    }

    //------------------------------------------
    // Agent's external action
    //------------------------------------------

    /**
     * Takes outgoing messages to send, clears the outbox and then returns messages
     * @return List<OutMessage>
     */
    public List<OutMessage> consumeOutgoingMessages() {
        final List<OutMessage> outMessages = new ArrayList<>(this.outbox);
        this.outbox.clear();
        return outMessages;
    }

    /**
     * Add incoming message to the in mailbox
     * @param incomingMessage message to add
     */
    public void addIncomingMessage(final OutMessage incomingMessage) {
        this.inbox.add(new InMessage(incomingMessage.sender, incomingMessage.payload));
    }

    //------------------------------------------
    // Class of messages
    //------------------------------------------

    /**
     * Defines an incoming message
     */
    private static class InMessage {
        private final Term sender;
        // private final Term receiver;
        private final Term payload;

        /**
         * Constructor for incoming messages
         * @param sender sender of message
         * @param payload content of message
         */
        private InMessage(final Term sender, final Term payload) {
            this.sender = sender;
            this.payload = payload;
        }

        private Term getSender() {
            return sender;
        }

        private Term getPayload() {
            return payload;
        }
    }

    /**
     * Defines an outgoing message
     */
    public static class OutMessage {
        private final Term sender;
        private final Term receiver;
        private final Term payload;

        /**
         * Constructor for outgoing messages
         * @param sender sender of message
         * @param receiver receiver of message
         * @param payload content of message
         */
        private OutMessage(final Term sender, final Term receiver, final Term payload) {
            this.sender = sender;
            this.receiver = receiver;
            this.payload = payload;
        }

        public Term getSender() {
            return sender;
        }

        public Term getReceiver() {
            return receiver;
        }

        public Term getPayload() {
            return payload;
        }
    }
}