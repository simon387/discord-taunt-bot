package it.simonecelia.discordtauntbot.enums;

public enum KothTimesEnum {
	UNO ( 5, 0 ),
	DUE ( 8, 0 ),
	TRE ( 10, 30 ),
	QUA ( 13, 0 ),
	CIN ( 17, 0 ),
	SEI ( 23, 0 );

	private final int hours;

	private final int minutes;

	public int getHours () {
		return hours;
	}

	public int getMinutes () {
		return minutes;
	}

	KothTimesEnum ( int hours, int minutes ) {
		this.hours = hours;
		this.minutes = minutes;
	}
}
