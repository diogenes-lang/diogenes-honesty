package it.unica.co2.model;

import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ExternalAction;
import it.unica.co2.model.contract.ExternalSum;
import it.unica.co2.model.contract.InternalAction;
import it.unica.co2.model.contract.InternalSum;
import it.unica.co2.model.contract.Recursion;
import it.unica.co2.model.contract.Sort;

import java.util.ArrayList;
import java.util.List;


public class ContractFactory {
	
//	public static InternalAction internalAction(String actionName) {
//		return internalAction(actionName, Sort.UNIT);
//	}
//	
//	public static InternalAction internalAction(String actionName, Sort sort) {
//		return internalAction(actionName, sort, null);
//	}
//	
//	public static InternalAction internalAction(String actionName, Contract next) {
//		return internalAction(actionName, Sort.UNIT, next);
//	}
//	
//	public static InternalAction internalAction(String actionName, Sort sort, Contract next) {
//		return new InternalAction(actionName, sort, next);
//	}
	
	
	public static InternalSum internalSum(String actionName, Sort sort) {
		return internalSum(actionName, sort, null);
	}
	
	public static InternalSum internalSum(String actionName, Contract next) {
		return internalSum(actionName, Sort.UNIT, next);
	}
	
	public static InternalSum internalSum(String actionName, Sort sort, Contract next) {
		return internalSum().add(actionName, sort, next);
	}
	
	
	
	
//	public static ExternalAction externalAction(String actionName) {
//		return externalAction(actionName, Sort.UNIT);
//	}
//	
//	public static ExternalAction externalAction(String actionName, Sort sort) {
//		return externalAction(actionName, sort, null);
//	}
//	
//	public static ExternalAction externalAction(String actionName, Contract next) {
//		return externalAction(actionName, Sort.UNIT, next);
//	}
//	
//	public static ExternalAction externalAction(String actionName, Sort sort, Contract next) {
//		return new ExternalAction(actionName, sort, next);
//	}
	
	
	public static ExternalSum externalSum(String actionName, Sort sort) {
		return externalSum(actionName, sort, null);
	}
	
	public static ExternalSum externalSum(String actionName, Contract next) {
		return externalSum(actionName, Sort.UNIT, next);
	}
	
	public static ExternalSum externalSum(String actionName, Sort sort, Contract next) {
		return externalSum().add(actionName, sort, next);
	}
	
	
	public static Recursion recursion() {
		return new Recursion();
	}
	
	public static InternalSum internalSum() {
		return new InternalSum();
	}
	
	public static ExternalSum externalSum() {
		return new ExternalSum();
	}
	
	public static InternalSum internalSum(InternalAction... actions) {
		return new InternalSum(actions);
	}
	
	public static ExternalSum externalSum(ExternalAction... actions) {
		return new ExternalSum(actions);
	}
	
	
	
	public static InternalSum internalSum(String... actionNames) {
		List<InternalAction> actions = new ArrayList<>();
		
		for (String action : actionNames) {
			actions.add( new InternalAction(action, Sort.UNIT, null) );
		}
		
		return new InternalSum(actions.toArray(new InternalAction[]{}));
	}
	
	
	public static ExternalSum externalSum(String... actionNames) {
		List<ExternalAction> actions = new ArrayList<>();
		
		for (String action : actionNames) {
			actions.add( new ExternalAction(action, Sort.UNIT, null) );
		}
		
		return new ExternalSum(actions.toArray(new ExternalAction[]{}));
	}
}
