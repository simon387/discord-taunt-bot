package it.simonecelia.discordtauntbot.enums;

public enum KothTimesEnum {
	VALUE0 ( 5, 0 ),
	VALUE1 ( 8, 0 ),
	VALUE2 ( 10, 30 ),
	VALUE3 ( 13, 0 ),
	VALUE4 ( 17, 0 ),
	VALUE5 ( 23, 0 );

	private final int hours;

	private final int minutes;

	KothTimesEnum ( int hours, int minutes ) {
		this.hours = hours;
		this.minutes = minutes;
	}

	public int getHours () {
		return hours;
	}

	public int getMinutes () {
		return minutes;
	}
}
