package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.Library;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;
import alice.tuprolog.UnknownVarException;
import alice.tuprolog.Var;
import alice.tuprolog.lib.InvalidObjectIdException;
import alice.tuprolog.lib.OOLibrary;
import it.unibo.alchemist.model.implementations.nodes.AgentsContainerNode;
import it.unibo.alchemist.model.interfaces.Context;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Position;
import it.unibo.alchemist.model.interfaces.Reaction;
import kotlin.Triple;
import org.apache.commons.math3.distribution.LevyDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

/**
 * Abstract definition of agent. Contains methods for managing the agent reasoning cycle.
 * Also implemented methods for managing tuple space.
 */
public abstract class AbstractAgent extends AbstractAction<Object> {

    private static final String BASE_THEORY = "agent_library";

    // Messages
    /**
     * Message for IO.
     */
    protected static final String IO_MSG = "Failed reading agent's theory file.";
    /**
     * Message for invalid theory.
     */
    protected static final String INVALID_THEORY_MSG = "Theory not valid.";
    /**
     * Message for malformed goal.
     */
    protected static final String MALFORMED_GOAL_MSG = "Malformed goal";
    /**
     * Message for no solution in tuProlog.
     */
    protected static final String NO_SOLUTION_MSG = "No solution for ";
    /**
     * Message for unknown var in tuProlog.
     */
    protected static final String UNKNOWN_VAR_MSG = "Error retrieving the term in the solution of ";
    /**
     * Message for no more solution in tuProlog.
     */
    protected static final String NO_MORE_SOLUTION_MSG = "Error retrieving next solution of ";
    /**
     * Separator for the messages.
     */
    protected static final String SEPARATOR = " || ";
    /**
     * Message for no implementation found on the theory.
     */
    protected static final String NO_IMPLEMENTATION_FOUND = "No implementation found for ";
    /**
     * Message for invalid object to refer in the theory.
     */
    protected static final String INVALID_OBJECT_MSG = "Invalid object to register into tuProlog engine";
    /**
     * Message for error executing intention.
     */
    protected static final String ERR_EXECUTING_INTENTION = "Error executing intention ";
    /**
     * Message for error inserting intention.
     */
    protected static final String ERR_INSERTING_INTENTION = "Error inserting intention ";
    /**
     * Message for success on removing intention.
     */
    protected static final String SUCCESS_REMOVE_INTENTION = "Intention removed from the intentions stack ";
    /**
     * Message for fail on removing intention.
     */
    protected static final String FAILED_REMOVE_INTENTION = "Failed removing intention from the intentions stack ";
    /**
     * Message for error when intention does not exists.
     */
    protected static final String INTENTION_NOT_EXISTS = "Agent not contains intention ";
    /**
     * Message for error when internal action is not recognized.
     */
    protected static final String INTERAL_ACTION_NOT_RECOGNIZED = "Not recognize internal action ";

    // List of triple used for the Spatial Tuple extension to reuse code
    private final List<Triple<Struct, String, String>> tripleListForTuples = new ArrayList<>();

    private final String agentName; // String for the agent name
    private boolean isInitialized = false; // Flag for init of the agent
    private final Queue<InMessage> inbox = new LinkedList<>(); // Mailbox IN queue
    private final Queue<OutMessage> outbox = new LinkedList<>(); // Mailbox OUT queue
    private final Prolog engine = new Prolog(); // tuProlog engine
    private final Library lib = this.engine.getLibrary("alice.tuprolog.lib.OOLibrary");
    private Reaction<Object> agentReaction; // Reference to reaction
    private RandomGenerator agentRandomGenerator; // Random generator of the reaction
    private LevyDistribution levyDistribution; // Distribution to be used with the random generator
    private Queue<String> intentionsStack = new LinkedList<>(); // Stack that contains intentions ID

    /**
     * Constructor for abstract agent.
     * @param name agent name.
     * @param node node where the agent is placed.
     * @param rand random generator.
     */
    protected AbstractAgent(final String name, final Node<Object> node, final RandomGenerator rand) {
        super(node);
        this.agentName = name;
        this.agentRandomGenerator = rand;
    }

