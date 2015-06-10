package it.unica.co2.model.process;

import java.util.ArrayList;
import java.util.List;

public class ParallelProcess extends Process {

private List<Process> processes = new ArrayList<>();
	
	public boolean add(Process system) {
		return processes.add(system);
	}
	
	public boolean remove(Process system) {
		return processes.remove(system);
	}
	
	@Override
	public void run() {
		System.out.println("number of processes: "+processes.size());
		
		for (Process p : processes) {
			System.out.println("starting process "+p);
			new Thread(p).start();
		}
		
	}
	
}
