package it.unica.co2.honesty.dto;

import java.util.ArrayList;
import java.util.List;


public class ProcessDefinitionDS extends ProcessDS {

	public String name;
	public List<String> freeNames = new ArrayList<String>();
	
	public ProcessDS process;
	public PrefixPlaceholderDS firstPrefix;
	
	public boolean alreadyBuilt = false;
	
}
