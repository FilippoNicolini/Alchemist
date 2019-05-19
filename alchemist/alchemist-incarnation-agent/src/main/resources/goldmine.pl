init :-
    agent <- generateNextRandom returns RAND,
    NUGGETS is RAND * 1.0,
    loadNuggets(NUGGETS).

loadNuggets(NUGGETS) :-
    NUGGETS > 0,
    assertz(nugget),
    N is NUGGETS - 1,
    loadNuggets(N).

loadNuggets(NUGGETS) :-
    NUGGETS < 0,
    agent <- setConcentration.

onAddBelief(_) :-
    true.