header {
package com.bluejungle.pf.destiny.parser;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import com.bluejungle.pf.domain.destiny.subject.SubjectType;
import com.bluejungle.framework.utils.StringUtils;


/* All sources, binaries and HTML pages (C) Copyright 2004 by Blue Jungle Inc,
 * Redwood City, CA.
 * Ownership remains with Blue Jungle Inc. All rights reserved worldwide.
 *
 * @author Sergey Kalinichenko
 * @version $Id: //depot/Destiny/D_Nimbus/pcv/Nimbus_Main/main/src/common/pf/src/java/main/com/bluejungle/pf/destiny/parser/pql.g#2 $
 */
}

class PQLParser extends Parser;

options {
    buildAST = true;
    exportVocab=PQLTokens;
    k = 2;
    defaultErrorHandler=false;
}

{
    private static Set keywords = null;

    private synchronized static Set keywords() {
        if ( keywords == null ) {
            Set tmp = new HashSet();
            for ( int i = 0 ; i != _tokenNames.length ; i++ ) {
                if ( _tokenNames[i].length() > 1 && _tokenNames[i].charAt(0) == '"' ) {
                    tmp.add( _tokenNames[i].substring(1,_tokenNames[i].length()-1) );
                }
            }
            keywords = Collections.unmodifiableSet( tmp );
        }
        return keywords;
    }

    /**
     * Adds quotes around names that need them.
     */
    public static String quoteName( String name ) {
        if ( name == null ) {
            throw new NullPointerException("name");
        }
        return
        (   name.length() == 0
        ||  Character.isDigit( name.charAt(0) )
        ||  NON_IDENT_PATTERN.matcher(name).find()
        ||  keywords().contains(name.toLowerCase())
        )   ? '"'+StringUtils.escape(name)+'"' : name;
    }

    public static char SEPARATOR = '/';
    private static final Pattern NON_IDENT_PATTERN = Pattern.compile("\\W");
}

/*--------------------------------------------*
 * The main entry point                       *
 *--------------------------------------------*/

program
    :   (annotated_definition)* EOF!
    ;

standalone_access_policy
    :   access_policy EOF!
    ;

annotated_definition!
    :   (a:annotations)? d:definition {
        if (#a != null) {
            #annotated_definition = #([ANNOTATED_DEFINITION], #a, #d);
        } else {
            #annotated_definition = #d;
        }
    }
    ;

annotations!
    :   i:idnumber (s:entity_status)? (c:creator)? (a:access_policy)? (h:HIDDEN)? {
        if ( #s == null ) {
            #s = #[STATUS, "empty"];
        }
        if ( #c == null ) {
            #c = #[CREATOR_ATTRIBUTE, "empty"];
        }
        if ( #a == null ) {
            #a = #([ACCESS_CONTROL_POLICY], [EMPTY], [EMPTY], [EMPTY]);
        }
        #annotations = #([ANNOTATIONS], i, s, c, a, h);
    }
    ;

entity_status
    :   STATUS! (n:IDENT_BASE | e:EMPTY) {
        if (#n != null) {
            #n.setType(STATUS);
        }
        if (#e != null) {
            #e.setType(STATUS);
        }
    }
    ;

creator!
    :   CREATOR (q:quoted_string | e:EMPTY) {
        String c = null;
        if (#q != null) {
            c = #q.getText();
        }
        if (#e != null) {
            c = #e.getText();
        }
        #creator = #[CREATOR_ATTRIBUTE, c];
    }
    ;

definition
    :   policy_def
    |   entity_def (SEMICOLON!)*
    ;

entity_def
    :   component_def
    |   action_def
    |   resource_def
    |   subject_def
    |   location_def
    |   folder_def
    ;

policy_def
    :   POLICY^ path_identifier policybody
    ;

component_def!
    :   COMPONENT name:path_identifier EQUAL (dd:description)? pred:predicate {
        #component_def = #( [COMPONENT], name, dd, #([COMPONENT_EXPR], pred));
    }
    ;

