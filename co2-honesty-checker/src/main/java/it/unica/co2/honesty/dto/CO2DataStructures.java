package it.unica.co2.honesty.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.unica.co2.generators.MaudeCo2Generator;
import it.unica.co2.model.contract.Sort;

public class CO2DataStructures {

	///////////////////////////////// ABSTRACT CLASSES ///////////////////////////////////
	public static abstract class ProcessDS {

		@Override
		public String toString() {
			return toMaude();
		}

		public String toMaude() {
			return toMaude("");
		}

		public String toMaude(String initialSpace) {
			return MaudeCo2Generator.toMaude(this, initialSpace);
		}
	}
	
	public static abstract class PrefixDS {

		public ProcessDS next;

		@Override
		public String toString() {
			return toMaude();
		}

		public String toMaude() {
			return toMaude("");
		}

		public String toMaude(String initialSpace) {
			return MaudeCo2Generator.toMaude(this, initialSpace);
		}
	}
	
	
	///////////////////////////////// PREFIXES ///////////////////////////////////
	public static class TauDS extends PrefixDS {
		
	}
	
	public static class TellDS extends PrefixDS {
		
		public String session;
		public String contractName;
		
	}

	public static class AskDS extends PrefixDS {

		public String session;

	}

	public static class DoReceiveDS extends PrefixDS {

		public String session;
		public String action;
		public Sort sort;
	}

	public static class DoSendDS extends PrefixDS {

		public String session;
		public String action;
		public Sort sort;

	}

	
	///////////////////////////////// PROCESSES ///////////////////////////////////
	public static class IfThenElseDS extends ProcessDS {

		public PrefixPlaceholderDS thenStmt;
		public PrefixPlaceholderDS elseStmt;
	}

	public static class ParallelProcessesDS extends ProcessDS {

		public ProcessDS processA;
		public ProcessDS processB;
	}

	public static class PrefixPlaceholderDS extends PrefixDS { }

	public static class ProcessCallDS extends ProcessDS {

		public ProcessDefinitionDS ref;
	}

	public static class ProcessDefinitionDS extends ProcessDS {

		public String name;
		public List<String> freeNames = new ArrayList<String>();

		public ProcessDS process;
		public PrefixPlaceholderDS firstPrefix;

		public boolean alreadyBuilt = false;

	}

	public static class SumDS extends ProcessDS {

		public SumDS(PrefixDS... prefixes) {
			this.prefixes.addAll(Arrays.asList(prefixes));
		}

		public List<PrefixDS> prefixes = new ArrayList<>();
	}

}
