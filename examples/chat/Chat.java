package chat;

import io.socket.SocketIO;

import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

public class Chat extends Thread {
    private SocketIO socket;
    private ChatCallback callback;
    
    public Chat(ChatCallbackAdapter callback) {
        this.callback = new ChatCallback(callback);
    }
    
    @Override
    public void run() {
        try {
			socket = new SocketIO("http://localhost:3000", callback);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
    }
    
    public void sendMessage(String message) {
        try {
            JSONObject json = new JSONObject();
            json.putOpt("message", message);
            socket.emit("user message", json);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }
    
    public void join(String nickname) {
        try {
            JSONObject json = new JSONObject();
            json.putOpt("nickname", nickname);
            socket.emit("nickname", callback, json);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
