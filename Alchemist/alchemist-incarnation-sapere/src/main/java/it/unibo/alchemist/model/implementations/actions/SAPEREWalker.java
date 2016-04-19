/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.actions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.danilopianini.lang.util.FasterString;

import it.unibo.alchemist.expressions.interfaces.ITreeNode;
import it.unibo.alchemist.model.implementations.molecules.LsaMolecule;
import it.unibo.alchemist.model.implementations.strategies.routing.OnStreets;
import it.unibo.alchemist.model.implementations.strategies.speed.InteractWithOthers;
import it.unibo.alchemist.model.implementations.strategies.target.FollowTrace;
import it.unibo.alchemist.model.interfaces.ILsaAction;
import it.unibo.alchemist.model.interfaces.ILsaMolecule;
import it.unibo.alchemist.model.interfaces.ILsaNode;
import it.unibo.alchemist.model.interfaces.IMapEnvironment;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Reaction;
import it.unibo.alchemist.model.interfaces.Vehicle;

/**
 */
public class SAPEREWalker extends MoveOnMap<List<? extends ILsaMolecule>> implements ILsaAction {

    /**
     * The default molecule that identifies an interacting object.
     */
    public static final ILsaMolecule DEFAULT_INTERACTING_TAG = new LsaMolecule("person");
    private static final long serialVersionUID = 8533918846332597708L;

    /**
     * @param environment
     *            the environment
     * @param node
     *            the node
     * @param reaction
     *            the reaction (used to compute the movement length)
     * @param speed
     *            the average speed of the node
     * @param interaction
     *            the interaction factor
     * @param range
     *            the interaction range
     */
    public SAPEREWalker(final IMapEnvironment<List<? extends ILsaMolecule>> environment, final ILsaNode node, final Reaction<List<? extends ILsaMolecule>> reaction, final double speed, final double interaction, final double range) {
        this(environment, node, reaction, DEFAULT_INTERACTING_TAG, speed, interaction, range);
    }

    /**
     * @param environment
     *            the environment
     * @param node
     *            the node
     * @param reaction
     *            the reaction (used to compute the movement length)
     * @param tag
     *            the molecule which identifies the interacting nodes
     * @param speed
     *            the average speed of the node
     * @param interaction
     *            the interaction factor
     * @param range
     *            the interaction range
     */
    public SAPEREWalker(final IMapEnvironment<List<? extends ILsaMolecule>> environment, final ILsaNode node, final Reaction<List<? extends ILsaMolecule>> reaction, final ILsaMolecule tag, final double speed, final double interaction, final double range) {
        super(environment, node,
                new OnStreets<>(environment, Vehicle.FOOT),
                new InteractWithOthers<>(environment, node, reaction, tag, speed, range, interaction),
                new FollowTrace<>(environment, node, reaction));
    }

    @Override
    public SAPEREWalker cloneOnNewNode(final Node<List<? extends ILsaMolecule>> n, final Reaction<List<? extends ILsaMolecule>> r) {
        return null;
    }

    @Override
    public List<? extends ILsaMolecule> getModifiedMolecules() {
        return Collections.emptyList();
    }

    @Override
    public ILsaNode getNode() {
        return (ILsaNode) super.getNode();
    }

    @Override
    public void setExecutionContext(final Map<FasterString, ITreeNode<?>> matches, final List<? extends ILsaNode> nodes) {
    }
}