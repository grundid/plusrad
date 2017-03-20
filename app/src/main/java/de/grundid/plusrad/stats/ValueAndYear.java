package de.grundid.plusrad.stats;

public class ValueAndYear {

	private int value;
	private int year;

	public ValueAndYear(int value, int year) {
		this.value = value;
		this.year = year;
	}

	public int getValue() {
		return value;
	}

	public int getYear() {
		return year;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ValueAndYear that = (ValueAndYear)o;
		if (value != that.value)
			return false;
		return year == that.year;
	}

	@Override
	public int hashCode() {
		int result = value;
		result = 31 * result + year;
		return result;
	}
}
