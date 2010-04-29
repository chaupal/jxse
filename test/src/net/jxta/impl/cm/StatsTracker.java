package net.jxta.impl.cm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class StatsTracker {

	private static final boolean WRITE_RESULTS_TO_FILE = false;
	
	private File resultsFile;
	private FileWriter writer;
	
	public double min = Double.MAX_VALUE;
	public double max = Double.MIN_VALUE;
	public double mean = 0.0;
	
	/**
	 * Temporary variable used for running calculation of the
	 * standard deviation.
	 */
	private double q = 0.0;
	
	public double stdDev = 0.0;
	public long numResults = 0;
	
	public StatsTracker() {
		if(WRITE_RESULTS_TO_FILE) {
			try {
				resultsFile = File.createTempFile("stats", null);
				writer = new FileWriter(resultsFile);
				System.out.println("Results going to " + resultsFile);
			} catch (IOException e) {
				System.err.println("Failed to open results file");
			}
		}
	}
	
	public synchronized void addResult(Number n) {
		writeResult(n);
		numResults++;
		double newVal = n.doubleValue();
		min = Math.min(min, newVal);
		max = Math.max(max, newVal);
		double lastMean = mean;
		mean += (n.doubleValue() - mean) / numResults;
		q += (newVal - lastMean) * (newVal - mean);
	}
	
	private void writeResult(Number n) {
		if(WRITE_RESULTS_TO_FILE) {
			try {
				writer.write(n.doubleValue() + "\n");
			} catch (IOException e) {
				System.err.println("Failed to write result");
			}
		}
	}

	public void finish() {
		if(WRITE_RESULTS_TO_FILE) {
			try {
				writer.close();
			} catch (IOException e) {
				System.err.println("Failed to close results file");
			}
		}
	}
	
	public double getMin() {
		return min;
	}
	
	public double getMax() {
		return max;
	}
	
	public double getMean() {
		return mean;
	}
	
	public double getStdDev() {
		return Math.sqrt(q / numResults);
	}
	
}
