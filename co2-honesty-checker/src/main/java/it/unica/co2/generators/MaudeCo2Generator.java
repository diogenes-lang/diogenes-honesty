package it.unica.co2.generators;

import org.apache.commons.lang3.StringUtils;

import it.unica.co2.honesty.dto.CO2DataStructures.AskDS;
import it.unica.co2.honesty.dto.CO2DataStructures.DoReceiveDS;
import it.unica.co2.honesty.dto.CO2DataStructures.DoSendDS;
import it.unica.co2.honesty.dto.CO2DataStructures.IfThenElseDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ParallelProcessesDS;
import it.unica.co2.honesty.dto.CO2DataStructures.PrefixDS;
import it.unica.co2.honesty.dto.CO2DataStructures.PrefixPlaceholderDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessCallDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDS;
import it.unica.co2.honesty.dto.CO2DataStructures.ProcessDefinitionDS;
import it.unica.co2.honesty.dto.CO2DataStructures.SumDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TauDS;
import it.unica.co2.honesty.dto.CO2DataStructures.TellDS;


public class MaudeCo2Generator {

	
	public static String toMaude(PrefixDS prefix, String initialSpace) {
		
		if (prefix instanceof AskDS) {
			return toMaude((AskDS)prefix, initialSpace);
		}
		else if (prefix instanceof TellDS) {
			return toMaude((TellDS)prefix, initialSpace);
		}
		else if (prefix instanceof DoReceiveDS) {
			return toMaude((DoReceiveDS)prefix, initialSpace);
			}
		else if (prefix instanceof DoSendDS) {
			return toMaude((DoSendDS)prefix, initialSpace);
		}
		else if (prefix instanceof TauDS) {
			return toMaude((TauDS)prefix, initialSpace);
		}
		else if (prefix instanceof PrefixPlaceholderDS) {
			return toMaude((PrefixPlaceholderDS)prefix, initialSpace);
		}
		
		throw new IllegalStateException("unexpected prefix "+prefix.getClass());
	}
	
	public static String toMaude(ProcessDS process, String initialSpace) {
		
		if (process instanceof ProcessDefinitionDS) {
			return toMaude((ProcessDefinitionDS)process, initialSpace);
		}
		else if (process instanceof ProcessCallDS) {
			return toMaude((ProcessCallDS)process, initialSpace);
		}
		else if (process instanceof SumDS) {
			return toMaude((SumDS)process, initialSpace);
		}
		else if (process instanceof IfThenElseDS) {
			return toMaude((IfThenElseDS)process, initialSpace);
		}
		else if (process instanceof ParallelProcessesDS) {
			return toMaude((ParallelProcessesDS)process, initialSpace);
		}
		
		throw new IllegalStateException("unexpected process "+process.getClass());
	}
	
	
	
	public static String toMaude(AskDS prefix, String initialSpace) {
		return "ask \""+prefix.session+"\" True . "+(prefix.next==null? "0": toMaude(prefix.next, initialSpace));
	}
	
	public static String toMaude(TellDS prefix, String initialSpace) {
		return "tell \""+prefix.session+"\" "+" "+prefix.contractName+" . "+(prefix.next==null? "0": toMaude(prefix.next, initialSpace));
	}

	public static String toMaude(DoReceiveDS prefix, String initialSpace) {
		return "do \""+prefix.session+"\" \""+prefix.action+"\" ? unit . "+(prefix.next==null? "0": toMaude(prefix.next, initialSpace));
	}

	public static String toMaude(DoSendDS prefix, String initialSpace) {
		return "do \""+prefix.session+"\" \""+prefix.action+"\" ! unit . "+(prefix.next==null? "0": toMaude(prefix.next, initialSpace));
	}

	public static String toMaude(TauDS prefix, String initialSpace) {
		return "t . "+(prefix.next==null? "0": toMaude(prefix.next, initialSpace));
	}

	public static String toMaude(PrefixPlaceholderDS prefix, String initialSpace) {
		return prefix.next==null? "0": toMaude(prefix.next, initialSpace);
	}

	public static String toMaude(ProcessDefinitionDS process, String initialSpace) {
		return process.name+"("+StringUtils.join(process.freeNames, " ; ")+ ") =def "+toMaude(process.process, initialSpace);
	}
	
	public static String toMaude(ProcessCallDS process, String initialSpace) {
		return process.ref.name+"("+StringUtils.join(process.ref.freeNames, " ; ")+ ") ";
	}
	
	public static String toMaude(SumDS process, String initialSpace) {
		StringBuilder sb = new StringBuilder();
		
		if (process.prefixes.size()>1) {
			sb.append("\n").append(initialSpace).append("(");
			initialSpace=addTab(initialSpace);
		}
		

		int i=0;
		for (PrefixDS p : process.prefixes) {
			
			if (i++>0) {
				sb.append(" + ");
			}
			
			if (process.prefixes.size()>1) {
				sb.append("\n");
				sb.append(initialSpace);
			}
			sb.append(toMaude(p, initialSpace));
		}
		
		if (process.prefixes.size()>1) {
			initialSpace=removeTab(initialSpace);
			sb.append("\n").append(initialSpace).append(")");
		}
		
		return sb.toString();
	}
	
	public static String toMaude(IfThenElseDS process, String initialSpace) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("\n").append(initialSpace).append("(");
		initialSpace=addTab(initialSpace);
		
		sb.append("\n").append(initialSpace).append("if exp");
		sb.append("\n").append(initialSpace).append("then ").append(process.thenStmt!=null? toMaude(process.thenStmt, initialSpace):"0");
		sb.append("\n").append(initialSpace).append("else ").append(process.elseStmt!=null? toMaude(process.elseStmt, initialSpace):"0");
		
		initialSpace=removeTab(initialSpace);
		sb.append("\n").append(initialSpace).append(")");
		
		return sb.toString();
	}
	
	public static String toMaude(ParallelProcessesDS process, String initialSpace) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("(");		
			sb.append("(").append(toMaude(process.processA, initialSpace)).append(")");
		sb.append("|");		
			sb.append("(").append(toMaude(process.processB, initialSpace)).append(")");
		sb.append(")");
		
		return sb.toString();
	}
	
	private static String TAB = "    ";
	
	private static String addTab(String initialSpace) {
		return TAB+initialSpace;
	}
	
	private static String removeTab(String initialSpace) {
		return initialSpace.replaceFirst(TAB, "");
	}
}
