import socket
import threading
import json
import datetime

class Member:
    connection_socket: socket = None
    name: str = None

    def __init__(self, connection_socket: socket, name: str):
        self.connection_socket = connection_socket
        self.name = name

class Message:
    message_id: int = None
    sender: Member = None
    post_date: datetime  = None
    subject: str = None
    content: str = None

    def __init__(self, message_id: int, sender: Member, post_date, subject: str, content: str):
        self.message_id = message_id
        self.sender = sender
        self.post_date = post_date
        self.subject = subject
        self.content = content

    def remove_content(self):
        return Message(self.message_id, self.sender, self.post_date, self.subject, "")

class Group:
    name: str = None
    id: int = None
    members: list[Member] = []
    messages: list[Message] = []

    def __init__(self, name: str, id: int, members: list[Member], messages: list[Message]):
        self.name = name
        self.id = id
        self.members = members
        self.messages = messages

    def get_last_messages(self, messages_to_include: int, remove_content_from_messages: bool):
        return None if messages_to_include == 0 else [
                    m.remove_content() if remove_content_from_messages else m
                    for m in self.messages[(-1 * messages_to_include):]
                ]

class WebServerBase: 
    server = None
    port: int = None
    groups: list[Group] = []
    public_group_id = 0

    def __init__(self, port: int):
        self.start_server(port)

    def handle_command(self, request_map, client_socket: socket):
        response_header = f"{request_map['command']} {request_map['version']}"
        match request_map['command']:
            case "JOIN":
                name = request_map["name"]
                group_match = [g for g in self.groups if request_map['group'] != None and g.id == request_map['group']]
                if (len(group_match) == 0):
                    return f"{response_header} STATUS={400}\n"
                elif (any(member for member in group_match[0].members if member.name == name)):
                    return f"{response_header} STATUS={409}\n"
                elif (name == None):
                    return f"{response_header} STATUS={400}\n"
                
                group_match = group_match[0]
                new_member = Member(client_socket, name)
                for member in group_match.members:
                    member.connection_socket.sendall(f"{response_header} STATUS={201} MEMBERS={json.dumps(new_member.name)}\n".encode())

                group_match.members.append(new_member)
                mapped_members = [member.name for member in group_match.members]
                mapped_messages = [message.remove_content().__dict__ for message in group_match.get_last_messages(2, True)]
                return f"{response_header} STATUS={200} MEMBERS={json.dumps(mapped_members)} MESSAGES={json.dumps(mapped_messages)}\n"
            case "POST":
               return f"{response_header} STATUS={200}\n"
            case "MEMBERS":
                group_match = [g for g in self.groups if request_map['group'] != None and g.id == request_map['group']]
                if (len(group_match) == 0):
                    return f"{response_header} STATUS={404}\n"
                
                group_match = group_match[0]
                mapped_members = [member.name for member in group_match.members]
                return f"{response_header} STATUS={200 if len(mapped_members) > 0 else 204} MEMBERS={json.dumps(mapped_members)}\n"
            case "MESSAGE":
               return f"{response_header} STATUS={200}\n"
            case "GROUPS":
                mapped_groups = [Group(group.name, group.id, [m.name for m in group.members], group.get_last_messages(0, True)).__dict__ for group in self.groups]
                if (request_map["version"] == "BBP/1"):
                    mapped_groups = mapped_groups[0]
                return f"{response_header} STATUS={200} GROUPS={json.dumps(mapped_groups)}\n"
            case "LEAVE":
                group_matches = [g for g in self.groups if request_map['group'] != None and g.id == request_map['group']]
                for group in group_matches:
                    members_in_group_matching_socket = [member for member in group.members if member.connection_socket == client_socket] 
                    if (len(members_in_group_matching_socket) > 0):
                        group.members = [member for member in group.members if member.connection_socket != client_socket]
                        for m in group.members:
                            m.connection_socket.sendall(f"{response_header} STATUS={200} MEMBERS={members_in_group_matching_socket[0].name}".encode())
                    else:
                        return f"{response_header} STATUS={400}\n"
                if (len(group_matches) == 0):
                    return f"{response_header} STATUS={400}\n"
                return f"{response_header} STATUS={200}\n"
            case "EXIT":
               for group in self.groups:
                   if (any(m for m in group.members if m.connection_socket == client_socket)):
                       return f"{response_header} STATUS={403}\n"
               return f"{response_header} STATUS={200}\n"
            case _:
               return f"{response_header} STATUS={405}\n"

    def get_request_map(self, request_line):
        name = [p.split("NAME=")[1] for p in request_line if p.startswith("NAME=")]
        group = [p.split("GROUP=")[1] for p in request_line if p.startswith("GROUP=")]
        message_id = [p.split("MESSAGE_ID=")[1] for p in request_line if p.startswith("MESSAGE_ID=")]
        message_subject = [p.split("MESSAGE_SUBJECT=")[1] for p in request_line if p.startswith("MESSAGE_SUBJECT=")]
        message_content = [p.split("MESSAGE_CONTENT=")[1] for p in request_line if p.startswith("MESSAGE_CONTENT=")]
        
        version = request_line[1] if len(request_line) > 0 else None

        return {
            "command": request_line[0] if len(request_line) >= 0 else None,
            "version": version,
            "name": name[0] if len(name) > 0 else None,
            "group": int(group[0]) if len(group) > 0 and version == "BBP/2" 
                        else self.public_group_id if version == "BBP/1" 
                        else None,
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

        return (response.encode(), request_map)

    def handle_client(self, client_socket, client_address):
        last_command = None
        status = None
        while (not (last_command == "EXIT" and status == "200")):
            try:
                request = client_socket.recv(1024)
            except:
                # TODO: Handle removing clients group members on a force close
                print("Client was forced closed")
                return

            (response, request_map) = self.process_request(request, client_socket)
            status_parsed_response = [p.split("STATUS=")[1] for p in response.decode().split() if p.startswith("STATUS=")]
            status = status_parsed_response[0] if len(status_parsed_response) > 0 else None
            last_command = request_map['command'] if request_map != None else None

            if (last_command != None)
                client_socket.sendall(response)

        print(f"Closing connection to {client_address}")
        client_socket.close()

    def start_server(self, port):
        # Create Groups 
        self.groups.append(Group("Public", self.public_group_id, [], []))
        for i in range(5):
            self.groups.append(Group(f"Group {i + 1}", self.public_group_id + i + 1, [], []))

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