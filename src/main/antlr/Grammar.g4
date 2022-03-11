grammar Grammar;

program
    : statement*
    ;

statement
    : SYMBOL '=' expression STATEMENT_DELIMITER     # assign
    | CLEAR                                         # clear
    | COMMENT                                       # comment
    | expression STATEMENT_DELIMITER                # printExpression
    | expression EOF                                # printExpression
    | STATEMENT_DELIMITER                           # blank
    ;

expression
    : additiveExpression
    ;

additiveExpression
    : multiplicativeExpression (op=('+' | '-') multiplicativeExpression)*
    ;

multiplicativeExpression
    : exponentialExpression (op=('*'|'/'|'%') exponentialExpression)*
    ;

exponentialExpression
    : atom (('^'|'**') atom)*
    ;

atom
    : '(' expression ')'                            # parenthesizedExpression
    | NUMBER                                        # number
    | SYMBOL                                        # symbol
    ;

NUMBER
    : '-'? INT '.' [0-9]+ EXP?
    | '-'? INT EXP
    | '-'? INT
    ;
fragment INT
    : '0' | [1-9] [0-9]*
    ;
fragment EXP
    : [Ee] [+\-]? INT
    ;

CLEAR
    : 'clear'
    ;

SYMBOL
    : [a-zA-Z_]+
    ;

STATEMENT_DELIMITER
    : '\r'? '\n'
    | ';'
    ;

WHITESPACE
    : [ \t]+ -> skip
    ;

COMMENT
    : ('#'|'//') .*? '\n'
    ;
