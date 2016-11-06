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
 * Serialize a {@code SessionType} to a timed-session-type accepted by the contract-oriented middleware.
 * 
 * @author Nicola Atzei
 */
public class TSTContractSerializer extends AbstractContractSerializer{
	
	private static TSTContractSerializer instance = new TSTContractSerializer();
	
	private TSTContractSerializer() {}

	public static TSTContractSerializer instance() {
		return instance;
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
		return "REC '"+normalize(rec.getName().toLowerCase())+"' [ "+ this.convert(rec.getContract()) +" ] ";
	}

	protected String convert(RecursionReference ref) {
		return "'"+normalize(ref.getReference().getName())+"'";
	}

	@Override
	protected String convert(EmptyContract recursion) {
		return "";
	}
	
	protected String convert(ContractReference ref) {
		throw new UnsupportedOperationException();
	}
	
	private String normalize(String s) {
		return s.toLowerCase().replaceAll("[^a-z]","");
		
	}
}