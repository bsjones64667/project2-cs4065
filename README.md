# Internet Protocol for Bulletin Board Protocol (BBP) Communication

## Status of This Memo

This document describes an Internet protocol for Bulletin Board Protocol (BBP) communication using pure unicast sockets. This memo provides information for the Internet community.

## Introduction

Bulletin Boards have been used for decades as a way for users to communicate and share information with each other. With the rise of the Internet, Bulletin boards have evolved into online forums and discussion boards. This protocol is designed to facilitate communication between a client and a BBP server using pure unicast sockets.

## Protocol Structure

The protocol follows the following header structure in its requests and responses:

```
**Requests**
<command>
BBP/<version>
NAME=<member name>
GROUP=<group id/name>
MESSAGE_ID=<message id>
MESSAGE_SUBJECT=<message subject text>
MESSAGE_CONTENT=<message content text>

**Responses**
<command>
BBP/<version>
STATUS=<status code>
MEMBERS=<member names>
GROUPS=<group id/names>
MESSAGES=<messages>
```

Each line is separated by CLRF to make parsing the content consistent on both the client and frontend.

Some of these headers are optional depending on the version of the protocol being used and whether the server or client is sending the information. There are 2 versions of BBP, BBP/1 (for task 1) and BBP/2 (for task 2).

## Commands

The following commands are supported by the BBP communication protocol regardless of version:

-  `JOIN`
   -  A JOIN request requires no additional fields in BBP/1 but does require a GROUP field in BBP/2. In both versions the NAME field is also required as well.
   -  A JOIN response will be sent to the client with the STATUS code corresponding if the user joined successfully or not. It will also be sent with the MEMBERS and MESSAGES fields filled with all users in the group and the last two messages sent in the group
-  `POST`
   -  A POST request requires the MESSAGE_SUBJECT and MESSAGE_CONTENT fields. It also requires the GROUP field when sending in BBP/2
   -  A POST response is sent with the STATUS corresponding to whether the POST was sent successfully or not. It also sends with the message, (not including the message content) that was sent as the only message in the MESSAGES field and sends to all members of the group, including the sender. The response sent to the client starting the POST request will have a different status code than the response sent to the other clients in the group to help the client determine if they created the message or not.
-  `MEMBERS`
   -  A MEMBERS request requires no additional fields in BBP/1 but requires the GROUP field in BBP/2
   -  A MEMBERS response requires the STATUS field to signify whether the MEMBERS request was successful or not. It also requires the MEMBERS field to be filled with comma separated names of all users in the group
-  `MESSAGE`
   -  A MESSAGE request requires the MESSAGE_ID field in both versions of BBP and also requires the GROUP field in BBP/2
   -  A MESSAGE response requires the STATUS field to signify whether the message content retrieval was successful and should fill the MESSAGES field with a single message including its message content
-  `GROUPS`
   -  A GROUPS request requires no additional fields
   -  A GROUPS response requires the STATUS field to signify whether the groups request was received successfully and should fill the GROUPS field with the list of all available groups on the BBP server
-  `LEAVE`
   -  A LEAVE request requires no additional fields in BBP/1 but requires the GROUP field in BBP/2
   -  A LEAVE response is sent with the STATUS field corresponding to whether the LEAVE was sent successfully or not. It also sends with the MEMBERS field filled with the user who initiated the LEAVE to all members of the group.
-  `EXIT`
   -  An EXIT request requires no additional fields
   -  An EXIT response is sent with the STATUS field corresponding to whether the EXIT was successful

## BBP Server Design

When building a BBP server a few things must take place to properly utilize the protocol. The server must be able to parse the given requests, allow client connections using sockets, store the connections and messages for the groups it provides, and build responses in the aforementioned structure.

The server should store a dictionary for the groups it supports. Each group in this dictionary should have information about the messages sent in the group and the current users in the group. Something like the following:

```
const groups = [
{
name: "Group 1",
members: [
   <connectionId1>: "connection 1 name in group",
   <connectionId2>: "connection 2 name in group"
],
messages: [
   {
         MessageID: 1,
         Sender: <connectionId1>,
         PostDate: "3/21/2023 9:36am",
         Subject: "Greeting",
         Content: "Hi there, How is everyone?"
   }
]
}
]
```

