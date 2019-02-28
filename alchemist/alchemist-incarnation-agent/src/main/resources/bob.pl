init :-
  takeTuple(blackboard,msg(bob,X)).

onResponseMessage(msg(bob,X)) :-
  writeTuple(blackboard,msg(alice,X)).