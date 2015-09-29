package it.unica.co2.api.contract.defeq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import it.unica.co2.api.contract.newapi.ContractDefinition;
import it.unica.co2.api.contract.newapi.ContractReference;
import it.unica.co2.api.contract.newapi.Recursion;
import it.unica.co2.api.contract.newapi.RecursionReference;


public class Bekic {

	private Map<String, ContractDefinition> env = new HashMap<String, ContractDefinition>();
	
	boolean bekicApplied = false;
	
	public Bekic(ContractDefinition... contracts) {
		
		for (ContractDefinition c : contracts) {
			
			ContractDefinition newDef = c;//new ContractDefinition(c.getName());
//			
			this.env.put(c.getName(), newDef);
//		
//			for (ContractDefinition c1 : contracts)
//				new ContractExplorer().findall(
//						c1.getContract(), 
//						ContractReference.class, 
//						(x)->(x.getReference()==c), 
//						(x)->{x.getPreceeding().next(new ContractReference(newDef));}
//						);
//			
//			newDef.setContract(c.getContract().deepCopy());
		}
	}
	
	public ContractDefinition[] defToRec() {
		if (!bekicApplied)
			bekic();
		return env.values().toArray(new ContractDefinition[]{});
	}
	
	public void bekic() {
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
			
			new ContractExplorer().findall(
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
			
			new ContractExplorer().findall(
					c.getContract(), 
					ContractReference.class, 
					(x)->{
						return true;
						},
					(ref)->{
						List<Recursion> recs = new ContractExplorer().findall(
								c.getContract(), 
								Recursion.class,
								(rec)->{return rec.getName().equals(ref.getReference().getName());},
								(rec)->{});
						
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
			
			new ContractExplorer().findall(
					c.getContract(), 
					Recursion.class, 
					(x)->{
						return true;
						},
					(rec)->{
						List<RecursionReference> lst = new ContractExplorer().findall(
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
	
	
	private void printEnv() {
		
		System.out.println("------ env ------");
		for (Entry<String, ContractDefinition> c : env.entrySet()) {
			System.out.println(c.getValue().toString());
		}
		System.out.println("-----------------");
	}
	
}
