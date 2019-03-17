package it.unibo.alchemist.model;

import com.google.common.collect.Lists;
import it.unibo.alchemist.model.implementations.actions.AbstractAgent;
import it.unibo.alchemist.model.implementations.actions.Blackboard;
import it.unibo.alchemist.model.implementations.actions.SimpleAgent;
import it.unibo.alchemist.model.implementations.actions.PostmanAgent;
import it.unibo.alchemist.model.implementations.conditions.AbstractCondition;
import it.unibo.alchemist.model.implementations.environments.Continuous2DEnvironment;
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule;
import it.unibo.alchemist.model.implementations.nodes.AgentsContainerNode;
import it.unibo.alchemist.model.implementations.reactions.AgentReaction;
import it.unibo.alchemist.model.implementations.timedistributions.DiracComb;
import it.unibo.alchemist.model.interfaces.Action;
import it.unibo.alchemist.model.interfaces.Condition;
import it.unibo.alchemist.model.interfaces.Context;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Incarnation;
import it.unibo.alchemist.model.interfaces.Molecule;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Position;
import it.unibo.alchemist.model.interfaces.Reaction;

import it.unibo.alchemist.model.interfaces.TimeDistribution;


import org.apache.commons.math3.random.RandomGenerator;



public class AgentIncarnation<P extends Position<? extends P>> implements Incarnation<Object, P> {

    public final static String POSTMAN_AGENT_NAME = "postman";
    public final static String BLACKBOARD_AGENT_NAME = "blackboard";

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
        return new AgentsContainerNode(param, (Environment<Object, Position<? extends Continuous2DEnvironment>>) env);
    }

    @Override
    public TimeDistribution<Object> createTimeDistribution(final RandomGenerator rand, final Environment<Object, P> env, final Node<Object> node, final String param) {
        System.out.println("Nodo: " + node.getId() + " || createTimeDistribution || param: " + param + "\n");
        return new DiracComb<>(Double.parseDouble(param)); // Generates a dirac comb with a value (random or taken from config)
    }

    @Override
    public Reaction<Object> createReaction(final RandomGenerator rand, final Environment<Object, P> env, final Node<Object> node, final TimeDistribution<Object> time, final String param) {
        System.out.println("Nodo: " + node.getId() + " || createReaction || param: " + param + "\n");

        final Reaction<Object> reaction = new AgentReaction(param + "React", node, time);
        final Condition<Object> condition = createCondition(rand, env, node, time, reaction, param); // Create condition
        final Action<Object> action = createAction(rand, env, node, time, reaction, param); // Create action

        ((AgentsContainerNode) node).addAgent((AbstractAgent) action); // Add the agent to the map of the node

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
                // Defines the depth of an action and it affects the performances
                return Context.LOCAL;// TODO va bene come profondità?
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
        if (param.contains(POSTMAN_AGENT_NAME)) {
            action = new PostmanAgent(param, node);
        } else if (param.contains(BLACKBOARD_AGENT_NAME)) {
            action = new Blackboard(param, node);
        } else {
            action = new SimpleAgent(param, node, reaction); // Ping and Pong agents are built with the same class
        }
        return action;
    }
}