package it.unica.co2.honesty.handlers;

import java.util.logging.Logger;

import co2api.Message;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;
import it.unica.co2.honesty.Co2Listener;
import it.unica.co2.honesty.ThreadState;
import it.unica.co2.honesty.handlers.InstructionHandler.InstructionWrapper;

public abstract class InstructionHandler implements HandlerI<InstructionWrapper> {

	protected final Logger log;
	
	protected InstructionHandler() {
		log = JPF.getLogger(this.getClass().getName());
	}
	
	public static class InstructionWrapper {
		public Co2Listener listener;
		public ThreadInfo ti;
		public Instruction insn;
	}
	
	abstract public void handle(Co2Listener listener, ThreadState tstate, ThreadInfo ti, Instruction insn);
	
	@Override
	public void handle(InstructionWrapper obj) {
		handle(obj.listener, obj.listener.getThreadState(obj.ti), obj.ti, obj.insn);
	}
	
	
	protected ElementInfo getMessage(ThreadInfo ti, String label, String value) {
		
		assert label!=null;
		assert value!=null;
		
		ClassInfo messageCI = ClassInfo.getInitializedClassInfo(Message.class.getName(), ti);
		ElementInfo messageEI = ti.getHeap().newObject(messageCI, ti);
		
		messageEI.setReferenceField("label", ti.getHeap().newString(label, ti).getObjectRef());
		messageEI.setReferenceField("stringVal", ti.getHeap().newString(value, ti).getObjectRef());
		
		return messageEI;
	}
	
	
}
