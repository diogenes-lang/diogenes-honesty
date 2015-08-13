package it.unica.co2.generators;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ExternalAction;
import it.unica.co2.model.contract.ExternalSum;
import it.unica.co2.model.contract.InternalAction;
import it.unica.co2.model.contract.InternalSum;
import it.unica.co2.model.contract.Recursion;

public class MaudeContractGenerator extends AbstractContractGenerator {
	
	private final boolean actionsAsString;
	
	public MaudeContractGenerator(Contract c) {
		this(c, true);
	}
	
	public MaudeContractGenerator(Contract c, boolean actionsAsString) {
		super(c);
		this.actionsAsString = actionsAsString;
	}
	
	@Override
	protected String convert(InternalSum contract) {
		if (contract.getActions().length==0)
			return "0";
		
		List<String> actions = new ArrayList<>();
		for (InternalAction a : contract.getActions()) {
			actions.add(this.convert(a));
		}
		return StringUtils.join(actions, " (+) ");
	}
	
	@Override
	protected String convert(ExternalSum contract) {
		if (contract.getActions().length==0)
			return "0";
			
		List<String> actions = new ArrayList<>();
		for (ExternalAction a : contract.getActions()) {
			actions.add(this.convert(a));
		}
		return StringUtils.join(actions, " + ");
	}
	
	@Override
	protected String convert(InternalAction action) {
		StringBuilder sb = new StringBuilder();
		
		if (actionsAsString)
			sb.append("\"");
		
		sb.append(action.getName());
		
		if (actionsAsString)
			sb.append("\"");
		
		sb.append(" ! ").append("unit");
		
		if (action.getNext()!=null)
			sb.append(" . ( ")
			.append( this.convert(action.getNext()))
			.append(" )");
		else
			sb.append(" . 0");

		return sb.toString();
	}
	
	@Override
	protected String convert(ExternalAction action) {
		StringBuilder sb = new StringBuilder();
		
		if (actionsAsString)
			sb.append("\"");
		
		sb.append(action.getName());
		
		if (actionsAsString)
			sb.append("\"");
		
		sb.append(" ? ").append("unit");
		
		if (action.getNext()!=null)
			sb.append(" . ( ")
			.append( this.convert(action.getNext()))
			.append(" )");
		else
			sb.append(" . 0");

		return sb.toString();
	}
	
	@Override
	protected String convert(Recursion recursion) {
		if (recursions.containsKey(recursion))
			return recursions.get(recursion);
		
		String recName = getRecursionName();
		recursions.put(recursion, recName);
		
		return "rec "+recName+" . ( "+ this.convert(recursion.getContract()) +" ) ";
	}
	
}