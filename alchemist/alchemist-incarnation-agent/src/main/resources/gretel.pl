init :-
    randomDirection(D),
    randomSpeed(S),
    node <- changeNodeSpeed(S),
    node <- changeDirectionAngle(D),
    addBelief(counter(0)),
    takeTuple(breadcrumb(hansel,here)),
    addBelief(stopped(false)).

onAddBelief(position(X,Y)) :-
    belief(stopped(false)),
    belief(counter(C)),
    C =< 0,
    handlePosition(X,Y),
    takeTuple(breadcrumb(hansel,here)).

onAddBelief(position(X,Y)) :-
    belief(stopped(true)),
    node <- changeNodeSpeed(0).

onAddBelief(position(X,Y)) :-
    removeBelief(counter(C)),
    C1 is C - 1,
    addBelief(counter(C1)).

handlePosition(X,Y) :-
    node <- getNodeDirectionAngle returns D,
    randomDirection(RD),
    D1 is D - RD,
    randomSpeed(S),
    node <- changeNodeSpeed(S),
    node <- changeDirectionAngle(D1).

onAddBelief(distance(hansel,ND,OD)) :-
    removeBelief(stopped(false)),
    addBelief(stopped(true)),
    node <- changeNodeSpeed(0).

onResponseMessage(msg(breadcrumb(hansel,here),X,Y)) :-
    removeBelief(counter(_)),
    addBelief(counter(5)),
    changeDirection(X,Y).

changeDirection(X2,Y2) :-
    belief(position(X1,Y1)),
    DX is X2 - X1,
    DY is Y2 - Y1,
    calculateAtan(DY,DX,RAD),
    randomSpeed(S),
    node <- changeNodeSpeed(S),
    node <- changeDirectionAngle(RAD).

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
    agent <- generateNextRandom returns RAND,
    agent <- getLevyDistributionDensity(RAND) returns X,
    R is X * 6.28.

randomSpeed(R) :-
    agent <- generateNextRandom returns RAND,
    agent <- getLevyDistributionDensity(RAND) returns X,
    R is X * 0.5.