package it.unica.co2.honesty;

import it.unica.co2.generators.MaudeContractGenerator;
import it.unica.co2.honesty.dto.ProcessDS;
import it.unica.co2.honesty.dto.ProcessDefinitionDS;
import it.unica.co2.model.contract.Contract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;


public class MaudeTemplate {

	
	public static String getMaudeProcess(MaudeListener maudeListener) {
		
		System.out.println("--------------------------------------------------");
		System.out.println("generating the maude process from basic template");
		
		String maudeProcess = maudeTemplate;

		String moduleName = "JAVA-PROCESS";
		String processName = maudeListener.getProcessUnderTestClass().getSimpleName();
		
		Collection<String> contractNames = maudeListener.getContracts().keySet();
		Collection<String> variableNames = getVariableNames(maudeListener.getContracts().values());
		Collection<String> processesIde =  maudeListener.getEnvProcessesNames();
		
		String ops = StringUtils.join( 
				getOps(
					Collections.singleton(processName),
					processesIde,
					variableNames,
					contractNames
				),
				"\n"
		);
		
		Collection<String> eqsList = getEqsContract(maudeListener.getContracts());
		eqsList.addAll(getEqProcess(processName, maudeListener.getSessions(), maudeListener.getCo2Process()));
		eqsList.addAll(getEqEnv(maudeListener.getEnvProcesses()));
		
		String eqs = StringUtils.join( 
				eqsList,
				"\n"
		);
		
		maudeProcess = maudeProcess.replace("${moduleName}", moduleName);
		maudeProcess = maudeProcess.replace("${processName}", processName);
		maudeProcess = maudeProcess.replace("${opList}", ops);
		maudeProcess = maudeProcess.replace("${eqList}", eqs);

		return maudeProcess;
	}
	
	
	
	private static Collection<String> getVariableNames(Collection<Contract> contracts) {
		
		Set<String> set = new HashSet<>();
		
		for (Contract c : contracts) {
			
			MaudeContractGenerator gen = new MaudeContractGenerator(c);
			gen.generate();
			
			set.addAll(gen.getRecursionNames());
		}

		return set;
	}



	private static final String maudeTemplate = 
			"in co2-abs .\n" + 
			"\n" + 
			"mod ${moduleName} is\n" + 
			"\n" + 
			"    including CO2-ABS-SEM .\n" + 
			"    including STRING .\n" + 
			"\n" + 
			"    subsort String < ActName .\n" + 
			"\n" + 
			"${opList}\n" + 
			"${eqList}\n" + 
			"    \n" + 
			"endm\n" + 
			"\n" + 
			"*** honesty\n" + 
			"red honest(${processName} , ['${moduleName}] , 50) .\n" + 
			"\n" + 
			"*** exit the program\n" + 
			"quit";
	
	private static final String maudeOpsTemplate = "    ops ${nameList} : -> ${type} .";
	private static final String maudeEqTemplate = "    eq ${name} = ${body} .";
	
	private static final String typeBType = "BType [ctor]";
	private static final String typeExpression = "Expression [ctor]";
	private static final String typeVar = "Var [ctor]";
	private static final String typeUniContract = "UniContract";
	private static final String typeProcess = "Process";
	private static final String typeProcIde = "ProcIde";
	
	private static List<String> getOps(
			Collection<String> processes, 
			Collection<String> processesIde, 
			Collection<String> variables, 
			Collection<String> contracts) {
		List<String> ops = new ArrayList<String>();
		
		ops.add(
				maudeOpsTemplate
				.replace("${nameList}", "unit int string")
				.replace("${type}", typeBType)
		);
		
		ops.add(
				maudeOpsTemplate
				.replace("${nameList}", "exp")
				.replace("${type}", typeExpression)
		);
		
		if (processes.size()>0)
			ops.add(
					maudeOpsTemplate
					.replace("${nameList}", StringUtils.join(processes, " "))
					.replace("${type}", typeProcess)
			);
		
		if (processesIde.size()>0)
			ops.add(
					maudeOpsTemplate
					.replace("${nameList}", StringUtils.join(processesIde, " "))
					.replace("${type}", typeProcIde)
			);
		
		if (variables.size()>0)
			ops.add(
					maudeOpsTemplate
					.replace("${nameList}", StringUtils.join(variables, " "))
					.replace("${type}", typeVar)
			);
		
		if (contracts.size()>0)
			ops.add(
					maudeOpsTemplate
					.replace("${nameList}", StringUtils.join(contracts, " "))
					.replace("${type}", typeUniContract)
			);
		
		return ops;
	}
	
	
	private static List<String> getEqsContract(Map<String, Contract> contracts) {
		List<String> eqs = new ArrayList<String>();
		
		eqs.add("\n    *** list of contracts");
		for (Entry<String, Contract> c : contracts.entrySet()) {
			eqs.add(
					maudeEqTemplate
					.replace("${name}", c.getKey())
					.replace("${body}", c.getValue().toMaude())
			);
		}
		
		return eqs;
	}
	
	private static List<String> getEqProcess(String processName, List<String> sessions, ProcessDS process) {
		List<String> eqs = new ArrayList<String>();
		String body = getProcessBody(sessions, process);
		
		eqs.add("\n    *** list of processes");
		eqs.add(
				maudeEqTemplate
				.replace("${name}", processName)
				.replace("${body}", body)
		);
		
		return eqs;
	}
	
	private static String getProcessBody(List<String> sessions, ProcessDS process) {
		StringBuilder sb = new StringBuilder();
		
		int i=0;
		sb.append("(");
		for (String session : sessions) {
			if (i++>0)
				sb.append(",");
			sb.append("\"").append(session).append("\"");
		}
		sb.append(") ");
		sb.append(process.toMaude("    "));
		
		return sb.toString();
	}

	private static List<String> getEqEnv(Collection<ProcessDefinitionDS> processes) {
		List<String> eqs = new ArrayList<String>();
		
		if (processes.size()==0)
			return eqs;
		
		String body = getEnv(processes);
		
		eqs.add("\n    *** env");
		eqs.add(
				maudeEqTemplate
				.replace("${name}", "env")
				.replace("${body}", body)
		);
		
		return eqs;
	}
	
	private static String getEnv(Collection<ProcessDefinitionDS> processes) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("(\n");
		
		int i=0;
		for (ProcessDefinitionDS p : processes) {
			if (i++>0)
				sb.append("        &\n");
			sb.append("        ").append(p.toMaude("        ")).append("\n");
		}
		
		sb.append("    )");
		
		return sb.toString();
	}
}
