init :- send('pong_agent','ping').

receive :-
  retract(ingoing(S,M)),
  handle(S,M).

handle(S,pong) :-
  send(S, ping).

handle(_,go_away) :-
  act(forward).

send(R, M) :-
  self(Sender),
  assertz(outgoing(Sender,R, M)).