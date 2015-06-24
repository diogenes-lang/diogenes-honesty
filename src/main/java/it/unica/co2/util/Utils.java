package it.unica.co2.util;

import it.unica.co2.model.contract.Sort;
import it.unica.co2.model.prefix.Action;

import java.util.ArrayList;
import java.util.List;


public class Utils {

	public static String[] getActionNames(Action... actions) {
		List<String> actionsList = new ArrayList<>();
		for (Action a : actions) {
			actionsList.add(a.getName());
		}
		return actionsList.toArray(new String[]{});
	}
	
	public static Sort[] getActionSorts(Action... actions) {
		List<Sort> actionsList = new ArrayList<>();
		for (Action a : actions) {
			actionsList.add(a.getSort());
		}
		return actionsList.toArray(new Sort[]{});
	}
}
