//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit;

import java.util.*;
import java.io.*;

import prism.ModelType;
import prism.PrismException;

/**
 * Simple explicit-state representation of a DTMC.
 */
public class DTMCSimple extends ModelSimple implements DTMC
{
	// Transition matrix (distribution list) 
	protected List<Distribution> trans;

	// Rewards
	// (if transRewardsConstant non-null, use this for all transitions; otherwise, use transRewards list)
	// (for transRewards, null in element s means no rewards for that state)
	protected Double transRewardsConstant;
	protected List<Double> transRewards;

	// Other statistics
	protected int numTransitions;

	// Constructors

	/**
	 * Constructor: empty DTMC.
	 */
	public DTMCSimple()
	{
		initialise(0);
	}

	/**
	 * Constructor: new DTMC with fixed number of states.
	 */
	public DTMCSimple(int numStates)
	{
		initialise(numStates);
	}

	/**
	 * Copy constructor.
	 */
	public DTMCSimple(DTMCSimple dtmc)
	{
		this(dtmc.numStates);
		for (int in : dtmc.getInitialStates()) {
			addInitialState(in);
		}
		for (int i = 0; i < numStates; i++) {
			trans.set(i, new Distribution(dtmc.trans.get(i)));
		}
		// TODO: copy rewards
		numTransitions = dtmc.numTransitions;
	}

	// Mutators (for ModelSimple)

	@Override
	public void initialise(int numStates)
	{
		super.initialise(numStates);
		trans = new ArrayList<Distribution>(numStates);
		for (int i = 0; i < numStates; i++) {
			trans.add(new Distribution());
		}
		clearAllRewards();
	}

	@Override
	public void clearState(int i)
	{
		// Do nothing if state does not exist
		if (i >= numStates || i < 0)
			return;
		// Clear data structures and update stats
		numTransitions -= trans.get(i).size();
		trans.get(i).clear();
	}

	@Override
	public int addState()
	{
		addStates(1);
		return numStates - 1;
	}

	@Override
	public void addStates(int numToAdd)
	{
		for (int i = 0; i < numToAdd; i++) {
			trans.add(new Distribution());
			numStates++;
		}
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		BufferedReader in;
		String s, ss[];
		int i, j, n, lineNum = 0;
		double prob;

		try {
			// Open file
			in = new BufferedReader(new FileReader(new File(filename)));
			// Parse first line to get num states
			s = in.readLine();
			lineNum = 1;
			if (s == null)
				throw new PrismException("Missing first line of .tra file");
			ss = s.split(" ");
			n = Integer.parseInt(ss[0]);
			// Initialise
			initialise(n);
			// Go though list of transitions in file
			s = in.readLine();
			lineNum++;
			while (s != null) {
				s = s.trim();
				if (s.length() > 0) {
					ss = s.split(" ");
					i = Integer.parseInt(ss[0]);
					j = Integer.parseInt(ss[1]);
					prob = Double.parseDouble(ss[2]);
					setProbability(i, j, prob);
				}
				s = in.readLine();
				lineNum++;
			}
			// Close file
			in.close();
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		} catch (NumberFormatException e) {
			throw new PrismException("Problem in .tra file (line " + lineNum + ") for " + getModelType());
		}
		// Set initial state (assume 0)
		initialStates.add(0);
	}

	// Mutators (other)

	/**
	 * Set the probability for a transition. 
	 */
	public void setProbability(int i, int j, double prob)
	{
		Distribution distr = trans.get(i);
		if (distr.get(i) != 0.0)
			numTransitions--;
		if (prob != 0.0)
			numTransitions++;
		trans.get(i).set(j, prob);
	}

	/**
	 * Remove all rewards from the model
	 */
	public void clearAllRewards()
	{
		transRewards = null;
		transRewardsConstant = null;
	}

	/**
	 * Set a constant reward for all transitions
	 */
	public void setConstantTransitionReward(double r)
	{
		// This replaces any other reward definitions
		transRewards = null;
		// Store as a Double (because we use null to check for its existence)
		transRewardsConstant = new Double(r);
	}

	/**
	 * Set the reward for (all) transitions in state s to r.
	 */
	public void setTransitionReward(int s, double r)
	{
		// This would replace any constant reward definition, if it existed
		transRewardsConstant = null;
		// If no rewards array created yet, create it
		if (transRewards == null) {
			transRewards = new ArrayList<Double>(numStates);
			for (int j = 0; j < numStates; j++)
				transRewards.add(0.0);
		}
		// Set reward
		transRewards.set(s, r);
	}

