package it.unica.co2.api.contract.generators;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import it.unica.co2.api.contract.EmptyContract;
import it.unica.co2.api.contract.ExternalAction;
import it.unica.co2.api.contract.ExternalSum;
import it.unica.co2.api.contract.InternalAction;
import it.unica.co2.api.contract.InternalSum;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.SessionType;

public class TSTContractGenerator extends AbstractContractGenerator{
	
	public TSTContractGenerator(SessionType c) {
		super(c);
	}

	
	@Override
	protected String convert(InternalSum contract) {
		List<String> actions = new ArrayList<>();
		for (InternalAction a : contract.getActions()) {
			actions.add(this.convert(a));
		}
		return StringUtils.join(actions, " + ");
	}
	
	@Override
	protected String convert(ExternalSum contract) {
		List<String> actions = new ArrayList<>();
		for (ExternalAction a : contract.getActions()) {
			actions.add(this.convert(a));
		}
		return StringUtils.join(actions, " & ");
	}
	
	@Override
	protected String convert(InternalAction action) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("!").append(action.getName());
		sb.append(action.getGuard());
		
		if (action.getNext()!=null && !(action.getNext() instanceof EmptyContract))
			sb.append(" . ( ")
			.append( this.convert(action.getNext()))
			.append(" )");

		return sb.toString();
	}
	
	@Override
	protected String convert(ExternalAction action) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("?").append(action.getName());
		sb.append(action.getGuard());
		
		if (action.getNext()!=null && !(action.getNext() instanceof EmptyContract))
			sb.append(" . ( ")
			.append( this.convert(action.getNext()))
			.append(" )");

		return sb.toString();
	}
	
	@Override
	protected String convert(Recursion rec) {
		return "REC '"+rec.getName()+"' [ "+ this.convert(rec.getContract()) +" ] ";
	}


	@Override
	protected String convert(EmptyContract recursion) {
		return "";
	}
	
	
}