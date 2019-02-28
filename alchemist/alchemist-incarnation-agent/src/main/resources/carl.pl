init :-
  takeTuple(blackboard,msg(carl,X)).

onResponseMessage(msg(carl,X)) :-
  writeTuple(blackboard,msg(alice,X)).