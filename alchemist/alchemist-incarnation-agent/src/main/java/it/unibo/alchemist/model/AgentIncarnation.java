package it.unibo.alchemist.model;

import com.google.common.collect.Lists;
import it.unibo.alchemist.model.implementations.actions.SimpleAgentAction;
import it.unibo.alchemist.model.implementations.actions.PostmanAction;
import it.unibo.alchemist.model.implementations.conditions.AbstractCondition;
import it.unibo.alchemist.model.implementations.environments.Continuous2DEnvironment;
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule;
import it.unibo.alchemist.model.implementations.nodes.AgentsContainerNode;
import it.unibo.alchemist.model.implementations.reactions.AgentReaction;
import it.unibo.alchemist.model.implementations.timedistributions.DiracComb;
import it.unibo.alchemist.model.interfaces.*;

import org.apache.commons.math3.random.RandomGenerator;



public class AgentIncarnation<P extends Position<? extends P>> implements Incarnation<Object, P> {

    private final static String POSTMAN_AGENT_NAME = "postman";

    @Override
    public double getProperty(final Node<Object> node, final Molecule mol, final String prop) {
        return (double) node.getConcentration(mol);
    }

    @Override
    public Molecule createMolecule(final String s) {
        return new SimpleMolecule(s);
    }

    @Override
    public Object createConcentration(final String s) {
        return s;
    }

    @Override
    public Node<Object> createNode(final RandomGenerator rand, final Environment<Object,P> env, final String param) {
        final Node<Object> node = new AgentsContainerNode(param, (Environment<Object, Position<? extends Continuous2DEnvironment>>) env);

        System.out.println("Nodo: " + node.getId() + " || createNode || param: " + param + "\n");
        return node;
    }

    @Override
    public TimeDistribution<Object> createTimeDistribution(final RandomGenerator rand, final Environment<Object, P> env, final Node<Object> node, final String param) {
        System.out.println("Nodo: " + node.getId() + " || createTimeDistribution || param: " + param + "\n");
        return new DiracComb<>(Double.parseDouble(param)); // generate a dirac comb with a value (that can be also random with rand.nextVal())

    }

    @Override
    public Reaction<Object> createReaction(final RandomGenerator rand, final Environment<Object, P> env, final Node<Object> node, final TimeDistribution<Object> time, final String param) {
        System.out.println("Nodo: " + node.getId() + " || createReaction || param: " + param + "\n");

        final Reaction<Object> reaction = new AgentReaction(param, node, time);
        final Condition<Object> condition = createCondition(rand, env, node, time, reaction, param); // create condition
        final Action<Object> action = createAction(rand, env, node, time, reaction, param); // create action

        ((SimpleAgentAction) action).setAgentReaction(reaction); // set reaction reference to the action
        ((AgentsContainerNode) node).addAgent((SimpleAgentAction) action); // add the agent to the map of the node

        reaction.setConditions(Lists.newArrayList(condition));
        reaction.setActions(Lists.newArrayList(action));

        return reaction;
    }

    @Override
    public Condition<Object> createCondition(final RandomGenerator rand, final Environment<Object, P> env, final Node<Object> node, final TimeDistribution<Object> time, final Reaction<Object> reaction, final String param) {
        System.out.println("Nodo: " + node.getId() + " || createCondition || param: " + param + "\n");
        return new AbstractCondition<Object>(node) {
            @Override
            public Context getContext() {
                return Context.NEIGHBORHOOD; // define the depth of an action and it affects the performances
            }

            @Override
            public double getPropensityContribution() {
                return 1;
            }

            @Override
            public boolean isValid() {
                return true; // TODO per adesso va bene così, deve sempre avvenire
            }
        };
    }

    @Override
    public Action<Object> createAction(final RandomGenerator rand, final Environment<Object, P> env, final Node<Object> node, final TimeDistribution<Object> time, final Reaction<Object> reaction, final String param) {
        System.out.println("Nodo: " + node.getId() + " || createAction || param: " + param + "\n");

        Action<Object> action;
        if (POSTMAN_AGENT_NAME.equals(param)) {
            action = new PostmanAction(param, node);
        } else {
            action = new SimpleAgentAction(param, node); // TODO per adesso ping e pong hanno comportamento identico cambia solo il nome
        }
        return action;
    }
}