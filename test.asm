COPY         START    1000
FIRST        LDX      ZERO
MOVECH       LDCH    STR1,X
             STCH    STR2,X
             TIX     ELEVEN
             JLT     MOVECH
              .
              .
STR1         BYTE       C'HELLO WORLD'
STR2         RESB       11
ZERO         WORD       0
ELEVEN       WORD       11
             END          FIRST
