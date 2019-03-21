init :-
    writeTuple(breadcrumb(hansel,here)),
    removeBelief(movement(S,D)),
    addBelief(movement(0.027,4.8)),
    addBelief(counter(0,0)),
    addBelief(spiralCorner(0.08)),
    takeTuple(stop(hansel)).

onAddBelief(position(X,Y)) :-
    removeBelief(counter(C1,C2)),
    handlePosition(C1,C2,X,Y).

handlePosition(C1,C2,X,Y) :-
    C1 < 300,
    C2 < 10,
    C1N is C1 + 1,
    C2N is C2 + 1,
    addBelief(counter(C1N,C2N)),
    writeTuple(breadcrumb(hansel,here)).

handlePosition(C1,C2,X,Y) :-
    C1 < 300,
    C1N is C1 + 1,
    C2 >= 10,
    addBelief(counter(C1N,0)),
    removeBelief(movement(S,D)),
    belief(spiralCorner(V)),
    D1 is D + V,
    addBelief(movement(S,D1)).

handlePosition(C1,C2,X,Y) :-
    C1 >= 300,
    C2N is C2 + 1,
    addBelief(counter(0,C2N)),
    removeBelief(spiralCorner(V)),
    TEMP is V * 0.30,
    V1 is V + TEMP,
    addBelief(spiralCorner(V1)).


onAddBelief(distance(_,_)) :-
    true.

onAddBelief(distance(_,_,_)) :-
    true.

onAddBelief(movement(_,_)) :-
    true.

onRemoveBelief(movement(_,_)) :-
    true.

onAddBelief(counter(C1,C2)) :-
    true.

onRemoveBelief(counter(C1,C2)) :-
    true.

onAddBelief(spiralCorner(V)) :-
    true.

onRemoveBelief(spiralCorner(V)) :-
    true.

onResponseMessage(msg(stop(hansel),X,Y)) :-
    removeBelief(movement(_,D)),
    addBelief(movement(0,D)),
    writeTuple(stop(gretel)).