    /**
     * Constructor for abstract agent.
     * @param name agent name.
     * @param node node where the agent is placed.
     * @param rand random generator.
     * @param reaction reaction of the agent.
     */
    protected AbstractAgent(final String name, final Node<Object> node, final RandomGenerator rand, final Reaction<Object> reaction) {
        super(node);
        this.agentName = name;
        this.agentRandomGenerator = rand;
        this.agentReaction = reaction;
    }

    /**
     * Get the name of the agent.
     * @return agent name.
     */
    public String getAgentName() {
        return this.agentName;
    }

    /**
     * Get the engine of the agent.
     * @return tuProlog engine.
     */
    protected Prolog getEngine() {
        return this.engine;
    }

    /**
     * Set flag initialized to true for the agent.
     */
    private void setInitialized() {
        this.isInitialized = true;
    }

    /**
     * Get flag initialized.
     * @return true if the agent is initialized.
     */
    protected boolean isInitialized() {
        return this.isInitialized;
    }

    /**
     * Get status of inbox.
     * @return true if inbox is empty.
     */
    private boolean inboxIsEmpty() {
        return this.inbox.size() == 0;
    }

    /**
     * Get the status of the intentionsStack.
     * @return true if the stack is empty
     */
    private boolean intentionsStackIsEmpty() {
        return this.intentionsStack.size() == 0;
    }

    /**
     * Get the random generator of the agent.
     * @return random generator
     */
    protected RandomGenerator getAgentRandomGenerator() {
        return this.agentRandomGenerator;
    }

    /**
     * Get the reaction of the agent.
     * @return reaction
     */
    protected Reaction<Object> getAgentReaction() {
        return this.agentReaction;
    }

    /**
     * Get the node of the action.
     * @return the node.
     */
    @Override
    public AgentsContainerNode getNode() {
        return (AgentsContainerNode) super.getNode();
    }

    /**
     * Get the context of the action.
     * @return the context.
     */
    @Override
    public Context getContext() {
        return Context.NEIGHBORHOOD; // TODO va bene come profondità?
    }

    //*********************************************//
    //**           Agent's system action         **//
    //*********************************************//

    /**
     * Load the tuProlog theory for the agent which contains the basic predicates and also add into it the Java Object References for this class and for the node.
     */
    private void loadAgentLibrary() {
        try {
            this.engine.addTheory(new Theory(Files.newInputStream(Paths.get("alchemist-incarnation-agent/src/main/resources/" + BASE_THEORY + ".pl"))));
        } catch (IOException e) {
            throw new IllegalArgumentException(this.getAgentName() + SEPARATOR + IO_MSG);
        } catch (InvalidTheoryException e) {
            throw new IllegalArgumentException(this.getAgentName() + SEPARATOR + INVALID_THEORY_MSG);
        }

        this.levyDistribution = new LevyDistribution(this.agentRandomGenerator, 0, 0.5); // Initialize the object for the levy distribution

        try {
            ((OOLibrary) this.lib).register(new Struct("agent"), this); // Object reference for internal actions
            ((OOLibrary) this.lib).register(new Struct("node"), this.getNode()); // Object reference for external actions
        } catch (InvalidObjectIdException e) {
            throw new IllegalArgumentException(this.getAgentName() + SEPARATOR + INVALID_OBJECT_MSG);
        }

        try {
            this.getEngine().addTheory(new Theory(Files.newInputStream(Paths.get("alchemist-incarnation-agent/src/main/resources/" + this.getAgentName() + ".pl"))));
        } catch (IOException e) {
            throw new IllegalArgumentException(this.getAgentName() + SEPARATOR + IO_MSG);
        } catch (InvalidTheoryException e) {
            throw new IllegalArgumentException(this.getAgentName() + SEPARATOR + INVALID_THEORY_MSG);
        }
    }

    /**
     * Initialize the agent theory adding some beliefs into it.
     */
    protected void initializeAgent() {
        this.loadAgentLibrary();

        // Name of the agent
        final Struct self = new Struct("self", Term.createTerm(this.getAgentName()));

        // Set 'self' in the theory of the agent
        this.engine.getTheoryManager().assertA(self, true, null, false);

        // Prepare list to retrieve tuples
        this.tripleListForTuples.add(new Triple<>(
                new Struct("retract",
                    new Struct("write", new Var("T"))), "write", "writeTuple"));
        this.tripleListForTuples.add(new Triple<>(
                new Struct("retract",
                    new Struct("read", new Var("T"))), "read", "readTuple"));
        this.tripleListForTuples.add(new Triple<>(
                new Struct("retract",
                    new Struct("take", new Var("T"))), "take", "takeTuple"));

        // Update the initialization flag
        this.setInitialized();
    }

