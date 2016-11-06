package it.unica.co2.api.contract.serializer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import it.unica.co2.api.contract.ContractReference;
import it.unica.co2.api.contract.EmptyContract;
import it.unica.co2.api.contract.ExternalAction;
import it.unica.co2.api.contract.ExternalSum;
import it.unica.co2.api.contract.InternalAction;
import it.unica.co2.api.contract.InternalSum;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.RecursionReference;

/**
 * Serialize a {@code SessionType} to its maude representation.
 * 
 * @author Nicola Atzei
 */
public class MaudeContractSerializer extends AbstractContractSerializer {
	
	private static MaudeContractSerializer instance = new MaudeContractSerializer();
	
	private MaudeContractSerializer() {}

	public static MaudeContractSerializer instance() {
		return instance;
	}
	
	@Override
	protected String convert(InternalSum contract) {
		if (contract.getActions().size()==0)
			return "0";
		
		List<String> actions = new ArrayList<>();
		for (InternalAction a : contract.getActions()) {
			actions.add(this.convert(a));
		}
		return StringUtils.join(actions, " (+) ");
	}
	
	@Override
	protected String convert(ExternalSum contract) {
		if (contract.getActions().size()==0)
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
		
		sb.append("\"");
		sb.append(action.getName());
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
		
		sb.append("\"");
		sb.append(action.getName());
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
	protected String convert(Recursion rec) {
		return "rec "+rec.getName()+" . ( "+ this.convert(rec.getContract()) +" ) ";
	}
	
	protected String convert(RecursionReference ref) {
		return ref.getReference().getName();
	}
	
	@Override
	protected String convert(EmptyContract recursion) {
		return "0";
	}
	
	protected String convert(ContractReference ref) {
		return ref.getReference().getName();
	}
}