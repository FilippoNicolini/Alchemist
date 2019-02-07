receive :- 
  retract(ingoing(S,M)),
  handle(S,M).
 
handle(S,ping) :-
  send(S, pong).
 
handle(_,go_away) :-
  act(forward).
 
send(R, M) :-  
  self(Sender),
  assertz(outgoing(Sender,R, M)).