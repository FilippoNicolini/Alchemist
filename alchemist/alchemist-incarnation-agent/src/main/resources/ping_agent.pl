init :-
    asserta(intention(0,[iSend('pong_agent','ping')])),
    execute(0).

onReceivedMessage(S,pong) :-
    iSend(S, ping).
