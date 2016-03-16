package it.unica.co2.honesty.handlers;

import java.util.List;

import co2api.Public;
import co2api.Session;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;
import it.unica.co2.honesty.dto.CO2DataStructures.PrefixPlaceholderDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessCallDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDefinitionDS;
import it.unica.co2.honesty.dto.CO2DataStructures.SumDS;


public class ProcessCallEnteredHandler extends MethodHandler {

	@Override
	public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, MethodInfo mi) {
		
		ClassInfo ci = ti.getExecutingClassInfo();

		log.info("");
		tstate.printInfo();
		log.info("--PROCESS CALL-- (method entered) -> "+ci.getSimpleName());
		
		String className = listener.getArgumentString(ti, 1);			// the classname of the process that we want to invoke
		List<ElementInfo> args = listener.getArgumentArray(ti, 2);
		
		if (listener.envProcessAlreadyProcessed(className)) {
			log.info("envProcess "+className+" already exists");
		}
		else {
			/*
			 * instantiate a new env process
			 */
			ProcessDefinitionDS proc = new ProcessDefinitionDS();
			proc.name = className;
			proc.firstPrefix = new PrefixPlaceholderDS();
			proc.process = new SumDS(proc.firstPrefix);
			
//			List<ElementInfo> args = getAllArgumentsAsElementInfo(ti);
			
			if (args.size()==0) {
				//add at least one argument to make the process valid
				proc.freeNames.add("exp");
			}
			
			for (ElementInfo ei : args) {
				if (ei.getClassInfo().isInstanceOf(Session.class.getName())) {

					String sessionName = listener.getSessionIDBySession(ti, ei);
					
					proc.freeNames.add("\""+sessionName+"\"");
					log.info("ctor arg: Session ("+sessionName+")");
				}
				else if (ei.getClassInfo().isInstanceOf(Public.class.getName())) {

					String sessionName = listener.getSessionIDByPublic(ei);
					
					proc.freeNames.add("\""+sessionName+"\"");
					log.info("ctor arg: Session2 ("+sessionName+")");
				}
				else if (ei.getClassInfo().isInstanceOf(Number.class.getName())) {
					proc.freeNames.add("exp");
					log.info("ctor arg: Number");
				}
				else if (ei.getClassInfo().isInstanceOf(String.class.getName())) {
					proc.freeNames.add("exp");
					log.info("ctor arg: String");
				}
			}
			
			// store the process for future retrieve (when another one1 call it)
			log.info("saving envProcess "+className);
			listener.addEnvProcess(className, proc);
		}
		
		
		
		ProcessCallDS pCall = new ProcessCallDS();

		pCall.name = className;
		log.info("processName: "+pCall.name);
		
		for (ElementInfo ei : listener.getArgumentArray(ti, 2)) {
			
			if (ei.getClassInfo().isInstanceOf(Session.class.getName())) {

				String sessionName = listener.getSessionIDBySession(ti, ei);
				
				pCall.params.add("\""+sessionName+"\"");
				log.info("param: Session ("+sessionName+")");
			}
			else if (ei.getClassInfo().isInstanceOf(Public.class.getName())) {

				String sessionName = listener.getSessionIDByPublic(ei);
				
				pCall.params.add("\""+sessionName+"\"");
				log.info("ctor arg: Session2 ("+sessionName+")");
			}
			else if (ei.getClassInfo().isInstanceOf(Number.class.getName())) {
				pCall.params.add("exp");
				log.info("param: Number");
			}
			else if (ei.getClassInfo().isInstanceOf(String.class.getName())) {
				pCall.params.add("exp");
				log.info("param: String");
			}
		}
		
		tstate.setCurrentProcess(pCall);
		tstate.setCurrentPrefix(null);
	}

}
