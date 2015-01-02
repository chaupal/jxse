package net.jxta.util.cardinality;

public class CardinalityException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private static final String S_ERR_CARDINALITY = "Cardinality exclusion: ";
	
	private ICardinality master;
	private ICardinality slave;
	private int result;

	public CardinalityException( Object source, ICardinality slave, int result) {
		super();
		this.slave = slave;
		this.result = result;
	}

	public CardinalityException( Object source, ICardinality master, ICardinality slave, int result) {
		super();
		this.master = master;
		this.slave = slave;
		this.result = result;
	}

	@Override
	public String getMessage() {
		StringBuffer buffer = new StringBuffer();
		buffer.append( S_ERR_CARDINALITY + "\n");
		if( master != null )
			buffer.append( "\tMaster " + master.getCardinality().toString() + "\n");
		else
			buffer.append( "The slave was not accepted\n");
		buffer.append( "\tSlave " + slave.getCardinality().toString() + "\n");
		if( result > 0 )
			buffer.append("\t => master rejects the slave\n");
		if( result < 0 )
			buffer.append("\t => slave rejects the master\n");
		
			return super.getMessage();
	}	
}
