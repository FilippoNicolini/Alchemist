receiveMessage :-
  retract(ingoing(S,M)),
  onReceivedMessage(S,M).

sendMessage(R, M) :-
  self(S),
  assertz(outgoing(S,R, M)).

addBelief(B) :-
  assertz(belief(B)),
  assertz(added_belief(B)).

removeBelief(B) :-
  retract(belief(B)),
  assertz(removed_belief(B)).

writeTuple(BB,T) :-
  assertz(write(BB,T)).

readTuple(BB,T) :-
  assertz(read(BB,T)).

takeTuple(BB,T) :-
  assertz(take(BB,T)).