jmp start
    a
    b = 1
    c = 9
start:
    Read a 
    LoadM a 
    SubtM c
    JmpZ end
again:
    Write a
    LoadM a
    SubtM b
    JmpZ end
    Store a 
    Jmp again
end:
    Halt
