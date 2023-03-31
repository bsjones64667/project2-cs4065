import socket
import threading

class WebServerBase: 
    server = None
    port = None
    connections = []

    def __init__(self, port):
        self.start_server(port)

    def handle_command(self, request_map, client_socket):
        response = None
        match request_map['command']:
            case "CONNECT":
                self.store_connection(client_socket)
            case "JOIN":
                response = None
            case "POST":
                response = None
            case "USERS":
                response = None
            case "MESSAGE":
                response = None
            case "GROUPS":
                response = None
            case "LEAVE":
                response = None
            case "EXIT":
                response = None
            case _:
                response = None

        return response

    def get_request_map(self, request_line):
        host = [p.split("HOST=")[1] for p in request_line if p.startswith("HOST=")]
        user = [p.split("USER=")[1] for p in request_line if p.startswith("USER=")]
        group = [p.split("GROUP=")[1] for p in request_line if p.startswith("GROUP=")]
        message_id = [p.split("MESSAGE_ID=")[1] for p in request_line if p.startswith("MESSAGE_ID=")]
        message_subject = [p.split("MESSAGE_SUBJECT=")[1] for p in request_line if p.startswith("MESSAGE_SUBJECT=")]
        message_content = [p.split("MESSAGE_CONTENT=")[1] for p in request_line if p.startswith("MESSAGE_CONTENT=")]
        
        return {
            "command": request_line[0],
            "version": request_line[1],
            "host": host[0] if len(host) > 0 else None,
            "user": user[0] if len(user) > 0 else None,
            "group": group[0] if len(group) > 0 else None,
            "message_id": message_id[0] if len(message_id) > 0 else None,
            "message_subject": message_subject[0] if len(message_subject) > 0 else None,
            "message_content": message_content[0] if len(message_content) > 0 else None,
        }

    def process_request(self, request, client_socket):
        request_line = request.decode().split()
        if (len(request_line) == 0):
            return ("\n".encode(), None)
        request_map = self.get_request_map(request_line)
        response = self.handle_command(request_map, client_socket)

        print(response, request_map)

        return ("\n".encode(), request_map)
    
    def store_connection(self, client_socket):
        self.connections.append(client_socket);

    def handle_client(self, client_socket, client_address):
        while (True):
            request = client_socket.recv(1024)

            (response, request_map) = self.process_request(request, client_socket)
            client_socket.sendall(response)
    
            if (request_map['command'] == "LEAVE"):
                self.connections.remove(client_socket)
                break

        client_socket.close()

    def start_server(self, port):
        self.port = port
        self.server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server.bind(('', self.port))
        self.server.listen(1)
        print(f"Server listening on port {port}")

        while True:
            client_socket, client_address = self.server.accept()
            print(f"Accepted connection from {client_address}")
            client_thread = threading.Thread(target=self.handle_client, args=(client_socket, client_address))
            client_thread.start()

if __name__ == '__main__':
    WebServerBase(3000)