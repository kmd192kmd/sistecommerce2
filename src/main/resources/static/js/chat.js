let stompClient = null;

// 1. 웹소켓 연결
function connect() {
    let socket = new SockJS('/ws-chat'); // WebSocketConfig의 엔드포인트
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        // 메시지 구독 (서버에서 이 주소로 데이터를 보냄)
        stompClient.subscribe('/topic/public', function (message) {
            showMessage(JSON.parse(message.body));
        });
    });
}

// 2. 메시지 전송
function sendMessage() {
    let messageContent = document.getElementById('chat-input').value;
    if (messageContent && stompClient) {
        let chatMessage = {
            memberSeq: 1, // 현재 로그인한 유저 ID를 동적으로 넣으세요
            senderType: 'user',
            content: messageContent
        };
        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify(chatMessage));
        document.getElementById('chat-input').value = '';
    }
}

// 3. 화면에 메시지 표시
function showMessage(message) {
    let chatArea = document.getElementById('chat-messages');
    let msgElement = document.createElement('div');
    msgElement.innerText = message.senderType + ": " + message.content;
    chatArea.appendChild(msgElement);
}

connect();