import asyncio
import websockets

async def handler(websocket, path):
    print(f"Client connected from {websocket.remote_address}")
    try:
        async for message in websocket:
            print(f"Received message: {message}")
            await websocket.send(f"Message received: {message}")
    except websockets.ConnectionClosed:
        print(f"Client disconnected from {websocket.remote_address}")

async def main():
    # Escuta em todas as interfaces de rede
    server = await websockets.serve(handler, '0.0.0.0', 5000)
    print("WebSocket server started on ws://0.0.0.0:5000")
    await server.wait_closed()

if __name__ == "__main__":
    asyncio.run(main())
