package de.grundid.plusrad.stats;

public class CountAndDistance {

	private int count;
	private double distance;

	public void incCount() {
		count++;
	}

	public void addDistance(double newDistance) {
		distance += newDistance;
	}

	public int getCount() {
		return count;
	}

	public double getDistance() {
		return distance;
	}
}
