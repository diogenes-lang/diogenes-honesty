package it.unica.co2.api.contract.bekic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.ContractReference;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.RecursionReference;
import it.unica.co2.api.contract.utils.ContractExplorer;


public class Bekic {

	private Map<String, ContractDefinition> env = new HashMap<String, ContractDefinition>();
	private Map<ContractDefinition,ContractDefinition> references = new HashMap<>();	// map old/new references
	
	private boolean bekicApplied = false;
	private static final String ANONYMOUS = "_ANONYMOUS";
	
	private static boolean DEBUG = false;
	
	/**
	 * Returns an Bekic instance. The environment is derived from the given contracts.
	 * @param contracts
	 * @return
	 */
	public static Bekic getInstance(ContractDefinition... contracts) {
		
		Set<ContractDefinition> env = new HashSet<>();
		
		for (ContractDefinition c : contracts) {
			env.add(c);
			
			Set<ContractDefinition> refs = ContractExplorer.getAllReferences(c);
			env.addAll( refs );
		}

		return new Bekic(env.toArray(new ContractDefinition[]{}));
	}
	

	public static Bekic getInstance(Contract contract) {
		
		ContractDefinition cDef = new ContractDefinition(ANONYMOUS);
		cDef.setContract(contract);
		return getInstance(cDef);
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
			
			log("[init] "+cEnv);
			
			ContractExplorer.findAll(
					cEnv.getContract(), 
					ContractReference.class, 
					(x)->{
						
						log("[init]     "+x);
						
						ContractDefinition newDef = references.get(x.getReference());	//get the new definition
						
						if (newDef==null)
							throw new IllegalArgumentException("the reference "+x+" was not found");
						
						x.getPreceeding().next(new ContractReference(newDef));
					}
				);
		}
	}
	
	public ContractDefinition[] getEnv() {
		if (!bekicApplied)
			applyBekicTheorem();
		
		return env.values().toArray(new ContractDefinition[]{});
	}
	
	public Contract defToRec() {
		
		if (!env.containsKey(ANONYMOUS))
			throw new IllegalStateException("this method is not allowed. You must use Bekic.getInstance(Contract)");
		
		if (!bekicApplied)
			applyBekicTheorem();
		
		return env.get(ANONYMOUS).getContract();
	}
	
	public ContractDefinition defToRec(ContractDefinition c) {
		if (!references.containsKey(c)) {
			throw new IllegalStateException("the environment does not contain the given contract");
		}
		
		if (!bekicApplied)
			applyBekicTheorem();
		
		return references.get(c);
	}
	
	private void applyBekicTheorem() {

		log("\n====================================================================== START");
		printEnv();
		
		List<String> contracts = new ArrayList<>(env.keySet());
		
		log("\n====================================================================== STEP 0");
		for (String cName : contracts) {
			
			ContractDefinition c = env.get(cName);
			
			Recursion rec = new Recursion(cName);
			log("[rec] "+rec.hashCode());			
			rec.setContract(c.getContract());
			
			c.setContract( rec );
		}
		printEnv();
		
		log("\n====================================================================== STEP 1");
		
		for (String cName : contracts) {
			
			ContractDefinition c = env.get(cName);

			log("[STEP 1] "+c.getId());
			
			ContractExplorer.findAll(
					c.getContract(), 
					ContractReference.class, 
					(x)->{
						return !x.getReference().getName().equals(cName);
						},
					(cRef)->{
						log("[STEP 1]    reference: "+cRef);
						log("[STEP 1]    preceding: "+cRef.getPreceeding());
						cRef.getPreceeding().next( cRef.getReference().getContract().deepCopy() );
					}
				);
			
			log("[STEP 1] NEW: "+c);	
		}
		
		printEnv();
		
		
		log("\n====================================================================== STEP 2");
		for (String cName : contracts) {
			
			ContractDefinition c = env.get(cName);
			
			log("[STEP 2] "+c.getId());
			
			ContractExplorer.findAll(
					c.getContract(), 
					ContractReference.class, 
					(ref)->{
						log("[STEP 2]    creference: "+ref);
						
						List<Recursion> recs = new ArrayList<>();

						ContractExplorer.findAll(
								c.getContract(), 
								Recursion.class,
								(rec)->{
									log("[STEP 2]        search: "+rec.getName());
									return rec.getName().equals(ref.getReference().getName());
								},
								(rec)->{
									recs.add(rec);
								});

						log("[STEP 2]    available recs: "+recs.size());
						
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

		printEnv();
		
		
		log("\n====================================================================== STEP 3");
		for (String cName : contracts) {
			
			ContractDefinition c = env.get(cName);
			
			ContractExplorer.findAll(
					c.getContract(), 
					Recursion.class, 
					(rec)->{
						List<RecursionReference> lst = new ArrayList<>();
						ContractExplorer.findAll(
								c.getContract(), 
								RecursionReference.class,
								(ref)->{return rec==ref.getReference();},
								(x)->{
									lst.add(x);
								});
						
						if (lst.size()==0) {	//remove recursion
							
							if (rec.getPreceeding()!=null) {
								rec.getPreceeding().next( rec.getContract() );
							}
							else {
								//you are the first contract
								c.setContract( rec.getContract() );
							}
						}
					}
				);	
		}
		
		printEnv();
		log("\n====================================================================== END");
		printEnv();
		
		bekicApplied=true;
	}
	
	
	private void printEnv() {
		
		for (Entry<String, ContractDefinition> c : env.entrySet()) {
			log("[ENV]    "+c.getValue().toString());
		}
	}
	
	private static void log(Object obj) {
		if (DEBUG)
			System.out.println(obj);
	}
	
}
