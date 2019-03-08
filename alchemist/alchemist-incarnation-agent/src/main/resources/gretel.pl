init :-
    removeBelief(movement(S,D)),
    addBelief(movement(0.02,D)),
    takeTuple(blackboard,breadcrumb(hansel,X,Y)),
    takeTuple(blackboard,stop(gretel)).

onAddBelief(position(X,Y)) :-
    checkPosition(X,Y).

onAddBelief(distance(_,_)) :-
    true.

onAddBelief(movement(_,_)) :-
    true.

onRemoveBelief(movement(_,_)) :-
    true.

onResponseMessage(breadcrumb(hansel,X,Y)) :-
    checkDistance(hansel),
    calculateAngle(X,Y).

checkDistance(A) :-
    belief(distance(A,D)),
    D > 0.3,
    takeTuple(blackboard,breadcrumb(hansel,X1,Y1)).

checkDistance(A) :-
    belief(distance(A,D)),
    D < 0.3,
    writeTuple(blackboard,stop(hansel)).

onResponseMessage(stop(gretel)) :-
    removeBelief(movement(_,D)),
    addBelief(movement(0,D)).

calculateAngle(X2,Y2) :-
    belief(position(X1,Y1)),
    DX is X1 - X2,
    DY is Y1 - Y2,
    RAD is DY / DX,
    TMP is RAD * 180,
    DEG is TMP / 3.14,
    changeDirection(DEG).

checkPosition(X,Y) :-
    checkX(X),
    checkY(Y).

checkX(X,Y) :- X < -10, changeDirection(90).

checkX(X,Y) :- X > 10, changeDirection(90).

checkX(X,Y) :- X > -10, X < 10, true.

checkY(X,Y) :- Y < -10, changeDirection(90).

checkY(X,Y) :- Y > 10, changeDirection(90).

checkX(X,Y) :- Y > -10, Y < 10, true.

changeDirection(A) :-
    removeBelief(movement(S,D)),
    D1 is D - A,
    D2 is mod(D1,360),
    addBelief(movement(S,D2)).