from member import Member
from message import Message


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
                  m.to_json(True) if remove_content_from_messages else m
                  for m in self.messages[(-1 * messages_to_include):]
               ]