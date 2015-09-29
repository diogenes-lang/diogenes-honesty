package it.unica.co2.generators;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import it.unica.co2.api.contract.newapi.Contract;
import it.unica.co2.api.contract.newapi.ExternalAction;
import it.unica.co2.api.contract.newapi.ExternalSum;
import it.unica.co2.api.contract.newapi.InternalAction;
import it.unica.co2.api.contract.newapi.InternalSum;
import it.unica.co2.api.contract.newapi.Recursion;

public class MiddlewareContractGenerator extends AbstractContractGenerator{
	
	public MiddlewareContractGenerator(Contract c) {
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
	protected String convert(InternalAction contract) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("!").append(contract.getName());
		
		if (contract.getNext()!=null)
			sb.append(" . (")
			.append( this.convert(contract.getNext()))
			.append(" )");

		return sb.toString();
	}
	
	@Override
	protected String convert(ExternalAction contract) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("?").append(contract.getName());
		
		if (contract.getNext()!=null)
			sb.append(" . ( ")
			.append( this.convert(contract.getNext()))
			.append(" )");

		return sb.toString();
	}
	
	@Override
	protected String convert(Recursion rec) {
		return "REC '"+rec.getName()+"' [ "+ this.convert(rec.getContract()) +" ] ";
	}
	
	
}