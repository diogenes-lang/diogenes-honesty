package it.unica.co2.honesty;

import it.unica.co2.honesty.dto.ProcessDTO;
import it.unica.co2.model.contract.Action;
import it.unica.co2.model.contract.Contract;
import it.unica.co2.model.contract.ExternalSum;
import it.unica.co2.model.contract.InternalSum;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;


public class MaudeTemplate {

	
	public static String getMaudeProcess(MaudeListener maudeListener) {
		
		System.out.println("--------------------------------------------------");
		System.out.println("generating the maude process from basic template");
		
		String maudeProcess = maudeProcessTemplate;

		String moduleName = "JAVA-PROCESS";
		String processName = "P";
		String actionNames = StringUtils.join(
				getActions(
						maudeListener.getActions(), maudeListener.getContracts().values()	//merge contract actions and process actions
						), 
				", ");
		String contractNames = StringUtils.join(maudeListener.getContracts().keySet(), ", ");
		String contractList = getContractList(maudeListener.getContracts());
		String process = getProcess(processName, maudeListener.getSessions(), maudeListener.getCo2Process());
		
		maudeProcess = maudeProcess.replace("${moduleName}", moduleName);
		maudeProcess = maudeProcess.replace("${processName}", processName);
		maudeProcess = maudeProcess.replace("${actions}", actionNames);
		maudeProcess = maudeProcess.replace("${processes}", processName);
		maudeProcess = maudeProcess.replace("${processesIde}", "");
		maudeProcess = maudeProcess.replace("${contracts}", contractNames);
		maudeProcess = maudeProcess.replace("${contractList}", contractList);
		maudeProcess = maudeProcess.replace("${processList}", process);
		

		if (MaudeProperties.getBooleanProperty("maude.verbose", false)) {
			System.out.println("-------------------------------------------------- maude process");
			System.out.println(maudeProcess);
		}
		
		return maudeProcess;
	}
	
	private static String getProcess(String processName, List<String> sessions, ProcessDTO process) {
		
		String body = getProcessBody(sessions, process);
		
		return processTemplate
				.replace("${processName}", processName)
				.replace("${processBody}", body);
	}
	
	private static String getProcessBody(List<String> sessions, ProcessDTO process) {
		StringBuilder sb = new StringBuilder();
		
		int i=0;
		sb.append("(");
		for (String session : sessions) {
			if (i++>0)
				sb.append(",");
			sb.append("\"").append(session).append("\"");
		}
		sb.append(") ");
		sb.append(process.toMaude());
		
		return sb.toString();
	}
	
	private static String getContractList(Map<String, Contract> contracts) {
		StringBuilder sb = new StringBuilder();
		
		for (Entry<String, Contract> c : contracts.entrySet()) {
			sb.append(
					contractTemplate
					.replace("${contractName}", c.getKey())
					.replace("${contractBody}", c.getValue().toMaude())
			);
		}
		
		return sb.toString();
	}
	
	private static Set<String> getActions(Set<String> actions, Collection<Contract> contracts) {
		for (Contract c : contracts)
			getActions(actions, c);
		return actions;
	}
	
	private static void getActions(Set<String> actions, Contract c) {
		
		if (c instanceof InternalSum) {
			for (Action a : ((InternalSum)c).getActions()) {
				actions.add(a.getName());
				getActions(actions, a.getNext());
			}
		}
		else if (c instanceof ExternalSum) {
			for (Action a : ((ExternalSum)c).getActions()) {
				actions.add(a.getName());
				getActions(actions, a.getNext());
			}
		}
		
	}
	
	
	private static final String contractTemplate = 
			"    eq ${contractName} = ${contractBody} .\n";
	
	private static final String processTemplate = 
			"    eq ${processName} = ${processBody} .\n";
	
	private static final String maudeProcessTemplate = 
			"\n" + 
			"in co2-abs .\n" + 
			"\n" + 
			"mod ${moduleName} is\n" + 
			"\n" + 
			"    including CO2-ABS-SEM .\n" + 
			"    \n" + 
			"    ops unit int string : -> BType [ctor] .\n" + 
			"    ops exp : -> Expression [ctor] .\n" + 
			"    \n" + 
			"    ops ${actions} : -> ActName [ctor] .\n" + 
			"    ops ${contracts} : -> UniContract .\n" + 
			"    ops ${processes} : -> Process .\n" + 
			"\n" + 
			"    *** list of contracts\n" +
			
			"${contractList}"+
			"    \n" + 
			"    \n" +
			
			"    *** list of processes\n" + 
			"${processList}"+
			
			"endm\n" + 
			"\n" + 
			"*** honesty\n" + 
			"red honest(${processName} , ['${moduleName}] , 50) .\n" + 
			"\n" + 
			"*** exit the program\n" + 
			"quit\n" + 
			"";
}
