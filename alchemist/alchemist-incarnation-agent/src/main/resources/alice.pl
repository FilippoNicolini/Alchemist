init :-
  writeTuple(blackboard,msg(bob,ciao)),
  writeTuple(blackboard,msg(carl,ciao)),
  takeTuple(blackboard,msg(alice,X)),
  takeTuple(blackboard,msg(alice,X)).

onResponseMessage(msg(X,Y)) :-
  true.