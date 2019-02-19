receiveMessage :-
  retract(ingoing(S,M)),
  onReceivedMessage(S,M).

sendMessage(R, M) :-
  self(Sender),
  assertz(outgoing(Sender,R, M)).

addBelief(B) :-
  assertz(belief(B)),
  assertz(added_belief(B)).

removeBelief(B) :-
  retract(belief(B)),
  assertz(removed_belief(B)).