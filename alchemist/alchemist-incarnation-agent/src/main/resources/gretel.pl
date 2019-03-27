init :-
    removeBelief(movement(_,_)),
    randomDirection(D),
    randomSpeed(S),
    addBelief(movement(S,D)),
    takeTuple(breadcrumb(hansel,here)),
    addBelief(stopped(false)).

onAddBelief(position(X,Y)) :-
    belief(stopped(false)),
    handlePosition(X,Y),
    takeTuple(breadcrumb(hansel,here)).

onAddBelief(position(X,Y)) :-
    belief(stopped(true)),
    removeBelief(movement(_,D)),
    addBelief(movement(0,D)).

onAddBelief(position(X,Y)) :-
    true.

handlePosition(X,Y) :-
    removeBelief(movement(_,D)),
    randomDirection(RD),
    D1 is D - RD,
    randomSpeed(S),
    addBelief(movement(S,D1)).

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
    changeDirection(X,Y).

changeDirection(X2,Y2) :-
    belief(position(X1,Y1)),
    DX is X2 - X1,
    DY is Y2 - Y1,
    calculateAtan(DY,DX,RAD),
    removeBelief(movement(_,_)),
    randomSpeed(S),
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

randomDirection(R) :-
    randomGenerator <- nextDouble returns X,
    TMP is X * 3.14,
    R is TMP - 1.57.

randomSpeed(R) :-
    randomGenerator <- nextDouble returns X,
    R is X * 0.3.