The server will also need to store a dictionary of its connections and assign an id to them to make sending messages to members of groups easier. This can look like the following:

```
const connections = {
<connectionId1>: WebSocketObject for connection 1,
<connectionId2>: WebSocketObject for connection 2
}
```

These two dictionaries will need to be shared amongst all connection threads because any given connection can manipulate or retrieve these dictionaries

## Status Codes

The server attaches a status code to tell the client whether their requests were successful and if not what went wrong. These status codes are set up in ranges similar to HTTPs status codes.

-  `1xx: Informational`
   -  Request received, continuing process
-  `2xx: Success`
   -  The action was successfully received, and accepted
-  `3xx: Redirection`
   -  Further action must be taken in order to the request
-  `4xx: Client Error`
   -  The request contains bad syntax or cannot fulfilled
-  `5xx: Server Error`
   -  The server failed to fulfill an apparently valid request

For the purposes of BBP we don’t need some of these ranges and some modifications to the status codes defined in HTTP. All the codes of BBP are as follows:

-  `"200": OK`
-  `"201": Created`
-  `"202": Accepted`
-  `"204": No Content`
-  `"400": Bad Request`
-  `"403": Forbidden`
-  `"404": Not Found`
-  `"405": Method Not Allowed`
-  `"409": Conflict`
-  `"500": Internal Server Error`
-  `"501": Not Implemented`
-  `"502": Bad Gateway`
-  `"503": Service Unavailable`

## Client Terminal Commands

-  `%connect`

   -  Syntax: `%connect [address] [port]`
   -  Description: This command is used to connect to a running bulletin board server. The command is followed by the address and port number of the server.

-  `%join`

   -  Syntax: `%join [member_name]`
   -  Description: This command is used to join the single message board. The command is followed by name for the client in the group being joined.

-  `%post`

   -  Syntax: `%post [message_subject] [message_content]`
   -  Description: This command is used to post a message to the board. The command is followed by the message subject and the message content or main body.

-  `%users`

   -  Syntax: `%users`
   -  Description: This command is used to retrieve a list of users in the same group.

-  `%leave`

   -  Syntax: `%leave`
   -  Description: This command is used to leave the group.

-  `%message`

   -  Syntax: `%message [message_id]`
   -  Description: This command is used to retrieve the content of a specific message posted earlier on the board. The command is followed by a message ID.

-  `%exit`
   -  Syntax: `%exit`
   -  Description: This command is used to disconnect from the server and exit the client program.

In addition to the above commands, the following commands use BBP/2 of the protocol:

-  `%groups`

   -  Syntax: `%groups`
   -  Description: This command is used to retrieve a list of all groups that can be joined.

-  `%groupjoin`

   -  Syntax: `%groupjoin [group_id] [member_name]`
   -  Description: This command is used to join a specific group. The command is followed by the group id/name and the name for the client in the group being joined. This command uses BBP/2 only.

-  `%grouppost`

   -  Syntax: `%grouppost [group_id] [message_subject] [message_content]`
   -  Description: This command is used to post a message to a message board owned by a specific group. The command is followed by the group id/name, the message subject, and the message content or main body. This command uses BBP/2 only.

-  `%groupusers`

   -  Syntax: `%groupusers [group_id]`
   -  Description: This command is used to retrieve a list of users in a given group. The command is followed by the group id/name.

-  `%groupleave`

   -  Syntax: `%groupleave [group_id]`
   -  Description: This command is used to leave a specific group. The command is followed by the group id/name.

-  `%groupmessage`
   -  Syntax: `%groupmessage [group_id] [message_id]`
   -  Description: This command is used to retrieve the content of a specific message posted earlier on a message board owned by a specific group. The command is followed by the group id/name and message ID. This command uses BBP/2 only.

## Security Considerations

The protocol does not provide any authentication or encryption mechanisms. Therefore, it is recommended to implement additional security measures to ensure secure communication between the client and server.
