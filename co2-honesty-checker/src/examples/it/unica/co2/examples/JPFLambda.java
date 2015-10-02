package it.unica.co2.examples;

import static it.unica.co2.api.contract.utils.ContractFactory.*;

import it.unica.co2.api.contract.Contract;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.generators.MaudeContractGenerator;

public class JPFLambda {
	
	public static void main (String[] args) {
		
		/*
		 * player's contract
		 */
		Recursion playerContract = recursion("x");
		
		Contract hit = internalSum().add("card", recRef(playerContract)).add("lose").add("abort");
		Contract end = internalSum().add("win").add("lose").add("abort");
		
		playerContract.setContract(externalSum().add("hit", hit).add("stand", end));
		
		/*
		 * deck service's contract
		 */
		Recursion dealerServiceContract = recursion("y");
		
		dealerServiceContract.setContract(internalSum().add("next", externalSum().add("card", recRef(dealerServiceContract))).add("abort"));

		
		System.out.println(
				new MaudeContractGenerator(playerContract).generate()
				);
	
		System.out.println(
				new MaudeContractGenerator(dealerServiceContract).generate()
				);
	}
	
	
	
}
