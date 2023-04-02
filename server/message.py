import datetime
import json

from member import Member


class Message:
   message_id: int = None
   sender: Member = None
   post_date: datetime.datetime = None
   subject: str = None
   content: str = None

   def __init__(self, message_id: int, sender: Member, post_date, subject: str, content: str):
      self.message_id = message_id
      self.sender = sender
      self.post_date = post_date
      self.subject = subject
      self.content = content

   def to_json(self, remove_content: bool):
      copy = Message(self.message_id, self.sender, self.post_date, self.subject, None if remove_content else self.content)
      copy.sender = copy.sender.name
      copy.post_date = str(copy.post_date)
      return json.dumps(copy.__dict__)