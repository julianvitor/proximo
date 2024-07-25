import asyncio
import websockets

async def chat():
    uri = input("Digite o endereço do servidor WebSocket (ex: ws://127.0.0.1:8080): ")
    async with websockets.connect(uri) as websocket:
        while True:
            message = input("Digite a mensagem para enviar: ")
            await websocket.send(message)
            response = await websocket.recv()
            print(f"Resposta do servidor: {response}")

if __name__ == "__main__":
    asyncio.run(chat())
