import asyncio
import websockets

async def handler(websocket, path):
    async for message in websocket:
        print(f"Received message: {message}")
        await websocket.send(f"Server received: {message}")

async def main():
    port = int(input("Digite a porta para o servidor: "))
    async with websockets.serve(handler, "0.0.0.0", port):
        print(f"Servidor WebSocket rodando na porta {port}")
        await asyncio.Future()  # run forever

if __name__ == "__main__":
    asyncio.run(main())
