package it.unica.co2.examples.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.unica.co2.api.process.Participant;
import it.unica.co2.honesty.HonestyChecker;
import it.unica.co2.honesty.HonestyResult;
import it.unica.co2.honesty.Statistics;

public class TestExperiments {
	
	public static void main(String[] args) {
		
		int N_experiments = 10;
		
		@SuppressWarnings("unchecked")
		Class<? extends Participant>[] examples = new Class[]{
			it.unica.co2.examples.plugin.Blackjack.P.class,
			it.unica.co2.examples.plugin.OnlineStore.P.class,
			it.unica.co2.examples.plugin.VoucherHonest.P.class,
			it.unica.co2.examples.plugin.VoucherDishonest.P.class
		};
		
		//check
		assert HonestyChecker.isHonest(it.unica.co2.examples.plugin.Blackjack.P.class)==HonestyResult.HONEST;
		assert HonestyChecker.isHonest(it.unica.co2.examples.plugin.OnlineStore.P.class)==HonestyResult.HONEST;
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
			List<Long> jpfTimes = new ArrayList<>();
			List<Long> maudeTimes = new ArrayList<>();
			List<Long> totalTimes = new ArrayList<>();
			for (Statistics processStats : stats.get(c)) {				
				sumJPF+=processStats.getJPFTime();
				sumMaude+=processStats.getMaudeTime();
				sumTotal+=processStats.getTotalTime();
				
				jpfTimes.add(processStats.getJPFTime());
				maudeTimes.add(processStats.getMaudeTime());
				totalTimes.add(processStats.getTotalTime());
			}
			System.out.println("all JPF:\t"+jpfTimes);
			System.out.println("all Maude:\t"+maudeTimes);
			System.out.println("all Total:\t"+totalTimes);
			
			System.out.println("JPF:\t"+sumJPF/stats.get(c).size());
			System.out.println("Maude:\t"+sumMaude/stats.get(c).size());
			System.out.println("Total:\t"+sumTotal/stats.get(c).size());
			System.out.println("--------------------------------");
		}

	}

}
