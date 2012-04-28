package io.socket;

import java.util.Timer;
import java.util.TimerTask;

public interface IOReconnectScheduler {
	public void scheduleReconnect(Timer timer, TimerTask task);
	public void onReconnect();
}
