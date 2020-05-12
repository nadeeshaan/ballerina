import ballerina/io;

public type Foo record {|
   string bar;
|};

public function main(string... args) {
    testExpressions();
}

function testExpressions() {
    //uni-expressions
    var a = -5;

    //binary-expressions
    int b = 1 + 1;
    io:println(a);
    io:println(b);

    //list-construct-expr
    var myList = [111, 222];
    var myList2 = [333, "444"];
    io:println(myList);
    io:println(myList2);

    //mapping-construct-expr
    //var myMap = {key : "value"}; //Temporary disable keyword as key
    var myMap = {myKey : "value"};
    var myMap2 = {"key" : "value"};
    io:println(myMap);
    io:println(myMap2);

    // field-access-expr
    Foo foo = {bar: "templers rd"};
    io:println(foo.bar);
}
