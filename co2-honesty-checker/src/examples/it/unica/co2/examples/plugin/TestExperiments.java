package it.unica.co2.examples.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.unica.co2.honesty.HonestyChecker;
import it.unica.co2.honesty.HonestyResult;
import it.unica.co2.honesty.Statistics;
import it.unica.co2.model.process.Participant;

public class TestExperiments {
	
	public static void main(String[] args) {
		
		int N_experiments = 10;
		
		@SuppressWarnings("unchecked")
		Class<? extends Participant>[] examples = new Class[]{
			it.unica.co2.examples.plugin.Blackjack.P.class,
			it.unica.co2.examples.plugin.Store.P.class,
			it.unica.co2.examples.plugin.VoucherHonest.P.class,
			it.unica.co2.examples.plugin.VoucherDishonest.P.class
		};
		
		//check
		assert HonestyChecker.isHonest(it.unica.co2.examples.plugin.Blackjack.P.class)==HonestyResult.HONEST;
		assert HonestyChecker.isHonest(it.unica.co2.examples.plugin.Store.P.class)==HonestyResult.HONEST;
		assert HonestyChecker.isHonest(it.unica.co2.examples.plugin.VoucherHonest.P.class)==HonestyResult.HONEST;
		assert HonestyChecker.isHonest(it.unica.co2.examples.plugin.VoucherDishonest.P.class)==HonestyResult.DISHONEST;
		
		Map<Class<? extends Participant>, List<Statistics>> stats = new HashMap<>();
		
		//collect the statistics
		for (Class<? extends Participant> c : examples) {
			
			stats.put(c, new ArrayList<>());
			
			for (int i=0; i<N_experiments; i++) {
				HonestyChecker.isHonest(c);
				stats.get(c).add(HonestyChecker.stats);
			}
		}
		//calculate average
		System.out.println("--------------------------------");
		System.out.println("------------ Stats ------------");
		System.out.println("--------------------------------");
		for (Class<? extends Participant> c : examples) {
			
			System.out.println("Process: "+c.getName());
		
			long sumJPF=0;
			long sumMaude=0;
			long sumTotal=0;
			for (Statistics processStats : stats.get(c)) {
				sumJPF+=processStats.getJPFTime();
				sumMaude+=processStats.getMaudeTime();
				sumTotal+=processStats.getTotalTime();
			}
			System.out.println("JPF:\t"+sumJPF/stats.get(c).size());
			System.out.println("Maude:\t"+sumMaude/stats.get(c).size());
			System.out.println("Total:\t"+sumTotal/stats.get(c).size());
			System.out.println("--------------------------------");
		}

	}

}
