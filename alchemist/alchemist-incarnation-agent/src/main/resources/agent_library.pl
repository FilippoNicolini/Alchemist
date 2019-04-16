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

unfold((A, B), C) :-
    !,
    unfold(A, UA),
    unfold(B, UB),
    append(UA, UB, C).

unfold(X, [X]).

execute(I) :-
    intention(I, []),
    agent <- removeCompletedIntention(I).

execute(I) :-
    retract(intention(I, [ACTION | REMINDER])),
    assert(intention(I, REMINDER)),
    execute(I, ACTION).

execute(I, achievement(GOAL)) :-
    !,
    add_goal(I, achievement(GOAL)).

add_goal(I, achievement(GOAL)) :-
    !,
    retract(intention(I, STACK)),
    '<-'(achievement(GOAL), GUARD, BODY),
    call(GUARD),
    append(BODY, STACK, NEWSTACK),
    assert(intention(I, NEWSTACK)).

execute(I, ACTION) :-
    is_internal(ACTION),
    %agent <- test('exec1').
    agent <- executeInternalAction(ACTION).

is_internal(ACTION) :-
 '='(ACTION,iSend(SENDER,MESSAGE)).

execute(I, ACTION) :-
    is_external(ACTION),
    node <- executeExternalAction(ACTION).