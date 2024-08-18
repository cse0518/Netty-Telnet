## Netty-Telnet

- Netty-Telnet을 사용하여 TCP server와 TCP client 소켓 통신을 구현해봅니다.

<br/>

![image](https://github.com/user-attachments/assets/3afa2d02-5214-4d67-9101-bde0a8be13f6)

- `Netty Client`
    - Kafka에 적재된 데이터를 Consume 합니다.
    - Netty Server로 데이터를 전송(소켓 통신)합니다.

- `Netty Server`
    - Netty Client로부터 받은 데이터를 처리합니다. (ex. 로깅, DB insert 등)
