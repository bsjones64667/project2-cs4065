import datetime
import json
import re
import socket
import threading

from group import Group
from member import Member
from message import Message

status_codes = {
   "OK": "200",
   "Created": "201",
   "Accepted": "202",
   "NoContent": "204",
   "BadRequest": "400",
   "Forbidden": "403",
   "NotFound": "404",
   "MethodNotAllowed": "405",
   "Conflict": "409",
   "InternalServerError": "500",
   "NotImplemented": "501",
   "BadGateway": "502",
   "ServiceUnavailable": "503",
}


class WebServerBase: 
   server = None
   port: int = None
   groups: list[Group] = []
   public_group_id = 0

   def __init__(self, port: int):
      self.start_server(port)

   def handle_join(self, request_map, response_header, client_socket: socket):
      name = request_map["name"]
      group_match = [g for g in self.groups if request_map['group'] != None and g.id == request_map['group']]
      if (len(group_match) == 0):
         return f"{response_header} STATUS={status_codes['BadRequest']}\n"
      elif (any(member for member in group_match[0].members if member.name == name or member.connection_socket == client_socket)):
         return f"{response_header} STATUS={status_codes['Conflict']}\n"
      elif (name == None):
         return f"{response_header} STATUS={status_codes['BadRequest']}\n"
      
      group_match = group_match[0]
      new_member = Member(client_socket, name)
      for member in group_match.members:
         member.connection_socket.sendall(f"{response_header} STATUS={status_codes['Created']} MEMBERS={json.dumps(new_member.name)}\n".encode())

      group_match.members.append(new_member)
      mapped_members = [member.name for member in group_match.members]
      mapped_messages = [message for message in group_match.get_last_messages(2, True)]
      return f"{response_header} STATUS={status_codes['OK']} MEMBERS={json.dumps(mapped_members)} MESSAGES={mapped_messages}\n"

   def handle_post(self, request_map, response_header, client_socket: socket):
      subject = request_map["message_subject"]
      content = request_map["message_content"]
      group = request_map["group"]

      if (subject == None or content == None or group == None):
         return f"{response_header} STATUS={status_codes['BadRequest']}\n"
      
      new_message = None
      for g in self.groups:
         if (g.id == group):
            member_matches = [m for m in g.members if m.connection_socket == client_socket]
            if (len(member_matches) == 0):
               return f"{response_header} STATUS={status_codes['Forbidden']}\n"
            new_message = Message(len(g.messages), member_matches[0], datetime.datetime.now(), subject, content)
            g.messages.append(new_message)
            for m in g.members:
               if (m.connection_socket != client_socket):
                  m.connection_socket.sendall(f"{response_header} STATUS={status_codes['Created']} MESSAGES={new_message.to_json(True)}\n".encode())

      if (new_message == None):
         f"{response_header} STATUS={status_codes['BadRequest']} MESSAGES={None}\n"

      return f"{response_header} STATUS={status_codes['Created']} MESSAGES={new_message.to_json(True)}\n"

   def handle_members(self, request_map, response_header):
      group_match = [g for g in self.groups if request_map['group'] != None and g.id == request_map['group']]
      if (len(group_match) == 0):
         return f"{response_header} STATUS={status_codes['NotFound']}\n"
      
      group_match = group_match[0]
      mapped_members = [member.name for member in group_match.members]
      return f"{response_header} STATUS={status_codes['OK'] if len(mapped_members) > 0 else status_codes['NoContent']} MEMBERS={json.dumps(mapped_members)}\n"

   def handle_message(self, request_map, response_header):
      message_id = request_map["message_id"]
      group = request_map["group"]

      if (message_id == None or group == None):
         return f"{response_header} STATUS={status_codes['BadRequest']} MESSAGES={None}\n"

      for g in self.groups:
         if (g.id == group):
            message_matches = [m for m in g.messages if m.message_id == message_id]
            if (len(message_matches) == 0):
               return f"{response_header} STATUS={status_codes['BadRequest']} MESSAGES={None}\n"
            return f"{response_header} STATUS={status_codes['OK']} MESSAGES={message_matches[0].to_json(False)}\n"

      return f"{response_header} STATUS={status_codes['BadRequest']} MESSAGES={None}\n"

   def handle_groups(self, request_map, response_header):
      mapped_groups = [Group(group.name, group.id, [m.name for m in group.members], group.get_last_messages(0, True)).__dict__ for group in self.groups]
      if (request_map["version"] == "BBP/1"):
         mapped_groups = mapped_groups[0]
      return f"{response_header} STATUS={status_codes['OK']} GROUPS={json.dumps(mapped_groups)}\n"

   def handle_leave(self, request_map, response_header, client_socket: socket):
      group_matches = [g for g in self.groups if request_map['group'] != None and g.id == request_map['group']]
      for group in group_matches:
         members_in_group_matching_socket = [member for member in group.members if member.connection_socket == client_socket] 
         if (len(members_in_group_matching_socket) > 0):
            group.members = [member for member in group.members if member.connection_socket != client_socket]
            for m in group.members:
                  m.connection_socket.sendall(f"{response_header} STATUS={status_codes['OK']} MEMBERS={members_in_group_matching_socket[0].name}\n".encode())
         else:
            return f"{response_header} STATUS={status_codes['BadRequest']}\n"
      if (len(group_matches) == 0):
         return f"{response_header} STATUS={status_codes['BadRequest']}\n"
      return f"{response_header} STATUS={status_codes['OK']}\n"

   def handle_force_leave(self, client_socket: socket):
      response_header = f"LEAVE BBP/2"
      for group in self.groups:
         members_in_group_matching_socket = [member for member in group.members if member.connection_socket == client_socket] 
         if (len(members_in_group_matching_socket) > 0):
            group.members = [member for member in group.members if member.connection_socket != client_socket]
            for m in group.members:
                  m.connection_socket.sendall(f"{response_header} STATUS={status_codes['OK']} MEMBERS={members_in_group_matching_socket[0].name}\n".encode())

   def handle_exit(self, response_header, client_socket: socket):
      for group in self.groups:
         if (any(m for m in group.members if m.connection_socket == client_socket)):
            return f"{response_header} STATUS={status_codes['Forbidden']}\n"
      return f"{response_header} STATUS={status_codes['OK']}\n"

   def handle_command(self, request_map, client_socket: socket):
      response_header = f"{request_map['command']} {request_map['version']}"
      match request_map['command']:
         case "JOIN":
            return self.handle_join(request_map, response_header, client_socket)
         case "POST":
            return self.handle_post(request_map, response_header, client_socket)
         case "MEMBERS":
            return self.handle_members(request_map, response_header)
         case "MESSAGE":
            return self.handle_message(request_map, response_header)
         case "GROUPS":
            return self.handle_groups(request_map, response_header)
         case "LEAVE":
            return self.handle_leave(request_map, response_header, client_socket)
         case "EXIT":
            return self.handle_exit(response_header, client_socket)
         case _:
            return f"{response_header} STATUS={status_codes['MethodNotAllowed']}\n"

   def get_request_map(self, request):
      # Define the regex pattern to match the string
      pattern = r'(?P<command>[^ ]+) (?P<version>BBP/\d+)?(?: NAME=(?P<name>[^=\s]+(?:\s(?!(?:GROUP|MESSAGE_ID|MESSAGE_SUBJECT|MESSAGE_CONTENT))[^=\s]+)*))?(?: GROUP=(?P<group>\d+))?(?: MESSAGE_ID=(?P<message_id>\d+))?(?: MESSAGE_SUBJECT=(?P<message_subject>[^=\s]+(?:\s(?!(?:GROUP|MESSAGE_ID|NAME|MESSAGE_CONTENT))[^=\s]+)*))?(?: MESSAGE_CONTENT=(?P<message_content>[^=\s]+(?:\s(?!(?:GROUP|MESSAGE_ID|MESSAGE_SUBJECT|NAME))[^=\s]+)*))?'

      # Use re.match to extract the values from the input string using the pattern
      match = re.match(pattern, request.decode())

      # Create a dictionary from the extracted values
      result = match.groupdict()

      if (result['version'] == "BBP/1"):
         result['group']= self.public_group_id
      elif(result['command'] != "GROUPS"):
         result['group']= int(result['group'])
      if (result['message_id'] != None):
         result['message_id']= int(result['message_id'])

      return result

   def process_request(self, request, client_socket):
      request_map = self.get_request_map(request)
      if (request_map['command'] == None or request_map['version'] == None):
         return ("\n".encode(), None)
      response = self.handle_command(request_map, client_socket)

      return (response.encode(), request_map)

   def handle_client(self, client_socket, client_address):
      last_command = None
      status = None
      while (not (last_command == "EXIT" and status == status_codes['OK'])):
         try:
               request = client_socket.recv(1024)
         except:
               self.handle_force_leave(client_socket)
               print("Client was forced closed")
               return

         (response, request_map) = self.process_request(request, client_socket)
         status_parsed_response = [p.split("STATUS=")[1] for p in response.decode().split() if p.startswith("STATUS=")]
         status = status_parsed_response[0] if len(status_parsed_response) > 0 else None
         last_command = request_map['command'] if request_map != None else None

         if (last_command != None):
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