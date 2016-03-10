package it.unica.co2.honesty.handlers;

import java.util.logging.Logger;

import co2api.Message;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;

abstract class AbstractHandler implements IHandler {

	protected final Logger log;
	
	protected AbstractHandler() {
		log = JPF.getLogger(this.getClass().getName());
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
