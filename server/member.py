import socket


class Member:
   connection_socket: socket = None
   name: str = None

   def __init__(self, connection_socket: socket, name: str):
      self.connection_socket = connection_socket
      self.name = name