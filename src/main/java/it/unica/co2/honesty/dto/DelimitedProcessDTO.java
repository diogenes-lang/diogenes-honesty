package it.unica.co2.honesty.dto;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;


public class DelimitedProcessDTO extends ProcessDTO {

	public String name;
	public List<String> freeNames = new ArrayList<String>();
	
	public ProcessDTO process;
	public PrefixDTO firstPrefix;
	
	@Override
	public String toMaude() {
		return name+"("+StringUtils.join(freeNames, " ; ")+") =def "+process.toMaude();
	}

	@Override
	public ProcessDTO copy() {
		DelimitedProcessDTO tmp = new DelimitedProcessDTO();
		tmp.name = name;
		tmp.freeNames = new ArrayList<String>(freeNames.size());
		for (String s : freeNames)
			tmp.freeNames.add(s);
		tmp.process = process.copy();
		return tmp;
	}

}
