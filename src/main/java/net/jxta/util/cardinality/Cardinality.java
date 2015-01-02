package net.jxta.util.cardinality;

import java.util.Collection;

public class Cardinality {

	private static final String S_ERR_ILLEGAL_VALUE = "The maximunm value may not be smaller than the mininmum: ";
	private static final String S_ERR_NEGATIVE_VALUE = "The value may not be negative: ";
	
	public enum Denominator{
		ONE,
		ONE_TO_N,
		ZERO,
		ZERO_OR_ONE,
		ZERO_TO_N,
		M_TO_N;

		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("[");
			switch( this ){
			case ONE:
				buffer.append("1..1]");
				break;
			case ONE_TO_N:
				buffer.append( "1..n");
				break;
			case M_TO_N:
				buffer.append( "m..n" );
				break;
			case ZERO_TO_N:
				buffer.append( "0..n" );
				break;
			default:
				buffer.append("0..0");
				break;
			}
			buffer.append("]");
			return buffer.toString();
		}
	}

	private Denominator demoninator;
	private int min;
	private int max;

	private Cardinality( Denominator denominator ) {
		this( denominator, 0, 0 );
	}

	private Cardinality( Denominator denominator, int max ) {
		this( denominator, 0, max );
	}

	private Cardinality( Denominator denominator, int min, int max ) {
		super();
		this.demoninator = denominator;
		this.max = max;
		if( this.max < 0 )
			throw new NumberFormatException( S_ERR_NEGATIVE_VALUE + "MAX = " + max);
		this.min = min;
		if( this.min < 0 )
			throw new NumberFormatException( S_ERR_NEGATIVE_VALUE + "MIN = " + min);
		if( this.max < this.min )
			throw new NumberFormatException( S_ERR_ILLEGAL_VALUE + "[" + max + "<" + min + "]");
	}

	public Denominator getDemoninator() {
		return demoninator;
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}	
	
	/**
	 * Create a new cardinality for the given denominator
	 * @param denominator
	 * @return
	 */
	public static final Cardinality create( Denominator denominator ){
		return new Cardinality( denominator );
	}

	/**
	 * Create a new cardinality for the given denominator
	 * @param denominator
	 * @return
	 */
	public static final Cardinality create( Denominator denominator, int maxValue ){
		if( maxValue == 0 )
			return Cardinality.create( denominator );
		else
			return new Cardinality( denominator, maxValue );
	}

	/**
	 * Create a new cardinality for the given denominator
	 * @param denominator
	 * @return
	 */
	public static final Cardinality create( Denominator denominator, int minValue, int maxValue ){
		if( minValue == 0 )
			return Cardinality.create(denominator, maxValue );
		else
			return new Cardinality( denominator, minValue, maxValue );
	}

	/**
	 * Returns true if the master accepts the slave, based on its cardinality
	 * @param master
	 * @param slave
	 * @return
	 */
	public static boolean accept( ICardinality master, ICardinality slave){
		boolean result = false;
		switch( master.getCardinality().getDemoninator() ){
			case ONE_TO_N:
			case ZERO_TO_N:
				result = true;
				break;
			default:
				result = false;
				break;
		}
		return result;
	}

	/**
	 * Returns true if the master accepts the amount of instances, based on its cardinality
	 * @param master
	 * @param slave
	 * @return
	 */
	public static boolean accept( ICardinality master, int amount ){
		boolean result = false;
		Cardinality card = master.getCardinality();
		switch( master.getCardinality().getDemoninator() ){
			case M_TO_N:
				result = ( card.getMin() <= amount ) && ( card.getMax() >= amount ); 
				break;
			case ZERO_TO_N:
				result = ( card.getMax() >= amount );
				break;
			case ONE:
				result = (amount == 1);
				break;
			case ONE_TO_N:
				result = (amount > 1);
				break;
			default:
				result = false;
				break;
		}
		return result;
	}

	/**
	 * Returns true if the collection accepts the new instance. Returns 0 if the instance is accepted,
	 * +1 if the collection rejects the instance, or -1 if the cardinality of the instance does not allow
	 * addition in the collection  
	 * @param ofOne
	 * @param ofTheOther
	 * @return
	 */
	public static int accept( ICardinality[] collection, ICardinality newInstance){
		int found = 0;
		for( ICardinality obj: collection ){
			if( !Cardinality.accept( obj,  newInstance ))
				return 1;
			if( !Cardinality.accept( newInstance, obj ))
				return -1;
			found++;	
		}
		if( Cardinality.accept( newInstance, found))
			return -1;
		for( ICardinality obj: collection ){
			if( Cardinality.accept( obj, found ))
				return 1;
		}
		return 0;
	}

	/**
	 * Returns true if the collection accepts the new instance. Returns 0 if the instance is accepted,
	 * +1 if the collection rejects the instance, or -1 if the cardinality of the instance does not allow
	 * addition in the collection.
	 * The method accepts the instance if it is null or empty 
	 * @param ofOne
	 * @param ofTheOther
	 * @return
	 */
	public static int accept( Collection<ICardinality> collection, ICardinality newInstance){
		if(( collection == null ) || ( collection.isEmpty() ))
			return 0;
		int found = 0;
		for( ICardinality obj: collection ){
			if( !Cardinality.accept( obj,  newInstance ))
				return 1;
			if( !Cardinality.accept( newInstance, obj ))
				return -1;
			found++;	
		}
		if( Cardinality.accept( newInstance, found))
			return -1;
		for( ICardinality obj: collection ){
			if( Cardinality.accept( obj, found ))
				return 1;
		}
		return 0;
	}

}
