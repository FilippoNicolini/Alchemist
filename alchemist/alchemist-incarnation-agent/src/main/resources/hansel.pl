init :-
    belief(position(X,Y)),
    writeTuple(blackboard,breadcrumb(hansel,X,Y)),
    removeBelief(movement(S,D)),
    addBelief(movement(0.02,D)),
    takeTuple(blackboard,stop(hansel)).

onAddBelief(position(X,Y)) :-
    checkPosition(X,Y).

onAddBelief(distance(_,_)) :-
    true.

onAddBelief(movement(_,_)) :-
    true.

onRemoveBelief(movement(_,_)) :-
    true.

onResponseMessage(stop(hansel)) :-
    removeBelief(movement(_,D)),
    addBelief(movement(0,D)),
    writeTuple(blackboard,stop(gretel)).

checkPosition(X,Y) :-
    checkBoundingBox(X),
    checkBoundingBox(Y),
    writeTuple(blackboard,breadcrumb(hansel,X,Y)).

checkBoundingBox(X) :- X < -10, changeDirection(180).

checkBoundingBox(X) :- X > 10, changeDirection(180).

checkBoundingBox(X) :- X > -10, X < 10.

changeDirection(A) :-
    removeBelief(movement(S,D)),
    D1 is D - A,
    D2 is mod(D1,360),
    addBelief(movement(S,D2)).