    /**
     * Invoke the init plan defined in the agent theory for the first reasoning.
     */
    protected void initReasoning() {
        // Solve init plan if present
        final SolveInfo init = this.getEngine().solve(new Struct("init"));
        if (!init.isSuccess()) {
            System.err.println(this.getAgentName() + this.getNode().getId() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " init.");
        }
//        else {
//            // System.out.println(this.getAgentName() + SEPARATOR + "init " + SUCCESS_PLAN);
//        }
    }

    /**
     * Create an intention to handle the message read.
     */
    protected void readMessage() {
        if (!this.inboxIsEmpty()) {
            final InMessage msg = this.inbox.poll();
            if (Objects.nonNull(msg)) {
                // Search the predicate in the theory
                final Struct receivedMessage = new Struct("onReceivedMessage", msg.getSender(), msg.getPayload());

                final Struct checkClause = new Struct("clause", receivedMessage, new Var("Body"));
                this.createIntention(checkClause, " onReceivedMessage.");
                System.out.println("LOG" + SEPARATOR + getAgentName() + this.getNode().getId() + SEPARATOR + "MESSAGE READ");
            }
        }
    }

    /**
     * Recover added or removed beliefs and then trigger the related plan.
     */
    protected void beliefBaseChanges() {
        try {
            // Get removed beliefs
            final Struct removedBeliefPredicate = new Struct("retract",
                    new Struct("removed_belief", new Var("B")));
            SolveInfo solveRemovedBelief = this.engine.solve(removedBeliefPredicate);
            while (solveRemovedBelief != null && solveRemovedBelief.isSuccess()) {
                /*********************ELIMINARE**************************/
                final String str = solveRemovedBelief.getTerm("B").toString();
                System.out.println("////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////removed:" + str);
                /*********************ELIMINARE**************************/

                // Search the predicate in the theory
                final Struct removedBeliefNotify = new Struct("onRemoveBelief", solveRemovedBelief.getTerm("B"));
                final Struct checkClause = new Struct("clause", removedBeliefNotify, new Var("Body"));
                this.createIntention(checkClause, " onRemoveBelief.");

                if (solveRemovedBelief.hasOpenAlternatives()) {
                    solveRemovedBelief = this.engine.solve(removedBeliefPredicate);
                } else {
                    solveRemovedBelief = null;
                }
            }

            // Get added beliefs
            final Struct addedBeliefPredicate = new Struct("retract",
                    new Struct("added_belief", new Var("B")));
            SolveInfo solveAddedBelief = this.engine.solve(addedBeliefPredicate);
            while (solveAddedBelief != null && solveAddedBelief.isSuccess()) {
                /*********************ELIMINARE**************************/
                final String str = solveAddedBelief.getTerm("B").toString();
                System.out.println("////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////added:" + str);
                /*********************ELIMINARE**************************/

                // Search the predicate in the theory
                final Struct addedBeliefNotify = new Struct("onAddBelief", solveAddedBelief.getTerm("B"));
                final Struct checkClause = new Struct("clause", addedBeliefNotify, new Var("Body"));
                this.createIntention(checkClause, " onAddBelief.");

                if (solveAddedBelief.hasOpenAlternatives()) {
                    solveAddedBelief = this.engine.solve(addedBeliefPredicate);
                } else {
                    solveAddedBelief = null;
                }
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + NO_SOLUTION_MSG + " belief base changes.");
        } catch (UnknownVarException e) {
            throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + UNKNOWN_VAR_MSG + " belief base changes.");
        }
    }

    /**
     * Create and add the intention to the agent intentions stack.
     * @param checkClause clause containing the body of the plan
     * @param errorMessage string to concat to the exception message
     */
    private void createIntention(final Struct checkClause, final String errorMessage) {
        try {
            SolveInfo solveCheckClause = this.engine.solve(checkClause);
            if (solveCheckClause.isSuccess()) {
                final List<Struct> unfoldedBodyList = new ArrayList<>();

                // Scan all callable plans
                while (solveCheckClause != null && solveCheckClause.isSuccess()) {
                    final Struct unfoldBody = new Struct("unfold", solveCheckClause.getTerm("Body"), new Var("BodyList"));

                    unfoldedBodyList.add(unfoldBody);

                    if (solveCheckClause.hasOpenAlternatives()) {
                        solveCheckClause = this.engine.solveNext();
                    } else {
                        solveCheckClause = null;
                    }
                }

                for (final Struct ub : unfoldedBodyList) {
                    final SolveInfo solveUB = this.engine.solve(ub);
                    // Generate the random id for the intention
                    final Term id = new alice.tuprolog.Double(this.agentRandomGenerator.nextDouble()); // TODO verificare univocità id
                    // Add intention to the theory
                    final Struct intention = new Struct("intention", id, solveUB.getTerm("BodyList"));
                    final SolveInfo solveIntention = this.engine.solve(new Struct("assertz", intention));

                    if (solveIntention.isSuccess()) {
                        // Add id to the stack
                        this.intentionsStack.add(id.toString());

                        // TODO remove log
                        if (!AbstractSpatialTuple.class.isAssignableFrom(this.getClass()) && !getAgentName().equals("deposit")) {
                            System.out.println("LOG(stack size: " + this.intentionsStack.size() + ")" + SEPARATOR + getAgentName() + this.getNode().getId() + SEPARATOR + intention);
                        }
                    } else {
                        System.err.println(this.getAgentName() + this.getNode().getId() + SEPARATOR + ERR_INSERTING_INTENTION + intention);
                    }
                }
            } else {
                System.err.println(this.getAgentName() + this.getNode().getId() + SEPARATOR + NO_IMPLEMENTATION_FOUND + errorMessage  + SEPARATOR + checkClause);
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + NO_SOLUTION_MSG + errorMessage);
        } catch (UnknownVarException e) {
            throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + UNKNOWN_VAR_MSG + errorMessage);
        } catch (NoMoreSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + NO_MORE_SOLUTION_MSG + errorMessage);
        }
    }

    /**
     * Choose the intention and then executes it. The choice is made with round-robin.
     * The intention selected is removed from the head and then added in the back.
     */
    protected void executeIntention() {
        if (!this.intentionsStackIsEmpty()) {
            // Remove an intention from the head and push it to the back (Round-Robin)
            final String id = this.intentionsStack.poll();
            final Term intentionID = Term.createTerm(Objects.requireNonNull(id));
            this.intentionsStack.add(id);

            try {
                final SolveInfo s = this.engine.solve(new Struct("intention", intentionID, new Var("X")));
                final Struct execIntention = new Struct("execute", intentionID);
                final SolveInfo solveExecIntention = this.engine.solve(execIntention);
                if (!solveExecIntention.isSuccess()) {
                    System.err.println(this.getAgentName() + SEPARATOR + ERR_EXECUTING_INTENTION + execIntention);
                    this.removeCompletedIntention(id);
                    if (s.isSuccess()) {
                        System.err.println("ERRORE || id intenzione: " + id + SEPARATOR + " array predicati da esguire " + s.getTerm("X"));
                    }
                } else {
                    System.out.println(this.getAgentName() + this.getNode().getId() + SEPARATOR + "esegita intenzione " + execIntention);
                }
            } catch (NoSolutionException e) {
                throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + NO_SOLUTION_MSG + "intention " + id);
            } catch (UnknownVarException e) {
                throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + UNKNOWN_VAR_MSG + "intention " + id);
            }
        }
    }

    //*********************************************//
    //**      Agent's action from the theory     **//
    //*********************************************//

    /**
     * Generate an id for the intention created in the theory.
     * @return the identifier
     */
    public long generateIntentionId() { // TODO non usato, verificare le teorie
        return new Date().getTime();
    }

    /**
     * Insert the intention id into the stack.
     * @param intentionID string of the id.
     */
    public void insertIntention(final String intentionID) {
        // TODO verificare se dalla teoria viene chiamato qui o i long
        System.out.println("Aggiunta intenzione allo stack dalla teoria (String)");
        // Add id to the stack
        this.intentionsStack.add(intentionID);
    }

    /**
     * Insert the intention id into the stack.
     * @param intentionID long of the id.
     */
    public void insertIntention(final long intentionID) {
        // TODO verificare se dalla teoria viene chiamato qui o le stringhe
        System.out.println("Aggiunta intenzione allo stack dalla teoria (long)");
        // Add id to the stack
        this.intentionsStack.add(intentionID + "");
    }

    /**
     * Remove a completed intention from the stack.
     * @param intentionID identifier of the intention to remove
     */
    public void removeCompletedIntention(final double intentionID) {
        if (this.intentionsStack.contains(intentionID + "")) {
            this.intentionsStack.remove(intentionID + "");

            final Struct removeCompletedIntention = new Struct("retract", new Struct("intention", new alice.tuprolog.Double(intentionID), new Var("X")));
            final SolveInfo solveRemovedCompletedIntention = this.engine.solve(removeCompletedIntention);
            if (solveRemovedCompletedIntention.isSuccess()) {
                System.out.println(this.getAgentName() + this.getNode().getId() + SEPARATOR + SUCCESS_REMOVE_INTENTION + intentionID);
            } else {
                System.err.println(this.getAgentName() + this.getNode().getId() + SEPARATOR + FAILED_REMOVE_INTENTION + intentionID);
            }
        } else {
            System.err.println(this.getAgentName() + this.getNode().getId() + SEPARATOR + INTENTION_NOT_EXISTS + intentionID);
        }
    }

    /**
     * Overload of removeCompletedIntention with string value.
     * @param intentionID string of the id.
     */
    public void removeCompletedIntention(final String intentionID) {
        removeCompletedIntention(Double.parseDouble(intentionID));
    }

    /**
     * Generate a random from the random generator.
     * @return the random generated.
     */
    public double generateNextRandom() {
        return this.agentRandomGenerator.nextDouble();
    }

    /**
     * Get the Levy distribution of a value.
     * @param value the double of the value to check
     * @return density of the value in the distribution
     */
    public double getLevyDistributionDensity(final double value) {
        return this.levyDistribution.density(value);
    }

    /**
     * Get the Levy distribution of a value.
     * @param value the string of the value to check
     * @return density of the value in the distribution
     */
    public double getLevyDistributionDensity(final String value) {
        return this.getLevyDistributionDensity(Double.parseDouble(value));
    }

    /**
     * Execute the selected action from the operation's stack of the intention.
     * @param action string of the action to execute
     */
    public void executeInternalAction(final String action) {
        System.out.println("executeInternalAction: " + action);
        try {
            SolveInfo match = this.engine.solve(new Struct("=", Term.createTerm(action), new Struct("iSend", new Var("R"), new Var("M"))));
            if (match.isSuccess()) {
                this.sendMessage(Term.createTerm(this.getAgentName()), match.getTerm("R"), match.getTerm("M"));
            } else {
                match = this.engine.solve(new Struct("=", Term.createTerm(action), new Struct("iPrint", new Var("M"))));
                if (match.isSuccess()) {
                    System.out.println(getAgentName() + this.getNode().getId() + SEPARATOR + "PRINT " + match.getTerm("M"));
                } else {
                    // TODO aggiungere altre eventuali azioni iterne
                    System.err.println(this.getAgentName() + this.getNode().getId() + SEPARATOR + INTERAL_ACTION_NOT_RECOGNIZED + action);
                }
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + NO_SOLUTION_MSG + " internal action " + action);
        } catch (UnknownVarException e) {
            throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + UNKNOWN_VAR_MSG + " internal action " + action);
        }
    }

    /**
     * Add a message to the outbox to be sent.
     * @param sender message sender
     * @param receiver message receiver
     * @param message message content
     */
    public void sendMessage(final Term sender, final Term receiver, final Term message) {
        this.outbox.add(new OutMessage(sender, receiver, message));
    }

