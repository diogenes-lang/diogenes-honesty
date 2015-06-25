package it.unica.co2.honesty.dto;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;


public class SumDTO extends ProcessDTO {

	public List<PrefixDTO> prefixes = new ArrayList<>();
	
	@Override
	public String toMaude() {
		
		List<String> prefixesMaude = new ArrayList<String>();

		for (PrefixDTO p : prefixes) {
			prefixesMaude.add(p.toMaude());
		}
		
		return "( "+StringUtils.join(prefixesMaude, " + ")+" )";
	}

	@Override
	public ProcessDTO copy() {
		SumDTO sum = new SumDTO();
		sum.prefixes = new ArrayList<PrefixDTO>(prefixes.size());
		for (PrefixDTO p : prefixes)
			sum.prefixes.add(p.copy());
		return sum;
	}

}
