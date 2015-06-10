package it.unica.co2.semantics.jpf;

import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.ATHROW;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Heap;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;
import it.unica.co2.semantics.LTSPropertyViolatedException;
import it.unica.co2.semantics.LTSState;
import it.unica.co2.util.ObjectUtils;

import java.io.IOException;
import java.util.List;

public class ComplianceListener extends PropertyListenerAdapter {

	static JPFLogger log = JPF.getLogger(ComplianceListener.class.getName());

	String msg = null;

	LTSState finalState;
	List<LTSState> path;
	
	@Override
	public boolean check(Search search, VM vm) {
		return msg==null;
	}

	@Override
	public String getErrorMessage() {
		return msg;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void executeInstruction(VM vm, ThreadInfo ti, Instruction insn) {
		
		if (insn instanceof ATHROW) {
			Heap heap = vm.getHeap();
			StackFrame frame = ti.getTopFrame();
			int xobjref = frame.peek();
			ElementInfo ei = heap.get(xobjref);
			ClassInfo ci = ei.getClassInfo();

			if (ci.getName().equals(LTSPropertyViolatedException.class.getName())) {
				int stateRef = ei.getReferenceField("finalState");
				int pathRef = ei.getReferenceField("path");
				
				ElementInfo eiState = heap.get(stateRef);
				ElementInfo eiPath = heap.get(pathRef);
				
				try {
					if (eiState != null)
						this.finalState = ObjectUtils.deserializeObjectFromString(eiState.asString(), LTSState.class);

					if (eiPath != null)
						this.path = ObjectUtils.deserializeObjectFromString(eiPath.asString(), List.class);
				
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
				
				msg="found a contract-configuration that is not safe";
				
				ti.skipInstruction(insn);
				ti.breakTransition("property violated");
			}
		}
	}

	public LTSState getFinalState() {
		return finalState;
	}


	public List<LTSState> getPath() {
		return path;
	}

}
