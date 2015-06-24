package it.unica.co2.trash;

import it.unica.co2.model.process.Process;

import java.util.ArrayList;
import java.util.List;

public class ParallelProcess extends Process {

	protected ParallelProcess(String username) {
		super(username);
	}

	private List<Process> processes = new ArrayList<>();
	
	public boolean add(Process system) {
		return processes.add(system);
	}
	
	public boolean remove(Process system) {
		return processes.remove(system);
	}
	
	@Override
	public void run() {
		logger.log("number of processes: "+processes.size());
		
		for (Process p : processes) {
			logger.log("starting process "+p);
			new Thread(p).start();
		}
		
	}
	
}
