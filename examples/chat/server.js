var http = require('http')
  , io 	 = require('socket.io');

var app = http.createServer();
app.listen(3000);

console.log('Server running at http://127.0.0.1:3000/');

// Socket.IO server
var io = io.listen(app)
	, nicknames = {};
	
io.sockets.on('connection', function (socket) {
	socket.on('user message', function (msg) {
  	socket.broadcast.emit('user message', {user: socket.nickname, message: msg.message});
  });

  socket.on('nickname', function (nick, fn) {
  	nickname = nick.nickname;
    if (nicknames[nickname]) {
      fn(true);
    } else {
      fn(false);
      nicknames[nickname] = socket.nickname = nickname;
      socket.broadcast.emit('announcement', {user: nickname, action: 'connected'});
      io.sockets.emit('nicknames', nicknames);
    }
  });

  socket.on('disconnect', function () {
    if (!socket.nickname) return;

    delete nicknames[socket.nickname];
    socket.broadcast.emit('announcement', {user: socket.nickname, action: 'disconected'});
    socket.broadcast.emit('nicknames', nicknames);
  });
});
