field(5,5,-5,-5).

onReceivedMessage(S,ping) :-
  sendMessage(S, pong).
 
onReceivedMessage(_,go_away) :-
  act(forward).

onAddBelief(position(X,Y)) :-
  checkPosition(X,Y).

checkPosition(X,Y) :-
  isInFieldX(X),
  isInFieldY(Y).

isInFieldX(X) :-
  field(T,R,B,L),
  (
    not (X =< R) -> removeBelief(movement(S,D)), D1 is D + 25, D2 is mod(D1,360), addBelief(movement(S,D2));
    not (X >= L) -> removeBelief(movement(S,D)), D1 is D - 25, D2 is mod(D1,360), addBelief(movement(S,D2));
    true
  ).

isInFieldY(Y) :-
  field(T,R,B,L),
  (
    not (Y =< T) -> removeBelief(movement(S,D)), D1 is D + 30, D2 is mod(D1,360), addBelief(movement(S,D2));
    not (Y >= B) -> removeBelief(movement(S,D)), D1 is D - 30, D2 is mod(D1,360), addBelief(movement(S,D2));
    true
  ).

onAddBelief(movement(S,D)) :-
  true.

onRemoveBelief(movement(S,D)) :-
  true.

onAddBelief(distance(ping_agent,DIST)) :-
  true.

onAddBelief(distance(postman,_)) :-
  true.