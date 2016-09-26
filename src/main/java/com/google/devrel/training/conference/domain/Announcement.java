package com.google.devrel.training.conference.domain;


public class Announcement {
	private String message;
	public Announcement(String message) {
		this.message = message;
	}
	public String getAnnouncement() {
		return message;
	}
}