	// Accessors (for ModelSimple)

	@Override
	public ModelType getModelType()
	{
		return ModelType.DTMC;
	}

	@Override
	public int getNumTransitions()
	{
		return numTransitions;
	}

	@Override
	public boolean isSuccessor(int s1, int s2)
	{
		return trans.get(s1).contains(s2);
	}

	@Override
	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		return (trans.get(s).isSubsetOf(set));
	}

	@Override
	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		return (trans.get(s).containsOneOf(set));
	}

	@Override
	public int getNumChoices(int s)
	{
		// Always 1 for a DTMC
		return 1;
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (trans.get(i).isEmpty() && (except == null || !except.get(i)))
				throw new PrismException("DTMC has a deadlock in state " + i);
		}
	}

	@Override
	public void exportToPrismExplicit(String baseFilename) throws PrismException
	{
		throw new PrismException("Export not yet supported");
	}

	@Override
	public void exportToDotFile(String filename, BitSet mark) throws PrismException
	{
		int i;
		try {
			FileWriter out = new FileWriter(filename);
			out.write("digraph " + getModelType() + " {\nsize=\"8,5\"\nnode [shape=box];\n");
			for (i = 0; i < numStates; i++) {
				if (mark != null && mark.get(i))
					out.write(i + " [style=filled  fillcolor=\"#cccccc\"]\n");
				for (Map.Entry<Integer, Double> e : trans.get(i)) {
					out.write(i + " -> " + e.getKey() + " [ label=\"");
					out.write(e.getValue() + "\" ];\n");
				}
			}
			out.write("}\n");
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not write " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	@Override
	public String infoString()
	{
		String s = "";
		s += numStates + " states";
		s += " (" + getNumInitialStates() + " initial)";
		s += ", " + numTransitions + " transitions";
		return s;
	}

	// Accessors (for DTMC)

	@Override
	public double getTransitionReward(int s)
	{
		if (transRewardsConstant != null)
			return transRewardsConstant;
		if (transRewards == null)
			return 0.0;
		return transRewards.get(s);
	}

	@Override
	public void mvMult(double vect[], double result[], BitSet subset, boolean complement)
	{
		int s;
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++)
				result[s] = mvMultSingle(s, vect);
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1))
				result[s] = mvMultSingle(s, vect);
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1))
				result[s] = mvMultSingle(s, vect);
		}
	}

	@Override
	public double mvMultSingle(int s, double vect[])
	{
		int k;
		double d, prob;
		Distribution distr;

		distr = trans.get(s);
		d = 0.0;
		for (Map.Entry<Integer, Double> e : distr) {
			k = (Integer) e.getKey();
			prob = (Double) e.getValue();
			d += prob * vect[k];
		}

		return d;
	}

	@Override
	public void mvMultRew(double vect[], double result[], BitSet subset, boolean complement)
	{
		int s;
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++)
				result[s] = mvMultRewSingle(s, vect);
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1))
				result[s] = mvMultRewSingle(s, vect);
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1))
				result[s] = mvMultRewSingle(s, vect);
		}
	}

	@Override
	public double mvMultRewSingle(int s, double vect[])
	{
		int k;
		double d, prob;
		Distribution distr;

		distr = trans.get(s);
		d = getTransitionReward(s);
		for (Map.Entry<Integer, Double> e : distr) {
			k = (Integer) e.getKey();
			prob = (Double) e.getValue();
			d += prob * vect[k];
		}

		return d;
	}

	// Accessors (other)

	/**
	 * Get the transitions (a distribution) for state s.
	 */
	public Distribution getTransitions(int s)
	{
		return trans.get(s);
	}

	// Standard methods

	@Override
	public String toString()
	{
		int i;
		boolean first;
		String s = "";
		first = true;
		s = "trans: [ ";
		for (i = 0; i < numStates; i++) {
			if (first)
				first = false;
			else
				s += ", ";
			s += i + ": " + trans.get(i);
		}
		s += " ]";
		return s;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof DTMCSimple))
			return false;
		if (!super.equals(o))
			return false;
		DTMCSimple dtmc = (DTMCSimple) o;
		if (!trans.equals(dtmc.trans))
			return false;
		// TODO: rewards
		if (numTransitions != dtmc.numTransitions)
			return false;
		return true;
	}
}