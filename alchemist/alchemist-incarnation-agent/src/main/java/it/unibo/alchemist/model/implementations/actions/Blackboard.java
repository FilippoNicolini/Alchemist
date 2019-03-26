package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.Double;
import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;
import alice.tuprolog.Var;
import alice.tuprolog.UnknownVarException;
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule;
import it.unibo.alchemist.model.interfaces.Action;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Position;
import it.unibo.alchemist.model.interfaces.Reaction;
import org.apache.commons.math3.random.RandomGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Implementation of a blackboard for tuple space extension.
 * Used by agents for 'write', 'read', 'take' messages.
 */
public class Blackboard extends AbstractAgent {

    private final List<Request> incomingRequests = new ArrayList<>();
    private final List<Request> pendingRequests = new ArrayList<>();

    /**
     * Constructor for blackboard.
     * @param blackboardName name of the blackboard.
     * @param node node where the agent is placed.
     */
    public Blackboard(final String blackboardName, final Node<Object> node, final RandomGenerator rand) {
        super(blackboardName, node, rand);

        node.setConcentration(new SimpleMolecule(blackboardName), 0);

        try {
            this.getEngine().addTheory(new Theory(new FileInputStream(new File("alchemist-incarnation-agent/src/main/resources/" + this.getAgentName() + ".pl"))));
        } catch (IOException e) {
            System.err.println(this.getAgentName() + SEPARATOR + IO_MSG);
        } catch (InvalidTheoryException e) {
            System.err.println(this.getAgentName() + SEPARATOR + INVALID_THEORY_MSG);
        }
    }

    @Override
    public Action<Object> cloneAction(final Node<Object> n, final Reaction<Object> r) {
        return new Blackboard("cloned_" + this.getAgentName(), getNode(), getRandomGenerator());
    }

    @Override
    public void execute() {
        if (!this.isInitialized()) {
            this.inizializeAgent();

            System.out.println("Nodo: " + getNode().getId() + " || agent " + this.getAgentName() + " inizializzato");

            this.initReasoning();
        } else {

            this.handleIncomingRequests();

            this.handlePendingRequests();
        }
    }


    //------------------------------------------
    // Agent's internal action
    //------------------------------------------

    /**
     * Handles new requests received from the blackboard.
     */
    private void handleIncomingRequests() {
        while (!(this.incomingRequests.size() == 0)) {
            final Request request = this.incomingRequests.remove(0);
            // System.out.println("REQUEST " + request.getAction() + " --> " + request.getTemplate());
            switch (request.getAction()) {
                case "write":
                    this.writeOnBlackboard(request);
                    break;
                case "read":
                    this.readFromBlackboard(request, false);
                    break;
                case "take":
                    this.takeFromBlackboard(request, false);
                    break;
            }
        }
    }

    /**
     * Iterates pending requests to try to solve them.
     */
    private void handlePendingRequests() {
        for (int i = 0; i < this.pendingRequests.size(); i++) {
            final Request currRequest = this.pendingRequests.get(i);
            switch (currRequest.getAction()) {
                case "read":
                    readFromBlackboard(currRequest, true);
                    break;
                case "take":
                    takeFromBlackboard(currRequest, true);
                    break;
            }
        }
    }

    /**
     * Write the request on the blackboard theory.
     * @param request the request to add.
     */
    private void writeOnBlackboard(final Request request) {
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
                    getNode().setConcentration(new SimpleMolecule("breadcrumb"), 0);
                    this.handlePendingRequests();
                } else {
                    System.err.println("Malformed template to write on blackboard");
                }
            }
        } else {
            final SolveInfo msgToWrite = this.getEngine().solve(new Struct("assertz", request.getTemplate()));
            if (msgToWrite.isSuccess()) {
                this.handlePendingRequests();
            } else {
                System.err.println("Malformed template to write on blackboard");
            }
        }
    }

    /**
     * Tries to read the request from blackboard theory. If has success return the solution to the agent.
     * @param request request to read.
     * @param isPending boolean to check if the request comes from the pending queue.
     */
    private void readFromBlackboard(final Request request, final boolean isPending) {
        try {
            final SolveInfo verifyMatch = this.getEngine().solve(request.getTemplate());
            if (verifyMatch.isSuccess()) {
                if (isPending) {
                    this.pendingRequests.remove(request);
                }
                request.getAgent().addResponseMessage(new Struct(
                        "msg",
                        verifyMatch.getSolution(),
                        new Double(getNode().getNodePosition().getCoordinate(0)),
                        new Double(getNode().getNodePosition().getCoordinate(1))));
            } else {
                if (!isPending) {
                    // System.out.println("ADD TO PENDING || " + request.getAction() + " || " + request.getTemplate());
                    this.pendingRequests.add(request);
                }
            }
        } catch (NoSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " to read on blackboard.");
        }
    }

    /**
     * Tries to take the request from blackboard theory. If has success remove the message from the theory and return the solution to the agent.
     * @param request request to take.
     * @param isPending boolean to check if the request comes from the pending queue.
     */
    private void takeFromBlackboard(final Request request, final boolean isPending) {
        try {
            final SolveInfo verifyMatch = this.getEngine().solve(new Struct("retract", request.getTemplate()));
            if (verifyMatch.isSuccess()) {
                if (isPending) {
                    this.pendingRequests.remove(request);
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
                            getNode().removeConcentration(new SimpleMolecule("breadcrumb"));
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
                    System.err.println(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " to match take on blackboard.");
                }

            } else {
                if (!isPending) {
                    // System.out.println("ADD TO PENDING || " + request.getAction() + " || " + request.getTemplate());
                    this.pendingRequests.add(request);
                }
            }
        } catch (NoSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " to take on blackboard.");
        }
    }


    //------------------------------------------
    // Agent's external action
    //------------------------------------------

    /**
     * Gets values and creates a request to put in the incoming queue.
     * @param template template to find (case of 'read' or 'take') or term to insert (case of 'write').
     * @param agent agent instance.
     * @param action action to performe ('write', 'read', 'take').
     */
    public void insertRequest(final Term template, final AbstractAgent agent, final String action) {
        this.incomingRequests.add(new Request(template, agent, action));
    }


    /**
     * Defines a waiting agent.
     */
    public final class Request {
        private final Term template;
        private final AbstractAgent agent;
        private final String action;

        /**
         * Constructor for the request.
         * @param template template to find (case of 'read' or 'take') or term to insert (case of 'write').
         * @param agent agent instance.
         * @param action action to performe ('write', 'read', 'take').
         */
        private Request(final Term template, final AbstractAgent agent, final String action) {
            this.template = template;
            this.agent = agent;
            this.action = action;
        }

        public Term getTemplate() {
            return this.template;
        }

        public AbstractAgent getAgent() {
            return this.agent;
        }

        public String getAction() {
            return this.action;
        }

    }
}
