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

writeTuple(T) :-
  assertz(write(T)).

readTuple(T) :-
  assertz(read(T)).

takeTuple(T) :-
  assertz(take(T)).