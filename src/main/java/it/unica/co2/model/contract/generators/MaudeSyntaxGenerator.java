package it.unica.co2.model.contract.generators;

import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ExternalAction;
import it.unica.co2.model.contract.ExternalSum;
import it.unica.co2.model.contract.InternalAction;
import it.unica.co2.model.contract.InternalSum;
import it.unica.co2.model.contract.Recursion;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class MaudeSyntaxGenerator extends Generator {
	
	public MaudeSyntaxGenerator(Contract c) {
		super(c);
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
		
		sb
		.append(action.getName())
		.append(" ! ")
		.append(action.getSort().toString().toLowerCase());
		
		if (action.getNext()!=null)
			sb.append(" . ( ")
			.append( this.convert(action.getNext()))
			.append(" ) ");
		else
			sb.append(" . 0");

		return sb.toString();
	}
	
	@Override
	protected String convert(ExternalAction action) {
		StringBuilder sb = new StringBuilder();
		
		sb
		.append(action.getName())
		.append(" ? ")
		.append(action.getSort().toString().toLowerCase());
		
		if (action.getNext()!=null)
			sb.append(" . ( ")
			.append( this.convert(action.getNext()))
			.append(" ) ");
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