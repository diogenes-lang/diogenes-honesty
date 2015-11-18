package it.unica.co2.util;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;

/**
 * https://code.google.com/p/xeger/ (slightly modified)
 */
public class Xeger {

	private final Automaton automaton;

	public Xeger(String regex) {
		assert (regex != null);
		this.automaton = new RegExp(regex).toAutomaton();
	}

	public static String generate(String regex) {
		return new Xeger(regex).generate();
	}
	
	public String generate() {
		StringBuilder builder = new StringBuilder();
		generate(builder, this.automaton.getInitialState());
		return builder.toString();
	}

	private void generate(StringBuilder builder, State state) {
		List<Transition> transitions = state.getSortedTransitions(true);
		if (transitions.size() == 0) {
			assert (state.isAccept());
			return;
		}
		int nroptions = state.isAccept() ? transitions.size() : transitions.size() - 1;
		int option = RandomUtils.nextInt(0, nroptions);
		if ((state.isAccept()) && (option == 0)) {
			return;
		}

		Transition transition = (Transition) transitions.get(option - (state.isAccept() ? 1 : 0));
		appendChoice(builder, transition);
		generate(builder, transition.getDest());
	}

	private void appendChoice(StringBuilder builder, Transition transition) {
		char c = (char) RandomUtils.nextInt(transition.getMin(), transition.getMax());
		builder.append(c);
	}
}
