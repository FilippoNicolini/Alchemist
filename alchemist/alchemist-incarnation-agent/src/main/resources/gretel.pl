init :-
    removeBelief(movement(_,_)),
    randomDirection(D),
    addBelief(movement(0.025,D)),
    takeTuple(breadcrumb(hansel,here)),
    randomSteps(P),
    addBelief(counterStep(P)),
    addBelief(stopped(false)).

onAddBelief(position(X,Y)) :-
    belief(stopped(false)),
    removeBelief(counterStep(S)),
    handlePosition(S,X,Y),
    takeTuple(breadcrumb(hansel,here)).

onAddBelief(position(X,Y)) :-
    true.

handlePosition(S,X,Y) :-
    S > 0,
    S1 is S - 1,
    addBelief(counterStep(S1)).

handlePosition(S,X,Y) :-
    S =< 0,
    randomSteps(P),
    addBelief(counterStep(P)),
    removeBelief(movement(_,D)),
    randomDirection(RD),
    D1 is D - RD,
    addBelief(movement(0.025,D1)).

onAddBelief(distance(hansel,ND,OD)) :-
    removeBelief(stopped(false)),
    addBelief(stopped(true)),
    removeBelief(movement(_,D)),
    addBelief(movement(0,D)).

onAddBelief(distance(A,ND,OD)) :-
    true.

onAddBelief(distance(A,ND)) :-
    true.

onAddBelief(movement(_,_)) :-
    true.

onRemoveBelief(movement(_,_)) :-
    true.

onAddBelief(counterStep(C)) :-
    true.

onRemoveBelief(counterStep(C)) :-
    true.

onResponseMessage(msg(breadcrumb(hansel,here),X,Y)) :-
    removeBelief(counterStep(_)),
    addBelief(counterStep(15)),
    removeBelief(movement(_,D)),
    addBelief(movement(0.05,D)),
    changeDirection(X,Y).

changeDirection(X2,Y2) :-
    belief(position(X1,Y1)),
    DX is X2 - X1,
    DY is Y2 - Y1,
    calculateAtan(DY,DX,RAD),
    removeBelief(movement(S,D)),
    addBelief(movement(S,RAD)).

calculateAtan(DY,DX,RAD) :-
    DX > 0,
    RAD is atan(DY / DX).

calculateAtan(DY,DX,RAD) :-
    DX < 0,
    DY >= 0,
    TMP is atan(DY / DX),
    RAD is TMP + 3.14.

calculateAtan(DY,DX,RAD) :-
    DX < 0,
    DY < 0,
    TMP is atan(DY / DX),
    RAD is TMP - 3.14.

calculateAtan(DY,DX,RAD) :-
    DX == 0,
    DY > 0,
    RAD is 3.14 / 2.

calculateAtan(DY,DX,RAD) :-
    DX == 0,
    DY < 0,
    RAD is -3.14 / 2.

randomSteps(R) :-
    rand_int(50,R).

randomDirection(R) :-
    rand_float(X),
    TMP is X * 3.14,
    R is TMP - 1.57.


