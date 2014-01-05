package com.axeda.innovation;

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import static org.junit.Assert.*
import groovy.util.slurpersupport.NodeChild
import groovy.xml.StreamingMarkupBuilder
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient

import org.junit.Test

/**
 * This test will verify that the HelloWorld service was properly installed
 * on your Axeda Platform Instance.
 *
 * Be sure to execute the 'mvn package -Prun-install' command from the root folder of the
 * HelloWorld project before executing this test with the 'mvn integration-test -P run-tests'
 * command.
 */
class HelloWorldIT {

  @Test
  void testHelloWorldRemote() {

    def serverInfo = [:]
    serverInfo.host = 'http://localhost:8080'
    serverInfo.username = 'admin'
    serverInfo.password = 'admin'

    def salutation = "No salutation received."

    def ourClient = new RESTClient(serverInfo.host)
    ourClient.auth.basic(serverInfo.username, serverInfo.password)
    def hello = ourClient.get(path: '/services/v1/rest/Scripto/execute/HelloWorld')
    def message = hello.getData()
    
    if (message?.salutation[0]) {
      salutation = message?.salutation[0].text()
    }
    else if (message?.faultstring[0]) {
      salutation = message?.faultstring[0].text()
    }
    
    assertEquals('Server did not say Hello...', 'Hello, Artisan World!', salutation)
    
    println '\nSUCCESS: Server is friendly!\n'
  }

}