package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.Double;
import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;
import alice.tuprolog.UnknownVarException;
import alice.tuprolog.Var;
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule;
import it.unibo.alchemist.model.interfaces.Action;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Position;
import it.unibo.alchemist.model.interfaces.Reaction;
import org.apache.commons.math3.random.RandomGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Implementation of tuple space for Goldminers problem.
 */
public class Goldmine extends AbstractSpatialTuple {

    /**
     * Constructor for a goldmine.
     * @param goldmineName name of the goldmine.
     * @param node node where the agent is placed.
     * @param rand random generator.
     */
    public Goldmine(final String goldmineName, final Node<Object> node, final RandomGenerator rand) {
        super(goldmineName, node, rand);

        node.setConcentration(new SimpleMolecule(goldmineName), 0);
    }

    /**
     * Clone the action.
     * @param node the node which the action is referred.
     * @param reaction the reaction which the action is referred.
     * @return a new Goldmine.
     */
    @Override
    public Action<Object> cloneAction(final Node<Object> node, final Reaction<Object> reaction) {
        return new Goldmine("cloned_" + this.getAgentName(), node, this.getAgentRandomGenerator());
    }

    /**
     * Execute the Goldmine reasoning cycle.
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
                        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++goldmine" + getNode().getId() + " TAKEN");

                        // check if contains others nuggets.
                        final SolveInfo checkNuggets = this.getEngine().solve(Term.createTerm("nugget"));
                        if (checkNuggets.isSuccess()) {
                            System.out.println(getNode().getId() + SEPARATOR + "CONTAINS OTHER nugget");
                            // if there are others nuggets and there's no concentration it is set.
                            if (!this.getNode().contains(new SimpleMolecule("nugget"))) {
                                this.getNode().setConcentration(new SimpleMolecule("nugget"), 0);
                            }
                        } else {
                            System.out.println(getNode().getId() + SEPARATOR + "NO MORE nugget");
                            // if there are no more nuggets and there's a concentration it is removed.
                            if (this.getNode().contains(new SimpleMolecule("nugget"))) {
                                this.getNode().removeConcentration(new SimpleMolecule("nugget"));
                            }
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
                    throw new IllegalStateException(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " take on goldmine.");
                }
            } else {
                if (!isPending) {
                    // System.out.println("ADD TO PENDING || " + request.getAction() + " || " + request.getTemplate());
                    this.addToPendingQueue(request);
                }
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " take on goldmine.");
        }
    }

    /**
     * Called from the theory when the initialization is completed. Used for the styling.
     */
    public void setConcentration() {
        try {
            SolveInfo test = this.getEngine().solve(Term.createTerm("nugget"));
            double counter = 0;
            while (test != null && test.isSuccess()) {
                counter++;
                if (test.hasOpenAlternatives()) {
                    test = this.getEngine().solveNext();
                } else {
                    test = null;
                }
            }
            System.out.println("GOLDMINE" + SEPARATOR + getAgentName() + getNode().getId() + SEPARATOR + "contained nuggets:" + counter);
        } catch (NoMoreSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + this.getNode().getId() + SEPARATOR + NO_MORE_SOLUTION_MSG + "nugget.");
        }
        this.getNode().setConcentration(new SimpleMolecule("nugget"), 0);
    }
}
