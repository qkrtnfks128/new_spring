// 전역 변수들
let stompClient = null;
let currentUserId = null;
let currentRoomId = null;
let localStream = null;
let peerConnections = {};
let rtcConfiguration = {
  iceServers: [
    { urls: "stun:stun.l.google.com:19302" },
    { urls: "stun:stun1.l.google.com:19302" },
  ],
};

// DOM 요소들
const loginPanel = document.getElementById(
  "login-panel"
);
const roomPanel =
  document.getElementById("room-panel");
const conferencePanel = document.getElementById(
  "conference-panel"
);
const userIdInput =
  document.getElementById("userId");
const roomIdInput =
  document.getElementById("roomId");
const loginBtn =
  document.getElementById("login-btn");
const enterRoomBtn = document.getElementById(
  "enter-room-btn"
);
const leaveRoomBtn = document.getElementById(
  "leave-room-btn"
);
const logoutBtn =
  document.getElementById("logout-btn");
const currentRoomIdSpan = document.getElementById(
  "current-room-id"
);
const participantsCountSpan =
  document.getElementById("participants-count");
const videoContainer = document.getElementById(
  "video-container"
);
const localVideo = document.getElementById(
  "local-video"
);

// 이벤트 리스너들
window.onload = init;
loginBtn.addEventListener("click", login);
logoutBtn.addEventListener("click", logout);
enterRoomBtn.addEventListener("click", enterRoom);
leaveRoomBtn.addEventListener("click", leaveRoom);

// WebSocket 연결 및 초기화
function init() {
  // 사용자가 페이지를 나갈 때 연결 종료
  window.addEventListener("beforeunload", () => {
    if (currentRoomId) leaveRoom();
    if (stompClient) logout();
  });
}

// WebRTC 관련 함수들
async function setupLocalStream() {
  try {
    const stream =
      await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: true,
      });
    localStream = stream;
    localVideo.srcObject = stream;
    return true;
  } catch (error) {
    console.error(
      "미디어 장치 접근 오류:",
      error
    );
    alert(
      "카메라 또는 마이크에 접근할 수 없습니다."
    );
    return false;
  }
}

async function createPeerConnection(userId) {
  const peerConnection = new RTCPeerConnection(
    rtcConfiguration
  );
  peerConnections[userId] = peerConnection;

  // 로컬 스트림의 모든 트랙을 피어 연결에 추가
  localStream.getTracks().forEach((track) => {
    peerConnection.addTrack(track, localStream);
  });

  // ICE 후보 생성 처리
  peerConnection.onicecandidate = (event) => {
    if (event.candidate) {
      sendMessage({
        type: "ICE_CANDIDATE",
        fromUserId: currentUserId,
        toUserId: userId,
        roomId: currentRoomId,
        data: event.candidate,
      });
    }
  };

  // 원격 스트림 처리
  peerConnection.ontrack = (event) => {
    let videoEl = document.getElementById(
      `video-${userId}`
    );
    if (!videoEl) {
      const videoItem =
        document.createElement("div");
      videoItem.className = "video-item";
      videoItem.id = `peer-${userId}`;

      videoEl = document.createElement("video");
      videoEl.id = `video-${userId}`;
      videoEl.autoplay = true;
      videoEl.playsInline = true;

      const label = document.createElement("div");
      label.className = "user-label";
      label.innerText = userId;

      videoItem.appendChild(videoEl);
      videoItem.appendChild(label);
      videoContainer.appendChild(videoItem);
    }

    if (videoEl.srcObject !== event.streams[0]) {
      videoEl.srcObject = event.streams[0];
    }
  };

  return peerConnection;
}

async function createOffer(userId) {
  try {
    const peerConnection =
      peerConnections[userId] ||
      (await createPeerConnection(userId));
    const offer =
      await peerConnection.createOffer();
    await peerConnection.setLocalDescription(
      offer
    );

    sendMessage({
      type: "OFFER",
      fromUserId: currentUserId,
      toUserId: userId,
      roomId: currentRoomId,
      data: offer,
    });
  } catch (error) {
    console.error("Offer 생성 오류:", error);
  }
}

