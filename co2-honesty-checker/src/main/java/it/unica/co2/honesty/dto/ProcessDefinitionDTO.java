package it.unica.co2.honesty.dto;

import java.util.ArrayList;
import java.util.List;


public class ProcessDefinitionDTO extends ProcessDTO {

	public String name;
	public List<String> freeNames = new ArrayList<String>();
	
	public boolean isDefinition = true;
	
	public ProcessDTO process;
	public PrefixDTO firstPrefix;
	
	@Override
	public ProcessDTO copy() {
		ProcessDefinitionDTO tmp = new ProcessDefinitionDTO();
		tmp.name = name;
		tmp.freeNames = new ArrayList<String>(freeNames.size());
		for (String s : freeNames)
			tmp.freeNames.add(s);
		tmp.process = process.copy();
		tmp.firstPrefix = firstPrefix.copy();
		return tmp;
	}

}
