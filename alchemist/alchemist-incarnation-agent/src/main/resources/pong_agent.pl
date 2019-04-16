init :-
    true.

onReceivedMessage(S,ping) :-
    iSend(S, pong).