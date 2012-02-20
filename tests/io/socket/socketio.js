var port = parseInt(process.argv[process.argv.length - 1]);

var io = require('socket.io').listen(port);
var stdin = process.openStdin();

stdin.setEncoding('utf8');

stdin.on('data', function (chunk) {
  process.stderr.write(chunk);
});



var main = io.sockets.on('connection', function(socket) {
	socket.on('echo', function(data) {
		socket.emit('echo', data);
	});
	socket.on('echoSend', function(data) {
		if(typeof data == 'object') {
			socket.send(JSON.parse(JSON.stringify(data)));
		}
		else {
			socket.send(data);
		}
	});
	socket.on('echoAck', function(data, ack) {
		ack(data);
	});
	socket.on('message', function(m) {
		process.stdout.write("__:MESSAGE:"+m+"\n");
	});
});

var ns1 = io.of('/ns1').on('connection', function(socket) {
	main.send("ns1");
	ns2.send("ns1");
});

var ns2 = io.of('/ns2').on('connection', function(socket) {
	main.send("ns2");
	ns1.send("ns2");
});

process.stdout.write("__:OK\n");
