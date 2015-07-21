package it.unica.co2.generators;

import it.unica.co2.honesty.dto.AskDTO;
import it.unica.co2.honesty.dto.DoReceiveDTO;
import it.unica.co2.honesty.dto.DoSendDTO;
import it.unica.co2.honesty.dto.IfThenElseDTO;
import it.unica.co2.honesty.dto.PrefixDTO;
import it.unica.co2.honesty.dto.ProcessDTO;
import it.unica.co2.honesty.dto.ProcessDefinitionDTO;
import it.unica.co2.honesty.dto.SumDTO;
import it.unica.co2.honesty.dto.TauDTO;
import it.unica.co2.honesty.dto.TellDTO;

import org.apache.commons.lang3.StringUtils;


public class MaudeCo2Generator {

	
	public static String toMaude(PrefixDTO prefix, String initialSpace) {
		
		if (prefix instanceof AskDTO) {
			return toMaude((AskDTO)prefix, initialSpace);
		}
		else if (prefix instanceof TellDTO) {
			return toMaude((TellDTO)prefix, initialSpace);
		}
		else if (prefix instanceof DoReceiveDTO) {
			return toMaude((DoReceiveDTO)prefix, initialSpace);
			}
		else if (prefix instanceof DoSendDTO) {
			return toMaude((DoSendDTO)prefix, initialSpace);
		}
		else if (prefix instanceof TauDTO) {
			return toMaude((TauDTO)prefix, initialSpace);
		}
		
		throw new IllegalStateException("unexpected prefix "+prefix.getClass());
	}
	
	public static String toMaude(ProcessDTO process, String initialSpace) {
		
		if (process instanceof ProcessDefinitionDTO) {
			return toMaude((ProcessDefinitionDTO)process, initialSpace);
		}
		else if (process instanceof SumDTO) {
			return toMaude((SumDTO)process, initialSpace);
		}
		else if (process instanceof IfThenElseDTO) {
			return toMaude((IfThenElseDTO)process, initialSpace);
		}
		
		throw new IllegalStateException("unexpected process "+process.getClass());
	}
	
	
	
	public static String toMaude(AskDTO prefix, String initialSpace) {
		return "ask \""+prefix.session+"\" (True) . "+(prefix.next==null? "0": toMaude(prefix.next, initialSpace));
	}
	
	public static String toMaude(TellDTO prefix, String initialSpace) {
		return "tell \""+prefix.session+"\" "+" "+prefix.contractName+" . "+(prefix.next==null? "0": toMaude(prefix.next, initialSpace));
	}

	public static String toMaude(DoReceiveDTO prefix, String initialSpace) {
		return "do \""+prefix.session+"\" \""+prefix.action+"\" ? unit . "+(prefix.next==null? "0": toMaude(prefix.next, initialSpace));
	}

	public static String toMaude(DoSendDTO prefix, String initialSpace) {
		return "do \""+prefix.session+"\" \""+prefix.action+"\" ! unit . "+(prefix.next==null? "0": toMaude(prefix.next, initialSpace));
	}

	public static String toMaude(TauDTO prefix, String initialSpace) {
		return "t . "+(prefix.next==null? "0": toMaude(prefix.next, initialSpace));
	}

	public static String toMaude(ProcessDefinitionDTO process, String initialSpace) {
		return process.name+"("+StringUtils.join(process.freeNames, " ; ")+")"+(process.isDefinition?" =def "+toMaude(process.process, initialSpace): "");
	}
	
	public static String toMaude(SumDTO process, String initialSpace) {
		StringBuilder sb = new StringBuilder();
		
		if (process.prefixes.size()>1) {
			sb.append("\n");
			sb.append(initialSpace);
			sb.append("(");
			initialSpace=addTab(initialSpace);
		}
		

		int i=0;
		for (PrefixDTO p : process.prefixes) {
			
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
			sb.append("\n");
			sb.append(initialSpace);
			sb.append(")");
		}
		
		return sb.toString();
	}
	
	public static String toMaude(IfThenElseDTO process, String initialSpace) {
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
	
	
	private static String TAB = "    ";
	
	private static String addTab(String initialSpace) {
		return TAB+initialSpace;
	}
	
	private static String removeTab(String initialSpace) {
		return initialSpace.replaceFirst(TAB, "");
	}
}