//    // TODO eliminare
//    public void test(final Object msg) {
//        System.out.println("test: " + msg);
//    }


    //*********************************************//
    //**       Agent's action from the node      **//
    //*********************************************//

    /**
     * Takes outgoing messages to send, clears the outbox and then returns messages.
     * @return List of outgoing messages.
     */
    public List<OutMessage> consumeOutgoingMessages() {
        final List<OutMessage> outMessages = new ArrayList<>(this.outbox);
        this.outbox.clear();
        return outMessages;
    }

    /**
     * Add incoming message to the in mailbox.
     * @param incomingMessage message to add.
     */
    public void addIncomingMessage(final OutMessage incomingMessage) {
        this.inbox.add(new InMessage(incomingMessage.getSender(), incomingMessage.getPayload()));
    }

    /**
     * Update the belief relative to the position.
     * @param updatedPosition object containing the updated values of position
     */
    public void updateAgentPosition(final Position updatedPosition) {
        // Remove old position
        final Struct removeOldPosition = new Struct("retract",
                new Struct("belief",
                        new Struct("position", new Var("X"), new Var("Y"))));
        final SolveInfo solveRemoveOldPosition = this.engine.solve(removeOldPosition);
        if (!solveRemoveOldPosition.isSuccess()) {
            System.err.println(this.getAgentName() + this.getNode().getId() + SEPARATOR + "No belief found for position(X,Y)");
        }

        final Struct newPosition = new Struct("position",
                new alice.tuprolog.Double(updatedPosition.getCoordinate(0)),
                new alice.tuprolog.Double(updatedPosition.getCoordinate(1)));

        // Insert new position in the theory
        final Struct updatePositionBelief = new Struct("belief", newPosition);
        this.engine.getTheoryManager().assertA(updatePositionBelief, true, null, false);

        // Create the intention to notify the position update
        final Struct updatePositionNotify = new Struct("onAddBelief", newPosition);
        final Struct checkClause = new Struct("clause", updatePositionNotify, new Var("Body"));
        this.createIntention(checkClause, " update agent position.");
    }

    /**
     * Update the distance between this instance and the other agents.
     */
    public void updateAgentsDistances() {
        try {
            final Map<String, Double> newDistances = this.getNode().getNeighborhoodDistances();
            final Struct retrieveDistances = new Struct("retract",
                    new Struct("belief",
                            new Struct("distance", new Var("A"), new Var("D"))));
            SolveInfo solveRetrieveDistances = this.engine.solve(retrieveDistances);
            // Notify the update of the distances already present
            while (solveRetrieveDistances != null && solveRetrieveDistances.isSuccess()) {
                final Term agentName = solveRetrieveDistances.getTerm("A");
                if (newDistances.keySet().contains(agentName.toString())) { // if the agent of the distance just removed is still among the neighbors
                    // Add in the theory 'belief(distance(AGENT_NAME, NEW_DISTANCE)).'
                    final Struct distanceBelief = new Struct("belief",
                            new Struct("distance", agentName, new alice.tuprolog.Double(newDistances.get(agentName.toString()))));
                    this.engine.getTheoryManager().assertA(distanceBelief, true, null, false);

                    // Add intention of 'distance(AGENT_NAME, NEW_DISTANCE, OLD_DISTANCE).'
                    final Struct distanceUpdateNotify = new Struct("onAddBelief",
                            new Struct("distance", agentName, new alice.tuprolog.Double(newDistances.get(agentName.toString())), solveRetrieveDistances.getTerm("D")));
                    final Struct checkClause = new Struct("clause", distanceUpdateNotify, new Var("Body"));
                    this.createIntention(checkClause, " update agent distances.");

                    newDistances.remove(agentName.toString());
                }

                if (solveRetrieveDistances.hasOpenAlternatives()) {
                    solveRetrieveDistances = this.engine.solve(retrieveDistances);
                } else {
                    solveRetrieveDistances = null;
                }
            }

            // Add remaining distances
            if (!newDistances.isEmpty()) {
                newDistances.forEach((strAgentName, distance) -> {
                    final Term agentTerm = Term.createTerm(strAgentName);
                    // Add in the theory 'belief(distance(AGENT_NAME, NEW_DISTANCE)).'
                    final Struct distanceUpdate = new Struct("distance", agentTerm, new alice.tuprolog.Double(distance));
                    final Struct distanceBelief = new Struct("belief", distanceUpdate);
//                    System.out.println(getAgentName() + " has new agent in the neighborhood: " + strAgentName);
                    this.engine.getTheoryManager().assertA(distanceBelief, true, null, false);

                    // Add intention of 'distance(AGENT_NAME, NEW_DISTANCE).'
                    final Struct distanceUpdateNotify = new Struct("onAddBelief",
                            new Struct("distance", agentTerm, new alice.tuprolog.Double(newDistances.get(strAgentName))));
                    final Struct checkClause = new Struct("clause", distanceUpdateNotify, new Var("Body"));
                    this.createIntention(checkClause, " update agent distances.");

                });
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + NO_SOLUTION_MSG + " to update agent distances.");
        } catch (UnknownVarException e) {
            throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + UNKNOWN_VAR_MSG + " to update agent distances.");
        }
    }

    //*********************************************//
    //**        Spatial Tuples Extension         **//
    //*********************************************//

    /**
     * Allows the agent to interact with a tuple space using the 'write', 'read', 'take' actions.
     */
    protected void retrieveTuples() {
        this.tripleListForTuples.forEach(tt -> {
            try {
                // Tries to solve the goal retracting the beliefs of the tuples
                SolveInfo solvedAction = this.engine.solve(tt.getFirst());
                while (solvedAction != null && solvedAction.isSuccess()) {
                    if (tt.getSecond().equals("write")) { // if the action is write the request is sent to the nearest spatial tuple
                        // Retrieves the nearest spatial tuple instance.
                        final AbstractSpatialTuple spatialTuple = getNode().getNearestSpatialTuple();
                        if (Objects.nonNull(spatialTuple)) {
                            spatialTuple.insertRequest(solvedAction.getTerm("T"), this, tt.getSecond());
                        }
                    } else { // otherwise, for read and take actions the requests is sent to the neighborhood spatial tuple
                        // Retrieves the spatial tuple neighborhood instances.
                        final List<AbstractSpatialTuple> spatialTupleNeighborhood = getNode().getSpatialTupleNeighborhood();
                        for (final AbstractSpatialTuple spatialTuple: spatialTupleNeighborhood) {
                            spatialTuple.insertRequest(solvedAction.getTerm("T"), this, tt.getSecond());
                        }
                    }

                    if (solvedAction.hasOpenAlternatives()) {
                        solvedAction = this.engine.solveNext();
                    } else {
                        solvedAction = null;
                    }
                }
            } catch (NoSolutionException e) {
                throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + NO_SOLUTION_MSG + " " + tt.getThird() + ".");
            } catch (UnknownVarException e) {
                throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + UNKNOWN_VAR_MSG + " " + tt.getThird() + ".");
            } catch (NoMoreSolutionException e) {
                throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + NO_MORE_SOLUTION_MSG + " " + tt.getThird() + ".");
            }
        });
    }

    /**
     * Add event response to the belief base changes.
     * @param response the event to notify.
     */
    protected void addResponseMessage(final Term response) {
        // search the predicate in the theory
        final Struct responseMessageNotify = new Struct("onResponseMessage", response);
        System.out.println("LOG" + SEPARATOR + getAgentName() + this.getNode().getId() + " received message: " + response);
        final Struct checkClause = new Struct("clause", responseMessageNotify, new Var("Body"));
        this.createIntention(checkClause, " onResponseMessage.");
    }

    //*********************************************//
    //**            Class of messages            **//
    //*********************************************//

    /**
     * Defines an incoming message.
     */
    private final class InMessage {
        private final Term sender;
        private final Term payload;

        /**
         * Constructor for incoming messages.
         * @param sender sender of message.
         * @param payload content of message.
         */
        private InMessage(final Term sender, final Term payload) {
            this.sender = sender;
            this.payload = payload;
        }

        private Term getSender() {
            return this.sender;
        }

        private Term getPayload() {
            return this.payload;
        }
    }

    /**
     * Defines an outgoing message.
     */
    public final class OutMessage {
        private final Term sender;
        private final Term receiver;
        private final Term payload;

        /**
         * Constructor for outgoing messages.
         * @param sender sender of message.
         * @param receiver receiver of message.
         * @param payload content of message.
         */
        private OutMessage(final Term sender, final Term receiver, final Term payload) {
            this.sender = sender;
            this.receiver = receiver;
            this.payload = payload;
        }

        /**
         * Get the sender of the message.
         * @return a term indicating the sender.
         */
        public Term getSender() {
            return this.sender;
        }
        /**
         * Get the receiver of the message.
         * @return a term indicating the receiver.
         */
        public Term getReceiver() {
            return this.receiver;
        }
        /**
         * Get the payload of the message.
         * @return a term indicating the payload.
         */
        public Term getPayload() {
            return this.payload;
        }
    }
}
