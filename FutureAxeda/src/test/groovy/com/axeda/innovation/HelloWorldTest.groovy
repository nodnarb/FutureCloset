package com.axeda.innovation;

import static org.junit.Assert.*

import org.junit.Test;

/**
 * Unit Test For Hello World.
 *
 * Execute this test with the 'mvn test -P run-tests' command.
 */
class HelloWorldTest {
  
 @Test
 public void testHelloWorldLocal() {
   def expected = """
     <message>
       <salutation>Hello, Artisan World!</salutation>
     </message>
   """
   
  GroovyShell gsh = new GroovyShell()
  def hello = gsh.evaluate(new File("src/main/groovy/com/axeda/innovation/HelloWorld.groovy"))
  
  assertEquals(hello."Content-Type", "application/xml; charset=utf-8")
  assertEquals(hello."Content".stripIndent().trim(), expected.stripIndent().trim())
 }
  
}
