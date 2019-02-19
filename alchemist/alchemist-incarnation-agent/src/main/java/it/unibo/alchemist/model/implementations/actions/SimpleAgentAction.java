package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.*;
import it.unibo.alchemist.model.implementations.nodes.AgentsContainerNode;
import it.unibo.alchemist.model.interfaces.*;

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
    private final Map<Term,String> knowledgeBaseChanges = new LinkedHashMap<>();

    public SimpleAgentAction(final String name, final Node<Object> node) {
        super(node);
        this.agentName = name;

        // Loads the theory only if the agent isn't the postman
        if (!"postman".equals(this.agentName)) {
            try {
                this.engine.setTheory(new Theory(new FileInputStream(new File("alchemist-incarnation-agent/src/main/resources/" + BASE_THEORY + ".pl"))));
                this.engine.addTheory(new Theory(new FileInputStream(new File("alchemist-incarnation-agent/src/main/resources/" + getAgentName() + ".pl"))));
            } catch (IOException e) {
                System.err.println(getAgentName() + SEPARATOR + IO_MSG);
            } catch (InvalidTheoryException e) {
                System.err.println(getAgentName() + SEPARATOR + INVALID_THEORY_MSG);
            }
        }
    }

    @Override
    public Action<Object> cloneAction(final Node<Object> n, final Reaction<Object> r) {
        return new SimpleAgentAction("cloned_" + getAgentName(),getNode());
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

            System.out.println("Nodo: " + getNode().getId() + " || agent " + getAgentName() + " inizializzato");
            try {
                // Computes the init (e.g. In ping/pong problem send the first message).
                final SolveInfo init = this.engine.solve("init.");
                if (init.isSuccess()) {
                    System.out.println(getAgentName() + SEPARATOR + "init " + SUCCESS_PLAN);
                } else {
                    System.err.println(getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " init.");
                }
                this.hanldeOutGoingMessages();
            } catch (MalformedGoalException e) {
                System.err.println(getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " init.");
            }
        } else {
            //Agent's reasoning cycle

            // Handles incoming messages
            this.handleIncomingMessages();

            //if (getAgentName().equals("ping_agent"))
            // Updates node position (invokes plan 'onPositionUpdated')
            this.positionUpdate();

            // Notify Knowledge Base changes
            this.notifyKnowledgeBaseChanges();

            // Receives message (invokes plan 'onReceivedMessage(S,M)')
            this.getMessage();

            // Gets knowledge base changes
            this.getKnowledgeBaseChanges();

            // Handles outgoing messages
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
        // Prepares the Struct with the name of the agent
        final Struct self = new Struct("self", Term.createTerm(getAgentName()));

        // Sets the agent name with belief 'self'
        this.engine.getTheoryManager().assertA(self, true, null, false);

        final Position nodePosition = ((AgentsContainerNode)getNode()).getNodePosition();
        final Struct position = new Struct(
                "position",
                Term.createTerm(Double.toString(nodePosition.getCoordinate(0))),
                Term.createTerm(Double.toString(nodePosition.getCoordinate(1))));

        final Struct movement = new Struct(
                "movement",
                Term.createTerm(((AgentsContainerNode)getNode()).getNodeSpeed().toString()),
                Term.createTerm(((AgentsContainerNode)getNode()).getNodeDirectionAngle().toString()));

        // Sets the agent position
        this.engine.getTheoryManager().assertA(position, true, null, false);

        // Sets the agent movement
        this.engine.getTheoryManager().assertA(movement, true, null, false);

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
            System.err.println(getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " outgoing.");
        } catch (NoSolutionException e) {
            System.err.println(getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " outgoing.");
        } catch (UnknownVarException e) {
            System.err.println(getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " outgoing.");
        } catch (NoMoreSolutionException e) {
            System.err.println(getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " outgoing.");
        }
    }

    /**
     * Solve receive plan.
     */
    private void getMessage() {
        try {
            final SolveInfo receive = this.engine.solve("receiveMessage.");
            if (receive.isSuccess()) {
                System.out.println(getAgentName() + SEPARATOR + "receiveMessage " + SUCCESS_PLAN + SEPARATOR + this.agentReaction.getTau().toDouble());
            }
        } catch (MalformedGoalException e) {
            System.err.println(getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " receiveMessage.");
        }
    }

    /**
     * Takes changes to the knowledge base occurred in the reasoning cycle.
     */
    private void getKnowledgeBaseChanges() {
        try {
            // Gets added beliefs
            try {
                SolveInfo insertBelief = this.engine.solve("retract(added_belief(B)).");
                while (insertBelief != null && insertBelief.isSuccess()) {
                    this.knowledgeBaseChanges.put(insertBelief.getTerm("B"),"added");
                    if (insertBelief.hasOpenAlternatives()) {
                        insertBelief = this.engine.solveNext();
                    } else {
                        insertBelief = null;
                    }
                }
            } catch (MalformedGoalException e) {
                System.err.println(getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " added_belief.");
            }

            // Gets removed beliefs
            try {
                SolveInfo removeBelief = this.engine.solve("retract(removed_belief(B)).");
                while (removeBelief != null && removeBelief.isSuccess()) {
                    this.knowledgeBaseChanges.put(removeBelief.getTerm("B"),"removed");
                    if (removeBelief.hasOpenAlternatives()) {
                        removeBelief = this.engine.solveNext();
                    } else {
                        removeBelief = null;
                    }
                }
            } catch (MalformedGoalException e) {
                System.err.println(getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " removed_belief.");
            }
        } catch (NoSolutionException e) {
            System.err.println(getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " addBelief/removeBelief.");
        } catch (UnknownVarException e) {
            System.err.println(getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " addBelief/removeBelief.");
        } catch (NoMoreSolutionException e) {
            System.err.println(getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " addBelief/removeBelief.");
        }
    }

    /**
     * Triggers events in the agent for changes to the knowledge base.
     */
    private void notifyKnowledgeBaseChanges() {
        this.knowledgeBaseChanges.forEach((term, str) -> {
            if(str.equals("added")) {
                try {
                    final SolveInfo onAdd = this.engine.solve("onAddBelief(" + term + ").");
                    if (onAdd.isSuccess()) {
                        System.out.println(getAgentName() + SEPARATOR + TRIGGERED_PLAN + " onAddBelief(" + term + ").");
                    } else {
                        System.err.println(getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " onAddBelief.");
                    }
                } catch (MalformedGoalException e) {
                    System.err.println(getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " onAddBelief.");
                }
            } else {
                try {
                    final SolveInfo onRemove = this.engine.solve("onRemoveBelief(" + term + ").");
                    if (onRemove.isSuccess()) {
                        System.out.println(getAgentName() + SEPARATOR + TRIGGERED_PLAN + " onRemoveBelief(" + term + ").");
                    } else {
                        System.err.println(getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " onRemoveBelief.");
                    }
                } catch (MalformedGoalException e) {
                    System.err.println(getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " onRemoveBelief.");
                }
            }
        });
        this.knowledgeBaseChanges.clear();
    }

    private void positionUpdate() {
        // Before changing speed is necessary to consolidate the position of the node
        ((AgentsContainerNode) getNode()).changeNodePosition(this.agentReaction.getTau());

        // Updates the belief 'position' in the agent
        try {
            final SolveInfo oldPosition = this.engine.solve("retract(position(X,Y)).");
            if (oldPosition.isSuccess()) {
                final Position nodePosition = ((AgentsContainerNode)getNode()).getNodePosition();
                final Struct newPosition = new Struct(
                        "position",
                        Term.createTerm(Double.toString(nodePosition.getCoordinate(0))),
                        Term.createTerm(Double.toString(nodePosition.getCoordinate(1))));
                this.engine.getTheoryManager().assertA(newPosition, true, null, false);
            } else {
                System.err.println(getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " position.");
            }
        } catch (MalformedGoalException e) {
            System.err.println(getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " position.");
        }

        // Invokes the callback implemented in the agent
        try {
            final SolveInfo positionUpdated = this.engine.solve("onPositionUpdated.");
            if (positionUpdated.isSuccess()) {
                System.out.println(getAgentName() + SEPARATOR + TRIGGERED_PLAN + " onPositionUpdated.");
            } else {
                System.err.println(getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " onPositionUpdated.");
            }
        } catch (MalformedGoalException e) {
            System.err.println(getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " onPositionUpdated.");
        }

        /*/ TEST -----------------------------//
        try {
            final SolveInfo pos = this.engine.solve("position(X,Y).");
            if (pos.isSuccess()) {
                System.out.println("POSITION: " + pos.getTerm("X") + ", " + pos.getTerm("Y"));
            } else {
                System.err.println("No position " + getAgentName());
            }
        } catch (MalformedGoalException e) {
            System.err.println("Malformed goal 'position' for the agent " + getAgentName());
        } catch (NoSolutionException e) {
            System.err.println("No solution for goal 'position' for the agent " + getAgentName());
        } catch (UnknownVarException e) {
            System.err.println("Error retrieve term for goal 'position' for the agent " + getAgentName());
        }
        // TEST -----------------------------/*/

        // Retrieves changes of speed and direction from the agent
        try {
            final SolveInfo movement = this.engine.solve("movement(S,D).");
            ((AgentsContainerNode)getNode()).changeDirectionAngle(Integer.parseInt(movement.getTerm("D").toString()), false);
            ((AgentsContainerNode)getNode()).changeNodeSpeed(Double.parseDouble(movement.getTerm("S").toString()));
        } catch (MalformedGoalException e) {
            System.err.println(getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " movement.");
        } catch (NoSolutionException e) {
            System.err.println(getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " movement.");
        } catch (UnknownVarException e) {
            System.err.println(getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " movement.");
        }
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
    public void addIncomingMessages(final OutMessage incomingMessage) {
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