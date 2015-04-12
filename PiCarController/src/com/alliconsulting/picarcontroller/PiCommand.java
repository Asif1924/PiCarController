package com.alliconsulting.picarcontroller;

public class PiCommand {
	public String toString(){
		return "action = " + action + "; speed = " + speed;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	String 	action;
	int		speed;
	String	state;
}
