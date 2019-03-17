init :-
    removeBelief(movement(S,D)),
    addBelief(movement(0.023,D)),
    takeTuple(breadcrumb(hansel,here)),
    addBelief(counter(0)),
    takeTuple(stop(gretel)).

onAddBelief(position(X,Y)) :-
    removeBelief(counter(C)),
    handlePosition(C,X,Y),
    checkDistance.

handlePosition(C,X,Y) :-
    C < 15,
    C1 is C + 1,
    addBelief(counter(C1)).

handlePosition(C,X,Y) :-
    C >= 15,
    addBelief(counter(0)),
    removeBelief(movement(S,D)),
    D1 is D - 0.5,
    addBelief(movement(S,D1)).

onAddBelief(distance(A,ND,OD)) :-
    true.

onAddBelief(distance(A,ND)) :-
    true.

onAddBelief(movement(_,_)) :-
    true.

onRemoveBelief(movement(_,_)) :-
    true.

onAddBelief(counter(C)) :-
    true.

onRemoveBelief(counter(C)) :-
    true.

onResponseMessage(msg(stop(gretel),X,Y)) :-
    removeBelief(movement(_,D)),
    addBelief(movement(0,D)).

onResponseMessage(msg(breadcrumb(hansel,here),X,Y)) :-
    removeBelief(counter(_)),
    addBelief(counter(0)),
    changeDirection(X,Y).

checkDistance :-
    belief(distance(hansel,D)),
    D < 0.08,
    writeTuple(stop(hansel)).

checkDistance :-
    takeTuple(breadcrumb(hansel,here)).

changeDirection(X2,Y2) :-
    belief(position(X1,Y1)),
    DX is X2 - X1,
    DY is Y2 - Y1,
    calculateAtan(DY,DX).

calculateAtan(DY,DX) :-
    DX > 0,
    RAD is atan(DY / DX),
    removeBelief(movement(S,D)),
    addBelief(movement(S,RAD)).

calculateAtan(DY,DX) :-
    DX < 0,
    DY >= 0,
    TMP is atan(DY / DX),
    RAD is TMP + 3.14,
    removeBelief(movement(S,D)),
    addBelief(movement(S,RAD)).

calculateAtan(DY,DX) :-
    DX < 0,
    DY < 0,
    TMP is atan(DY / DX),
    RAD is TMP - 3.14,
    removeBelief(movement(S,D)),
    addBelief(movement(S,RAD)).

calculateAtan(DY,DX) :-
    DX == 0,
    DY > 0,
    RAD is 3.14 / 2,
    removeBelief(movement(S,D)),
    addBelief(movement(S,RAD)).

calculateAtan(DY,DX) :-
    DX == 0,
    DY < 0,
    RAD is -3.14 / 2,
    removeBelief(movement(S,D)),
    addBelief(movement(S,RAD)).






