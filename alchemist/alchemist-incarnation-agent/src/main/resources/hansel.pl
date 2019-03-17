init :-
    writeTuple(breadcrumb(hansel,here)),
    removeBelief(movement(S,D)),
    addBelief(movement(0.022,0)),
    addBelief(counter(0)),
    takeTuple(stop(hansel)).

onAddBelief(position(X,Y)) :-
    removeBelief(counter(C)),
    handlePosition(C,X,Y).

handlePosition(C,X,Y) :-
    C < 10,
    C1 is C + 1,
    addBelief(counter(C1)),
    writeTuple(breadcrumb(hansel,here)).

handlePosition(C,X,Y) :-
    C >= 10,
    addBelief(counter(0)),
    removeBelief(movement(S,D)),
    D1 is D + 0.2,
    addBelief(movement(S,D1)).

onAddBelief(distance(_,_)) :-
    true.

onAddBelief(distance(_,_,_)) :-
    true.

onAddBelief(movement(_,_)) :-
    true.

onRemoveBelief(movement(_,_)) :-
    true.

onAddBelief(counter(C)) :-
    true.

onRemoveBelief(counter(C)) :-
    true.

onResponseMessage(msg(stop(hansel),X,Y)) :-
    removeBelief(movement(_,D)),
    addBelief(movement(0,D)),
    writeTuple(stop(gretel)).