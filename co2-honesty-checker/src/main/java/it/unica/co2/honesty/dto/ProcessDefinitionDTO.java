package it.unica.co2.honesty.dto;

import java.util.ArrayList;
import java.util.List;


public class ProcessDefinitionDTO extends ProcessDTO {

	public String name;
	public List<String> freeNames = new ArrayList<String>();
	
	public ProcessDTO process;
	public PrefixPlaceholderDTO firstPrefix;
	
	public boolean alreadyBuilt = false;
	
}
