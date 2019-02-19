field(5,5,-5,-5).

onReceivedMessage(S,ping) :-
  sendMessage(S, pong).
 
onReceivedMessage(_,go_away) :-
  act(forward).

onPositionUpdated :-
  position(X,Y),
  checkPosition(X,Y).

checkPosition(X,Y) :-
  isInFieldX(X),
  isInFieldY(Y).

isInFieldX(X) :-
  field(T,R,B,L),
  (
    not (X =< R) -> retract(movement(S,D)), D1 is D + 50, asserta(movement(S,D1));
    not (X >= L) -> retract(movement(S,D)), D1 is D - 80, asserta(movement(S,D1));
    true
  ).

isInFieldY(Y) :-
  field(T,R,B,L),
  (
    not (Y =< T) -> retract(movement(S,D)), D1 is D + 20, asserta(movement(S,D1));
    not (Y >= B) -> retract(movement(S,D)), D1 is D - 100, asserta(movement(S,D1));
    true
  ).