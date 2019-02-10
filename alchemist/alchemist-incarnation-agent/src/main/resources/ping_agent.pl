field(5,5,-5,-5).

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

isInFieldX(X) :-
  field(T,R,B,L),
  (
    not (X =< R) -> asserta(reachedLimit('R'));
    not (X >= L) -> asserta(reachedLimit('L'));
    true
  ).

isInFieldY(Y) :-
  field(T,R,B,L),
  (
    not (Y =< T) -> asserta(reachedLimit('T'));
    not (Y >= B) -> asserta(reachedLimit('B'));
    true
  ).

checkPosition(X,Y) :-
  isInFieldX(X),
  isInFieldY(Y).