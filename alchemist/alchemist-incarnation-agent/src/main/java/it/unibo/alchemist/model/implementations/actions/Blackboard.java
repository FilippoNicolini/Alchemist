package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.Double;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Var;
import alice.tuprolog.UnknownVarException;
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule;
import it.unibo.alchemist.model.interfaces.Action;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Position;
import it.unibo.alchemist.model.interfaces.Reaction;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Implementation of a blackboard for tuple space extension.
 * Used by agents for 'write', 'read', 'take' messages.
 */
public class Blackboard extends AbstractSpatialTuple {

    /**
     * Constructor for blackboard.
     * @param blackboardName name of the blackboard.
     * @param node node where the agent is placed.
     * @param rand random generator.
     */
    public Blackboard(final String blackboardName, final Node<Object> node, final RandomGenerator rand) {
        super(blackboardName, node, rand);

        node.setConcentration(new SimpleMolecule(blackboardName), 0);
    }

    /**
     * Clone the action.
     * @param node the node which the action is referred.
     * @param reaction the reaction which the action is referred.
     * @return a new Blackboard.
     */
    @Override
    public Action<Object> cloneAction(final Node<Object> node, final Reaction<Object> reaction) {
        return new Blackboard("cloned_" + this.getAgentName(), node, this.getAgentRandomGenerator());
    }

    /**
     * Execute the Blackboard reasoning cycle.
     */
    @Override
    public void execute() {
        if (!this.isInitialized()) {
            this.initializeAgent();

            System.out.println("Nodo: " + this.getNode().getId() + " || agent " + this.getAgentName() + " inizializzato");

            this.initReasoning();
        } else {

            this.handleIncomingRequests();

            this.handlePendingRequests();
        }
    }


    //*********************************************//
    //**         Agent's internal action         **//
    //*********************************************//

    /**
     * Insert a term into the theory.
     * @param request object that contains the term to write into the theory.
     */
    @Override
    protected void writeOnSpatialTuple(final Request request) {
        final SolveInfo matchBreadcrumb = this.getEngine().solve(new Struct(
                "=",
                request.getTemplate(),
                new Struct(
                        "breadcrumb",
                        Term.createTerm(request.getAgent().getAgentName()),
                        Term.createTerm("here"))));
        // if the tuple to write is breadcrumb it is added only the first time.
        if (matchBreadcrumb.isSuccess()) {
            final SolveInfo checkIfPresent = this.getEngine().solve(request.getTemplate());
            if (!checkIfPresent.isSuccess()) {
                final SolveInfo msgToWrite = this.getEngine().solve(new Struct("assertz", request.getTemplate()));
                if (msgToWrite.isSuccess()) {
                    this.getNode().setConcentration(new SimpleMolecule("breadcrumb"), 0);
                    this.handlePendingRequests();
                } else {
                    throw new IllegalArgumentException("Malformed template to write on blackboard");
                }
            }
        } else {
            final SolveInfo msgToWrite = this.getEngine().solve(new Struct("assertz", request.getTemplate()));
            if (msgToWrite.isSuccess()) {
                this.handlePendingRequests();
            } else {
                throw new IllegalArgumentException("Malformed template to write on blackboard");
            }
        }
    }

    /**
     * Try to read a template from the theory. If it has success return the solution to the agent.
     * @param request object that contains the template to read from the theory.
     * @param isPending boolean that indicate if the request object is taken from pending queue.
     */
    protected void readFromSpatialTuple(final Request request, final boolean isPending) {
        try {
            final SolveInfo verifyMatch = this.getEngine().solve(request.getTemplate());
            if (verifyMatch.isSuccess()) {
                if (isPending) {
                    this.removeFromPendingQueue(request);
                }
                request.getAgent().addResponseMessage(new Struct(
                        "msg",
                        verifyMatch.getSolution(),
                        new Double(getNode().getNodePosition().getCoordinate(0)),
                        new Double(getNode().getNodePosition().getCoordinate(1))));
            } else {
                if (!isPending) {
                    // System.out.println("ADD TO PENDING || " + request.getAction() + " || " + request.getTemplate());
                    this.addToPendingQueue(request);
                }
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " read on blackboard.");
        }
    }

    /**
     * Try to take a template from the theory. If it has success (a term is removed from the theory) return the solution to the agent.
     * @param request object that contains the template to read from the theory.
     * @param isPending boolean that indicate if the request object is taken from pending queue.
     */
    @Override
    protected void takeFromSpatialTuple(final Request request, final boolean isPending) {
        try {
            final SolveInfo verifyMatch = this.getEngine().solve(new Struct("retract", request.getTemplate()));
            if (verifyMatch.isSuccess()) {
                if (isPending) {
                    this.removeFromPendingQueue(request);
                }
                try {
                    final SolveInfo match = this.getEngine().solve(new Struct(
                            "=",
                            verifyMatch.getSolution(),
                            new Struct(
                                    "retract",
                                    new Var("X"))));
                    if (match.isSuccess()) {
                        final SolveInfo matchBreadcrumb = this.getEngine().solve(new Struct(
                                "=",
                                request.getTemplate(),
                                new Struct(
                                        "breadcrumb",
                                        Term.createTerm("hansel"),
                                        Term.createTerm("here"))));
                        if (matchBreadcrumb.isSuccess() && getNode().contains(new SimpleMolecule("breadcrumb"))) {
                            this.getNode().removeConcentration(new SimpleMolecule("breadcrumb"));
                        }
                        final Term term = match.getTerm("X");
                        final Position nodePosition = getNode().getNodePosition();
                        request.getAgent().addResponseMessage(new Struct(
                                "msg",
                                term,
                                new Double(nodePosition.getCoordinate(0)),
                                new Double(nodePosition.getCoordinate(1))));
                    }
                } catch (UnknownVarException e) {
                    throw new IllegalStateException(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " take on blackboard.");
                }

            } else {
                if (!isPending) {
                    // System.out.println("ADD TO PENDING || " + request.getAction() + " || " + request.getTemplate());
                    this.addToPendingQueue(request);
                }
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " take on blackboard.");
        }
    }
}
