package it.unica.co2.honesty.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SumDTO extends ProcessDTO {

	public SumDTO(PrefixDTO... prefixes) {
		this.prefixes.addAll(Arrays.asList(prefixes));
	}

	public List<PrefixDTO> prefixes = new ArrayList<>();
	
	@Override
	public ProcessDTO copy() {
		SumDTO sum = new SumDTO();
		sum.prefixes = new ArrayList<PrefixDTO>(prefixes.size());
		for (PrefixDTO p : prefixes)
			sum.prefixes.add(p.copy());
		return sum;
	}

}
