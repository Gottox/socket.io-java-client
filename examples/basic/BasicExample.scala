import io.socket._
import org.json.JSONException
import org.json.JSONObject

/*
 * socket.io-java-client Test.scala
 *
 * Test application port to Scala
 * socket.io-java-client is a implementation of the socket.io protocol in Java.
 * 
 */

object HelloSocketIO {
	def main(args: Array[String]) {
	  var socket = new SocketIO("http://127.0.0.1:3000")
	  socket.connect(new IOCallback() {
	    
	    @Override
	    def onMessage(json: JSONObject, ack: IOAcknowledge ){
	      try println("Server said" + json.toString())
	      catch {
	        case e: JSONException => println(e)
	      }
	    }
	    
	    @Override
	    def onMessage(data: String, ack: IOAcknowledge)= 
	      println("server said" + data)
	    
	    @Override
	    def onError(sIoException: SocketIOException){
	      println("error ocurred")
	      sIoException.printStackTrace()
	    }
	    
	    @Override
	    def onDisconnect()= println("disconnected")
	    
	    @Override
	    def onConnect()= println("connection stabilished")
	    
	    @Override
	    def on(event: String, ack: IOAcknowledge, args: Object*) = println("event")
	  })
	  socket.send("hello world")
	}
}

