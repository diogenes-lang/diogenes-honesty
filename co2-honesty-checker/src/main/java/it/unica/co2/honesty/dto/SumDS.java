package it.unica.co2.honesty.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SumDS extends ProcessDS {

	public SumDS(PrefixDS... prefixes) {
		this.prefixes.addAll(Arrays.asList(prefixes));
	}

	public List<PrefixDS> prefixes = new ArrayList<>();
	
}
