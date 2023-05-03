## Netty-Telnet

- Netty-Telnet을 사용하여 TCP server와 TCP client 소켓 통신을 구현해봅니다.

<br/>

![image](https://user-images.githubusercontent.com/60170616/232954108-1e620726-df87-4938-9bda-d454dd3f45bc.png)

- `Netty Client`
    - Kafka에 적재된 데이터를 Consume 합니다.
    - Netty Server로 데이터를 전송(소켓 통신)합니다.

- `Netty Server`
    - Netty Client로부터 받은 데이터를 처리합니다. (ex. 로깅, DB insert 등)
