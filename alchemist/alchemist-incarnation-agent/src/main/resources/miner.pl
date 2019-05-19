init :-
    addBelief(deposit(2,2)),
    randomDirection(D),
    randomSpeed(S),
    node <- changeNodeSpeed(S),
    node <- changeDirectionAngle(D),
    addBelief(harvesting).

%(1) [stato = harvesting] --> movimento casuale e ricerca pepita
onAddBelief(position(X,Y)) :-
    belief(harvesting),
    handlePosition(X,Y),
    takeTuple(nugget).

%(4) arrivo (vicinanze) miniera --> [stato = harvesting]
onAddBelief(position(X,Y)) :-
    belief(mine(MX,MY)),
    calculateDistanceBetweenPoints(X,Y,MX,MY,D),
    D < 1,
    removeBelief(mine(_,_)),
    removeBelief(toMine),
    addBelief(harvesting),
    changeDirection(MX,MY).

handlePosition(X,Y) :-
    node <- getNodeDirectionAngle returns D,
    randomDirection(RD),
    D1 is D - RD,
    randomSpeed(S),
    node <- changeDirectionAngle(D1),
    node <- changeNodeSpeed(S).

%(2) recuperata pepita --> salva posizione miniera, [stato = to_deposit]
onResponseMessage(msg(nugget,X,Y)) :-
    removeBelief(harvesting),
    addBelief(toDeposit),
    addBelief(mine(X,Y)),
    belief(deposit(DX,DY)),
    changeDirection(DX,DY).

%(3) arrivo deposito --> invio pepita, [stato = to_mine]
onAddBelief(distance(deposit,ND)) :-
    removeBelief(toDeposit),
    iSend(deposit,nugget),
    addBelief(toMine),
    belief(mine(X,Y)),
    changeDirection(X,Y).

%Utils
changeDirection(X2,Y2) :-
    belief(position(X1,Y1)),
    DX is X2 - X1,
    DY is Y2 - Y1,
    calculateAtan(DY,DX,RAD),
    randomSpeed(S),
    node <- changeDirectionAngle(RAD),
    node <- changeNodeSpeed(S).

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

calculateDistanceBetweenPoints(X1,Y1,X2,Y2,D) :-
    DX is X2 - X1,
    DY is Y2 - Y1,
    DX2 is DX * DX,
    DY2 is DY * DY,
    DD is DX2 + DY2,
    D is sqrt(DD).