async function handleOffer(message) {
  try {
    const fromUserId = message.fromUserId;
    const peerConnection =
      peerConnections[fromUserId] ||
      (await createPeerConnection(fromUserId));
    const offer = message.data;

    await peerConnection.setRemoteDescription(
      new RTCSessionDescription(offer)
    );
    const answer =
      await peerConnection.createAnswer();
    await peerConnection.setLocalDescription(
      answer
    );

    sendMessage({
      type: "ANSWER",
      fromUserId: currentUserId,
      toUserId: fromUserId,
      roomId: currentRoomId,
      data: answer,
    });
  } catch (error) {
    console.error("Offer 처리 오류:", error);
  }
}

async function handleAnswer(message) {
  try {
    const fromUserId = message.fromUserId;
    const peerConnection =
      peerConnections[fromUserId];

    if (peerConnection) {
      const answer = message.data;
      await peerConnection.setRemoteDescription(
        new RTCSessionDescription(answer)
      );
    }
  } catch (error) {
    console.error("Answer 처리 오류:", error);
  }
}

async function handleIceCandidate(message) {
  try {
    const fromUserId = message.fromUserId;
    const peerConnection =
      peerConnections[fromUserId];

    if (peerConnection) {
      await peerConnection.addIceCandidate(
        new RTCIceCandidate(message.data)
      );
    }
  } catch (error) {
    console.error("ICE 후보 처리 오류:", error);
  }
}

// WebSocket 관련 함수들
function connectWebSocket() {
  const socket = new SockJS("/signaling");
  stompClient = Stomp.over(socket);

  stompClient.connect({}, onConnected, onError);
}

function onConnected() {
  stompClient.subscribe(
    `/user/${currentUserId}/queue/messages`,
    onMessageReceived
  );

  sendMessage({
    type: "LOGIN",
    fromUserId: currentUserId,
  });
}

function onError(error) {
  console.error("WebSocket 연결 오류:", error);
  alert(
    "서버에 연결할 수 없습니다. 나중에 다시 시도해주세요."
  );
}

function sendMessage(message) {
  if (!stompClient) return;

  let destination;
  switch (message.type) {
    case "LOGIN":
      destination = "/app/login";
      break;
    case "LOGOUT":
      destination = "/app/logout";
      break;
    case "ENTER_ROOM":
      destination = "/app/room/enter";
      break;
    case "LEAVE_ROOM":
      destination = "/app/room/leave";
      break;
    default:
      destination = "/app/signal";
  }

  stompClient.send(
    destination,
    {},
    JSON.stringify(message)
  );
}

function onMessageReceived(payload) {
  const message = JSON.parse(payload.body);
  console.log("수신된 메시지:", message);

  switch (message.type) {
    case "LOGIN_RESPONSE":
      handleLoginResponse(message);
      break;
    case "LOGOUT_RESPONSE":
      handleLogoutResponse(message);
      break;
    case "ENTER_ROOM_RESPONSE":
      handleEnterRoomResponse(message);
      break;
    case "LEAVE_ROOM_RESPONSE":
      handleLeaveRoomResponse(message);
      break;
    case "BROADCAST_ENTER_ROOM":
      handleBroadcastEnterRoom(message);
      break;
    case "BROADCAST_LEAVE_ROOM":
      handleBroadcastLeaveRoom(message);
      break;
    case "OFFER":
      handleOffer(message);
      break;
    case "ANSWER":
      handleAnswer(message);
      break;
    case "ICE_CANDIDATE":
      handleIceCandidate(message);
      break;
    case "ERROR":
      handleError(message);
      break;
  }
}

// UI 관련 함수들
function showPanel(panel) {
  loginPanel.classList.add("hidden");
  roomPanel.classList.add("hidden");
  conferencePanel.classList.add("hidden");

  panel.classList.remove("hidden");
}

