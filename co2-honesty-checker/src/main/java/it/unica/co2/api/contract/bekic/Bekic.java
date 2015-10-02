package it.unica.co2.api.contract.bekic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.ContractReference;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.RecursionReference;
import it.unica.co2.api.contract.utils.ContractExplorer;

import java.util.Set;


public class Bekic {

	private Map<String, ContractDefinition> env = new HashMap<String, ContractDefinition>();
	private Map<ContractDefinition,ContractDefinition> references = new HashMap<>();	// map old/new references
	
	boolean bekicApplied = false;
	
	
	/**
	 * Returns an Bekic instance. The environment is derived from the given contracts.
	 * @param contracts
	 * @return
	 */
	public static Bekic getInstance(ContractDefinition... contracts) {
		
		Set<ContractDefinition> env = new HashSet<>();
		
		for (ContractDefinition c : contracts)
			env.addAll( ContractExplorer.getAllReferences(c) );
		
		return new Bekic(env.toArray(new ContractDefinition[]{}));
	}
	
	
	/**
	 * Transform the given contracts using the Bekic theorem. All <code>ContractReference</code>s
	 * are replaced by <Recursion> definitions.
	 * 
	 * @param contracts 
	 * 			all the contracts to be converted
	 */
	private Bekic(ContractDefinition... contracts) {
		
		for (ContractDefinition c : contracts) {
			
			// craete new definition
			ContractDefinition newDef = new ContractDefinition(c.getName());
			newDef.setContract(c.getContract().deepCopy());		// ContractReference(s) are not deep-copied (infinite loop)
			
			// store it into env
			this.env.put(newDef.getName(), newDef);
			
			// save the old and the new object (to fix references later)
			references.put(c, newDef);	// c is replaced by newDef
		}
		
		// fix all other contracts into the new env
		for (ContractDefinition cEnv : env.values()) {
			ContractExplorer.findAll(
					cEnv.getContract(), 
					ContractReference.class, 
					(x)->(true), 
					(x)->{
						ContractDefinition newDef = references.get(x.getReference());	//get the new definition
						
						if (newDef==null)
							throw new IllegalArgumentException("the reference "+x+" was not found");
						
//						System.out.println("changing others: "+x.getReference().hashCode()+" -> "+newDef.hashCode());
						x.getPreceeding().next(new ContractReference(newDef));
					}
				);
		}
	}
	
	public ContractDefinition[] defToRec() {
		if (!bekicApplied)
			applyBekicTheorem();
		
		return env.values().toArray(new ContractDefinition[]{});
	}
	
	public ContractDefinition defToRec(ContractDefinition c) {
		if (!references.containsKey(c)) {
			throw new IllegalStateException("the environment does not contain the given contract");
		}
		
		if (!bekicApplied)
			applyBekicTheorem();
		
		return env.get(references.get(c));
	}
	
	private void applyBekicTheorem() {
		printEnv();
		List<String> contracts = new ArrayList<>(env.keySet());
		
		for (String cName : contracts) {
//			System.out.println("[STEP 0] cName:"+cName);
			
			ContractDefinition c = env.get(cName);
			
			Recursion rec = new Recursion(cName).setContract(c.getContract());
			c.setContract( rec );
		}
		
		for (String cName : contracts) {
//			System.out.println("[STEP 1] cName:"+cName);
			
			ContractDefinition c = env.get(cName);
			
			ContractExplorer.findAll(
					c.getContract(), 
					ContractReference.class, 
					(x)->{
						return !x.getReference().getName().equals(cName);
						},
					(cRef)->{
						cRef.getPreceeding().next( cRef.getReference().getContract().deepCopy() );
						}
					);		
		}
		
//		printEnv();
		
		
		for (String cName : contracts) {
//			System.out.println("[STEP 2] cName:"+cName);
			
			ContractDefinition c = env.get(cName);
			
			ContractExplorer.findAll(
					c.getContract(), 
					ContractReference.class, 
					(x)->{
						return true;
						},
					(ref)->{
						
						List<Recursion> recs = new ArrayList<>();
						
						ContractExplorer.findAll(
								c.getContract(), 
								Recursion.class,
								(rec)->{return rec.getName().equals(ref.getReference().getName());},
								(rec)->{
									recs.add(rec);
								});
						
						if (recs.size()>=1) {
							ref.getPreceeding().next( new RecursionReference(recs.get(0)) );
						}
						else {
							Recursion rec = (Recursion) ref.getReference().getContract();
							ref.getPreceeding().next( rec );
						}
						
						}
					);	
		}
		
//		printEnv();
		
		
		for (String cName : contracts) {
//			System.out.println("[STEP 3] cName:"+cName);
			
			ContractDefinition c = env.get(cName);
			
			ContractExplorer.findAll(
					c.getContract(), 
					Recursion.class, 
					(x)->{
						return true;
						},
					(rec)->{
						List<RecursionReference> lst = new ArrayList<>();
						ContractExplorer.findAll(
								c.getContract(), 
								RecursionReference.class,
								(ref)->{return rec==ref.getReference();},
								(x)->{});
						
						if (lst.size()==0) {
							//remove recursion
							
//							System.out.println("removing "+rec);
							
							if (rec.getPreceeding()!=null) {
								rec.getPreceeding().next( rec.getContract() );
							}
							else {
								//you are the first contract
								c.setContract( rec.getContract() );
							}
							
						}
//						else {
//							System.out.println("not empty "+rec);
//							System.out.println("references "+lst);
//						}
						
						}
					);	
		}
		
		printEnv();
		
	}
	
	
	@SuppressWarnings("unused")
	private void printEnv() {
		
		System.out.println("------ env ------");
		for (Entry<String, ContractDefinition> c : env.entrySet()) {
			System.out.println(c.getValue().toString());
		}
		System.out.println("-----------------");
	}
	
	
}