action_def!
    :   ACTION name:path_identifier EQUAL (dd:description)? pred:predicate {
        #action_def = #( [ACTION], name, dd, #([ACTION_EXPR], pred));
    }
    ;

resource_def!
    :   RESOURCE name:path_identifier EQUAL (dd:description)? pred:predicate {
        #resource_def = #( [RESOURCE], name, dd, #([RESOURCE_EXPR], pred));
    }
    ;

subject_def!
    :   pk:principal_keyword { #pk.setType(PRINCIPAL); }
        name:path_identifier EQUAL (dd:description)? pred:predicate {
        #subject_def = #( pk, name, dd, #([SUBJECT_EXPR], pred));
    }
    ;

principal_keyword
    :   p:PRINCIPAL   { #p.setText(SubjectType.AGGREGATE.getName()); }
    |   u:USER        { #u.setText(SubjectType.USER.getName()); }
    |   r:RECIPIENT   { #r.setText(SubjectType.RECIPIENT.getName()); }
    |   a:APPLICATION { #a.setText(SubjectType.APP.getName()); }
    |   h:HOST        { #h.setText(SubjectType.HOST.getName()); }
    ;

folder_def
    : FOLDER^ path_identifier (description)?
    ;

location_def
    :   LOCATION^ identifier EQUAL! quoted_string (dd:description)?
    ;

policybody
    :   (   description
        |   policy_attribute_specifier
        |   policy_severity_specifier
        |   policy_tag_specifier
        |   target
        |   effect_clause
        |   obligation_clause
        |   default_clause
        |   condition_clause
        |   deployment_clause
        |   exceptions_clause
        |   SEMICOLON!
        ) *
        (rule_def)*
    ;

policy_attribute_specifier
    :   ATTRIBUTE^ identifier
    ;

policy_severity_specifier
    :   SEVERITY^ UNSIGNED
    ;

policy_tag_specifier
    :   TAG^ identifier EQUAL! quoted_string
    ;

target!
    :   FOR r:predicate ON a:predicate (TO r2:predicate)? (SENT_TO r3:predicate)? BY s:predicate {
        if (#r2 != null && #r3 != null) {
            throw new RecognitionException("Illegal policy type: both 'to' and 'send-to' are present.");
        }
        if ( #r2 != null ) {
            #target = #([TARGET], #( [FROM], #([RESOURCE_EXPR], r) ), #([ACTION_EXPR], a), #([TO], #([RESOURCE_EXPR], r2)), #([SUBJECT_EXPR], s) );
        } else if ( #r3 != null ) {
            #target = #([TARGET], #( [FROM], #([RESOURCE_EXPR], r) ), #([ACTION_EXPR], a), #([EMAIL_TO], #([SUBJECT_EXPR], r3)), #([SUBJECT_EXPR], s) );
        } else {
            #target = #([TARGET], #( [FROM], #([RESOURCE_EXPR], r) ), #([ACTION_EXPR], a), #([SUBJECT_EXPR], s) );
        }
    }
    ;

condition_clause
    :   WHERE^ predicate
    ;

effect_clause
    :   e:DO^ effect_type (quoted_string)? {
        #e.setType(EFFECT_CLAUSE);
    }
    ;

effect_type
    :   a:ALLOW {
        #a.setType(EFFECT_TYPE);
    }
    |   dc:DONTCARE {
        #dc.setType(EFFECT_TYPE);
    }
    |   dn:DENY {
        #dn.setType(EFFECT_TYPE);
    }
    |   q:QUERY {
        #q.setType(EFFECT_TYPE);
    }
    |   c:CONFIRM {
        #c.setType(EFFECT_TYPE);
    }
    ;

obligation_clause!
    :   op:obligation_preamble DO ol:obligation_list {
        #obligation_clause = #([OBLIGATION_CLAUSE], op, ol );
    }
    ;

obligation_preamble
    :   ON^ (LOCAL)? effect_type
    ;

obligation_list
    :   obligation (COMMA! obligation)*
    ;

obligation!
    :   QUIET op:obligation_primitive {
        #obligation = #( [OBLIGATION], [QUIET], op );
    }
    |   p:obligation_primitive {
        #obligation = #([OBLIGATION], p);
    }
    ;

obligation_primitive
    :   LOG^ (user_defined_value (identifier)?)?
    |   DONTLOG
    |   (NOTIFY^ email_list quoted_string notification_preference) => NOTIFY^ email_list quoted_string notification_preference
    |   NOTIFY^ email_list quoted_string
    |   !name:identifier (args:argument_list)? {
        #obligation_primitive = #([CUSTOM_OBLIGATION], name, args);
    }
    ;

email_list
    :   quoted_string
    ;

notification_preference
    :   BY^ (EMAIL | IM)
    ;

argument_list
    :   LPAREN! (user_defined_value) (COMMA! user_defined_value)* RPAREN!
    ;

default_clause
    :   BY! DEFAULT^ effect_clause
    ;

rule_def :
    RULE^
    (description)?
    target
    (   description
    |   condition_clause
    |   SEMICOLON!
    ) *
    effect_clause
    (   description
    |   obligation_clause
    |   default_clause
    |   SEMICOLON!
    ) *
    ;

access_policy!
    :   ACCESS_POLICY! acb:access_control_body  aeb:allowed_entities_body  {
            #access_policy = #([ACCESS_CONTROL_POLICY], acb, aeb);
    }
    |   ACCESS_POLICY EMPTY {
            #access_policy = #([ACCESS_CONTROL_POLICY], [EMPTY], [EMPTY]);
    }
    ;

access_control_body
    :   ACCESS_CONTROL^ acpl:access_control_policybody_list
    ;

allowed_entities_body
    :   ALLOWED_ENTITIES^ (allowed_entity_list)?
    ;

allowed_entity_list
    :   allowed_entity (COMMA! allowed_entity)*
    ;

allowed_entity
    :   POLICY_ENTITY
    |   USER_ENTITY
    |   RESOURCE_ENTITY
    |   PORTAL_ENTITY
    |   DEVICE_ENTITY
    |   SAP_ENTITY
    |   ENOVIA_ENTITY
    |   HOST_ENTITY
    |   APPLICATION_ENTITY
    |   LOCATION_ENTITY
    |   ACTION_ENTITY
    ;

access_control_policybody_list
    :   (PBAC policybody)*
    ;

deployment_clause
    :     DEPLOYED^ TO! deployment_target
    ;

deployment_target
    :   deployment_atom ((o:OR!|c:COMMA!) deployment_atom)* {
        if (#o != null || #c != null) {
            #deployment_target = #( [OR], deployment_target );
        }
    }
    ;

deployment_atom
    :   (predicate WITH) => predicate WITH! primary_predicate {
        #deployment_atom = #( [AND], deployment_atom );
    }
    |   primary_predicate
    |   LPAREN! deployment_target RPAREN!
    ;

exceptions_clause
    :   EXCEPTIONS^ combination_type policy_exception (COMMA! policy_exception)*
    ;

combination_type
    :   DENY_OVERRIDES
    |   ALLOW_OVERRIDES
    ;

policy_exception
    :   path_identifier
    ;

predicate
    :   conjunction ((o:OR!|c:COMMA!) conjunction)* {
        if (#o != null || #c != null) {
            #predicate = #( [OR], predicate );
        }
    }
    ;

conjunction
    :   negation (a:AND! negation)* {
        if (#a != null) {
            #conjunction = #( [AND], conjunction );
        }
    }
    ;

negation
    :   (NOT^)? (primary_predicate | composite_predicate)
    ;

primary_predicate
    :   relation
    |   TRUE
    |   FALSE
    |   STAR
    |   ALL { #primary_predicate = #[STAR,"*"]; }
    |   EMPTY
    // Certain keywords "shadow" useful action names.
    // This production takes care of these keywords, making the parse tree uniform:
    |   e:EMAIL  { #e.setType(IDENTIFIER); }
    |   i:IM     { #i.setType(IDENTIFIER); }
    ;

composite_predicate
    :   LPAREN! predicate RPAREN!
    ;

relation!
    :   lhs:expression (op:relation_op rhs:expression)? {
        if ((#op != null) && (#rhs != null)) {
            #relation = #(op, lhs, rhs);
        } else {
            #relation = #lhs;
        }
    }
    ;

call_function_arguments
    :   LPAREN! quoted_string COMMA! quoted_string (COMMA! expression)* RPAREN!
    ;

expression
    :   dot_expression_header ( d:DOT! dot_expression_element )* {
        if (#d != null) {
            #expression = #([DOT_EXPRESSION], expression);
        }
    }
    |   CALL_FUNCTION^ call_function_arguments
    |   quoted_string
    |   numeric_literal
    |   idnumber
    |   NULL
    ;

relation_op
    :   op1:MATCHES {
        #op1.setType(RELATION_OP);
        #op1.setText("=");
    }
    |   op2:EQUAL (EQUAL!)? {
        #op2.setType(RELATION_OP);
        #op2.setText("=");
    }
    |   op3:NOT_EQUALS {
        #op3.setType(RELATION_OP);
    }
    |   op8:IN {
        #op8.setType(RELATION_OP);
        #op8.setText("=");
    }
    |   op9:HAS {
        #op9.setType(RELATION_OP);
    }
    |   op10:DOES_NOT_HAVE {
        #op10.setType(RELATION_OP);
    }
    |   op11:INCLUDES {
        #op11.setType(RELATION_OP);
    }
    |   op12:EQUALS_UNORDERED {
        #op12.setType(RELATION_OP);
    }
    |   RELATION_OP
    ;

dot_expression_header
    :   ac:ACTION      {#ac.setType(IDENTIFIER);}
    |   re:RESOURCE    {#re.setType(IDENTIFIER);}
    |   us:USER        {#us.setType(IDENTIFIER);}
    |   ho:HOST        {#ho.setType(IDENTIFIER);}
    |   ap:APPLICATION {#ap.setType(IDENTIFIER);}
    |   pr:PRINCIPAL   {#pr.setType(IDENTIFIER);}
    |   lo:LOCATION    {#lo.setType(IDENTIFIER);}
    |   gr:GROUP       {#gr.setType(IDENTIFIER);}
    |   rp:RECIPIENT   {#rp.setType(IDENTIFIER);}
    |   id:IDENT_BASE  {#id.setType(IDENTIFIER);}
    ;

dot_expression_element
    :   dot_expression_header
    |   rw01:EMAIL {#rw01.setType(IDENTIFIER);}
    |   rw02:ID {#rw02.setType(IDENTIFIER);}
    |   qs:quoted_string {#qs.setType(IDENTIFIER);}
    ;

user_defined_value
    :   quoted_or_unquoted_string
    |   numeric_literal
    ;

numeric_literal
    :   ((NUM_SIGN UNSIGNED | UNSIGNED) ~DOT) => integer
    |!  (UNSIGNED DOT|NUM_SIGN UNSIGNED DOT) => i:integer DOT f:UNSIGNED { #f.setType(INTEGER); } {
        #numeric_literal = #([FLOATING_POINT], i, f);
    }
    |!  DOT sf1:UNSIGNED { #sf1.setType(INTEGER); } {
        #numeric_literal = #([FLOATING_POINT], [INTEGER, "0"], sf1);
    }
    |!  (NUM_SIGN DOT) => (ns:NUM_SIGN) DOT sf2:UNSIGNED { #sf2.setType(INTEGER); } {
        String intTxt = #ns.getText()+"0";
        #numeric_literal = #([FLOATING_POINT], [INTEGER, intTxt], sf2);
    }
    ;

// This production uses syntactic lookup to deal with the trailing
// semicolon to avoid nondeterminism we've introduced by making
// semicolons optional. Fortunately, cases like this are isolated.

description
    :   DESCRIPTION! s:quoted_string ((SEMICOLON)=>SEMICOLON!)? { #s.setType(DESCRIPTION); }
    ;

idnumber
    :   ID! i:integer {
        #i.setType(IDNUMBER);
    }
    |   ID! n:NULL {
        #n.setType(IDNUMBER);
    }
    ;

integer
    :   u:UNSIGNED {
        #u.setType(INTEGER);
    }
    |!  s:NUM_SIGN i:UNSIGNED {
        String val = #s.getText()+#i.getText();
        #integer = #[INTEGER, val];
    }
    ;

identifier
    :   i:IDENT_BASE    {
        #i.setType( IDENTIFIER );
    }
    |   q:quoted_string {
        #q.setType( IDENTIFIER );
    }
    ;

quoted_or_unquoted_string
    :   i:IDENT_BASE  { #i.setType( QUOTED_STRING ); }
    |   quoted_string
    ;

path_identifier
    :   i:IDENT_BASE {
        #i.setType( IDENTIFIER );
    }
    |   q:quoted_string {
        #q.setType( IDENTIFIER );
        if (   #q.getText().length() == 0
            || #q.getText().charAt(0) == SEPARATOR
            || #q.getText().charAt(#q.getText().length()-1) == SEPARATOR
            || #q.getText().indexOf(""+SEPARATOR+SEPARATOR) != -1) {
            throw new RecognitionException("Illegal identifier detected: '"+#q.getText()+"'");
        }
    }
    ;

call_function
    :   CALL_FUNCTION
    ;

quoted_string
    :   q:QUOTED_STRING {
        // Remove the quotation marks programmatically
        String text = #q.getText();
        #q.setText( text.substring( 1, text.length()-1 ) );
    }
    ;

//-----------------------------------------------------

class PQLLexer extends Lexer;

options {
    charVocabulary='\3'..'\377' | '\u0000'..'\u7FFF';
    caseSensitiveLiterals=false;
    filter=false;
    k=2;
}

tokens {
    ACCESS_CONTROL    = "access_control";
    ACCESS_POLICY     = "access_policy";
    ACTION            = "action";
    ACTION_ENTITY     = "action_entity";
    ALLOW             = "allow";
    ALLOW_OVERRIDES   = "allow_overrides";
    ALLOWED_ENTITIES  = "allowed_entities";
    ALL               = "all";
    AND               = "and";
    APPLICATION       = "application";
    APPLICATION_ENTITY = "application_entity";
    ATTRIBUTE         = "attribute";
    BY                = "by";
    CALL_FUNCTION     = "call_function";
    COMBINE           = "combine";
    COMPONENT         = "component";
    CONFIRM           = "confirm";
    CREATOR           = "creator";
    DEFAULT           = "default";
    DENY              = "deny";
    DENY_OVERRIDES    = "deny_overrides";
    DEPLOYED          = "deployed";
    DESCRIPTION       = "description";
    DEVICE_ENTITY     = "device_entity";
    DO                = "do";
    DOES_NOT_HAVE     = "does_not_have";
    DONTLOG           = "dontlog";
    ENOVIA_ENTITY     = "enovia_entity";
    EMAIL             = "email";
    EMPTY             = "empty";
    EQUALS_UNORDERED  = "equals_unordered";
    EXCEPTIONS        = "subpolicy";
    FALSE             = "false";
    FOLDER            = "folder";
    FOR               = "for";
    HAS               = "has";
    HIDDEN            = "hidden";
    HOST              = "host";
    HOST_ENTITY       = "host_entity";
    ID                = "id";
    IM                = "im";
    IN                = "in";
    INCLUDES          = "includes";
    LOCAL             = "local";
    LOCATION          = "location";
    LOCATION_ENTITY   = "location_entity";
    LOG               = "log";
    MATCHES           = "matches";
    NOT               = "not";
    NOTIFY            = "notify";
    NULL              = "null";
    ON                = "on";
    OR                = "or";
    PBAC              = "pbac";
    POLICY            = "policy";
    POLICY_ENTITY     = "policy_entity";
    PORTAL_ENTITY     = "portal_entity";
    PRINCIPAL         = "principal";
    QUERY             = "query";
    QUIET             = "quiet";
    RECIPIENT         = "recipient";
    RESOURCE          = "resource";
    RESOURCE_ENTITY   = "resource_entity";
    RULE              = "rule";
    SAP_ENTITY        = "SAP_entity";
    SEVERITY          = "severity";
    SENT_TO           = "sent_to";
    STATUS            = "status";
    TAG               = "tag";
    TO                = "to";
    TRUE              = "true";
    DONTCARE          = "dontcare";
    USING             = "using";
    USER              = "user";
    USER_ENTITY       = "user_entity";
    WHERE             = "where";
    WITH              = "with";

    // Pseudo-tokens
    ACCESS_CONTROL_POLICY;
    ACTION_EXPR;
    ANNOTATED_DEFINITION;
    ANNOTATIONS;
    COMPONENT_EXPR;
    CREATOR_ATTRIBUTE;
    CUSTOM_OBLIGATION;
    DOT_EXPRESSION;
    EFFECT_CLAUSE;
    EFFECT_TYPE;
    EMAIL_TO;
    FLOATING_POINT;
    FROM;
    IDENTIFIER;
    IDNUMBER;
    INTEGER;
    OBLIGATION;
    OBLIGATION_CLAUSE;
    RESOURCE_EXPR;
    RESOURCE_TYPE;
    SUBJECT_EXPR;
    TARGET;
}

BACKSLASH   : '\\';
COLON       : ':';
COMMA       : ',';
DOT         : '.';
EQUAL       : '=';
RELATION_OP : '>' | '>''=' | '<' | '<''=';
NOT_EQUALS  : '!''=';
LPAREN      : '(';
RPAREN      : ')';
SEMICOLON   : ';';
STAR        : '*';
AND         : '&';
OR          : '|';
NOT         : '!';

// Unicode Byte Order Marker - filter out for now
BOM    : ('\uFEFF') { $setType(Token.SKIP); };

WS :
    (   ' '
    |   '\t'
    |   '\n'     { newline(); }
    |   '\r''\n' { newline(); }
    |   '\r'     { newline(); }
    )            { $setType(Token.SKIP); };

// Single-line comments
SL_COMMENT :
    "//"
    (~('\n'|'\r'))*
    ('\n'|'\r'('\n')?)? { $setType(Token.SKIP); newline(); };

// multiple-line comments
ML_COMMENT :
    "/*"
    ( options { greedy = false; /* also note that lexer's k is 2 */ }
    :   '\n'                   { newline(); }
    |   ('\r''\n') => '\r''\n' { newline(); }
    |   '\r'                   { newline(); }
    |  ~('\n'|'\r')
    ) *
    "*/"
    {    $setType( Token.SKIP ); };

protected ALPHA : ( 'a'..'z'|'A'..'Z');
protected NUMERIC : ('0'..'9');
protected NOT_WS : ~(' ' | '\t' | '\n' | '\r' );
protected UNDERSCORE : '_';
protected CHARACTER : (ALPHA | NUMERIC | UNDERSCORE);

protected IDENT_CHAR   : 'a'..'z' | 'A'..'Z' | '0'..'9' | '_' | '-' | '$' | '/';
protected IDENT_FIRST  : 'a'..'z' | 'A'..'Z' | '_';

IDENT_BASE : (IDENT_FIRST)(IDENT_CHAR)*;

UNSIGNED : (NUMERIC)+;

NUM_SIGN : ( PLUS | MINUS );

QUOTED_STRING : QUOTE (QUOTED_CHAR)* QUOTE { setText( StringUtils.unescape( getText() ) ); };

protected QUOTED_CHAR   : (~('\"' | '\n' | '\r' | '\\' ) | ESC_SEQUENCE);
protected ESC_SEQUENCE : (ESC_QUOTE | ESC_SLASH | ESC_CR | ESC_LF);
protected ESC_SLASH : SLASH SLASH;
protected ESC_QUOTE : SLASH QUOTE;
protected ESC_CR : SLASH 'n';
protected ESC_LF : SLASH 'r';

protected SLASH : '\\';
protected QUOTE : '\"';

protected PLUS : '+';
protected MINUS : '-';