// 시그널링 메시지 처리 함수들
function login() {
  const userId = userIdInput.value.trim();
  if (!userId) {
    alert("사용자 ID를 입력해주세요");
    return;
  }

  currentUserId = userId;
  connectWebSocket();
}

function handleLoginResponse(message) {
  if (message.data && message.data.success) {
    showPanel(roomPanel);
  } else {
    alert("로그인에 실패했습니다.");
    currentUserId = null;
  }
}

function logout() {
  if (stompClient) {
    sendMessage({
      type: "LOGOUT",
      fromUserId: currentUserId,
    });
  }
}

function handleLogoutResponse(message) {
  showPanel(loginPanel);
  userIdInput.value = "";
  currentUserId = null;

  if (stompClient) {
    stompClient.disconnect();
    stompClient = null;
  }
}

async function enterRoom() {
  const roomId = roomIdInput.value.trim();
  if (!roomId) {
    alert("방 ID를 입력해주세요");
    return;
  }

  // 미디어 스트림 설정
  const streamSetupSuccess =
    await setupLocalStream();
  if (!streamSetupSuccess) return;

  sendMessage({
    type: "ENTER_ROOM",
    fromUserId: currentUserId,
    roomId: roomId,
  });
}

function handleEnterRoomResponse(message) {
  if (message.data && message.data.success) {
    currentRoomId = message.roomId;
    showPanel(conferencePanel);

    currentRoomIdSpan.textContent = currentRoomId;
    leaveRoomBtn.disabled = false;

    const roomInfo = message.data.roomInfo;
    participantsCountSpan.textContent =
      roomInfo.userCount;

    // 방에 있는 다른 사용자와 연결 수립
    if (
      roomInfo.users &&
      roomInfo.users.length > 0
    ) {
      roomInfo.users.forEach((userId) => {
        if (userId !== currentUserId) {
          createOffer(userId);
        }
      });
    }
  } else {
    alert("방 입장에 실패했습니다.");
  }
}

function leaveRoom() {
  if (currentRoomId) {
    sendMessage({
      type: "LEAVE_ROOM",
      fromUserId: currentUserId,
      roomId: currentRoomId,
    });
  }
}

function handleLeaveRoomResponse(message) {
  cleanupRoom();
  showPanel(roomPanel);
}

function cleanupRoom() {
  // 모든 피어 연결 닫기
  Object.keys(peerConnections).forEach(
    (userId) => {
      peerConnections[userId].close();
      const peerEl = document.getElementById(
        `peer-${userId}`
      );
      if (peerEl) peerEl.remove();
    }
  );

  peerConnections = {};

  // 로컬 비디오 스트림 정리
  if (localStream) {
    localStream
      .getTracks()
      .forEach((track) => track.stop());
    localStream = null;
  }

  currentRoomId = null;
  leaveRoomBtn.disabled = true;
  roomIdInput.value = "";
  participantsCountSpan.textContent = "0";
}

function handleBroadcastEnterRoom(message) {
  const newUserId = message.fromUserId;

  // 새 참가자가 들어왔을 때 참가자 수 업데이트
  participantsCountSpan.textContent =
    parseInt(participantsCountSpan.textContent) +
    1;

  // 새 참가자에게 Offer 생성
  createOffer(newUserId);
}

function handleBroadcastLeaveRoom(message) {
  const userId = message.fromUserId;

  // 참가자가 나갔을 때 참가자 수 업데이트
  const currentCount = parseInt(
    participantsCountSpan.textContent
  );
  participantsCountSpan.textContent = Math.max(
    0,
    currentCount - 1
  );

  // Peer Connection 정리
  if (peerConnections[userId]) {
    peerConnections[userId].close();
    delete peerConnections[userId];
  }

  // 비디오 요소 제거
  const peerEl = document.getElementById(
    `peer-${userId}`
  );
  if (peerEl) peerEl.remove();
}

function handleError(message) {
  console.error("서버 오류:", message.data);
  alert(
    "오류가 발생했습니다: " +
      (message.data.message || "알 수 없는 오류")
  );
}
