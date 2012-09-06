/**
 * Copyright 2012 John Crygier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.crygier.spring.util

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource

import spock.lang.Specification


class SystemPropertyPlaceholderConfigurerSpec extends Specification {
	def "test only classpath resource"() {
		when:
		SystemPropertyPlaceholderConfigurer sp = new SystemPropertyPlaceholderConfigurer()
		sp.setLocations([new ClassPathResource("test.properties")] as Resource[])
		Properties props = sp.mergeProperties()
		
		then:
		props.get('property.one') == "This is a test"
		props.size() == 2
	}
	
	def "test only system property"() {
		when:
		System.getProperties().put("properties.location", "src/test/resources/system.properties")
		
		SystemPropertyPlaceholderConfigurer sp = new SystemPropertyPlaceholderConfigurer()
		sp.setSystemProperty("properties.location")
		Properties props = sp.mergeProperties()
		
		then:
		props.get('property.one') == "System Properties Test"
		props.size() == 2
	}
	
	def "test both system property and classpath"() {
		when:
		System.getProperties().put("properties.location", "src/test/resources/system.properties")
		
		SystemPropertyPlaceholderConfigurer sp = new SystemPropertyPlaceholderConfigurer()
		sp.setLocations([new ClassPathResource("test.properties")] as Resource[])
		sp.setSystemProperty("properties.location")
		Properties props = sp.mergeProperties()
		
		then:
		props.get('property.one') == "System Properties Test"
		props.get('test.only.property') == "This is another test"
		props.get('system.only.property') == "System Only Property Test"
		props.size() == 3
	}
	
	def "test both system property (Exists) and classpath (NonExists)"() {
		when:
		System.getProperties().put("properties.location", "src/test/resources/system.properties")
		
		SystemPropertyPlaceholderConfigurer sp = new SystemPropertyPlaceholderConfigurer()
		sp.setLocations([new ClassPathResource("test.properties.fake")] as Resource[])
		sp.setSystemProperty("properties.location")
		Properties props = sp.mergeProperties()
		
		then:
		thrown(FileNotFoundException)
	}
	
	def "test both system property (NonExists) and classpath (Exists)"() {
		when:
		System.getProperties().put("properties.location", "src/test/resources/system.properties.fake")
		
		SystemPropertyPlaceholderConfigurer sp = new SystemPropertyPlaceholderConfigurer()
		sp.setLocations([new ClassPathResource("test.properties")] as Resource[])
		sp.setSystemProperty("properties.location")
		Properties props = sp.mergeProperties()
		
		then:
		thrown(FileNotFoundException)
	}
}
